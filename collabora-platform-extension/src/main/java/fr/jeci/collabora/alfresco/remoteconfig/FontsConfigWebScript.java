// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;

/**
 * Serves the Collabora Online font configuration JSON, generated dynamically
 * from the fonts stored in the repository.
 * <p>
 * GET /collabora/fonts-config (no authentication)
 */
public class FontsConfigWebScript extends AbstractWebScript {

	private RemoteConfigService remoteConfigService;

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		String content = remoteConfigService.getFontsConfig();
		String etag = remoteConfigService.getFontsConfigETag();

		String ifNoneMatch = req.getHeader("If-None-Match");
		if (etag.equals(ifNoneMatch)) {
			res.setStatus(304);
			return;
		}

		res.setHeader("ETag", etag);
		res.setHeader("Cache-Control", "no-cache");
		res.setContentType("application/json;charset=UTF-8");
		res.getWriter()
				.write(content);
	}

	public void setRemoteConfigService(RemoteConfigService remoteConfigService) {
		this.remoteConfigService = remoteConfigService;
	}
}
