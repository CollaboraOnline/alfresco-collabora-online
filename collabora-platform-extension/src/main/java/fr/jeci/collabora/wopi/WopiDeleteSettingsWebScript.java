// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

/**
 * WOPI "Delete settings file" endpoint.
 * <p>
 * {@code DELETE /wopi/settings?fileId=/settings/{type}/{category}/{filename}&access_token=...}
 * <p>
 * Deleting a {@code systemconfig} file requires administrator rights. Returns
 * {@code { "status": "success", "message": "..." }}.
 */
public class WopiDeleteSettingsWebScript extends AbstractWopiSettingsWebScript {

	@Override
	public void executeAsUser(final WebScriptRequest req, final WebScriptResponse res) throws IOException {
		final String fileId = queryParam(req, FILE_ID);
		if (fileId == null || fileId.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Parameter 'fileId' is required");
		}

		this.collaboraSettingsService.deleteSettingsFile(fileId);

		final Map<String, Object> model = new LinkedHashMap<>(2);
		model.put(STATUS, SUCCESS);
		model.put(MESSAGE, "Deleted " + fileId);

		writeJson(res, Status.STATUS_OK, model);
	}
}
