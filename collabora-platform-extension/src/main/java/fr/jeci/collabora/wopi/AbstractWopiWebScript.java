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
package fr.jeci.collabora.wopi;

import fr.jeci.collabora.alfresco.CollaboraOnlineService;
import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;
import net.sf.acegisecurity.Authentication;
import org.alfresco.model.ContentModel;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.*;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class AbstractWopiWebScript extends AbstractWebScript implements WopiHeader {
	private static final Log logger = LogFactory.getLog(AbstractWopiWebScript.class);

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

	public abstract void executeAsUser(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef)
			throws IOException;

	@Override
	public void execute(final WebScriptRequest req, final WebScriptResponse res) throws IOException {
		final WOPIAccessTokenInfo wopiToken = wopiToken(req);
		forceCurrentUser(wopiToken);
		final NodeRef nodeRef = getFileNodeRef(wopiToken.getFileId());

		if (logger.isDebugEnabled()) {
			String currentLockId = this.collaboraOnlineService.lockGet(nodeRef);
			logger.debug(req.getPathInfo() + " user='" + wopiToken.getUserName() + "' nodeRef='" + nodeRef + "' lockId="
					+ currentLockId);
		}

		if (nodeRef == null) {
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
					"No noderef for WOPIAccessTokenInfo:" + wopiToken);
		}
		this.executeAsUser(req, res, nodeRef);
	}


	/**
	 * Returns a NodeRef given a file Id. Note: Checks to see if the node exists aren't performed
	 *
	 * @param fileId Node UUID
	 * @return file nodeRef
	 */
	protected NodeRef getFileNodeRef(String fileId) {
		return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, fileId);
	}



	/**
	 * Check and renew token if needed
	 *
	 * @param req Request
	 * @return WOPI Token
	 */
	protected WOPIAccessTokenInfo wopiToken(WebScriptRequest req) {
		final String fileId = req.getServiceMatch().getTemplateVars().get(FILE_ID);
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

	protected void forceCurrentUser(final WOPIAccessTokenInfo wopiToken) {
		Authentication originalFullAuthentication = AuthenticationUtil.getFullAuthentication();
		if (originalFullAuthentication == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("CurrentAuthentication is null - setting CurrentUser to " + wopiToken.getUserName());
			}
			AuthenticationUtil.setFullyAuthenticatedUser(wopiToken.getUserName());
		} else {
			logger.info("Authenticate with user is " + originalFullAuthentication.getPrincipal());
		}
	}

	protected void jsonResponse(final WebScriptResponse res, int code, Map<String, String> response) throws IOException {
		boolean start = true;
		StringBuilder sb = new StringBuilder("{");
		for (Entry<String, String> e : response.entrySet()) {
			if (start) {
				start = false;
			} else {
				sb.append(", ");
			}

			sb.append('"').append(e.getKey()).append('"');
			sb.append(": \"").append(e.getValue()).append('"');
		}
		sb.append('}');
		jsonResponse(res, code, sb.toString());
	}

	protected void jsonResponse(final WebScriptResponse res, int code, String response) throws IOException {
		res.reset();
		res.setStatus(code);
		res.setContentType("application/json;charset=UTF-8");
		res.getWriter().append(response);
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
		RetryingTransactionHelper.RetryingTransactionCallback<Version> callback = new RetryingTransactionHelper.RetryingTransactionCallback<>() {
			@Override
			public Version execute() {
				ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);

				// both streams are closed by putContent
				writer.putContent(new BufferedInputStream(inputStream));

				Map<String, Serializable> versionProperties = new HashMap<>(2);
				versionProperties.put(VersionBaseModel.PROP_VERSION_TYPE, VersionType.MINOR);
				if (isAutosave) {
					versionProperties.put(VersionBaseModel.PROP_DESCRIPTION, CollaboraOnlineService.AUTOSAVE_DESCRIPTION);
				}
				versionProperties.put(CollaboraOnlineService.LOOL_AUTOSAVE, isAutosave);
				return versionService.createVersion(nodeRef, versionProperties);
			}
		};

		return retryingTransactionHelper.doInTransaction(callback, false, true);
	}

	protected void askForRendition(final NodeRef nodeRef) {

		for (String name : renditions) {
			try {
				this.renditionService.render(nodeRef, name);
			} catch (UnsupportedOperationException | java.lang.IllegalArgumentException exp) {
				logger.warn("Rendition '" + name + "' not supported for " + nodeRef);
			}
		}

	}

	protected void headerActions(final WebScriptRequest req, final NodeRef nodeRef) {
		final QName aspectToAdd = extractQname(req, X_PRISTY_ADD_ASPECT);
		final QName aspectToDel = extractQname(req, X_PRISTY_DEL_ASPECT);
		final Map<QName, Serializable> delProperties = extractQnamesValues(req, X_PRISTY_DEL_PROPERTY);
		final Map<QName, Serializable> properties = extractQnamesValues(req, X_PRISTY_ADD_PROPERTY);

		retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
			@Override
			public Void execute() {

				if (aspectToDel != null && nodeService.hasAspect(nodeRef, aspectToDel)) {
					nodeService.removeAspect(nodeRef, aspectToDel);
				}
				if (aspectToDel != null) {
					nodeService.addAspect(nodeRef, aspectToAdd, properties);
				}
				for (QName prop : delProperties.keySet()) {
					nodeService.removeProperty(nodeRef, prop);
				}

				/* As we only receive String, we must convert value to proper datatype */
				for (Entry<QName, Serializable> prop : properties.entrySet()) {
					DataTypeDefinition dataType = dictionaryService.getProperty(prop.getKey()).getDataType();
					prop.setValue((Serializable) DefaultTypeConverter.INSTANCE.convert(dataType, prop.getValue()));
				}

				nodeService.addProperties(nodeRef, properties);

				return null;
			}
		}, false, true);

	}

	private QName extractQname(WebScriptRequest req, String headerName) {
		final String aspectToAddHdr = req.getHeader(headerName);

		QName aspectToAdd = null;
		if (StringUtils.isNotBlank(aspectToAddHdr)) {
			if (logger.isDebugEnabled()) {
				logger.debug(headerName + "=" + aspectToAddHdr);
			}

			aspectToAdd = QName.resolveToQName(prefixResolver, aspectToAddHdr);
		}
		return aspectToAdd;
	}

	private Map<QName, Serializable> extractQnamesValues(WebScriptRequest req, String headerName) {
		final String[] aspectToAddHdr = req.getHeaderValues(headerName);
		if (aspectToAddHdr == null) {
			return Collections.emptyMap();
		}

		if (logger.isDebugEnabled()) {
			logger.debug(headerName + "=" + ArrayUtils.toString(aspectToAddHdr));
		}

		Map<QName, Serializable> aspectToAdd = new HashMap<>(aspectToAddHdr.length);
		for (String prop : aspectToAddHdr) {
			if (StringUtils.isNotBlank(prop)) {
				String[] split = prop.split("=");
				aspectToAdd.put(QName.resolveToQName(prefixResolver, split[0]), split.length > 1 ? split[1] : null);
			}
		}
		return aspectToAdd;
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
}
