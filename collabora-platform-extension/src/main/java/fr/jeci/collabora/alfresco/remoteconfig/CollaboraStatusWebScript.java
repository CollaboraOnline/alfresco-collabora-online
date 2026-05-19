// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import fr.jeci.collabora.alfresco.WopiDiscovery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin webscript returning the Collabora Online integration status.
 * Supports an optional {@code reload} parameter to re-attempt connection
 * to the Collabora Online server (useful when Alfresco starts before Collabora).
 * <p>
 * GET /collabora/admin/status?reload={reload} (admin authentication)
 */
public class CollaboraStatusWebScript extends DeclarativeWebScript {

	private static final Logger logger = LoggerFactory.getLogger(CollaboraStatusWebScript.class);

	private WopiDiscovery wopiDiscovery;
	private URL collaboraPublicUrl;
	private RemoteConfigService remoteConfigService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		String reloadParam = req.getParameter("reload");
		if ("true".equals(reloadParam)) {
			logger.info("Reloading Collabora Online discovery...");
			wopiDiscovery.reload();
		}

		String enableParam = req.getParameter("enableRemoteConfig");
		if ("true".equals(enableParam)) {
			remoteConfigService.enableRemoteConfig();
		} else if ("false".equals(enableParam)) {
			remoteConfigService.disableRemoteConfig();
		}

		final Map<String, Object> model = new HashMap<>(3);
		model.put("online", wopiDiscovery.hasCollaboraOnline());
		model.put("serverUrl", collaboraPublicUrl != null ? collaboraPublicUrl.toString() : "");
		model.put("remoteConfigEnabled", remoteConfigService.isRemoteConfigEnabled());
		return model;
	}

	public void setWopiDiscovery(WopiDiscovery wopiDiscovery) {
		this.wopiDiscovery = wopiDiscovery;
	}

	public void setCollaboraPublicUrl(URL collaboraPublicUrl) {
		this.collaboraPublicUrl = collaboraPublicUrl;
	}

	public void setRemoteConfigService(RemoteConfigService remoteConfigService) {
		this.remoteConfigService = remoteConfigService;
	}
}
