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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.alfresco.error.AlfrescoRuntimeException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Load and parse the WopiDiscovery.xml file from Collabora Online
 */
public class WopiDiscovery {
	private static final String DEFAULT_HOSTING_DISCOVERY = "/hosting/discovery";

	private Document discoveryDoc;
	private URL wopiDiscoveryURL;
	private URL collaboraPrivateUrl;

	public void init() {
		try {
			this.wopiDiscoveryURL = new URL(this.collaboraPrivateUrl, DEFAULT_HOSTING_DISCOVERY);
		} catch (MalformedURLException e) {
			throw new AlfrescoRuntimeException(
					"Bas Wopi Discovery URI : " + this.collaboraPrivateUrl + "/" + DEFAULT_HOSTING_DISCOVERY, e);
		}
	}

	/**
	 * Return the srcurl for a given mimetype and action..
	 *
	 * @param mimeType
	 * @param action
	 * @return
	 */
	public String getSrcURL(String mimeType, String action) {
		loadDiscoveryXML();

		final XPathFactory xPathFactory = XPathFactory.newInstance();
		final XPath xPath = xPathFactory.newXPath();
		final String xPathExpr = ("/wopi-discovery/net-zone/app[@name='${mimeType}']/action[@name='${action}']/@urlsrc")
				.replace("${mimeType}", mimeType).replace("${action}", action);
		try {
			return xPath.evaluate(xPathExpr, this.discoveryDoc);
		} catch (XPathExpressionException e) {
			throw new AlfrescoRuntimeException(
					"wopi-discovery, XPath Error for mimeType=" + mimeType + " and action=" + action, e);
		}
	}

	/**
	 * Load discovery.xml from Collabora Online server
	 */
	private void loadDiscoveryXML() {
		if (this.discoveryDoc != null) {
			return;
		}

		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) this.wopiDiscoveryURL.openConnection();
			final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			// XML parsers should not be vulnerable to XXE attacks (java:S2755)
			builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			builderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			final DocumentBuilder builder = builderFactory.newDocumentBuilder();
			this.discoveryDoc = builder.parse(connection.getInputStream());
		} catch (IOException | SAXException | ParserConfigurationException e) {
			throw new AlfrescoRuntimeException("Can't load  discovery.xml : " + this.wopiDiscoveryURL.toString(), e);
		}

	}

	public void setCollaboraPrivateUrl(URL collaboraPrivateUrl) {
		this.collaboraPrivateUrl = collaboraPrivateUrl;
	}
}
