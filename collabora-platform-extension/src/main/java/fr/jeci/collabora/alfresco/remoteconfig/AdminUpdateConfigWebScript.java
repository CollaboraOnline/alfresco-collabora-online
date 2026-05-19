// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;

/**
 * Admin webscript that updates the remote configuration JSON.
 * The body is parsed and re-serialized to ensure it is valid JSON.
 * <p>
 * POST /collabora/admin/remote-config (admin authentication)
 */
public class AdminUpdateConfigWebScript extends AbstractWebScript {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private RemoteConfigService remoteConfigService;

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		String body = req.getContent()
				.getContent();
		if (body == null || body.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Request body is required");
		}

		String sanitized = parseAndSerialize(body);
		remoteConfigService.updateRemoteConfig(sanitized);

		res.setContentType("application/json;charset=UTF-8");
		res.getWriter()
				.write("{\"success\":true}");
	}

	private static String parseAndSerialize(String raw) {
		try {
			JsonNode node = objectMapper.readTree(raw);
			if (!node.isObject()) {
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Request body must be a JSON object");
			}
			if (!node.has("kind")) {
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "JSON must contain a 'kind' field");
			}
			return objectMapper.writeValueAsString(node);
		} catch (JsonProcessingException e) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid JSON: " + e.getOriginalMessage());
		}
	}

	public void setRemoteConfigService(RemoteConfigService remoteConfigService) {
		this.remoteConfigService = remoteConfigService;
	}
}
