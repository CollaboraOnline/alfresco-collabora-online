// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.settings;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

/**
 * Implementation of {@link CollaboraSettingsService} persisting settings files in the Alfresco repository.
 * <p>
 * Shared (systemconfig) settings live under the Data Dictionary and are resolved once at startup. Per-user
 * (userconfig) settings live under each user's home folder and are resolved per call, under the currently
 * authenticated user.
 */
public class CollaboraSettingsServiceImpl implements CollaboraSettingsService {

	private static final Logger logger = LoggerFactory.getLogger(CollaboraSettingsServiceImpl.class);

	private static final String SHARED_FOLDER_NAME = "shared-settings";
	private static final String KIND_SHARED = "shared";
	private static final String KIND_USER = "user";

	private String basePath;
	private String userPath;
	private String wopiBaseUrl;

	private NodeService nodeService;
	private ContentService contentService;
	private FileFolderService fileFolderService;
	private NodeLocatorService nodeLocatorService;
	private AuthorityService authorityService;
	private VersionService versionService;

	private NodeRef sharedRootRef;

	public void init() {
		AuthenticationUtil.runAsSystem(() -> {
			NodeRef companyHome = nodeLocatorService.getNode("companyhome", null, null);
			NodeRef dataDictionary = getDataDictionary(companyHome);
			NodeRef baseFolder = getOrCreateFolder(dataDictionary, basePath);
			this.sharedRootRef = getOrCreateFolder(baseFolder, SHARED_FOLDER_NAME);
			return null;
		});
		logger.info("Collabora settings initialized: shared=/Data Dictionary/{}/{}, user=<userhome>/{}", basePath,
				SHARED_FOLDER_NAME, userPath);
	}

	@Override
	public SettingsListing listSettings(String type) {
		validateType(type);
		if (TYPE_SYSTEMCONFIG.equals(type)) {
			return AuthenticationUtil.runAsSystem(() -> listFrom(type, sharedRootRef, KIND_SHARED));
		}
		return listFrom(type, getUserRoot(false), KIND_USER);
	}

	private SettingsListing listFrom(String type, NodeRef root, String kind) {
		if (root == null) {
			return new SettingsListing(kind, new LinkedHashMap<>());
		}

		final Map<String, List<SettingFile>> categories = new LinkedHashMap<>();
		for (FileInfo categoryFolder : fileFolderService.listFolders(root)) {
			String category = categoryFolder.getName();
			List<SettingFile> files = new ArrayList<>();
			for (FileInfo file : fileFolderService.listFiles(categoryFolder.getNodeRef())) {
				String virtualPath = "%s%s/%s/%s".formatted(SETTINGS_PREFIX, type, category, file.getName());
				files.add(new SettingFile(computeStamp(file.getNodeRef()), buildDownloadUri(virtualPath)));
			}
			categories.put(category, files);
		}
		return new SettingsListing(kind, categories);
	}

	@Override
	public ContentReader getSettingsFile(String fileId) {
		ParsedPath path = parse(fileId);
		if (TYPE_SYSTEMCONFIG.equals(path.type)) {
			return AuthenticationUtil.runAsSystem(() -> readerFor(path, sharedRootRef));
		}
		return readerFor(path, getUserRoot(false));
	}

	private ContentReader readerFor(ParsedPath path, NodeRef root) {
		NodeRef fileNode = resolveFile(root, path, false);
		if (fileNode == null) {
			throw new WebScriptException(Status.STATUS_NOT_FOUND, "Settings file not found: " + path.filename);
		}
		ContentReader reader = contentService.getReader(fileNode, ContentModel.PROP_CONTENT);
		if (reader == null || !reader.exists()) {
			throw new WebScriptException(Status.STATUS_NOT_FOUND, "Settings content not found: " + path.filename);
		}
		return reader;
	}

	@Override
	public SettingFile uploadSettingsFile(String fileId, InputStream content, String mimeType) {
		ParsedPath path = parse(fileId);
		String mime = (mimeType != null && !mimeType.isBlank()) ? mimeType : guessMimeType(path.filename);

		if (TYPE_SYSTEMCONFIG.equals(path.type)) {
			requireAdmin();
			return AuthenticationUtil.runAsSystem(() -> doUpload(path, sharedRootRef, content, mime));
		}
		return doUpload(path, getUserRoot(true), content, mime);
	}

