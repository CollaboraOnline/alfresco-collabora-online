// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.restriction;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Admin webscript that returns the current license status: enabled flag, license count,
 * maximum allowed licenses, and the list of licensed user IDs.
 * <p>
 * GET /collabora/license
 */
public class LicenseListWebScript extends DeclarativeWebScript {

	private FeatureRestrictionService featureRestrictionService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		final Map<String, Object> model = new HashMap<>(4);
		model.put("enabled", featureRestrictionService.isFeatureRestrictionEnabled());
		model.put("maxLicenses", featureRestrictionService.getMaxLicenses());

		Set<String> users = featureRestrictionService.getLicensedUsers();
		model.put("count", users.size());
		model.put("users", new ArrayList<>(users));

		return model;
	}

	public void setFeatureRestrictionService(FeatureRestrictionService featureRestrictionService) {
		this.featureRestrictionService = featureRestrictionService;
	}
}
