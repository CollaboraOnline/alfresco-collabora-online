// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * Serves individual font files as binary content for Collabora Online.
 * <p>
 * GET /collabora/fonts/{font_name} (no authentication)
 */
public class FontFileWebScript extends AbstractWebScript {

	private static final Logger logger = LoggerFactory.getLogger(FontFileWebScript.class);
	private static final int BUFFER_SIZE = 1024 * 4;

	private RemoteConfigService remoteConfigService;

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		String fontName = req.getServiceMatch()
				.getTemplateVars()
				.get("font_name");
		if (fontName == null || fontName.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Font name is required");
		}

		ContentReader reader = remoteConfigService.getFontContentReader(fontName);

		res.setContentType(reader.getMimetype());
		res.setHeader("Cache-Control", "public, max-age=86400");

		try (InputStream inputStream = reader.getContentInputStream()) {
			long copied = IOUtils.copyLarge(inputStream, res.getOutputStream(), new byte[BUFFER_SIZE]);
			logger.debug("Served font '{}' ({} bytes)", fontName, copied);
		}
	}

	public void setRemoteConfigService(RemoteConfigService remoteConfigService) {
		this.remoteConfigService = remoteConfigService;
	}
}
