// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.extensions.webscripts.WebScriptException;

import fr.jeci.collabora.alfresco.settings.CollaboraSettingsServiceImpl.ParsedPath;

public class CollaboraSettingsServiceImplTest {

	private CollaboraSettingsServiceImpl service;
	private AuthorityService authorityService;
	private NodeService nodeService;
	private FileFolderService fileFolderService;

	@Before
	public void setUp() {
		service = new CollaboraSettingsServiceImpl();
		authorityService = mock(AuthorityService.class);
		nodeService = mock(NodeService.class);
		fileFolderService = mock(FileFolderService.class);
		service.setAuthorityService(authorityService);
		service.setNodeService(nodeService);
		service.setFileFolderService(fileFolderService);
		service.setWopiBaseUrl("https://acs.example.com/s/wopi/settings");
	}

	// ========== Path parsing ==========

	@Test
	public void testParse_userConfigPath() {
		ParsedPath path = service.parse("/settings/userconfig/wordbook/en_US.dic");

		assertEquals(CollaboraSettingsService.TYPE_USERCONFIG, path.type);
		assertEquals(List.of("wordbook"), path.folders);
		assertEquals("en_US.dic", path.filename);
	}

	@Test
	public void testParse_systemConfigBrowserSetting() {
		ParsedPath path = service.parse("/settings/systemconfig/browsersetting/browsersetting.json");

		assertEquals(CollaboraSettingsService.TYPE_SYSTEMCONFIG, path.type);
		assertEquals(List.of("browsersetting"), path.folders);
		assertEquals("browsersetting.json", path.filename);
	}

	@Test
	public void testParse_nestedCategories() {
		ParsedPath path = service.parse("/settings/userconfig/autotext/group/entry.bau");

		assertEquals(List.of("autotext", "group"), path.folders);
		assertEquals("entry.bau", path.filename);
		assertEquals("/settings/userconfig/autotext/group/entry.bau", path.virtualPath());
	}

	@Test(expected = WebScriptException.class)
	public void testParse_rejectsMissingPrefix() {
		service.parse("/foo/userconfig/wordbook/en_US.dic");
	}

	@Test(expected = WebScriptException.class)
	public void testParse_rejectsUnknownType() {
		service.parse("/settings/bogusconfig/wordbook/en_US.dic");
	}

	@Test(expected = WebScriptException.class)
	public void testParse_rejectsTraversal() {
		service.parse("/settings/userconfig/../../etc/passwd");
	}

	@Test(expected = WebScriptException.class)
	public void testParse_rejectsTooShort() {
		service.parse("/settings/userconfig/en_US.dic");
	}

	@Test(expected = WebScriptException.class)
	public void testParse_rejectsNull() {
		service.parse(null);
	}

	@Test(expected = WebScriptException.class)
	public void testParse_rejectsEmptyString() {
		service.parse("");
	}

	@Test(expected = WebScriptException.class)
	public void testParse_rejectsPrefixOnly() {
		service.parse("/settings/");
	}

	@Test(expected = WebScriptException.class)
	public void testParse_rejectsTypeCaseSensitive() {
		// types are exact: "UserConfig" is not "userconfig"
		service.parse("/settings/UserConfig/wordbook/en_US.dic");
	}

	@Test(expected = WebScriptException.class)
	public void testParse_rejectsBlankMiddleSegment() {
		// a double slash yields a blank segment
		service.parse("/settings/userconfig//en_US.dic");
	}

	@Test(expected = WebScriptException.class)
	public void testParse_rejectsCurrentDirSegment() {
		service.parse("/settings/userconfig/./en_US.dic");
	}

	@Test
	public void testParse_filenameWithMultipleDots() {
		ParsedPath path = service.parse("/settings/userconfig/wordbook/fr.FR.v2.dic");
		assertEquals("fr.FR.v2.dic", path.filename);
		assertEquals(List.of("wordbook"), path.folders);
	}

	@Test
	public void testParse_virtualPathRoundTrip() {
		String original = "/settings/systemconfig/autotext/team/snippet.bau";
		assertEquals(original, service.parse(original)
				.virtualPath());
	}

	// ========== Download URI ==========

	/**
	 * Invoke the private buildDownloadUri(String, String) via reflection.
	 */
	private String invokeBuildDownloadUri(String virtualPath, String accessToken) throws Exception {
		Method method = CollaboraSettingsServiceImpl.class.getDeclaredMethod("buildDownloadUri", String.class,
				String.class);
		method.setAccessible(true);
		return (String) method.invoke(service, virtualPath, accessToken);
	}

