// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;

/**
 * Admin webscript that returns the current remote configuration JSON.
 * <p>
 * GET /collabora/admin/remote-config (admin authentication)
 */
public class AdminGetConfigWebScript extends AbstractWebScript {

	private RemoteConfigService remoteConfigService;

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		String content = remoteConfigService.getRemoteConfig();
		res.setContentType("application/json;charset=UTF-8");
		res.getWriter()
				.write(content);
	}

	public void setRemoteConfigService(RemoteConfigService remoteConfigService) {
		this.remoteConfigService = remoteConfigService;
	}
}
