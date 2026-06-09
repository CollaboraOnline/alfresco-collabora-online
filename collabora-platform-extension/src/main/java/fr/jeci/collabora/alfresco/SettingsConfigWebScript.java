// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.security.AuthorityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Returns everything the frontend needs to launch the Collabora Online settings management iframe (WOPI Settings API):
 * a settings access token (not bound to a document), the discovery {@code settings} action urlsrc and the WOPI base
 * URL Collabora calls back.
 * <p>
 * {@code GET /collabora/settings-config?type=user|admin} (user authentication). Requesting {@code admin} requires
 * administrator rights, since shared settings are admin-only to write.
 */
public class SettingsConfigWebScript extends DeclarativeWebScript {

	private static final Logger logger = LoggerFactory.getLogger(SettingsConfigWebScript.class);

	private static final int STATUS_NOT_IMPLEMENTED = 501;

	private static final String PARAM_TYPE = "type";
	private static final String TYPE_USER = "user";
	private static final String TYPE_ADMIN = "admin";

	private static final String ACCESS_TOKEN = "access_token";
	private static final String ACCESS_TOKEN_TTL = "access_token_ttl";
	private static final String SETTINGS_URL = "settings_url";
	private static final String WOPI_SETTING_BASE_URL = "wopi_setting_base_url";
	private static final String IFRAME_TYPE = "iframe_type";

	private CollaboraOnlineService collaboraOnlineService;
	private WopiDiscovery wopiDiscovery;
	private AuthorityService authorityService;
	private String wopiBaseUrl;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		String type = req.getParameter(PARAM_TYPE);
		if (type == null || type.isBlank()) {
			type = TYPE_USER;
		}
		if (!TYPE_USER.equals(type) && !TYPE_ADMIN.equals(type)) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid 'type', expected 'user' or 'admin'");
		}
		if (TYPE_ADMIN.equals(type) && !authorityService.hasAdminAuthority()) {
			throw new WebScriptException(Status.STATUS_FORBIDDEN, "Administrator rights required for admin settings");
		}

		final String settingsUrl = wopiDiscovery.getSettingsUrlSrc();
		if (settingsUrl == null) {
			throw new WebScriptException(STATUS_NOT_IMPLEMENTED, "Settings are not supported by this Collabora server");
		}

		final WOPIAccessTokenInfo token = collaboraOnlineService.createSettingsAccessToken();
		logger.debug("Created settings config for type '{}'", type);

		final Map<String, Object> model = new HashMap<>(5);
		model.put(ACCESS_TOKEN, token.getAccessToken());
		model.put(ACCESS_TOKEN_TTL, token.getExpiresAt()
				.toDate()
				.getTime());
		model.put(SETTINGS_URL, settingsUrl);
		model.put(WOPI_SETTING_BASE_URL, wopiBaseUrl);
		model.put(IFRAME_TYPE, type);
		return model;
	}

	public void setCollaboraOnlineService(CollaboraOnlineService collaboraOnlineService) {
		this.collaboraOnlineService = collaboraOnlineService;
	}

	public void setWopiDiscovery(WopiDiscovery wopiDiscovery) {
		this.wopiDiscovery = wopiDiscovery;
	}

	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}

	public void setWopiBaseUrl(String wopiBaseUrl) {
		this.wopiBaseUrl = wopiBaseUrl;
	}
}
