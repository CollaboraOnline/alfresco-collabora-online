/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package fr.jeci.collabora.alfresco;

import fr.jeci.collabora.alfresco.WopiDiscovery.DiscoveryAction;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.lock.mem.Lifetime;
import org.alfresco.repo.lock.mem.LockState;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.LocalDateTime;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollaboraOnlineServiceImpl implements CollaboraOnlineService {

	private static final String CANT_LOCK = "Can't lock node %s, ";
	private static final String CANT_UNLOCK = "Can't unlock node %s, ";
	private static final String CANT_REFRESH = "Can't refresh node %s, ";
	private static final String LOCK_ID_IS_BLANK = "lockId is blank";
	private static final String NODE_NOT_LOCK = "node is not lock";
	private static final String EMPTY_STRING = "";
	private static final String FALSE = "false";
	private static final String TRUE = "true";

	private static final Log logger = LogFactory.getLog(CollaboraOnlineServiceImpl.class);

	private static final int ONE_HOUR_MS = 1000 * 60 * 60;

	private static final int DEFAULT_TOKEN_TTL_MS = ONE_HOUR_MS * 24;
	private int tokenTtlMs = -1;

	private URL collaboraPublicUrl;
	private URL alfrescoPublicURL;
	private URL alfrescoPrivateURL;

	private WopiDiscovery wopiDiscovery;
	private NodeService nodeService;
	private PermissionService permissionService;
	private LockService lockService;

	private final SecureRandom random = new SecureRandom();

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
		LocalDateTime now = LocalDateTime.now();
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
	private LocalDateTime newExpiresAt(final LocalDateTime now) {
		if (this.tokenTtlMs < 1) {
			this.tokenTtlMs = DEFAULT_TOKEN_TTL_MS;
		} else if (this.tokenTtlMs < ONE_HOUR_MS) {
			logger.warn("Token TTL is short : " + this.tokenTtlMs + " ms");
		}
		return now.plusMillis(this.tokenTtlMs);
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
		if (accessToken == null) {
			throw new WebScriptException(Status.STATUS_UNAUTHORIZED, "AccessToken is null");
		}

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
			this.serverInfo.put(USER_CAN_NOT_WRITE_RELATIVE, FALSE);
			this.serverInfo.put(POST_MESSAGE_ORIGIN, this.alfrescoPublicURL.toString());
			this.serverInfo.put(SUPPORTS_LOCKS, TRUE);
			
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
	 * https://wopi.readthedocs.io/en/latest/discovery.html#wopi-actions
	 *
	 * @param nodeRef
	 * @param action  "view", "edit", etc.
	 */
	@Override
	public String getWopiSrcURL(NodeRef nodeRef, String action) throws IOException {
		if (!this.wopiDiscovery.hasCollaboraOnline()) {
			throw new WebScriptException(Status.STATUS_BAD_GATEWAY, "Collabora is Offline");
		}

		final String filename = (String) this.nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
		if (filename == null) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "This node as no name: " + nodeRef);
		}

		int lastDot = filename.lastIndexOf('.');
		if (lastDot < 0) {
			logger.warn("This node has no extension: " + nodeRef + " fileName=" + filename + " use mimeType (legacy)");
			final ContentData contentData = (ContentData) nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
			return this.wopiDiscovery.getSrcURL(contentData.getMimetype(), action);
		}
		List<DiscoveryAction> actions = this.wopiDiscovery.getAction(filename.substring(lastDot + 1));

		if (actions == null || actions.isEmpty()) {
			throw new WebScriptException(Status.STATUS_NOT_IMPLEMENTED,
					"No action for node=" + nodeRef + " fileName=" + filename);
		}

		DiscoveryAction found = null;
		for (DiscoveryAction act : actions) {
			if (act.getName().equals(action)) {
				found = act;
				break;
			}
		}

		if (found == null) {
			logger.warn("Action name not found for action=" + action + " fileName=" + filename);
			found = actions.get(0);
		}

		return found.getUrlsrc();
	}

	@Override
	public String lock(NodeRef nodeRef, String lockId) throws ConflictException {
		if (logger.isDebugEnabled()) {
			logger.debug("LOCK '" + nodeRef + "'");
		}

		if (StringUtils.isBlank(lockId)) {
			String lockFailureReason = String.format(CANT_LOCK + LOCK_ID_IS_BLANK, nodeRef);
			throw new ConflictException(EMPTY_STRING, lockFailureReason);
		}

		this.lockService.lock(nodeRef, LockType.WRITE_LOCK, 30 * 60, Lifetime.EPHEMERAL, lockId);

		return lockId;
	}

	@Override
	public String lockGet(NodeRef nodeRef) {
		if (logger.isDebugEnabled()) {
			logger.debug("GET LOCK '" + nodeRef + "'");
		}

		if (isNodeLock(nodeRef)) {
			LockState lockState = this.lockService.getLockState(nodeRef);
			return lockState.getAdditionalInfo();
		} else {
			return EMPTY_STRING;
		}
	}

	@Override
	public void lockRefresh(NodeRef nodeRef, String lockId) throws ConflictException {
		if (logger.isDebugEnabled()) {
			logger.debug("REFRESH LOCK '" + nodeRef + "'");
		}

		if (StringUtils.isBlank(lockId)) {
			String lockFailureReason = String.format(CANT_REFRESH + LOCK_ID_IS_BLANK, nodeRef);
			throw new ConflictException(EMPTY_STRING, lockFailureReason);
		}

		if (isNodeLock(nodeRef)) {
			this.lockService.lock(nodeRef, LockType.NODE_LOCK, 30 * 60, Lifetime.EPHEMERAL, lockId);
		} else {
			String lockFailureReason = String.format(CANT_REFRESH + NODE_NOT_LOCK, nodeRef);
			throw new ConflictException(EMPTY_STRING, lockFailureReason);
		}
	}

	@Override
	public String lockUnlock(NodeRef nodeRef, String lockId) throws ConflictException {
		if (logger.isDebugEnabled()) {
			logger.debug("UNLOCK '" + nodeRef + "'");
		}

		if (StringUtils.isBlank(lockId)) {
			String lockFailureReason = String.format(CANT_UNLOCK + LOCK_ID_IS_BLANK, nodeRef);
			throw new ConflictException(EMPTY_STRING, lockFailureReason);
		}

		if (isNodeLock(nodeRef)) {
			this.lockService.unlock(nodeRef);
		} else {
			String lockFailureReason = String.format(CANT_UNLOCK + NODE_NOT_LOCK, nodeRef);
			throw new ConflictException(EMPTY_STRING, lockFailureReason);
		}

		return lockId;

	}

	@Override
	public void lockSteal(NodeRef nodeRef, String lockId) throws ConflictException {
		if (logger.isDebugEnabled()) {
			logger.debug("STEAL LOCK '" + nodeRef + "'");
		}

		String cLockId = this.lockGet(nodeRef);
		if (StringUtils.isBlank(cLockId)) {
			if (logger.isDebugEnabled()) {
				logger.debug("No lock-id on " + nodeRef + ". No steal");
			}
			return;
		}

		if (!cLockId.equals(lockId)) {
			throw new ConflictException(cLockId, "Lock-id on " + nodeRef + " is not " + lockId);
		}

		final LockState lockState = this.lockService.getLockState(nodeRef);

		String runAsUser = AuthenticationUtil.getRunAsUser();
		if (!runAsUser.equals(lockState.getOwner())) {
			AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Void>() {
				@Override
				public Void doWork() throws Exception {
					lockService.unlock(nodeRef);
					return null;
				}
			}, lockState.getOwner());
		}

		this.lockService.lock(nodeRef, LockType.WRITE_LOCK, 30 * 60, Lifetime.EPHEMERAL, lockId);
	}

	@Override
	public void unlock(NodeRef nodeRef, boolean force) {
		if (logger.isDebugEnabled()) {
			logger.debug("UNLOCK '" + nodeRef + "'");
		}

		this.lockService.unlock(nodeRef);
	}

	private boolean isNodeLock(NodeRef nodeRef) {
		if (this.nodeService.hasAspect(nodeRef, ContentModel.ASPECT_LOCKABLE)) {
			LockType lockType = this.lockService.getLockType(nodeRef);
			if (logger.isDebugEnabled()) {
				logger.debug("Node is lock type=" + lockType);
			}
			return LockType.WRITE_LOCK.equals(lockType);
		}

		return false;
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

	public void setTokenTtlMs(int tokenTtlMs) {
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

	public void setLockService(LockService lockService) {
		this.lockService = lockService;
	}
}
