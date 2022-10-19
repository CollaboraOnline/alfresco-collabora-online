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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import fr.jeci.collabora.alfresco.CollaboraOnlineService;
import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;

public abstract class AbstractWopiWebScript extends AbstractWebScript implements WopiHeader {
	private static final Log logger = LogFactory.getLog(AbstractWopiWebScript.class);

	static final String ACCESS_TOKEN = "access_token";
	static final String FILE_ID = "file_id";
	static final String LAST_MODIFIED_TIME = "LastModifiedTime";

	static final int STATUS_CONFLICT = 409;
	protected NodeService nodeService;
	protected CollaboraOnlineService collaboraOnlineService;
	protected ContentService contentService;
	protected VersionService versionService;
	protected RetryingTransactionHelper retryingTransactionHelper;
	protected NamespacePrefixResolver prefixResolver;
	protected DictionaryService dictionaryService;

	/**
	 * Returns a NodeRef given a file Id. Note: Checks to see if the node exists aren't performed
	 * 
	 * @param fileId
	 * @return
	 */
	protected NodeRef getFileNodeRef(String fileId) {
		return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, fileId);
	}

	/**
	 * Will return a file nodeRef only il file exist and is accessible
	 *
	 * @param tokenInfo
	 * @return
	 */
	protected NodeRef getFileNodeRef(final WOPIAccessTokenInfo wopiToken) {

		AuthenticationUtil.pushAuthentication();
		try {
			AuthenticationUtil.setRunAsUser(wopiToken.getUserName());

			final NodeRef fileNodeRef = getFileNodeRef(wopiToken.getFileId());
			if (nodeService.exists(fileNodeRef)) {
				return fileNodeRef;
			}
			return null;

		} finally {
			AuthenticationUtil.popAuthentication();
		}
	}

	protected Map<QName, Serializable> runAsGetProperties(final WOPIAccessTokenInfo wopiToken, final NodeRef nodeRef) {
		AuthenticationUtil.pushAuthentication();
		try {
			AuthenticationUtil.setRunAsUser(wopiToken.getUserName());

			return nodeService.getProperties(nodeRef);
		} finally {
			AuthenticationUtil.popAuthentication();
		}
	}

	protected Version runAsGetCurrentVersion(final WOPIAccessTokenInfo wopiToken, final NodeRef nodeRef) {
		AuthenticationUtil.pushAuthentication();
		try {
			AuthenticationUtil.setRunAsUser(wopiToken.getUserName());

			return versionService.getCurrentVersion(nodeRef);
		} finally {
			AuthenticationUtil.popAuthentication();
		}
	}

	/**
	 * Check and renew token if needed
	 * 
	 * @param req
	 * @return
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
	 * @param InputStream input stream data
	 * @param isAutosave  id true, set PROP_DESCRIPTION, "Edit with Collabora"
	 * @param wopiToken
	 * @param nodeRef     node to update
	 * @return The new version create
	 */
	protected Version writeFileToDisk(final InputStream inputStream, final boolean isAutosave,
			final WOPIAccessTokenInfo wopiToken, final NodeRef nodeRef) {

		AuthenticationUtil.pushAuthentication();
		try {
			AuthenticationUtil.setRunAsUser(wopiToken.getUserName());
			return retryingTransactionHelper
					.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Version>() {
						@Override
						public Version execute() throws Throwable {
							ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);

							// both streams are closed by putContent
							writer.putContent(new BufferedInputStream(inputStream));

							Map<String, Serializable> versionProperties = new HashMap<>(2);
							versionProperties.put(VersionModel.PROP_VERSION_TYPE, VersionType.MINOR);
							if (isAutosave) {
								versionProperties.put(VersionModel.PROP_DESCRIPTION,
										CollaboraOnlineService.AUTOSAVE_DESCRIPTION);
							}
							versionProperties.put(CollaboraOnlineService.LOOL_AUTOSAVE, isAutosave);
							return versionService.createVersion(nodeRef, versionProperties);
						}
					});

		} finally {
			AuthenticationUtil.popAuthentication();
		}
	}

	protected void headerActions(final WebScriptRequest req, final NodeRef nodeRef) {
		QName aspectToAdd = extractQname(req, X_PRISTY_ADD_ASPECT);
		QName aspectToDel = extractQname(req, X_PRISTY_DEL_ASPECT);
		Map<QName, Serializable> delProperties = extractQnamesValues(req, X_PRISTY_DEL_PROPERTY);
		Map<QName, Serializable> properties = extractQnamesValues(req, X_PRISTY_ADD_PROPERTY);

		retryingTransactionHelper.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {
			@Override
			public Void execute() throws Throwable {

				if (aspectToDel != null) {
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
		});

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
			logger.debug(headerName + "=" + aspectToAddHdr);
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

}