	private SettingFile doUpload(ParsedPath path, NodeRef root, InputStream content, String mime) {
		NodeRef folder = getOrCreateFolderPath(root, path.folders);
		NodeRef fileNode = nodeService.getChildByName(folder, ContentModel.ASSOC_CONTAINS, path.filename);
		if (fileNode == null) {
			Map<QName, Serializable> props = new LinkedHashMap<>(1);
			props.put(ContentModel.PROP_NAME, path.filename);
			fileNode = nodeService.createNode(folder, ContentModel.ASSOC_CONTAINS, QName.createQName(
					NamespaceService.CONTENT_MODEL_1_0_URI, path.filename), ContentModel.TYPE_CONTENT, props)
					.getChildRef();
		}
		writeContent(fileNode, content, mime);
		ensureVersioningEnabled(fileNode);
		logger.info("Stored settings file {}", path.virtualPath());
		return new SettingFile(computeStamp(fileNode), buildDownloadUri(path.virtualPath()));
	}

	@Override
	public void deleteSettingsFile(String fileId) {
		ParsedPath path = parse(fileId);
		if (TYPE_SYSTEMCONFIG.equals(path.type)) {
			requireAdmin();
			AuthenticationUtil.runAsSystem(() -> {
				doDelete(path, sharedRootRef);
				return null;
			});
		} else {
			doDelete(path, getUserRoot(false));
		}
	}

	private void doDelete(ParsedPath path, NodeRef root) {
		NodeRef fileNode = resolveFile(root, path, false);
		if (fileNode == null) {
			throw new WebScriptException(Status.STATUS_NOT_FOUND, "Settings file not found: " + path.filename);
		}
		nodeService.deleteNode(fileNode);
		logger.info("Deleted settings file {}", path.virtualPath());
	}

	// ---------------------------------------------------------------------
	// Path handling
	// ---------------------------------------------------------------------

	/**
	 * Parsed representation of a {@code /settings/{type}/{folders...}/{filename}} virtual path.
	 */
	static final class ParsedPath {
		final String type;
		final List<String> folders;
		final String filename;

		ParsedPath(String type, List<String> folders, String filename) {
			this.type = type;
			this.folders = folders;
			this.filename = filename;
		}

		String virtualPath() {
			return "%s%s/%s/%s".formatted(SETTINGS_PREFIX, type, String.join("/", folders), filename);
		}
	}

