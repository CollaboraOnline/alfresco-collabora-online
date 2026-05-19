// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.restriction;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin webscript that grants a Collabora Online license to a user by adding them
 * to the licensed group. Fails if the maximum number of licenses has been reached.
 * <p>
 * POST /collabora/license/grant?user={user}
 */
public class LicenseGrantWebScript extends DeclarativeWebScript {

	private static final String PARAM_USER = "user";

	private FeatureRestrictionService featureRestrictionService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		final String user = req.getParameter(PARAM_USER);
		if (user == null || user.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Parameter 'user' is required");
		}

		featureRestrictionService.grantLicense(user);

		final Map<String, Object> model = new HashMap<>(4);
		model.put("success", true);
		model.put("user", user);
		model.put("count", featureRestrictionService.getLicenseCount());
		model.put("maxLicenses", featureRestrictionService.getMaxLicenses());
		return model;
	}

	public void setFeatureRestrictionService(FeatureRestrictionService featureRestrictionService) {
		this.featureRestrictionService = featureRestrictionService;
	}
}
