// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Don't think this is usefull
 */
public class ServiceUrlInfoWebScript extends DeclarativeWebScript {
	private static final String LOOL_HOST_URL = "lool_host_url";
	private static final String ALFRESCO_SERVICE = "service/";

	protected CollaboraOnlineService collaboraOnlineService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		URL alfrescoPrivateURL = this.collaboraOnlineService.getAlfrescoPrivateURL();
		URL colServiceUrl;
		try {
			colServiceUrl = new URL(alfrescoPrivateURL, ALFRESCO_SERVICE);
		} catch (MalformedURLException e) {
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Invalid Wopi Server URL :"
																									+ alfrescoPrivateURL);
		}

		Map<String, Object> model = new HashMap<>(1);
		model.put(LOOL_HOST_URL, colServiceUrl.toString());
		return model;
	}

	public void setCollaboraOnlineService(CollaboraOnlineService collaboraOnlineService) {
		this.collaboraOnlineService = collaboraOnlineService;
	}

}