	/**
	 * Parse and validate a settings {@code fileId}. Rejects unknown types, blank or {@code ..} segments and any path
	 * not anchored under {@link #SETTINGS_PREFIX}.
	 */
	ParsedPath parse(String fileId) {
		if (fileId == null || !fileId.startsWith(SETTINGS_PREFIX)) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid settings fileId: " + fileId);
		}
		String[] segments = fileId.substring(SETTINGS_PREFIX.length())
				.split("/");
		// Need at least: type / category / filename
		if (segments.length < 3) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid settings fileId: " + fileId);
		}
		String type = segments[0];
		validateType(type);

		List<String> rest = new ArrayList<>(Arrays.asList(segments)
				.subList(1, segments.length));
		String filename = rest.remove(rest.size() - 1);
		for (String segment : segments) {
			if (segment.isBlank() || "..".equals(segment) || ".".equals(segment)) {
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid path segment in fileId: " + fileId);
			}
		}
		return new ParsedPath(type, rest, filename);
	}

	private void validateType(String type) {
		if (!TYPE_USERCONFIG.equals(type) && !TYPE_SYSTEMCONFIG.equals(type)) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Unknown settings type: " + type);
		}
	}

	private NodeRef resolveFile(NodeRef root, ParsedPath path, boolean create) {
		NodeRef folder = create ? getOrCreateFolderPath(root, path.folders) : getFolderPath(root, path.folders);
		if (folder == null) {
			return null;
		}
		return nodeService.getChildByName(folder, ContentModel.ASSOC_CONTAINS, path.filename);
	}

	/**
	 * Resolve the per-user settings root under the current user's home folder.
	 *
	 * @param create when {@code true}, create the {@code userPath} folders if missing
	 * @return the user settings root, or {@code null} when {@code create} is {@code false} and it does not exist
	 */
	private NodeRef getUserRoot(boolean create) {
		NodeRef userHome = nodeLocatorService.getNode("userhome", null, null);
		if (userHome == null) {
			throw new WebScriptException(Status.STATUS_NOT_FOUND, "No home folder for current user");
		}
		List<String> folders = Arrays.asList(userPath.split("/"));
		return create ? getOrCreateFolderPath(userHome, folders) : getFolderPath(userHome, folders);
	}

	private NodeRef getFolderPath(NodeRef parent, List<String> folders) {
		NodeRef current = parent;
		for (String folder : folders) {
			if (folder.isBlank()) {
				continue;
			}
			current = nodeService.getChildByName(current, ContentModel.ASSOC_CONTAINS, folder);
			if (current == null) {
				return null;
			}
		}
		return current;
	}

	private NodeRef getOrCreateFolderPath(NodeRef parent, List<String> folders) {
		NodeRef current = parent;
		for (String folder : folders) {
			if (folder.isBlank()) {
				continue;
			}
			current = getOrCreateFolder(current, folder);
		}
		return current;
	}

	private NodeRef getOrCreateFolder(NodeRef parentRef, String folderName) {
		NodeRef existing = nodeService.getChildByName(parentRef, ContentModel.ASSOC_CONTAINS, folderName);
		if (existing != null) {
			return existing;
		}
		return fileFolderService.create(parentRef, folderName, ContentModel.TYPE_FOLDER)
				.getNodeRef();
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

	// ---------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------

	private void requireAdmin() {
		if (!authorityService.hasAdminAuthority()) {
			throw new WebScriptException(Status.STATUS_FORBIDDEN,
					"Administrator rights required to manage shared settings");
		}
	}

	private void ensureVersioningEnabled(NodeRef nodeRef) {
		if (!nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE)) {
			Map<QName, Serializable> props = new LinkedHashMap<>(2);
			props.put(ContentModel.PROP_AUTO_VERSION, true);
			props.put(ContentModel.PROP_AUTO_VERSION_PROPS, false);
			versionService.ensureVersioningEnabled(nodeRef, props);
		}
	}

	private void writeContent(NodeRef nodeRef, InputStream content, String mimeType) {
		ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
		writer.setMimetype(mimeType);
		writer.setEncoding(StandardCharsets.UTF_8.name());
		writer.putContent(content);
	}

	private String computeStamp(NodeRef nodeRef) {
		Date modified = (Date) nodeService.getProperty(nodeRef, ContentModel.PROP_MODIFIED);
		return Long.toString(modified != null ? modified.getTime() : 0);
	}

	private String buildDownloadUri(String virtualPath) {
		// wopiBaseUrl is the full /wopi/settings endpoint; the download operation is "<base>/download".
		return wopiBaseUrl + "/download?fileId=" + URLEncoder.encode(virtualPath, StandardCharsets.UTF_8);
	}

	@Override
	public String settingsUrl(String type, String accessToken) {
		validateType(type);
		return "%s?access_token=%s&type=%s&fileId=-1".formatted(wopiBaseUrl, URLEncoder.encode(accessToken == null
				? ""
				: accessToken, StandardCharsets.UTF_8), type);
	}

	@Override
	public String settingsStamp(String type) {
		validateType(type);
		if (TYPE_SYSTEMCONFIG.equals(type)) {
			return AuthenticationUtil.runAsSystem(() -> maxModified(sharedRootRef));
		}
		return maxModified(getUserRoot(false));
	}

	/**
	 * Most recent {@code cm:modified} (millis) across all settings files under the given root, or {@code "0"} when the
	 * root is missing or empty.
	 */
	private String maxModified(NodeRef root) {
		long max = 0;
		if (root != null) {
			for (FileInfo categoryFolder : fileFolderService.listFolders(root)) {
				for (FileInfo file : fileFolderService.listFiles(categoryFolder.getNodeRef())) {
					Date modified = (Date) nodeService.getProperty(file.getNodeRef(), ContentModel.PROP_MODIFIED);
					if (modified != null && modified.getTime() > max) {
						max = modified.getTime();
					}
				}
			}
		}
		return Long.toString(max);
	}

	private static String guessMimeType(String fileName) {
		int dot = fileName.lastIndexOf('.');
		if (dot >= 0) {
			switch (fileName.substring(dot + 1)
					.toLowerCase()) {
			case "json":
				return "application/json";
			case "xml":
			case "xcu":
				return "application/xml";
			default:
				break;
			}
		}
		return "application/octet-stream";
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public void setUserPath(String userPath) {
		this.userPath = userPath;
	}

	public void setWopiBaseUrl(String wopiBaseUrl) {
		this.wopiBaseUrl = normalizeUrl(wopiBaseUrl);
	}

	/**
	 * Collapse duplicate slashes (except after the scheme) and drop any trailing slash, so a base built from a
	 * trailing-slashed {@code alfresco.public.url} does not yield {@code /alfresco//s/wopi/settings}.
	 */
	private static String normalizeUrl(String url) {
		if (url == null) {
			return null;
		}
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

	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}

	public void setVersionService(VersionService versionService) {
		this.versionService = versionService;
	}
}
