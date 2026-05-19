// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin webscript that deletes a font file from the fonts folder.
 * <p>
 * DELETE /collabora/admin/fonts/{font_name} (admin authentication)
 */
public class AdminDeleteFontWebScript extends DeclarativeWebScript {

	private RemoteConfigService remoteConfigService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		String fontName = req.getServiceMatch()
				.getTemplateVars()
				.get("font_name");
		remoteConfigService.deleteFont(fontName);

		final Map<String, Object> model = new HashMap<>(2);
		model.put("success", true);
		model.put("font", fontName);
		return model;
	}

	public void setRemoteConfigService(RemoteConfigService remoteConfigService) {
		this.remoteConfigService = remoteConfigService;
	}
}
