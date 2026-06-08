// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import java.io.IOException;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.jeci.collabora.alfresco.CollaboraOnlineService;
import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;
import fr.jeci.collabora.alfresco.settings.CollaboraSettingsService;

/**
 * Base class for the WOPI Settings endpoints.
 * <p>
 * Unlike {@link AbstractWopiWebScript}, the settings {@code fileId} is a virtual settings path (or {@code -1}), not a
 * document nodeRef. The access token is therefore resolved to its user only (no fileId&harr;nodeRef check), and the
 * subclass runs as that user.
 */
public abstract class AbstractWopiSettingsWebScript extends AbstractWebScript {

	private static final Logger logger = LoggerFactory.getLogger(AbstractWopiSettingsWebScript.class);
	private static final ObjectMapper objectMapper = new ObjectMapper();

	static final String ACCESS_TOKEN = "access_token";
	// Collabora's WOPI Settings API uses the camelCase query parameter "fileId" (e.g. ?fileId=-1 or
	// ?fileId=/settings/...). This intentionally differs from the document WOPI flow, where AbstractWopiWebScript reads
	// the "file_id" URL template variable mandated by the Microsoft WOPI spec. Do not unify the two.
	static final String FILE_ID = "fileId";
	static final String TYPE = "type";

	// JSON response keys / values shared by the WOPI Settings endpoints
	static final String KIND = "kind";
	static final String STATUS = "status";
	static final String SUCCESS = "success";
	static final String MESSAGE = "message";
	static final String STAMP = "stamp";
	static final String URI = "uri";
	static final String FILENAME = "filename";
	static final String DETAILS = "details";

	protected CollaboraOnlineService collaboraOnlineService;
	protected CollaboraSettingsService collaboraSettingsService;

	public abstract void executeAsUser(final WebScriptRequest req, final WebScriptResponse res) throws IOException;

	@Override
	public void execute(final WebScriptRequest req, final WebScriptResponse res) throws IOException {
		final String accessToken = req.getParameter(ACCESS_TOKEN);
		final WOPIAccessTokenInfo token = this.collaboraOnlineService.resolveToken(accessToken);

		logger.debug("{} user='{}'", req.getPathInfo(), token.getUserName());

		AuthenticationUtil.pushAuthentication();
		try {
			AuthenticationUtil.setFullyAuthenticatedUser(token.getUserName());
			executeAsUser(req, res);
		} finally {
			AuthenticationUtil.popAuthentication();
		}
	}

	protected void writeJson(final WebScriptResponse res, int code, Object response) throws IOException {
		try {
			res.reset();
			res.setStatus(code);
			res.setContentType("application/json;charset=UTF-8");
			res.getWriter()
					.append(objectMapper.writeValueAsString(response));
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize response to JSON", e);
			throw new IOException("Failed to serialize response to JSON", e);
		}
	}

	public void setCollaboraOnlineService(CollaboraOnlineService collaboraOnlineService) {
		this.collaboraOnlineService = collaboraOnlineService;
	}

	public void setCollaboraSettingsService(CollaboraSettingsService collaboraSettingsService) {
		this.collaboraSettingsService = collaboraSettingsService;
	}
}
