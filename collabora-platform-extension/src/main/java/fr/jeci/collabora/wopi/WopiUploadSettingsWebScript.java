// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import fr.jeci.collabora.alfresco.settings.SettingFile;

/**
 * WOPI "Upload settings" endpoint.
 * <p>
 * {@code POST /wopi/settings/upload?fileId=/settings/{type}/{category}/{filename}&access_token=...} with the file
 * content as the raw request body (this is how Collabora uploads settings; it does not use a multipart form).
 * <p>
 * Uploading a {@code systemconfig} file requires administrator rights; {@code userconfig} targets the caller's home.
 * Returns {@code { "status": "success", "filename": "...", "details": { "stamp": "...", "uri": "..." } }}.
 */
public class WopiUploadSettingsWebScript extends AbstractWopiSettingsWebScript {

	@Override
	public void executeAsUser(final WebScriptRequest req, final WebScriptResponse res) throws IOException {
		final String fileId = req.getParameter(FILE_ID);
		if (fileId == null || fileId.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Parameter 'fileId' is required");
		}

		// Collabora posts the settings file as the raw request body. Read it directly: do not call parseContent(),
		// which would consume/close the stream before getInputStream() (Stream closed).
		final InputStream content = req.getContent() != null
				? req.getContent()
						.getInputStream()
				: null;
		if (content == null) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Request body is required");
		}

		final SettingFile stored = this.collaboraSettingsService.uploadSettingsFile(fileId, content, null, req
				.getParameter(ACCESS_TOKEN));

		final Map<String, Object> details = new LinkedHashMap<>(2);
		details.put(STAMP, stored.getStamp());
		details.put(URI, stored.getUri());

		final Map<String, Object> model = new LinkedHashMap<>(3);
		model.put(STATUS, SUCCESS);
		model.put(FILENAME, filenameOf(fileId));
		model.put(DETAILS, details);

		writeJson(res, Status.STATUS_OK, model);
	}

	private static String filenameOf(String fileId) {
		int slash = fileId.lastIndexOf('/');
		return slash >= 0 ? fileId.substring(slash + 1) : fileId;
	}
}
