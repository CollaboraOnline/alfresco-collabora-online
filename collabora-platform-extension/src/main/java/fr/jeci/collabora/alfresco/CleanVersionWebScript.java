/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package fr.jeci.collabora.alfresco;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Remove automatic and explicit versions.
 * 
 * @author Jeremie Lesage
 *
 */
public class CleanVersionWebScript extends DeclarativeWebScript {
	private static final Log logger = LogFactory.getLog(CleanVersionWebScript.class);

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
			final Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
			final String storeType = WebscriptHelper.getParam(templateArgs, PARAM_STORE_TYPE);
			final String storeId = WebscriptHelper.getParam(templateArgs, PARAM_STORE_ID);
			final String guid = WebscriptHelper.getParam(templateArgs, PARAM_ID);
			final NodeRef nodeRef = new NodeRef(storeType, storeId, guid);

			if (logger.isDebugEnabled()) {
				logger.error("Cleaning Noderef " + nodeRef);
			}

			// Number automatique version to keep
			Integer keepAuto = WebscriptHelper.intergerValue(req, PARAM_KEEP_AUTO);
			keepAuto = keepAuto == null ? -1 : keepAuto;

			if (logger.isDebugEnabled()) {
				logger.error("Keep " + keepAuto + " auto-save versions");
			}

			// Number explicit version to keep
			Integer keepExp = WebscriptHelper.intergerValue(req, PARAM_KEEP_EXP);
			keepExp = keepExp == null ? -1 : keepExp;

			if (logger.isDebugEnabled()) {
				logger.error("Keep " + keepExp + " explicit versions");
			}

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
	 * 
	 * @param nodeRef
	 * @param keepAuto
	 * @param keepExp
	 */
	private void cleanVersion(final NodeRef nodeRef, Integer keepAuto, Integer keepExp) {
		final VersionHistory history = versionService.getVersionHistory(nodeRef);

		int countAuto = 0;
		int countExp = 0;
		for (Version version : history.getAllVersions()) {
			Serializable collaboraautosave = version.getVersionProperties().get(CollaboraOnlineService.COOL_AUTOSAVE);
			if (collaboraautosave == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("v." + version.getVersionLabel() + " - not cool - keep");
				}

				// Not Cool Version, ignoring
				continue;
			}

			if (keepAuto >= 0) {
				Boolean autosave = (Boolean) collaboraautosave;
				// Removing old auto-save version
				if (Boolean.TRUE.equals(autosave) && ++countAuto > keepAuto) {
					if (logger.isDebugEnabled()) {
						logger.debug("v." + version.getVersionLabel() + " - remove auto");
					}

					versionService.deleteVersion(nodeRef, version);
				}

				// Removing old save version (only from collabora)
				if (Boolean.FALSE.equals(autosave) && ++countExp > keepExp) {
					if (logger.isDebugEnabled()) {
						logger.debug("v." + version.getVersionLabel() + " - remove explicit");
					}

					versionService.deleteVersion(nodeRef, version);
				}
			}
		}
	}

	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}

}
