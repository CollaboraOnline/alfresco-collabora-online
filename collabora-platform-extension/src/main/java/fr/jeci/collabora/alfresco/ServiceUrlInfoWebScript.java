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
 *
 */
public class ServiceUrlInfoWebScript extends DeclarativeWebScript {
	private static final String COOL_HOST_URL = "cool_host_url";
	private static final String ALFRESCO_SERVICE = "service/";
	
	protected CollaboraOnlineService collaboraOnlineService;

	@Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		URL alfrescoPrivateURL = this.collaboraOnlineService.getAlfrescoPrivateURL();
		URL colServiceUrl;
		try {
			colServiceUrl = new URL(alfrescoPrivateURL, ALFRESCO_SERVICE);
		} catch (MalformedURLException e) {
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
					"Invalid Wopi Server URL :" + alfrescoPrivateURL);
		}

		Map<String, Object> model = new HashMap<>(1);
		model.put(COOL_HOST_URL, colServiceUrl.toString());
		return model;
	}

	public void setCollaboraOnlineService(CollaboraOnlineService collaboraOnlineService) {
		this.collaboraOnlineService = collaboraOnlineService;
	}

}