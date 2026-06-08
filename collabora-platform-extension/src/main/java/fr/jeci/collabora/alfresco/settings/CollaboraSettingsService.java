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
	 *
	 * @param type {@link #TYPE_USERCONFIG} or {@link #TYPE_SYSTEMCONFIG}
	 * @return the listing exposed to Collabora's "Fetch settings"
	 */
	SettingsListing listSettings(String type);

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
	 * @param fileId   virtual path {@code /settings/{type}/{category}/{filename}}
	 * @param content  file content
	 * @param mimeType MIME type, may be {@code null} to guess from the extension
	 * @return the stored file descriptor (stamp + download uri)
	 */
	SettingFile uploadSettingsFile(String fileId, InputStream content, String mimeType);

	/**
	 * Delete a settings file. Deleting a {@code systemconfig} file requires administrator rights.
	 *
	 * @param fileId virtual path {@code /settings/{type}/{category}/{filename}}
	 */
	void deleteSettingsFile(String fileId);
}
