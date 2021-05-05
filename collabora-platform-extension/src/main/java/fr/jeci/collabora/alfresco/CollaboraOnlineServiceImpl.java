package fr.jeci.collabora.alfresco;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

import dk.magenta.libreoffice.online.service.WOPIAccessTokenInfo;

public class CollaboraOnlineServiceImpl implements CollaboraOnlineService {

	private static final String FALSE = "false";

	private static final Log logger = LogFactory.getLog(CollaboraOnlineServiceImpl.class);

	private static final long ONE_HOUR_MS = 1000 * 60 * 60;

	private static final long DEFAULT_TOKEN_TTL_MS = ONE_HOUR_MS * 24;
	private long tokenTtlMs = -1;

	private URL collaboraPublicUrl;
	private URL collaboraPrivateUrl;
	private URL alfrescoPublicURL;
	private URL alfrescoPrivateURL;

	private WopiDiscovery wopiDiscovery;
	private NodeService nodeService;
	private PermissionService permissionService;

	private SecureRandom random = new SecureRandom();

	public void init() {
		if (collaboraPublicUrl == null) {
			throw new AlfrescoRuntimeException("Invalid Configuration, need collaboraPublicUrl (collabora.public.url)");
		}
	}

	/**
	 * This holds a map of the the "token info(s)" mapped to a file. Each token info
	 * is mapped to a user, so in essence a user may only have one token info per
	 * file. <FileId, <userName, tokenInfo> >
	 * <p>
	 * { fileId: { <== The id of the nodeRef that refers to the file userName:
	 * WOPIAccessTokenInfo } }
	 *
	 *
	 * fileIdAccessTokenMap is an Hazelcast IMap see:
	 * https://docs.hazelcast.org/docs/2.4/javadoc/com/hazelcast/core/IMap.html The
	 * get(Object key) method returns a clone of original value, modifying the
	 * returned value does not change the actual value in the map. One should put
	 * modified value back to make changes visible to all nodes.
	 */
	private SimpleCache<String, WOPIAccessTokenInfo> tokenMap;

	public void setTokenMap(SimpleCache<String, WOPIAccessTokenInfo> tokenMap) {
		this.tokenMap = tokenMap;
	}

	/**
	 * Generate and store an access token only valid for the current user/file id
	 * combination.
	 *
	 * We check if we have at least READ permission, so a user must be connected.
	 * 
	 * @param nodeRef
	 * @return
	 */
	@Override
	public WOPIAccessTokenInfo createAccessToken(NodeRef nodeRef) {
		if (AuthenticationUtil.isRunAsUserTheSystemUser()) {
			logger.warn("Create token for System user, it is not desirable");
		} else {
			AccessStatus perm = this.permissionService.hasPermission(nodeRef, PermissionService.READ);
			if (AccessStatus.ALLOWED != perm) {
				throw new WebScriptException(Status.STATUS_UNAUTHORIZED, "Not allow to READ " + nodeRef);
			}
		}

		final String userName = AuthenticationUtil.getRunAsUser();
		final String fileId = nodeRef.getId();
		final Date now = new Date();
		WOPIAccessTokenInfo tokenInfo = new WOPIAccessTokenInfo(generateAccessToken(), now, newExpiresAt(now), fileId,
				userName);
		this.tokenMap.put(tokenInfo.getAccessToken(), tokenInfo);

		if (logger.isDebugEnabled()) {
			logger.debug("Created Access Token for user '" + userName + "' and nodeRef '" + nodeRef + "'");
		}
		return tokenInfo;
	}

	/**
	 * Compute token time to live
	 * 
	 * @return Now + tokenTtlMs
	 */
	private Date newExpiresAt(final Date now) {
		if (this.tokenTtlMs < 1) {
			this.tokenTtlMs = DEFAULT_TOKEN_TTL_MS;
		} else if (this.tokenTtlMs < ONE_HOUR_MS) {
			logger.warn("Token TTL is short : " + this.tokenTtlMs + " ms");
		}
		return new Date(now.getTime() + tokenTtlMs);
	}

