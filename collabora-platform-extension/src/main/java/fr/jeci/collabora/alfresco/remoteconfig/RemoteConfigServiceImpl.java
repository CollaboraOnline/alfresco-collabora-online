// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Implementation of {@link RemoteConfigService} storing configuration and fonts
 * in the Alfresco Data Dictionary under a configurable base path.
 */
public class RemoteConfigServiceImpl implements RemoteConfigService {

	private static final Logger logger = LoggerFactory.getLogger(RemoteConfigServiceImpl.class);

	private static final String CONFIG_FILE_NAME = "remote-config.json";
	private static final String FONTS_CONFIG_FILE_NAME = "fonts-config.json";
	private static final String FONTS_FOLDER_NAME = "fonts";
	private static final String JSON_MIMETYPE = "application/json";

	private String basePath;
	private String fontsBaseUrl;

	private NodeService nodeService;
	private ContentService contentService;
	private FileFolderService fileFolderService;
	private NodeLocatorService nodeLocatorService;

	private NodeRef baseFolderRef;
	private NodeRef fontsFolderRef;

	public void init() {
		AuthenticationUtil.runAsSystem(() -> {
			ensureFolderStructure();
			ensureDefaultConfig();
			regenerateFontsConfigFile();
			return null;
		});
		logger.info("Collabora remote config initialized at /Data Dictionary/{}", basePath);
	}

	private void ensureFolderStructure() {
		NodeRef companyHome = nodeLocatorService.getNode("companyhome", null, null);
		NodeRef dataDictionary = getDataDictionary(companyHome);
		baseFolderRef = getOrCreateFolder(dataDictionary, basePath);
		fontsFolderRef = getOrCreateFolder(baseFolderRef, FONTS_FOLDER_NAME);
	}

