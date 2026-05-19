// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Load and parse the WopiDiscovery.xml file from Collabora Online
 */
public class WopiDiscovery {
	private static final Logger logger = LoggerFactory.getLogger(WopiDiscovery.class);

	private static final String DEFAULT_HOSTING_DISCOVERY = "/hosting/discovery";
	private static final int READ_TIMEOUT_MS = 500;

	private Document discoveryDoc;
	private URL collaboraPrivateUrl;

	private List<DiscoveryApp> applications = Collections.emptyList();
	private Map<String, List<DiscoveryAction>> actions = Collections.emptyMap();
	private Map<String, DiscoveryAction> legacyActions = Collections.emptyMap();

	private final AtomicBoolean hasCollaboraOnline = new AtomicBoolean(false);

	public void init() {
		URL wopiDiscoveryURL;
		try {
			wopiDiscoveryURL = new URL(this.collaboraPrivateUrl, DEFAULT_HOSTING_DISCOVERY);
		} catch (java.net.MalformedURLException e) {
			logger.error("Invalid Collabora private URL: {}", this.collaboraPrivateUrl);
			return;
		}

		try {
			logger.info("Load Wopi Discovery URI : {}", wopiDiscoveryURL);
			URLConnection openConnection = wopiDiscoveryURL.openConnection();
			openConnection.setReadTimeout(READ_TIMEOUT_MS);
			loadDiscoveryXML(openConnection.getInputStream());
			this.hasCollaboraOnline.set(true);
		} catch (IOException | XMLStreamException e) {
			logger.warn("Can’t load Wopi Discovery URI : {}", wopiDiscoveryURL);
		}
	}

	public void reload() {
		this.discoveryDoc = null;
		this.hasCollaboraOnline.set(false);
		this.applications = Collections.emptyList();
		this.actions = Collections.emptyMap();
		this.legacyActions = Collections.emptyMap();
		init();
	}

	public boolean hasCollaboraOnline() {
		return this.hasCollaboraOnline.get();
	}

	public URL getCollaboraPrivateUrl() {
		return this.collaboraPrivateUrl;
	}

	/**
	 * Return the srcurl for a given mimetype and action..
	 *
	 * @deprecated You should use wopiDiscovery.getAction
	 */
	@Deprecated
	public String getSrcURL(String mimeType, String action) {
		if (action == null) {
			logger.warn("get srcURL for null action");
			return null;
		}
		if (mimeType == null) {
			logger.warn("get srcURL for null mimeType");
			return null;
		}

		DiscoveryAction discoveryAction = this.legacyActions.get(String.format("%s/%s", mimeType.toLowerCase(), action
				.toLowerCase()));
		if (discoveryAction == null) {
			return null;
		}
		return discoveryAction.urlsrc;
	}

	public Map<String, List<DiscoveryAction>> getActions() {
		return this.actions;
	}

	public List<DiscoveryAction> getAction(String extension) {
		if (extension == null) {
			logger.warn("get action for null extension");
			return Collections.emptyList();
		}
		return this.actions.get(extension.toLowerCase());
	}

	/**
	 * Load discovery.xml from Collabora Online server
	 */
	protected void loadDiscoveryXML(InputStream in) throws XMLStreamException {
		if (this.discoveryDoc != null) {
			return;
		}

		final XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_COALESCING, true);
		// Security - Disable DOCTYPE declarations (Prevent XML External Entity (XXE) attack)
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		// Security -  Disable external entity declarations  (Prevent XML External Entity (XXE) attack)
		factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);

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
						logger.warn("Bad xml format, app is null for action: {}", action);
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
					logger.debug("Not Used:{}", xr.getLocalName());
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

	public static class DiscoveryApp {
		private String name;
		private String favIconUrl;
		private final List<DiscoveryAction> actions = new ArrayList<>();

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("\n{");
			sb.append("\nname: \"")
					.append(name)
					.append("\", ");
			sb.append("\nfavIconUrl: \"")
					.append(favIconUrl)
					.append("\", ");
			sb.append("\nactions: [");
			for (DiscoveryAction action : actions) {
				sb.append(action)
						.append(',');
			}
			sb.append(']');
			return sb.toString();
		}
	}

	public static class DiscoveryAction {
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
			return String.format("{%n\tname: \"%s\", %n\text: \"%s\", %n\turlsrc: \"%s\"%n\t}", name, ext, urlsrc);
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