	/**
	 * Generates a random access token.
	 * 
	 * @return
	 */
	private String generateAccessToken() {
		return new BigInteger(130, random).toString(32);
	}

	/**
	 * Check the access token given in the request and return the nodeRef
	 * corresponding to the file id passed to the request.
	 *
	 * @param req
	 * @throws WebScriptException
	 * @return
	 */
	@Override
	public WOPIAccessTokenInfo checkAccessToken(final String accessToken, final NodeRef nodeRef) {
		WOPIAccessTokenInfo tokenInfo = this.tokenMap.get(accessToken);

		if (tokenInfo == null) {
			throw new WebScriptException(Status.STATUS_UNAUTHORIZED, "No token access found for " + accessToken);
		}

		if (!tokenInfo.getFileId().equals(nodeRef.getId())) {
			throw new WebScriptException(Status.STATUS_UNAUTHORIZED,
					"Tokens stored for " + accessToken + ", not match the given file" + nodeRef);
		}

		return tokenInfo;
	}

	private HashMap<String, String> serverInfo = null;

	@Override
	public Map<String, String> serverInfo() {
		if (serverInfo == null) {
			this.serverInfo = new HashMap<>(7);

			// We need to enable this if we want to be able to insert image into the
			// documents
			this.serverInfo.put(DISABLE_COPY, FALSE);
			this.serverInfo.put(DISABLE_PRINT, FALSE);
			this.serverInfo.put(DISABLE_EXPORT, FALSE);
			this.serverInfo.put(HIDE_EXPORT_OPTION, FALSE);
			this.serverInfo.put(HIDE_SAVE_OPTION, FALSE);
			this.serverInfo.put(HIDE_PRINT_OPTION, FALSE);
			this.serverInfo.put(POST_MESSAGE_ORIGIN, this.alfrescoPublicURL.toString());
			
			// Host from which token generation request originated
			// Search https://www.collaboraoffice.com/category/community-en/ for
			// EnableOwnerTermination
			this.serverInfo.put(ENABLE_OWNER_TERMINATION, FALSE);
		}

		Map<String, String> infos = new HashMap<>(this.serverInfo.size());
		infos.putAll(this.serverInfo);
		return infos;
	}

	/**
	 * Returns the WOPI src URL for a given nodeRef and action.
	 *
	 * @param nodeRef
	 * @param action
	 * @return
	 */
	@Override
	public String getWopiSrcURL(NodeRef nodeRef, String action) throws IOException {
		final ContentData contentData = (ContentData) nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
		return wopiDiscovery.getSrcURL(contentData.getMimetype(), action);
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	/**
	 * Public fqdn use by the browser to load leaflet
	 * 
	 * collabora.public.url=https://___/
	 * 
	 * @param collaboraPublicUrl
	 */
	public void setCollaboraPublicUrl(URL collaboraPublicUrl) {
		this.collaboraPublicUrl = collaboraPublicUrl;
	}

	/**
	 * If Collabora Online is on the same host or network, define the internal url
	 * of the server.
	 * 
	 * collabora.private.url=http://localhost:9980/
	 * 
	 * @param collaboraPrivateUrl
	 */
	public void setCollaboraPrivateUrl(URL collaboraPrivateUrl) {
		this.collaboraPrivateUrl = collaboraPrivateUrl;
	}

	public void setTokenTtlMs(long tokenTtlMs) {
		this.tokenTtlMs = tokenTtlMs;
	}

	public void setAlfrescoPrivateURL(URL alfrescoPrivateURL) {
		this.alfrescoPrivateURL = alfrescoPrivateURL;
	}

	@Override
	public URL getAlfrescoPrivateURL() {
		return alfrescoPrivateURL;
	}

	public void setAlfrescoPublicURL(URL alfrescoPublicURL) {
		this.alfrescoPublicURL = alfrescoPublicURL;
	}

	public void setWopiDiscovery(WopiDiscovery wopiDiscovery) {
		this.wopiDiscovery = wopiDiscovery;
	}

}
