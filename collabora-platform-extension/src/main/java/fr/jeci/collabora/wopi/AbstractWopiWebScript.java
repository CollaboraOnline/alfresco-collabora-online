// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import fr.jeci.collabora.alfresco.CollaboraOnlineService;
import fr.jeci.collabora.alfresco.LogSanitizer;
import fr.jeci.collabora.alfresco.NodeRefValidator;
import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;
import net.sf.acegisecurity.Authentication;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.rendition2.RenditionService2;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.version.VersionBaseModel;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class AbstractWopiWebScript extends AbstractWebScript implements WopiHeader {
	private static final Logger logger = LoggerFactory.getLogger(AbstractWopiWebScript.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	static final String ACCESS_TOKEN = "access_token";
	static final String FILE_ID = "file_id";
	static final String LAST_MODIFIED_TIME = "LastModifiedTime";

	private String[] renditions;

	static final int STATUS_CONFLICT = 409;
	protected NodeService nodeService;
	protected CollaboraOnlineService collaboraOnlineService;
	protected ContentService contentService;
	protected VersionService versionService;
	protected RetryingTransactionHelper retryingTransactionHelper;
	protected NamespacePrefixResolver prefixResolver;
	protected DictionaryService dictionaryService;
	protected RenditionService2 renditionService;
	protected BehaviourFilter behaviourFilter;

	public abstract void executeAsUser(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef)
			throws IOException;

	@Override
	public void execute(final WebScriptRequest req, final WebScriptResponse res) throws IOException {
		final WOPIAccessTokenInfo wopiToken = wopiToken(req);
		forceCurrentUser(wopiToken);
		final NodeRef nodeRef = getFileNodeRef(wopiToken.getFileId());
		validateNodeExists(nodeRef);

		if (logger.isDebugEnabled()) {
			String currentLockId = this.collaboraOnlineService.lockGet(nodeRef);
			logger.debug("{} user='{}' nodeRef='{}' lockId={}", req.getPathInfo(), wopiToken.getUserName(), nodeRef,
					currentLockId);
		}
		try {
			this.executeAsUser(req, res, nodeRef);
		} catch (Throwable e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Caught exception; decorating with appropriate status template", e);
			}

			throw createStatusException(e, req, res);
		}
	}

	/**
	 * Returns a validated NodeRef given a file Id.
	 * Validates UUID format and checks that the node exists.
	 *
	 * @param fileId Node UUID
	 * @return file nodeRef
	 * @throws WebScriptException if fileId is invalid or node does not exist
	 */
	protected NodeRef getFileNodeRef(String fileId) {
		NodeRefValidator.validateUUID(fileId);
		return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, fileId);
	}

	/**
	 * Check and renew token if needed
	 *
	 * @param req Request
	 * @return WOPI Token
	 */
	protected WOPIAccessTokenInfo wopiToken(WebScriptRequest req) {
		final String fileId = req.getServiceMatch()
				.getTemplateVars()
				.get(FILE_ID);
		final NodeRef nodeRef = getFileNodeRef(fileId);
		final String accessToken = req.getParameter(ACCESS_TOKEN);

		if (fileId == null) {
			throw new WebScriptException("No 'file_id' parameter supplied");
		}

		WOPIAccessTokenInfo wopiToken = this.collaboraOnlineService.checkAccessToken(accessToken, nodeRef);

		if (!wopiToken.isValid()) {
			// try to renew
			AuthenticationUtil.pushAuthentication();
			try {
				AuthenticationUtil.setRunAsUser(wopiToken.getUserName());
				return this.collaboraOnlineService.createAccessToken(nodeRef);
			} finally {
				AuthenticationUtil.popAuthentication();
			}
		}
		return wopiToken;
	}

	/**
	 * Validates that a node exists in the repository.
	 *
	 * @param nodeRef the NodeRef to check
	 * @throws WebScriptException with NOT_FOUND status if node does not exist
	 */
	protected void validateNodeExists(NodeRef nodeRef) {
		if (nodeRef == null) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "NodeRef is required");
		}
		if (!this.nodeService.exists(nodeRef)) {
			throw new WebScriptException(Status.STATUS_NOT_FOUND, "Node not found: " + nodeRef.getId());
		}
	}

	protected void forceCurrentUser(final WOPIAccessTokenInfo wopiToken) {
		Authentication originalFullAuthentication = AuthenticationUtil.getFullAuthentication();
		if (originalFullAuthentication == null) {
			logger.debug("CurrentAuthentication is null - setting CurrentUser to {}", wopiToken.getUserName());
			AuthenticationUtil.setFullyAuthenticatedUser(wopiToken.getUserName());
		} else {
			logger.info("Authenticate with user is {}", originalFullAuthentication.getPrincipal());
		}
	}

	protected void jsonResponse(final WebScriptResponse res, int code, Map<String, String> response) throws IOException {
		try {
			String json = objectMapper.writeValueAsString(response);
			jsonResponse(res, code, json);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize response to JSON", e);
			throw new IOException("Failed to serialize response to JSON", e);
		}
	}

	protected void jsonResponse(final WebScriptResponse res, int code, String response) throws IOException {
		res.reset();
		res.setStatus(code);
		res.setContentType("application/json;charset=UTF-8");
		res.getWriter()
				.append(response);
	}

	/**
	 * Write content file to disk on set version properties.
	 *
	 * @param inputStream input stream data
	 * @param isAutosave  id true, set PROP_DESCRIPTION, "Edit with Collabora"
	 * @param nodeRef     node to update
	 * @return The new version create
	 */
	protected Version writeFileToDisk(final InputStream inputStream, final boolean isAutosave, final NodeRef nodeRef) {
		return retryingTransactionHelper.doInTransaction(() -> {

			// Inhibit auto-version, we will create Version manually
			this.behaviourFilter.disableBehaviour(ContentModel.ASPECT_VERSIONABLE);
			try {
				ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
				// both streams are closed by putContent
				writer.putContent(new BufferedInputStream(inputStream));
			} catch (Exception e) {
				logger.warn("Exception when writing content \"{}\": \"{}\" - will retry", nodeRef, e.getMessage());
				throw new AlfrescoRuntimeException("Error when writing content - retry", e);
			} finally {
				this.behaviourFilter.enableBehaviour(ContentModel.ASPECT_VERSIONABLE);
			}

			try {
				Map<String, Serializable> versionProperties = new HashMap<>(2);
				versionProperties.put(VersionBaseModel.PROP_VERSION_TYPE, VersionType.MINOR);
				if (isAutosave) {
					versionProperties.put(VersionBaseModel.PROP_DESCRIPTION, CollaboraOnlineService.AUTOSAVE_DESCRIPTION);
				}
				versionProperties.put(CollaboraOnlineService.LOOL_AUTOSAVE, isAutosave);
				return versionService.createVersion(nodeRef, versionProperties);
			} catch (Exception e) {
				logger.warn("Exception when creating version \"{}\": \"{}\" - will retry", nodeRef, e.getMessage());
				throw new AlfrescoRuntimeException("Error when creating version - retry", e);
			}

		}, false, true);
	}

	protected void askForRendition(final NodeRef nodeRef) {
		for (String name : renditions) {
			try {
				this.renditionService.render(nodeRef, name);
			} catch (UnsupportedOperationException | java.lang.IllegalArgumentException exp) {
				logger.warn("Rendition '{}' not supported for {}", name, nodeRef);
			}
		}
	}

	/**
	 * Do actions on node. This modifications will not trigger policy to prevent cascading effect.
	 *
	 * @param req
	 * @param nodeRef
	 */
	protected void headerActions(final WebScriptRequest req, final NodeRef nodeRef) {
		final QName aspectToAdd = extractQname(req, X_PRISTY_ADD_ASPECT);
		final QName aspectToDel = extractQname(req, X_PRISTY_DEL_ASPECT);
		final Map<QName, Serializable> delProperties = extractQnamesValues(req, X_PRISTY_DEL_PROPERTY);
		final Map<QName, Serializable> properties = extractQnamesValues(req, X_PRISTY_ADD_PROPERTY);

		/* As we only receive String, we must convert value to proper datatype */
		for (Entry<QName, Serializable> prop : properties.entrySet()) {
			DataTypeDefinition dataType = dictionaryService.getProperty(prop.getKey())
					.getDataType();
			prop.setValue((Serializable) DefaultTypeConverter.INSTANCE.convert(dataType, prop.getValue()));
		}

		retryingTransactionHelper.doInTransaction((RetryingTransactionHelper.RetryingTransactionCallback<Void>) () -> {
			if (aspectToDel != null && nodeService.hasAspect(nodeRef, aspectToDel)) {
				nodeService.removeAspect(nodeRef, aspectToDel);
			}
			if (aspectToAdd != null) {
				nodeService.addAspect(nodeRef, aspectToAdd, properties);
			}
			for (QName prop : delProperties.keySet()) {
				nodeService.removeProperty(nodeRef, prop);
			}

			nodeService.addProperties(nodeRef, properties);
			return null;
		}, false, true);

	}

	private QName extractQname(WebScriptRequest req, String headerName) {
		final String aspectToAddHdr = req.getHeader(headerName);

		if (StringUtils.isNotBlank(aspectToAddHdr)) {
			logger.debug("{}={}", headerName, LogSanitizer.sanitize(aspectToAddHdr));
			return QName.resolveToQName(prefixResolver, aspectToAddHdr);
		}
		return null;
	}

	private Map<QName, Serializable> extractQnamesValues(WebScriptRequest req, String headerName) {
		final String[] headers = req.getHeaderValues(headerName);
		if (headers == null) {
			return Collections.emptyMap();
		}

		if (logger.isDebugEnabled()) {
			String[] sanitized = new String[headers.length];
			for (int i = 0; i < headers.length; i++) {
				sanitized[i] = LogSanitizer.sanitize(headers[i]);
			}
			logger.debug("{}={}", headerName, ArrayUtils.toString(sanitized));
		}

		Map<QName, Serializable> result = new HashMap<>(headers.length);
		for (String prop : headers) {
			if (StringUtils.isBlank(prop)) {
				continue;
			}

			int eqIndex = prop.indexOf('=');
			if (eqIndex <= 0) {
				logger.warn("Invalid property format (missing or empty key): {}", LogSanitizer.sanitize(prop));
				continue;
			}

			String key = prop.substring(0, eqIndex);
			String value = eqIndex < prop.length() - 1 ? prop.substring(eqIndex + 1) : null;

			try {
				QName qname = QName.resolveToQName(prefixResolver, key);
				if (qname != null) {
					result.put(qname, value);
				} else {
					logger.warn("Could not resolve QName: {}", LogSanitizer.sanitize(key));
				}
			} catch (Exception e) {
				logger.warn("Invalid QName format: {}", LogSanitizer.sanitize(key));
			}
		}
		return result;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setCollaboraOnlineService(CollaboraOnlineService service) {
		this.collaboraOnlineService = service;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}

	public void setRetryingTransactionHelper(RetryingTransactionHelper retryingTransactionHelper) {
		this.retryingTransactionHelper = retryingTransactionHelper;
	}

	public void setPrefixResolver(NamespacePrefixResolver prefixResolver) {
		this.prefixResolver = prefixResolver;
	}

	public void setDictionaryService(DictionaryService dictionaryService) {
		this.dictionaryService = dictionaryService;
	}

	public void setRenditionService(RenditionService2 renditionService) {
		this.renditionService = renditionService;
	}

	public void setRenditions(String renditions) {
		if (StringUtils.isNotBlank(renditions)) {
			this.renditions = renditions.split(",");
		} else {
			this.renditions = new String[] {};
		}
	}

	public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
		this.behaviourFilter = behaviourFilter;
	}
}
