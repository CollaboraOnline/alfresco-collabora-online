// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.settings;

import java.util.List;
import java.util.Map;

/**
 * Result of "Fetch settings" for a given configuration type.
 * <p>
 * {@code kind} is {@code "user"} for userconfig or {@code "shared"} for systemconfig. {@code categories} maps each
 * category folder name (e.g. {@code autotext}, {@code wordbook}, {@code browsersetting}) to its files. The webscript
 * flattens this into the wire shape {@code { "kind": ..., "<category>": [ {stamp, uri}, ... ] }}.
 */
public class SettingsListing {

	private final String kind;
	private final Map<String, List<SettingFile>> categories;

	public SettingsListing(String kind, Map<String, List<SettingFile>> categories) {
		this.kind = kind;
		this.categories = categories;
	}

	public String getKind() {
		return kind;
	}

	public Map<String, List<SettingFile>> getCategories() {
		return categories;
	}
}
