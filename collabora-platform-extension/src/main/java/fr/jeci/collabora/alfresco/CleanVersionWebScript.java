// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Remove automatic and explicit versions.
 *
 * @author Jeremie Lesage
 */
public class CleanVersionWebScript extends DeclarativeWebScript {
	private static final Logger logger = LoggerFactory.getLogger(CleanVersionWebScript.class);

	private static final String PARAM_STORE_TYPE = "store_type";
	private static final String PARAM_STORE_ID = "store_id";
	private static final String PARAM_ID = "id";
	private static final String PARAM_KEEP_EXP = "keep_exp";
	private static final String PARAM_KEEP_AUTO = "keep_auto";

	private VersionService versionService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		final Map<String, Object> model = new HashMap<>();

		try {
			final Map<String, String> templateArgs = req.getServiceMatch()
					.getTemplateVars();
			final String storeType = WebscriptHelper.getParam(templateArgs, PARAM_STORE_TYPE);
			final String storeId = WebscriptHelper.getParam(templateArgs, PARAM_STORE_ID);
			final String guid = WebscriptHelper.getParam(templateArgs, PARAM_ID);
			final NodeRef nodeRef = new NodeRef(storeType, storeId, guid);

			logger.error("Cleaning Noderef {}", nodeRef);

			// Number automatique version to keep
			Integer keepAuto = WebscriptHelper.integerValue(req, PARAM_KEEP_AUTO);
			keepAuto = keepAuto == null ? -1 : keepAuto;

			logger.error("Keep {} auto-save versions", keepAuto);

			// Number explicit version to keep
			Integer keepExp = WebscriptHelper.integerValue(req, PARAM_KEEP_EXP);
			keepExp = keepExp == null ? -1 : keepExp;

			logger.error("Keep {} explicit versions", keepExp);

			cleanVersion(nodeRef, keepAuto, keepExp);

			model.put("success", "true");
		} catch (Exception e) {
			logger.error("CleanVersionWebScript Error ", e);
			model.put("success", "false");
		}
		return model;

	}

	/**
	 * Removing version by using Alfresco Java API
	 */
	private void cleanVersion(final NodeRef nodeRef, Integer keepAuto, Integer keepExp) {
		final VersionHistory history = versionService.getVersionHistory(nodeRef);

		int countAuto = 0;
		int countExp = 0;
		for (Version version : history.getAllVersions()) {
			Serializable collaboraautosave = version.getVersionProperties()
					.get(CollaboraOnlineService.LOOL_AUTOSAVE);
			if (collaboraautosave == null) {
				logger.debug("v.{} - not lool - keep", version.getVersionLabel());

				// Not Lool Version, ignoring
				continue;
			}

			if (keepAuto >= 0) {
				Boolean autosave = (Boolean) collaboraautosave;
				// Removing old auto-save version
				if (Boolean.TRUE.equals(autosave) && ++countAuto > keepAuto) {
					logger.debug("v.{} - remove auto", version.getVersionLabel());

					versionService.deleteVersion(nodeRef, version);
				}

				// Removing old save version (only from collabora)
				if (Boolean.FALSE.equals(autosave) && ++countExp > keepExp) {
					logger.debug("v.{} - remove explicit", version.getVersionLabel());

					versionService.deleteVersion(nodeRef, version);
				}
			}
		}
	}

	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}

}
