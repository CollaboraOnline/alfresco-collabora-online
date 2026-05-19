// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;

/**
 * Serves the Collabora Online remote configuration JSON.
 * Polled by coolwsd every 60 seconds with ETag-based caching.
 * <p>
 * GET /collabora/remote-config (no authentication)
 */
public class RemoteConfigWebScript extends AbstractWebScript {

	private RemoteConfigService remoteConfigService;

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		String content = remoteConfigService.getRemoteConfig();
		String etag = remoteConfigService.getRemoteConfigETag();

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
