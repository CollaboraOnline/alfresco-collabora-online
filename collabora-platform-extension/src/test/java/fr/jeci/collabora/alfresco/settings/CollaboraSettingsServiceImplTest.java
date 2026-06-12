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
import java.util.List;

import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.extensions.webscripts.WebScriptException;

import fr.jeci.collabora.alfresco.settings.CollaboraSettingsServiceImpl.ParsedPath;

public class CollaboraSettingsServiceImplTest {

	private CollaboraSettingsServiceImpl service;
	private AuthorityService authorityService;
	private NodeService nodeService;

	@Before
	public void setUp() {
		service = new CollaboraSettingsServiceImpl();
		authorityService = mock(AuthorityService.class);
		nodeService = mock(NodeService.class);
		service.setAuthorityService(authorityService);
		service.setNodeService(nodeService);
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

	// ========== Download URI ==========

	/**
	 * Invoke the private buildDownloadUri(String) via reflection.
	 */
	private String invokeBuildDownloadUri(String virtualPath) throws Exception {
		Method method = CollaboraSettingsServiceImpl.class.getDeclaredMethod("buildDownloadUri", String.class);
		method.setAccessible(true);
		return (String) method.invoke(service, virtualPath);
	}

	@Test
	public void testBuildDownloadUri_encodesFileId() throws Exception {
		String uri = invokeBuildDownloadUri("/settings/userconfig/wordbook/en US.dic");

		assertTrue("uri should target the download endpoint", uri.startsWith(
				"https://acs.example.com/s/wopi/settings/download?fileId="));
		assertTrue("path separators should be percent-encoded", uri.contains("%2Fsettings%2Fuserconfig"));
		assertTrue("space should be encoded", uri.contains("en+US.dic") || uri.contains("en%20US.dic"));
	}

	@Test
	public void testSettingsUrl_format() {
		String url = service.settingsUrl(CollaboraSettingsService.TYPE_USERCONFIG, "tok en/+");

		assertTrue("targets the settings endpoint", url.startsWith("https://acs.example.com/s/wopi/settings?"));
		assertTrue("carries the type", url.contains("type=userconfig"));
		assertTrue("carries fileId=-1", url.contains("fileId=-1"));
		assertTrue("access token is encoded", url.contains("access_token=tok+en%2F%2B"));
	}

	@Test(expected = WebScriptException.class)
	public void testSettingsUrl_rejectsUnknownType() {
		service.settingsUrl("bogus", "tok");
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

	// ========== Shared settings authorization ==========

	@Test(expected = WebScriptException.class)
	public void testUploadSystemConfig_deniedForNonAdmin() {
		when(authorityService.hasAdminAuthority()).thenReturn(false);

		InputStream content = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));
		service.uploadSettingsFile("/settings/systemconfig/wordbook/en_US.dic", content, null);
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
