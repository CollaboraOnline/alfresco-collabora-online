// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import java.io.IOException;
import java.io.InputStream;

import org.alfresco.service.cmr.repository.ContentReader;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * WOPI "Download settings" endpoint, serving the raw content pointed to by a {@code uri} from "Fetch settings"
 * (including {@code browsersetting.json}).
 * <p>
 * {@code GET /wopi/settings/download?fileId=/settings/{type}/{category}/{filename}&access_token=...}
 */
public class WopiDownloadSettingsWebScript extends AbstractWopiSettingsWebScript {

	private static final Logger logger = LoggerFactory.getLogger(WopiDownloadSettingsWebScript.class);
	private static final int BUFFER_SIZE = 1024 * 4;

	@Override
	public void executeAsUser(final WebScriptRequest req, final WebScriptResponse res) throws IOException {
		final String fileId = req.getParameter(FILE_ID);
		if (fileId == null || fileId.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Parameter 'fileId' is required");
		}

		final ContentReader reader = this.collaboraSettingsService.getSettingsFile(fileId);

		res.setContentType(reader.getMimetype());
		try (InputStream inputStream = reader.getContentInputStream()) {
			long copied = IOUtils.copyLarge(inputStream, res.getOutputStream(), new byte[BUFFER_SIZE]);
			logger.debug("Served settings file '{}' ({} bytes)", fileId, copied);
		}
	}
}
