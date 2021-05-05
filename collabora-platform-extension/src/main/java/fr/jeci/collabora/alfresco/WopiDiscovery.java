package fr.jeci.collabora.alfresco;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.alfresco.error.AlfrescoRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Load and parse the WopiDiscovery.xml file from Collabora Online
 */
public class WopiDiscovery {
	private static final Log logger = LogFactory.getLog(WopiDiscovery.class);

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
	 * Return the srcurl for a given mimetype.
	 *
	 * @param mimeType
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
			logger.error("XPath Error return null", e);
		}
		return null;
	}

	private void loadDiscoveryXML() {
		if (this.discoveryDoc != null) {
			return;
		}

		HttpURLConnection connection;
		try {
			connection = (HttpURLConnection) this.wopiDiscoveryURL.openConnection();
			final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
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
