// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import org.alfresco.service.cmr.repository.ContentReader;

import java.io.InputStream;
import java.util.List;

/**
 * Manages the Collabora Online remote/dynamic configuration stored in the Alfresco repository.
 * <p>
 * The configuration is stored under {@code /Data Dictionary/collabora-online/} and served
 * to Collabora Online via unauthenticated webscripts. Collabora polls the remote config
 * endpoint every 60 seconds using ETag-based caching.
 *
 * @see <a href="https://sdk.collaboraonline.com/docs/installation/Configuration.html#remote-dynamic-configuration">
 *      Collabora Online Remote Configuration</a>
 */
public interface RemoteConfigService {

	/**
	 * Returns the remote configuration JSON content.
	 */
	String getRemoteConfig();

	/**
	 * Computes the ETag for the current remote configuration content.
	 */
	String getRemoteConfigETag();

	/**
	 * Updates the remote configuration JSON content.
	 *
	 * @param jsonContent JSON with {@code "kind": "configuration"}
	 */
	void updateRemoteConfig(String jsonContent);

	/**
	 * Generates the font configuration JSON dynamically from the fonts folder contents.
	 * Returns a JSON object with {@code "kind": "fontconfiguration"}.
	 */
	String getFontsConfig();

	/**
	 * Computes the ETag for the current font configuration.
	 */
	String getFontsConfigETag();

	/**
	 * Lists font file names available in the fonts folder.
	 */
	List<String> listFonts();

	/**
	 * Returns a content reader for the given font file.
	 */
	ContentReader getFontContentReader(String fontName);

	/**
	 * Uploads a font file to the fonts folder.
	 */
	void uploadFont(String fontName, InputStream content, String mimeType);

	/**
	 * Deletes a font file from the fonts folder.
	 */
	void deleteFont(String fontName);
}
