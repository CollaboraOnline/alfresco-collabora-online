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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import fr.jeci.collabora.alfresco.ConflictException;
import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;

/**
 * X-WOPI-ValidRelativeTarget IS NOT IMPLEMENT
 * 
 * @author jlesage
 *
 */
public class WopiPutRelativeFileWebScript extends AbstractWopiWebScript {
	private static final Log logger = LogFactory.getLog(WopiPutRelativeFileWebScript.class);

	static final String X_WOPI_SUGGESTED_TARGET = "X-WOPI-SuggestedTarget";
	static final String X_WOPI_RELATIVE_TARGET = "X-WOPI-RelativeTarget";
	static final String X_WOPI_OVERWRITE_RELATIVE_TARGET = "X-WOPI-OverwriteRelativeTarget";
	static final String X_WOPI_SIZE = "X-WOPI-Size";
	static final String X_WOPI_FILE_CONVERSION = "X-WOPI-FileConversion";

	private static final int MAX_RETRY = 5;
	private static final String SUFFIX = "_1";

	private CopyService copyService;

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		final String wopiOverrideHeader = req.getHeader(X_WOPI_OVERRIDE);
		if (wopiOverrideHeader == null) {
			throw new WebScriptException(X_WOPI_OVERRIDE + " header must be present");
		}

		final String wopiSize = req.getHeader(X_WOPI_SIZE);
		if (StringUtils.isNotBlank(wopiSize)) {
			logger.warn("Header " + X_WOPI_SIZE + " is not implements: " + wopiSize);
		}

		final WOPIAccessTokenInfo wopiToken = wopiToken(req);

		final NodeRef nodeRef = getFileNodeRef(wopiToken);

