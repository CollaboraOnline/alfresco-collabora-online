// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
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
	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";
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
		final String accessToken = resolveAccessToken(req);
		final WOPIAccessTokenInfo token;
		try {
			token = this.collaboraOnlineService.resolveToken(accessToken);
		} catch (final WebScriptException e) {
			// Surface rejected authentication: until now these failures were silent, which made the
			// settings round-trip (e.g. wordbook upload) hard to diagnose.
			logger.warn("Rejected settings request {}: {}", req.getPathInfo(), e.getMessage());
			throw e;
		}

		logger.debug("{} user='{}'", req.getPathInfo(), token.getUserName());

		AuthenticationUtil.pushAuthentication();
		try {
			AuthenticationUtil.setFullyAuthenticatedUser(token.getUserName());
			executeAsUser(req, res);
		} finally {
			AuthenticationUtil.popAuthentication();
		}
	}

	/**
	 * Resolve the WOPI access token from the request, accepting either delivery mechanism Collabora uses.
	 * <p>
	 * Fetch and Download URLs carry the token in the {@code access_token} query parameter (the WOPI host embeds it when
	 * it builds those URLs). The preset round-trip upload ({@code wordbook}/{@code xcu}/{@code themes}), however, builds
	 * its own URL and sends the token only as an {@code Authorization: Bearer <token>} header — see
	 * {@code DocumentBroker::uploadPresetsToWopiHost} in Collabora Online, which (unlike every other WOPI call) does not
	 * call {@code authorizeURI}. Without the header fallback those uploads are rejected and never persist.
	 *
	 * @param req the request
	 * @return the access token, or {@code null} if neither source provides one
	 */
	static String resolveAccessToken(final WebScriptRequest req) {
		final String queryToken = queryParam(req, ACCESS_TOKEN);
		if (queryToken != null && !queryToken.isBlank()) {
			return queryToken;
		}

		final String authorization = req.getHeader(AUTHORIZATION);
		if (authorization != null && authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			final String bearer = authorization.substring(BEARER_PREFIX.length())
					.trim();
			if (!bearer.isEmpty()) {
				return bearer;
			}
		}

		return null;
	}

	/**
	 * Read a request parameter from the raw query string only.
	 * <p>
	 * The upload endpoint receives the file as the request body; Collabora posts it with
	 * {@code Content-Type: application/x-www-form-urlencoded}. Calling {@code req.getParameter(...)} would make the
	 * servlet container parse (and consume) that body to build the parameter map, leaving an empty body for
	 * {@code getContent().getInputStream()} — the file would be stored empty. Parsing the query string ourselves never
	 * touches the body.
	 *
	 * @param req  the request
	 * @param name parameter name
	 * @return the decoded value, or {@code null} if absent
	 */
	protected static String queryParam(final WebScriptRequest req, final String name) {
		String queryString = req.getQueryString();
		if (queryString == null) {
			String url = req.getURL();
			int q = url.indexOf('?');
			queryString = q >= 0 ? url.substring(q + 1) : null;
		}
		if (queryString == null) {
			return null;
		}
		for (String pair : queryString.split("&")) {
			int eq = pair.indexOf('=');
			String key = eq >= 0 ? pair.substring(0, eq) : pair;
			if (key.equals(name)) {
				String value = eq >= 0 ? pair.substring(eq + 1) : "";
				return URLDecoder.decode(value, StandardCharsets.UTF_8);
			}
		}
		return null;
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