	@Test
	public void testBuildDownloadUri_encodesFileIdAndEmbedsToken() throws Exception {
		String uri = invokeBuildDownloadUri("/settings/userconfig/wordbook/en US.dic", "tok+1");

		assertTrue("uri should target the download endpoint", uri.startsWith(
				"https://acs.example.com/s/wopi/settings/download?fileId="));
		assertTrue("path separators should be percent-encoded", uri.contains("%2Fsettings%2Fuserconfig"));
		assertTrue("space should be encoded", uri.contains("en+US.dic") || uri.contains("en%20US.dic"));
		assertTrue("access token is embedded and encoded", uri.contains("&access_token=tok%2B1"));
		// Collabora derives the wordbook/autotext destination filename from the file_name parameter.
		assertTrue("file_name carries the real filename", uri.contains("&file_name=en+US.dic") || uri.contains(
				"&file_name=en%20US.dic"));
	}

	@Test
	public void testBuildDownloadUri_omitsTokenWhenBlank() throws Exception {
		String uri = invokeBuildDownloadUri("/settings/userconfig/wordbook/en_US.dic", null);

		assertFalse("no access_token when none provided", uri.contains("access_token"));
		// file_name and fileId are always present, even without a token
		assertTrue("fileId present", uri.contains("fileId="));
		assertTrue("file_name present", uri.contains("&file_name=en_US.dic"));
	}

	@Test
	public void testBuildDownloadUri_blankTokenTreatedAsAbsent() throws Exception {
		String uri = invokeBuildDownloadUri("/settings/userconfig/wordbook/en_US.dic", "   ");
		assertFalse("blank token is not appended", uri.contains("access_token"));
	}

	@Test
	public void testBuildDownloadUri_sharedConfigPath() throws Exception {
		String uri = invokeBuildDownloadUri("/settings/systemconfig/xcu/documentView.xcu", "t");
		assertTrue(uri.contains("%2Fsettings%2Fsystemconfig%2Fxcu%2FdocumentView.xcu"));
		assertTrue(uri.contains("&file_name=documentView.xcu"));
	}

	@Test
	public void testSettingsUrl_format() {
		String url = service.settingsUrl(CollaboraSettingsService.TYPE_USERCONFIG, "tok en/+");

		assertTrue("targets the settings endpoint", url.startsWith("https://acs.example.com/s/wopi/settings?"));
		assertTrue("carries the type", url.contains("type=userconfig"));
		assertTrue("carries fileId=-1", url.contains("fileId=-1"));
		assertTrue("access token is encoded", url.contains("access_token=tok+en%2F%2B"));
	}

	@Test
	public void testSettingsUrl_systemConfig() {
		String url = service.settingsUrl(CollaboraSettingsService.TYPE_SYSTEMCONFIG, "tok");
		assertTrue(url.contains("type=systemconfig"));
		assertTrue(url.contains("fileId=-1"));
		assertTrue(url.contains("access_token=tok"));
	}

	@Test
	public void testSettingsUrl_nullTokenYieldsEmpty() {
		String url = service.settingsUrl(CollaboraSettingsService.TYPE_USERCONFIG, null);
		assertTrue("null token becomes empty access_token", url.contains("access_token=&") || url.endsWith(
				"access_token="));
	}

	@Test(expected = WebScriptException.class)
	public void testSettingsUrl_rejectsUnknownType() {
		service.settingsUrl("bogus", "tok");
	}

	// ========== Base URL normalization ==========

	@Test
	public void testWopiBaseUrl_alreadyCleanUnchanged() {
		service.setWopiBaseUrl("https://acs.example.com/alfresco/s/wopi/settings");
		assertEquals("https://acs.example.com/alfresco/s/wopi/settings", service.getWopiBaseUrl());
	}

	@Test
	public void testWopiBaseUrl_dropsTrailingSlash() {
		service.setWopiBaseUrl("https://acs.example.com/s/wopi/settings/");
		assertEquals("https://acs.example.com/s/wopi/settings", service.getWopiBaseUrl());
	}

	@Test
	public void testWopiBaseUrl_collapsesInternalSlashesWithoutTouchingScheme() {
		service.setWopiBaseUrl("https://acs.example.com//alfresco///s/wopi/settings");
		assertEquals("https://acs.example.com/alfresco/s/wopi/settings", service.getWopiBaseUrl());
	}

	@Test
	public void testWopiBaseUrl_noScheme() {
		service.setWopiBaseUrl("/alfresco//s/wopi/settings");
		assertEquals("/alfresco/s/wopi/settings", service.getWopiBaseUrl());
	}

