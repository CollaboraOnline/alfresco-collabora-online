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
import org.springframework.extensions.webscripts.servlet.FormData;

import fr.jeci.collabora.alfresco.settings.SettingFile;

/**
 * WOPI "Upload settings" endpoint.
 * <p>
 * {@code POST /wopi/settings/upload?fileId=/settings/{type}/{category}/{filename}&access_token=...} with a multipart
 * {@code file} field carrying the content.
 * <p>
 * Uploading a {@code systemconfig} file requires administrator rights; {@code userconfig} targets the caller's home.
 * Returns {@code { "status": "success", "filename": "...", "details": { "stamp": "...", "uri": "..." } }}.
 */
public class WopiUploadSettingsWebScript extends AbstractWopiSettingsWebScript {

	private static final String FILE_FIELD = "file";

	@Override
	public void executeAsUser(final WebScriptRequest req, final WebScriptResponse res) throws IOException {
		String fileId = req.getParameter(FILE_ID);
		InputStream content = null;

		final Object parsed = req.parseContent();
		if (parsed instanceof FormData) {
			final FormData formData = (FormData) parsed;
			for (FormData.FormField field : formData.getFields()) {
				if (FILE_FIELD.equals(field.getName()) && field.getIsFile()) {
					content = field.getInputStream();
				} else if ((fileId == null || fileId.isBlank()) && FILE_ID.equals(field.getName())) {
					fileId = field.getValue();
				}
			}
		} else if (req.getContent() != null) {
			content = req.getContent()
					.getInputStream();
		}

		if (fileId == null || fileId.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Parameter 'fileId' is required");
		}
		if (content == null) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Multipart 'file' field is required");
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
