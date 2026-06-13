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

import fr.jeci.collabora.alfresco.settings.SettingsListing;

/**
 * WOPI "Fetch settings" endpoint.
 * <p>
 * {@code GET /wopi/settings?type=userconfig|systemconfig&fileId=-1&access_token=...}
 * <p>
 * Returns the list of available settings files for the requested type, grouped by category, in the shape
 * {@code { "kind": "user|shared", "<category>": [ {"stamp": "...", "uri": "..."}, ... ] }}.
 */
public class WopiFetchSettingsWebScript extends AbstractWopiSettingsWebScript {

	@Override
	public void executeAsUser(final WebScriptRequest req, final WebScriptResponse res) throws IOException {
		final String type = queryParam(req, TYPE);
		if (type == null || type.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Parameter 'type' is required");
		}

		final SettingsListing listing = this.collaboraSettingsService.listSettings(type, queryParam(req, ACCESS_TOKEN));

		final Map<String, Object> model = new LinkedHashMap<>(listing.getCategories()
				.size() + 1);
		model.put(KIND, listing.getKind());
		model.putAll(listing.getCategories());

		writeJson(res, Status.STATUS_OK, model);
	}
}