	@Test
	public void testWopiBaseUrl_collapsesDoubleSlash() {
		// alfresco.public.url often ends with "/", which would yield /alfresco//s/wopi/settings
		service.setWopiBaseUrl("https://acs.example.com/alfresco//s/wopi/settings/");

		String url = service.settingsUrl(CollaboraSettingsService.TYPE_SYSTEMCONFIG, "tok");
		assertTrue("no double slash after the host: " + url, url.startsWith(
				"https://acs.example.com/alfresco/s/wopi/settings?"));
		assertFalse("scheme slashes preserved, path slashes collapsed", url.replaceFirst("https://", "")
				.contains("//"));
	}

	@Test
	public void testGuessMimeType() throws Exception {
		Method method = CollaboraSettingsServiceImpl.class.getDeclaredMethod("guessMimeType", String.class);
		method.setAccessible(true);

		assertEquals("text/plain", method.invoke(null, "standard.dic"));
		assertEquals("application/json", method.invoke(null, "browsersetting.json"));
		assertEquals("application/xml", method.invoke(null, "documentView.xcu"));
		assertEquals("application/xml", method.invoke(null, "config.xml"));
		assertEquals("application/octet-stream", method.invoke(null, "entry.bau"));
		assertEquals("application/octet-stream", method.invoke(null, "noextension"));
		// extension matching is case-insensitive
		assertEquals("text/plain", method.invoke(null, "STANDARD.DIC"));
		assertEquals("application/json", method.invoke(null, "Browsersetting.JSON"));
	}

	// ========== Settings stamp (maxModified) ==========

	private String invokeMaxModified(NodeRef root) throws Exception {
		Method m = CollaboraSettingsServiceImpl.class.getDeclaredMethod("maxModified", NodeRef.class);
		m.setAccessible(true);
		return (String) m.invoke(service, root);
	}

	private FileInfo fileInfo(NodeRef ref) {
		FileInfo fi = mock(FileInfo.class);
		when(fi.getNodeRef()).thenReturn(ref);
		return fi;
	}

	@Test
	public void testMaxModified_picksMostRecentAcrossCategories() throws Exception {
		NodeRef root = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "root");
		NodeRef wordbook = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "wordbook");
		NodeRef xcu = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "xcu");
		NodeRef f1 = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "f1");
		NodeRef f2 = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "f2");
		NodeRef f3 = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "f3");

		// Build the FileInfo mocks first: stubbing inside a thenReturn(...) argument triggers UnfinishedStubbing.
		FileInfo wordbookFolder = fileInfo(wordbook);
		FileInfo xcuFolder = fileInfo(xcu);
		FileInfo file1 = fileInfo(f1);
		FileInfo file2 = fileInfo(f2);
		FileInfo file3 = fileInfo(f3);

		when(fileFolderService.listFolders(root)).thenReturn(List.of(wordbookFolder, xcuFolder));
		when(fileFolderService.listFiles(wordbook)).thenReturn(List.of(file1, file2));
		when(fileFolderService.listFiles(xcu)).thenReturn(List.of(file3));
		when(nodeService.getProperty(f1, ContentModel.PROP_MODIFIED)).thenReturn(new Date(1000L));
		when(nodeService.getProperty(f2, ContentModel.PROP_MODIFIED)).thenReturn(new Date(9000L));
		when(nodeService.getProperty(f3, ContentModel.PROP_MODIFIED)).thenReturn(new Date(5000L));

		assertEquals("9000", invokeMaxModified(root));
	}

	@Test
	public void testMaxModified_nullRootIsZero() throws Exception {
		assertEquals("0", invokeMaxModified(null));
	}

	@Test
	public void testMaxModified_emptyRootIsZero() throws Exception {
		NodeRef root = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "empty");
		when(fileFolderService.listFolders(root)).thenReturn(List.of());
		assertEquals("0", invokeMaxModified(root));
	}

	// ========== Shared settings authorization ==========

	@Test(expected = WebScriptException.class)
	public void testUploadSystemConfig_deniedForNonAdmin() {
		when(authorityService.hasAdminAuthority()).thenReturn(false);

		InputStream content = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
		service.uploadSettingsFile("/settings/systemconfig/wordbook/en_US.dic", content, null, "tok");
	}

	@Test
	public void testDeleteSystemConfig_deniedForNonAdmin_doesNotTouchRepository() {
		when(authorityService.hasAdminAuthority()).thenReturn(false);

		try {
			service.deleteSettingsFile("/settings/systemconfig/wordbook/en_US.dic");
		} catch (WebScriptException expected) {
			// admin gate must fire before any node access
		}

		verify(nodeService, never()).deleteNode(org.mockito.ArgumentMatchers.any());
	}
}
