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
 * Admin webscript that updates the maximum number of Collabora Online licenses at runtime.
 * The new value is persisted and survives restarts.
 * <p>
 * POST /collabora/license/config?maxLicenses={maxLicenses}
 */
public class LicenseConfigWebScript extends DeclarativeWebScript {

	private static final String PARAM_MAX_LICENSES = "maxLicenses";

	private FeatureRestrictionService featureRestrictionService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		final String maxStr = req.getParameter(PARAM_MAX_LICENSES);
		if (maxStr == null || maxStr.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Parameter 'maxLicenses' is required");
		}

		int maxLicenses;
		try {
			maxLicenses = Integer.parseInt(maxStr);
		} catch (NumberFormatException e) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Parameter 'maxLicenses' must be an integer");
		}

		if (maxLicenses < -1) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST,
					"Parameter 'maxLicenses' must be -1 (unlimited) or a positive integer");
		}

		featureRestrictionService.setMaxLicenses(maxLicenses);

		final Map<String, Object> model = new HashMap<>(2);
		model.put("success", true);
		model.put("maxLicenses", maxLicenses);
		return model;
	}

	public void setFeatureRestrictionService(FeatureRestrictionService featureRestrictionService) {
		this.featureRestrictionService = featureRestrictionService;
	}
}