		if (nodeRef == null) {
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
					"No noderef for WOPIAccessTokenInfo:" + wopiToken);
		}

		try {
			Map<String, String> model;
			AuthenticationUtil.pushAuthentication();
			try {
				AuthenticationUtil.setRunAsUser(wopiToken.getUserName());
				model = wopiOverrideSwicth(req, res, nodeRef, wopiToken);
			} finally {
				AuthenticationUtil.popAuthentication();
			}
			jsonResponse(res, Status.STATUS_OK, model);

		} catch (ConflictException e) {
			res.setHeader(X_WOPI_LOCK, e.getCurrentLockId());
			res.setHeader(X_WOPI_LOCk_FAILURE_REASON, e.getLockFailureReason());
			jsonResponse(res, STATUS_CONFLICT, e.getLockFailureReason());
		}

	}

	private final Map<String, String> wopiOverrideSwicth(final WebScriptRequest req, final WebScriptResponse res,
			final NodeRef nodeRef, final WOPIAccessTokenInfo wopiToken) throws ConflictException, IOException {
		final String wopiOverrideHeader = req.getHeader(X_WOPI_OVERRIDE);
		if (wopiOverrideHeader == null) {
			throw new WebScriptException(X_WOPI_OVERRIDE + " header must be present");
		}

		WopiOverride override;

		try {
			override = WopiOverride.valueOf(wopiOverrideHeader);
		} catch (IllegalArgumentException e) {
			throw new WebScriptException(X_WOPI_OVERRIDE + " unkown value " + wopiOverrideHeader);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("WopiOverride=" + override.name());
		}

		final String lockId = req.getHeader(X_WOPI_LOCK);

		final Map<String, String> model = new HashMap<>(1);
		String currentLockId = null;
//		String itemVersion = null;
		switch (override) {
		case PUT:
			logger.warn("PUT without LOCK for node " + nodeRef);
			break;
		case PUT_RELATIVE:
			checkHeadersRelative(req);
			NodeRef newNodeRef = createNodeWithValidName(req, wopiToken);
			model.putAll(saveAs(req, wopiToken, newNodeRef));
			break;
		case LOCK:
			currentLockId = this.collaboraOnlineService.lock(nodeRef, lockId);
			break;
		case GET_LOCK:
			currentLockId = this.collaboraOnlineService.lockGet(nodeRef);
			break;
		case REFRESH_LOCK:
			this.collaboraOnlineService.lockRefresh(nodeRef, lockId);
			break;
		case UNLOCK:
			currentLockId = this.collaboraOnlineService.lockUnlock(nodeRef, lockId);
			break;
		default:
			break;
		}

		if (currentLockId != null) {
			res.setHeader(X_WOPI_LOCK, currentLockId);
		}

//		if (itemVersion != null) {
//			res.setHeader(X_WOPI_ITEM_VERSION, itemVersion);
//		}

		return model;

	}

	private Map<String, String> saveAs(WebScriptRequest req, final WOPIAccessTokenInfo wopiToken, NodeRef newNodeRef) {
		if (logger.isDebugEnabled()) {
			logger.debug("saveAs newNodeRef=" + newNodeRef);
		}

		final InputStream inputStream = req.getContent().getInputStream();
		if (inputStream == null) {
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
					"No inputStream for WOPIAccessTokenInfo:" + wopiToken);
		}

		try {
			writeFileToDisk(inputStream, false, wopiToken, newNodeRef);

			String newUrl = generateUrl(wopiToken, newNodeRef);

			if (logger.isDebugEnabled()) {
				logger.debug("WopiPutRelativeFileWebScript newUrl = '" + newUrl + "'");
			}

			final Map<String, String> model = new HashMap<>(2);
			final Map<QName, Serializable> properties = runAsGetProperties(wopiToken, newNodeRef);
			model.put("Name", (String) properties.get(ContentModel.PROP_NAME));
			model.put("Url", newUrl);
			return model;

		} catch (ContentIOException we) {
			final String msg = "Error writing to file";
			logger.error(msg, we);
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, msg);
		}
	}

	private String generateUrl(final WOPIAccessTokenInfo wopiToken, NodeRef newNodeRef) {
		String newUrl;
		AuthenticationUtil.pushAuthentication();
		try {
			AuthenticationUtil.setRunAsUser(wopiToken.getUserName());
			newUrl = retryingTransactionHelper
					.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<String>() {
						@Override
						public String execute() throws Throwable {
							WOPIAccessTokenInfo tokenInfo = collaboraOnlineService.createAccessToken(newNodeRef);
							String alfrescoUrl = collaboraOnlineService.getAlfrescoPrivateURL().toString();
							return String.format("%s%s%s?access_token=%s", alfrescoUrl, "s/wopi/files/",
									newNodeRef.getId(), tokenInfo.getAccessToken());
						}
					});

		} finally {
			AuthenticationUtil.popAuthentication();
		}
		return newUrl;
	}

	private NodeRef createNodeWithValidName(WebScriptRequest req, final WOPIAccessTokenInfo wopiToken) {
		final String suggested = req.getHeader(X_WOPI_SUGGESTED_TARGET);
		final String relative = req.getHeader(X_WOPI_RELATIVE_TARGET);
		final String overwrite = req.getHeader(X_WOPI_OVERWRITE_RELATIVE_TARGET);

		boolean isSuggested = StringUtils.isNotBlank(suggested);
		boolean isRelative = StringUtils.isNotBlank(relative);
		boolean isOverwrite = isOverwrite(overwrite);

		final NodeRef nodeRef = getFileNodeRef(wopiToken);

		if (nodeRef == null) {
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
					"No noderef for WOPIAccessTokenInfo: " + wopiToken);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("createNodeWithValidName " + nodeRef);
		}

		NodeRef newNodeRef = null;
		AuthenticationUtil.pushAuthentication();
		try {
			AuthenticationUtil.setRunAsUser(wopiToken.getUserName());
			newNodeRef = retryingTransactionHelper
					.doInTransaction(new RetryingTransactionHelper.RetryingTransactionCallback<NodeRef>() {
						@Override
						public NodeRef execute() throws Throwable {
							String targetFileName;
							if (isSuggested) {
								final String sourceFileName = (String) nodeService.getProperty(nodeRef,
										ContentModel.PROP_NAME);
								targetFileName = suggested(suggested, sourceFileName);
							} else {
								targetFileName = relative;
							}

							if (logger.isDebugEnabled()) {
								logger.debug("targetFileName " + targetFileName);
							}

							int retry = MAX_RETRY;
							NodeRef newNodeRef;
							do {
								newNodeRef = createNode(isRelative, isOverwrite, nodeRef, targetFileName);
								if (--retry < 0) {

									if (logger.isDebugEnabled()) {
										logger.debug("No retry >> " + retry);
									}
									break;
								}
								targetFileName = addSuffix(SUFFIX, targetFileName);

								if (logger.isDebugEnabled()) {
									logger.debug("Retry >> " + targetFileName);
								}
							} while (newNodeRef == null);

							return newNodeRef;
						}
					});

		} finally {
			AuthenticationUtil.popAuthentication();
		}

		if (newNodeRef == null) {
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
					"Fail to create node copy of: " + nodeRef);
		}

		return newNodeRef;
	}

	private boolean isOverwrite(final String overwrite) {
		if (StringUtils.isNotBlank(overwrite)) {
			return Boolean.parseBoolean(overwrite);
		} else {
			return false;
		}
	}

	/**
	 * Create a node in the same directory as the original node and with the same
	 * type and proprties (like a copy).
	 * 
	 * @param isRelative     false, return null if the node can't be create. If
	 *                       true, it's depends of overwitre parameter
	 * @param overwrite      if the target node already exist and overwrite is true,
	 *                       the target noderef node is return, else throw an
	 *                       exeption
	 * @param nodeRef        Node ref of the original node
	 * @param properties     Properties of the original node
	 * @param targetFileName Name of the new node
	 * @return noderef of the node to write to or null
	 */
	private NodeRef createNode(boolean isRelative, boolean overwrite, final NodeRef sourceNodeRef,
			String targetFileName) {

		if (logger.isDebugEnabled()) {
			logger.debug("createNode " + sourceNodeRef + " >> " + targetFileName);
		}

		ChildAssociationRef assocRef = nodeService.getPrimaryParent(sourceNodeRef);
		NodeRef targetParentRef = assocRef.getParentRef();

		// Have the node created
		NodeRef newNodeRef = null;
		try {
			QName newQname = QName.createQName(assocRef.getQName().getNamespaceURI(),
					QName.createValidLocalName(targetFileName));
			newNodeRef = copyService.copy(sourceNodeRef, targetParentRef, assocRef.getTypeQName(), newQname, true);
			nodeService.setProperty(newNodeRef, ContentModel.PROP_NAME, targetFileName);

		} catch (AccessDeniedException e) {
			throw new WebScriptException(Status.STATUS_FORBIDDEN, "You don't have permission to create the node");
		} catch (InvalidNodeRefException e) {
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
					"the parent reference is invalid: " + targetParentRef);
		} catch (DuplicateChildNodeNameException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("NodeExistsException " + targetFileName, e);
			}

			if (isRelative) {
				if (overwrite) {
					if (logger.isDebugEnabled()) {
						logger.debug("NodeExistsException OVERWRITE !");
					}
//					TODO
//					newNodeRef = e.getNodePair().getSecond();
				} else {
					throw new WebScriptException(STATUS_CONFLICT,
							"File with the specified name already exists: " + targetFileName);
				}
			} else {
				logger.info(
						"File with the specified name already exists: " + targetFileName + " try with another name");
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("createNode =>> " + newNodeRef);
		}

		return newNodeRef;
	}

	private void checkHeadersRelative(WebScriptRequest req) {

		final String wopiFileConversion = req.getHeader(X_WOPI_FILE_CONVERSION);
		if (StringUtils.isNotBlank(wopiFileConversion)) {
			logger.warn("Header " + X_WOPI_FILE_CONVERSION + " is not implements: " + wopiFileConversion);
		}

		final String suggested = req.getHeader(X_WOPI_SUGGESTED_TARGET);
		final String relative = req.getHeader(X_WOPI_RELATIVE_TARGET);

		boolean isSuggested = StringUtils.isNotBlank(suggested);
		boolean isRelative = StringUtils.isNotBlank(relative);

		if (isSuggested && isRelative) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST,
					"Can't have both " + X_WOPI_SUGGESTED_TARGET + " and " + X_WOPI_RELATIVE_TARGET + " header");
		}
		if (!isSuggested && !isRelative) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST,
					"Need one of " + X_WOPI_SUGGESTED_TARGET + " or " + X_WOPI_RELATIVE_TARGET + " header");
		}
	}

	/**
	 * If the string begins with a period (.), it is a file extension. Otherwise, it
	 * is a full file name.
	 * 
	 * @param suggested      new file name or new extension for the file.
	 * @param sourceFileName name of the initial file
	 * @return
	 */
	private String suggested(final String suggested, final String sourceFileName) {
		String targetFileName;
		if (suggested.startsWith(".")) {
			int lastDot = sourceFileName.lastIndexOf('.');
			String basename = sourceFileName.substring(0, lastDot);
			targetFileName = String.format("%s.%s", basename, suggested);
		} else {
			targetFileName = suggested;
		}
		return targetFileName;
	}

	private String addSuffix(final String suffix, final String sourceFileName) {
		int lastDot = sourceFileName.lastIndexOf('.');
		String basename = sourceFileName.substring(0, lastDot);
		String ext = sourceFileName.substring(lastDot);
		return String.format("%s%s%s", basename, suffix, ext);
	}

	public void setCopyService(CopyService copyService) {
		this.copyService = copyService;
	}
}
