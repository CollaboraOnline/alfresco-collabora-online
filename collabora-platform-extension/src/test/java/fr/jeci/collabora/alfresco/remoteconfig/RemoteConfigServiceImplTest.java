// SPDX-FileCopyrightText: 2026 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.nodelocator.NodeLocatorService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.junit.Before;
import org.junit.Test;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

/**
 * Unit tests for RemoteConfigServiceImpl
 */
public class RemoteConfigServiceImplTest {

	private static final String BASE_PATH = "collabora-online";
	private static final String EMPTY_FONTS_CONFIG = "{\"kind\":\"fontconfiguration\",\"server\":\"Alfresco\",\"fonts\":[]}";

	private NodeService nodeService;
	private FileFolderService fileFolderService;
	private RemoteConfigServiceImpl service;

	@Before
	public void setUp() {
		nodeService = mock(NodeService.class);
		fileFolderService = mock(FileFolderService.class);
		NodeLocatorService nodeLocatorService = mock(NodeLocatorService.class);

		NodeRef companyHome = new NodeRef("workspace://SpacesStore/company-home");
		NodeRef dataDictionary = new NodeRef("workspace://SpacesStore/data-dictionary");
		NodeRef baseFolder = new NodeRef("workspace://SpacesStore/collabora-online");
		QName dictionaryQName = QName.createQName(NamespaceService.APP_MODEL_1_0_URI, "dictionary");

		when(nodeLocatorService.getNode("companyhome", null, null)).thenReturn(companyHome);
		when(nodeService.getChildAssocs(companyHome, ContentModel.ASSOC_CONTAINS, dictionaryQName)).thenReturn(List.of(
				new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, companyHome, dictionaryQName, dataDictionary)));
		when(nodeService.getChildByName(dataDictionary, ContentModel.ASSOC_CONTAINS, BASE_PATH)).thenReturn(baseFolder);

		service = new RemoteConfigServiceImpl();
		service.setBasePath(BASE_PATH);
		service.setFontsBaseUrl("http://localhost:8080/alfresco/s/collabora/fonts");
		service.setNodeService(nodeService);
		service.setContentService(mock(ContentService.class));
		service.setFileFolderService(fileFolderService);
		service.setNodeLocatorService(nodeLocatorService);
		service.onBootstrap(null);
	}

	// ========== Remote config disabled ==========

	@Test
	public void testGetFontsConfig_disabledReturnsEmptyListWithoutWriting() {
		assertEquals(EMPTY_FONTS_CONFIG, service.getFontsConfig());
		verify(fileFolderService, never()).listFiles(any());
		verify(nodeService, never()).createNode(any(), any(), any(), any(), any());
	}

	@Test
	public void testListFonts_disabledReturnsEmptyList() {
		assertTrue(service.listFonts()
				.isEmpty());
	}

	@Test
	public void testGetFontContentReader_disabledAnswersNotFound() {
		WebScriptException e = assertThrows(WebScriptException.class, () -> service.getFontContentReader(
				"OpenSans-Regular.ttf"));
		assertEquals(Status.STATUS_NOT_FOUND, e.getStatus());
	}

	@Test
	public void testUploadFont_disabledAnswersBadRequest() {
		WebScriptException e = assertThrows(WebScriptException.class, () -> service.uploadFont("OpenSans-Regular.ttf",
				new ByteArrayInputStream(new byte[0]), "font/ttf"));
		assertEquals(Status.STATUS_BAD_REQUEST, e.getStatus());
		verify(nodeService, never()).createNode(any(), any(), any(), any(), any());
	}

	@Test
	public void testDeleteFont_disabledAnswersNotFound() {
		WebScriptException e = assertThrows(WebScriptException.class, () -> service.deleteFont("OpenSans-Regular.ttf"));
		assertEquals(Status.STATUS_NOT_FOUND, e.getStatus());
	}

	// ========== sanitizeFontName Tests ==========

	@Test
	public void testSanitizeFontName_safeNameUnchanged() {
		assertEquals("OpenSans-Regular.ttf", RemoteConfigServiceImpl.sanitizeFontName("OpenSans-Regular.ttf"));
	}

	@Test
	public void testSanitizeFontName_variableFontBrackets() {
		// Square brackets are rejected by Tomcat even when percent-encoded
		assertEquals("MonaspaceRadonVarVF_wght_wdth_slnt_.ttf", RemoteConfigServiceImpl.sanitizeFontName(
				"MonaspaceRadonVarVF[wght,wdth,slnt].ttf"));
	}

	@Test
	public void testSanitizeFontName_spaces() {
		assertEquals("My_Font.otf", RemoteConfigServiceImpl.sanitizeFontName("My Font.otf"));
	}

	@Test
	public void testSanitizeFontName_accentedCharacters() {
		assertEquals("Trait__d_uni_n.woff2", RemoteConfigServiceImpl.sanitizeFontName("Traité d'unión.woff2"));
	}
}