	private NodeRef getDataDictionary(NodeRef companyHome) {
		List<ChildAssociationRef> assocs = nodeService.getChildAssocs(companyHome, ContentModel.ASSOC_CONTAINS, QName
				.createQName(NamespaceService.APP_MODEL_1_0_URI, "dictionary"));
		if (assocs.isEmpty()) {
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
					"Data Dictionary (app:dictionary) not found");
		}
		return assocs.get(0)
				.getChildRef();
	}

	private void ensureDefaultConfig() {
		NodeRef configNode = nodeService.getChildByName(baseFolderRef, ContentModel.ASSOC_CONTAINS, CONFIG_FILE_NAME);
		if (configNode == null) {
			String fontsConfigUrl = fontsBaseUrl.replaceAll("/fonts$", "/fonts-config");
			String defaultConfig = "{\"kind\":\"configuration\",\"remote_font_config\":{\"url\":\"" + fontsConfigUrl
										  + "\"}}";
			createJsonFile(baseFolderRef, CONFIG_FILE_NAME, defaultConfig);
			logger.info("Created default remote-config.json");
		}
	}

	@Override
	public String getRemoteConfig() {
		return AuthenticationUtil.runAsSystem(() -> {
			NodeRef configNode = getConfigNode();
			return readContent(configNode);
		});
	}

	@Override
	public String getRemoteConfigETag() {
		return computeETag(getRemoteConfig());
	}

	@Override
	public void updateRemoteConfig(String jsonContent) {
		AuthenticationUtil.runAsSystem(() -> {
			NodeRef configNode = getConfigNode();
			writeContent(configNode, jsonContent, JSON_MIMETYPE);
			logger.info("Remote config updated");
			return null;
		});
	}

	@Override
	public String getFontsConfig() {
		return AuthenticationUtil.runAsSystem(() -> {
			NodeRef fontsConfigNode = nodeService.getChildByName(baseFolderRef, ContentModel.ASSOC_CONTAINS,
					FONTS_CONFIG_FILE_NAME);
			if (fontsConfigNode == null) {
				return regenerateFontsConfigFile();
			}
			return readContent(fontsConfigNode);
		});
	}

	@Override
	public String getFontsConfigETag() {
		return computeETag(getFontsConfig());
	}

	private String regenerateFontsConfigFile() {
		List<FileInfo> fonts = fileFolderService.listFiles(fontsFolderRef);
		StringBuilder sb = new StringBuilder();
		sb.append("{\"kind\":\"fontconfiguration\",\"server\":\"Alfresco\",\"fonts\":[");
		boolean first = true;
		for (FileInfo font : fonts) {
			if (!first) {
				sb.append(",");
			}
			first = false;
			String name = font.getName();
			Date modified = (Date) nodeService.getProperty(font.getNodeRef(), ContentModel.PROP_MODIFIED);
			String stamp = Long.toString(modified != null ? modified.getTime() : 0);
			sb.append("{\"uri\":\"")
					.append(fontsBaseUrl)
					.append("/")
					.append(name)
					.append("\",\"stamp\":\"")
					.append(stamp)
					.append("\"}");
		}
		sb.append("]}");

		String json = sb.toString();
		NodeRef fontsConfigNode = nodeService.getChildByName(baseFolderRef, ContentModel.ASSOC_CONTAINS,
				FONTS_CONFIG_FILE_NAME);
		if (fontsConfigNode != null) {
			writeContent(fontsConfigNode, json, JSON_MIMETYPE);
		} else {
			createJsonFile(baseFolderRef, FONTS_CONFIG_FILE_NAME, json);
		}
		logger.debug("Regenerated {}", FONTS_CONFIG_FILE_NAME);
		return json;
	}

	@Override
	public List<String> listFonts() {
		return AuthenticationUtil.runAsSystem(() -> {
			List<FileInfo> fonts = fileFolderService.listFiles(fontsFolderRef);
			List<String> names = new ArrayList<>(fonts.size());
			for (FileInfo font : fonts) {
				names.add(font.getName());
			}
			return names;
		});
	}

	@Override
	public ContentReader getFontContentReader(String fontName) {
		return AuthenticationUtil.runAsSystem(() -> {
			NodeRef fontNode = nodeService.getChildByName(fontsFolderRef, ContentModel.ASSOC_CONTAINS, fontName);
			if (fontNode == null) {
				throw new WebScriptException(Status.STATUS_NOT_FOUND, "Font not found: " + fontName);
			}
			ContentReader reader = contentService.getReader(fontNode, ContentModel.PROP_CONTENT);
			if (reader == null || !reader.exists()) {
				throw new WebScriptException(Status.STATUS_NOT_FOUND, "Font content not found: " + fontName);
			}
			return reader;
		});
	}

	@Override
	public void uploadFont(String fontName, InputStream content, String mimeType) {
		AuthenticationUtil.runAsSystem(() -> {
			NodeRef existing = nodeService.getChildByName(fontsFolderRef, ContentModel.ASSOC_CONTAINS, fontName);
			if (existing != null) {
				writeContent(existing, content, mimeType);
				logger.info("Updated font '{}'", fontName);
			} else {
				Map<QName, Serializable> props = new HashMap<>(1);
				props.put(ContentModel.PROP_NAME, fontName);

				NodeRef fontNode = nodeService.createNode(fontsFolderRef, ContentModel.ASSOC_CONTAINS, QName.createQName(
						NamespaceService.CONTENT_MODEL_1_0_URI, fontName), ContentModel.TYPE_CONTENT, props)
						.getChildRef();
				writeContent(fontNode, content, mimeType);
				logger.info("Uploaded font '{}'", fontName);
			}
			regenerateFontsConfigFile();
			return null;
		});
	}

	@Override
	public void deleteFont(String fontName) {
		AuthenticationUtil.runAsSystem(() -> {
			NodeRef fontNode = nodeService.getChildByName(fontsFolderRef, ContentModel.ASSOC_CONTAINS, fontName);
			if (fontNode == null) {
				throw new WebScriptException(Status.STATUS_NOT_FOUND, "Font not found: " + fontName);
			}
			nodeService.deleteNode(fontNode);
			logger.info("Deleted font '{}'", fontName);
			regenerateFontsConfigFile();
			return null;
		});
	}

	private NodeRef getConfigNode() {
		NodeRef configNode = nodeService.getChildByName(baseFolderRef, ContentModel.ASSOC_CONTAINS, CONFIG_FILE_NAME);
		if (configNode == null) {
			throw new WebScriptException(Status.STATUS_NOT_FOUND, "Remote config file not found");
		}
		return configNode;
	}

	private String readContent(NodeRef nodeRef) {
		ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
		if (reader == null || !reader.exists()) {
			return "{}";
		}
		return reader.getContentString();
	}

	private void writeContent(NodeRef nodeRef, String content, String mimeType) {
		ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
		writer.setMimetype(mimeType);
		writer.setEncoding(StandardCharsets.UTF_8.name());
		writer.putContent(content);
	}

	private void writeContent(NodeRef nodeRef, InputStream content, String mimeType) {
		ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
		writer.setMimetype(mimeType);
		writer.putContent(content);
	}

	private NodeRef createJsonFile(NodeRef parentRef, String name, String content) {
		Map<QName, Serializable> props = new HashMap<>(1);
		props.put(ContentModel.PROP_NAME, name);
		NodeRef nodeRef = nodeService.createNode(parentRef, ContentModel.ASSOC_CONTAINS, QName.createQName(
				NamespaceService.CONTENT_MODEL_1_0_URI, name), ContentModel.TYPE_CONTENT, props)
				.getChildRef();
		writeContent(nodeRef, content, JSON_MIMETYPE);
		return nodeRef;
	}

	private NodeRef getOrCreateFolder(NodeRef parentRef, String folderName) {
		NodeRef existing = nodeService.getChildByName(parentRef, ContentModel.ASSOC_CONTAINS, folderName);
		if (existing != null) {
			return existing;
		}
		return fileFolderService.create(parentRef, folderName, ContentModel.TYPE_FOLDER)
				.getNodeRef();
	}

	private static String computeETag(String content) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : digest) {
				sb.append(String.format("%02x", b));
			}
			return "\"" + sb + "\"";
		} catch (NoSuchAlgorithmException e) {
			return "\"" + content.hashCode() + "\"";
		}
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public void setFontsBaseUrl(String fontsBaseUrl) {
		this.fontsBaseUrl = normalizeUrl(fontsBaseUrl);
	}

	private static String normalizeUrl(String url) {
		int schemeEnd = url.indexOf("://");
		if (schemeEnd < 0) {
			return url.replaceAll("/+", "/");
		}
		String scheme = url.substring(0, schemeEnd + 3);
		String rest = url.substring(schemeEnd + 3)
				.replaceAll("/+", "/");
		if (rest.endsWith("/")) {
			rest = rest.substring(0, rest.length() - 1);
		}
		return scheme + rest;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}

	public void setNodeLocatorService(NodeLocatorService nodeLocatorService) {
		this.nodeLocatorService = nodeLocatorService;
	}
}
