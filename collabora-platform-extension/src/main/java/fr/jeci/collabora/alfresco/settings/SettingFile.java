// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.settings;

/**
 * A single settings file entry as exposed to Collabora Online in the "Fetch settings" response.
 * <p>
 * The {@code stamp} is an opaque cache key (changes whenever the file changes) and {@code uri} is the absolute URL
 * Collabora must call to download the file content.
 */
public class SettingFile {

	private final String stamp;
	private final String uri;

	public SettingFile(String stamp, String uri) {
		this.stamp = stamp;
		this.uri = uri;
	}

	public String getStamp() {
		return stamp;
	}

	public String getUri() {
		return uri;
	}
}
