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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.version.VersionModel;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.cmr.version.VersionType;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import fr.jeci.collabora.alfresco.CollaboraOnlineService;
import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;

public abstract class AbstractWopiWebScript extends AbstractWebScript {
	static final String ACCESS_TOKEN = "access_token";
	static final String FILE_ID = "file_id";

	static final String X_LOOL_WOPI_IS_AUTOSAVE = "X-LOOL-WOPI-IsAutosave";
	static final String X_LOOL_WOPI_TIMESTAMP = "X-LOOL-WOPI-Timestamp";
	static final String X_WOPI_OVERRIDE = "X-WOPI-Override";
	static final String X_WOPI_LOCK = "X-WOPI-Lock";
	static final String X_WOPI_OLD_LOCK = "X-WOPI-OldLock";
	static final String X_WOPI_LOCk_FAILURE_REASON = "X-WOPI-LockFailureReason";
	static final String X_WOPI_ITEM_VERSION = "X-WOPI-ItemVersion";
	
	static final int STATUS_CONFLICT = 409;
	protected NodeService nodeService;
	protected CollaboraOnlineService collaboraOnlineService;
	protected ContentService contentService;
	protected VersionService versionService;
	protected RetryingTransactionHelper retryingTransactionHelper;

	/**
	 * Returns a NodeRef given a file Id. Note: Checks to see if the node exists
	 * aren't performed
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

	protected void jsonResponse(final WebScriptResponse res, int code, Map<String, String> response)
			throws IOException {
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
}
