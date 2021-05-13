package fr.jeci.collabora.wopi;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
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

	protected NodeService nodeService;
	protected CollaboraOnlineService collaboraOnlineService;

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
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> e : response.entrySet()) {
			if (start) {
				sb.append('{');
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

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setCollaboraOnlineService(CollaboraOnlineService service) {
		this.collaboraOnlineService = service;
	}

}
