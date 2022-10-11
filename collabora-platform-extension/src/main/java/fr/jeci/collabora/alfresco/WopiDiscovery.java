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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

/**
 * Load and parse the WopiDiscovery.xml file from Collabora Online
 * 
 */
public class WopiDiscovery {
	private static final Log logger = LogFactory.getLog(WopiDiscovery.class);

	private static final String DEFAULT_HOSTING_DISCOVERY = "/hosting/discovery";
	private static final int READ_TIMEOUT_MS = 500;

	private Document discoveryDoc;
	private URL collaboraPrivateUrl;

	private List<DiscoveryApp> applications = Collections.emptyList();
	private Map<String, List<DiscoveryAction>> actions = Collections.emptyMap();
	private Map<String, DiscoveryAction> legacyActions = Collections.emptyMap();

	private final AtomicBoolean hasCollaboraOnline = new AtomicBoolean(false);

	public void init() {
		try {
			URL wopiDiscoveryURL = new URL(this.collaboraPrivateUrl, DEFAULT_HOSTING_DISCOVERY);
			URLConnection openConnection = wopiDiscoveryURL.openConnection();
			openConnection.setReadTimeout(READ_TIMEOUT_MS);
			loadDiscoveryXML(openConnection.getInputStream());
			this.hasCollaboraOnline.set(true);
		} catch (IOException | XMLStreamException e) {
			logger.warn("Can't load Wopi Discovery URI : " + this.collaboraPrivateUrl + "/" + DEFAULT_HOSTING_DISCOVERY);
		}
	}

	public boolean hasCollaboraOnline() {
		return this.hasCollaboraOnline.get();
	}

	public void hasCollaboraOnline(boolean online) {
		this.hasCollaboraOnline.set(online);
	}

	/**
	 * Return the srcurl for a given mimetype and action..
	 * 
	 * @deprecated
	 * 
	 * @param mimeType
	 * @param action
	 * @return
	 */
	public String getSrcURL(String mimeType, String action) {
		DiscoveryAction discoveryAction = this.legacyActions.get(String.format("%s/%s", mimeType, action));
		return discoveryAction.urlsrc;
	}

	public Map<String, List<DiscoveryAction>> getActions() {
		return this.actions;
	}

	public List<DiscoveryAction> getAction(String extension) {
		return this.actions.get(extension);
	}

	/**
	 * Load discovery.xml from Collabora Online server
	 * 
	 * @throws XMLStreamException
	 * @throws IOException
	 */
	protected void loadDiscoveryXML(InputStream in) throws XMLStreamException, IOException {
		if (this.discoveryDoc != null) {
			return;
		}

		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_COALESCING, true);

		XMLStreamReader xr = factory.createXMLStreamReader(in);

		List<DiscoveryApp> mApplications = new ArrayList<>(7);
		Map<String, List<DiscoveryAction>> mActions = new HashMap<>();
		Map<String, DiscoveryAction> mLegacyActions = new HashMap<>();

		DiscoveryApp app = null;
		DiscoveryAction action = null;
		while (xr.hasNext()) {
			int next = xr.next();
			if (next == XMLStreamConstants.START_ELEMENT) {

				switch (xr.getLocalName()) {
				case "app":
					app = new DiscoveryApp();
					app.name = xr.getAttributeValue(null, "name");
					app.favIconUrl = xr.getAttributeValue(null, "favIconUrl");

					if (!app.name.contains("/")) {
						mApplications.add(app);
					}
					break;

				case "action":
					action = new DiscoveryAction();
					action.ext = xr.getAttributeValue(null, "ext");
					action.name = xr.getAttributeValue(null, "name");
					action.urlsrc = xr.getAttributeValue(null, "urlsrc");

					if (app == null) {
						logger.warn("Bad xml format, app is null for action: " + action);
						break;
					}

					if (app.name.contains("/")) {
						// legacy format
						mLegacyActions.put(String.format("%s/%s", app.name, action.name), action);
					} else {
						app.actions.add(action);

						List<DiscoveryAction> extActions = mActions.get(action.ext);
						if (extActions == null) {
							extActions = new ArrayList<>(1);
							mActions.put(action.ext, extActions);
						}
						extActions.add(action);
					}
					break;

				default:
					if (logger.isDebugEnabled()) {
						logger.debug("Not Used:" + xr.getLocalName());
					}
					break;
				}

			}
		}

		this.applications = mApplications;
		this.actions = mActions;
		this.legacyActions = mLegacyActions;
	}

	public List<DiscoveryApp> getApplications() {
		return applications;
	}

	class DiscoveryApp {
		private String name;
		private String favIconUrl;
		private List<DiscoveryAction> actions = new ArrayList<>();

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("\n{");
			sb.append("\nname: \"").append(name).append("\", ");
			sb.append("\nfavIconUrl: \"").append(favIconUrl).append("\", ");
			sb.append("\nactions: [");
			for (DiscoveryAction action : actions) {
				sb.append(action).append(',');
			}
			sb.append(']');
			return sb.toString();
		}
	}

	class DiscoveryAction {
		private String ext;
		private String name;
		private String urlsrc;

		DiscoveryAction() {
			// empty constructor
		}

		DiscoveryAction(String ext, String name, String urlsrc) {
			this.ext = ext;
			this.name = name;
			this.urlsrc = urlsrc;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("{");
			sb.append("\n\tname: \"").append(name).append("\", ");
			sb.append("\n\text: \"").append(ext).append("\", ");
			sb.append("\n\turlsrc: \"").append(urlsrc).append("\"\n\t}");

			return sb.toString();
		}

		public String getExt() {
			return ext;
		}

		public String getName() {
			return name;
		}

		public String getUrlsrc() {
			return urlsrc;
		}
	}

	public void setCollaboraPrivateUrl(URL collaboraPrivateUrl) {
		this.collaboraPrivateUrl = collaboraPrivateUrl;
	}
}
