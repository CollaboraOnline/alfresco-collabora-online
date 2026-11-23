// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.rendition2.RenditionDefinitionRegistry2;
import org.alfresco.repo.rendition2.RenditionService2;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.junit.Before;
import org.junit.Test;

public class AskForRenditionTest {
	private static final NodeRef NODE = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "node");

	private ContentService contentService;
	private RenditionService2 renditionService;
	private RenditionDefinitionRegistry2 registry;
	private WopiPutFileWebScript webScript;

	@Before
	public void setUp() {
		contentService = mock(ContentService.class);
		renditionService = mock(RenditionService2.class);
		registry = mock(RenditionDefinitionRegistry2.class);
		when(renditionService.getRenditionDefinitionRegistry2()).thenReturn(registry);
		webScript = new WopiPutFileWebScript();
		webScript.setContentService(contentService);
		webScript.setRenditionService(renditionService);
		webScript.setRenditions("imgpreview,doclib,pdf");
	}

	private void content(String mimetype, long size) {
		final ContentReader reader = mock(ContentReader.class);
		when(reader.exists()).thenReturn(true);
		when(reader.getMimetype()).thenReturn(mimetype);
		when(reader.getSize()).thenReturn(size);
		when(contentService.getReader(NODE, ContentModel.PROP_CONTENT)).thenReturn(reader);
	}

	@Test
	public void requestsOnlyTheSupportedRenditions() {
		content("application/pdf", 19341L);
		when(registry.getRenditionNamesFrom("application/pdf", 19341L)).thenReturn(Set.of("doclib", "imgpreview"));

		webScript.askForRendition(NODE);

		verify(renditionService).render(NODE, "imgpreview");
		verify(renditionService).render(NODE, "doclib");
		verify(renditionService, never()).render(NODE, "pdf");
	}

	@Test
	public void requestsNothingWhenNoRenditionIsSupported() {
		content("application/octet-stream", 19341L);
		when(registry.getRenditionNamesFrom("application/octet-stream", 19341L)).thenReturn(Set.of());

		webScript.askForRendition(NODE);

		verify(renditionService, never()).render(any(NodeRef.class), anyString());
	}

	@Test
	public void requestsNothingWithoutContent() {
		webScript.askForRendition(NODE);

		verify(renditionService, never()).render(any(NodeRef.class), anyString());
		verify(renditionService, never()).getRenditionDefinitionRegistry2();
	}
}
