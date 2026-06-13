// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.settings;

import java.io.InputStream;

import org.alfresco.service.cmr.repository.ContentReader;

/**
 * Stores and retrieves Collabora Online settings files (the WOPI Settings API).
 * <p>
 * Settings are addressed by a virtual {@code fileId} path of the form
 * {@code /settings/{type}/{category}/{filename}} where {@code type} is {@link #TYPE_USERCONFIG} or
 * {@link #TYPE_SYSTEMCONFIG}. They are persisted in the Alfresco repository:
 * <ul>
 * <li><b>systemconfig</b> (shared) &rarr; {@code /Company Home/Data Dictionary/<basePath>/shared-settings/...}</li>
 * <li><b>userconfig</b> (per user) &rarr; {@code /Company Home/User Homes/<userid>/<userPath>/...}, resolved under the
 * currently authenticated user.</li>
 * </ul>
 */
public interface CollaboraSettingsService {

	String TYPE_USERCONFIG = "userconfig";
	String TYPE_SYSTEMCONFIG = "systemconfig";

	/** Virtual path prefix every settings {@code fileId} must start with. */
	String SETTINGS_PREFIX = "/settings/";

	/**
	 * List the settings files available for the given type, grouped by category.
	 * <p>
	 * The access token is embedded in each file's download {@code uri}: Collabora fetches those URIs verbatim when
	 * installing presets (it does not append a token in that flow), so the token must already be present.
	 *
	 * @param type        {@link #TYPE_USERCONFIG} or {@link #TYPE_SYSTEMCONFIG}
	 * @param accessToken the token to embed in each download uri
	 * @return the listing exposed to Collabora's "Fetch settings"
	 */
	SettingsListing listSettings(String type, String accessToken);

	/**
	 * Return a reader on the content of a single settings file.
	 *
	 * @param fileId virtual path {@code /settings/{type}/{category}/{filename}}
	 * @return content reader, never {@code null}
	 */
	ContentReader getSettingsFile(String fileId);

	/**
	 * Create or replace a settings file.
	 * <p>
	 * Writing a {@code systemconfig} file requires administrator rights; {@code userconfig} always targets the caller's
	 * own home folder.
	 *
	 * @param fileId      virtual path {@code /settings/{type}/{category}/{filename}}
	 * @param content     file content
	 * @param mimeType    MIME type, may be {@code null} to guess from the extension
	 * @param accessToken the token to embed in the returned download uri
	 * @return the stored file descriptor (stamp + download uri)
	 */
	SettingFile uploadSettingsFile(String fileId, InputStream content, String mimeType, String accessToken);

	/**
	 * Delete a settings file. Deleting a {@code systemconfig} file requires administrator rights.
	 *
	 * @param fileId virtual path {@code /settings/{type}/{category}/{filename}}
	 */
	void deleteSettingsFile(String fileId);

	/**
	 * Build the settings fetch URL advertised in WOPI CheckFileInfo ({@code UserSettings} / {@code SharedSettings}), so
	 * Collabora loads the settings when a document is opened.
	 *
	 * @param type        {@link #TYPE_USERCONFIG} or {@link #TYPE_SYSTEMCONFIG}
	 * @param accessToken the document access token Collabora will reuse
	 * @return {@code <base>/wopi/settings?access_token=...&type=...&fileId=-1}
	 */
	String settingsUrl(String type, String accessToken);

	/**
	 * Compute an opaque cache stamp for the settings of a given type. It changes only when a settings file of that type
	 * changes, so Collabora re-fetches only when needed.
	 *
	 * @param type {@link #TYPE_USERCONFIG} or {@link #TYPE_SYSTEMCONFIG}
	 * @return stamp (millis of the most recently modified settings file, {@code "0"} when none)
	 */
	String settingsStamp(String type);

	/**
	 * @return the normalized WOPI settings base URL (the full {@code /wopi/settings} endpoint), without duplicate
	 *         slashes
	 */
	String getWopiBaseUrl();
}
