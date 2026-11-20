// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.junit.Before;
import org.junit.Test;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WopiFileIdsTest {

	private static final String ID = "11111111-2222-3333-4444-555555555555";
	private static final NodeRef LIVE = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, ID);
	private static final NodeRef VERSION = new NodeRef(new StoreRef("workspace", "version2Store"), ID);
	private static final NodeRef FROZEN_STATE = new NodeRef(new StoreRef("versionStore", "version2Store"), ID);
	private static final NodeRef FROZEN_LIVE = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
			"66666666-7777-8888-9999-000000000000");
	private static final String SERVICE_CONTEXT = "alfresco/module/collabora-platform-extension/context/service-context.xml";

	private NodeService nodeService;
	private WopiFileIds fileIds;

	@Before
	public void setUp() {
		nodeService = mock(NodeService.class);
		fileIds = new WopiFileIds(nodeService);
	}

	@Test
	public void resolvesLiveNode() {
		when(nodeService.exists(LIVE)).thenReturn(true);
		when(nodeService.exists(VERSION)).thenReturn(true);

		assertEquals(LIVE, fileIds.resolve(ID));
	}

	@Test
	public void resolvesVersionNode() {
		when(nodeService.exists(VERSION)).thenReturn(true);
		when(nodeService.getProperty(VERSION, Version2Model.PROP_QNAME_FROZEN_NODE_REF)).thenReturn(FROZEN_LIVE);
		when(nodeService.exists(FROZEN_LIVE)).thenReturn(true);

		assertEquals(VERSION, fileIds.resolve(ID));
	}

	@Test
	public void versionOfTrashedDocumentFallsBackToSpacesStore() {
		when(nodeService.exists(VERSION)).thenReturn(true);
		when(nodeService.getProperty(VERSION, Version2Model.PROP_QNAME_FROZEN_NODE_REF)).thenReturn(FROZEN_LIVE);

		assertEquals(LIVE, fileIds.resolve(ID));
	}

	@Test
	public void versionWithoutFrozenNodeRefFallsBackToSpacesStore() {
		when(nodeService.exists(VERSION)).thenReturn(true);

		assertEquals(LIVE, fileIds.resolve(ID));
	}

	@Test
	public void existsAcceptsLiveNode() {
		when(nodeService.exists(LIVE)).thenReturn(true);

		assertTrue(fileIds.exists(LIVE));
	}

	@Test
	public void existsRejectsMissingLiveNode() {
		assertFalse(fileIds.exists(LIVE));
		assertFalse(fileIds.exists(null));
	}

	@Test
	public void existsAcceptsVersionOfLiveDocument() {
		when(nodeService.exists(VERSION)).thenReturn(true);
		when(nodeService.getProperty(VERSION, Version2Model.PROP_QNAME_FROZEN_NODE_REF)).thenReturn(FROZEN_LIVE);
		when(nodeService.exists(FROZEN_LIVE)).thenReturn(true);

		assertTrue(fileIds.exists(VERSION));
	}

	@Test
	public void existsRejectsVersionOfTrashedDocument() {
		when(nodeService.exists(VERSION)).thenReturn(true);
		when(nodeService.getProperty(VERSION, Version2Model.PROP_QNAME_FROZEN_NODE_REF)).thenReturn(FROZEN_LIVE);

		assertFalse(fileIds.exists(VERSION));
	}

	@Test
	public void existsReadsFrozenStateRefThroughWorkspaceProtocol() {
		when(nodeService.exists(FROZEN_STATE)).thenReturn(true);
		when(nodeService.exists(VERSION)).thenReturn(true);
		when(nodeService.getProperty(VERSION, Version2Model.PROP_QNAME_FROZEN_NODE_REF)).thenReturn(FROZEN_LIVE);
		when(nodeService.exists(FROZEN_LIVE)).thenReturn(true);

		assertTrue(fileIds.exists(FROZEN_STATE));
	}

	@Test
	public void normalizeTurnsFrozenStateRefIntoWorkspaceRef() {
		assertEquals(VERSION, WopiFileIds.normalize(FROZEN_STATE));
		assertEquals(LIVE, WopiFileIds.normalize(LIVE));
		assertEquals(null, WopiFileIds.normalize(null));
	}

	@Test
	public void beanUsesUnsecuredNodeService() throws Exception {
		final Document doc;
		try (InputStream in = getClass().getClassLoader()
				.getResourceAsStream(SERVICE_CONTEXT)) {
			assertNotNull(SERVICE_CONTEXT, in);
			doc = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder()
					.parse(in);
		}
		final Element bean = findBean(doc, "WopiFileIds");
		assertNotNull("WopiFileIds bean", bean);
		final NodeList properties = bean.getElementsByTagName("property");
		String ref = null;
		for (int i = 0; i < properties.getLength(); i++) {
			final Element property = (Element) properties.item(i);
			if (!"nodeService".equals(property.getAttribute("name"))) {
				continue;
			}
			ref = property.getAttribute("ref");
		}
		assertEquals("nodeService", ref);
	}

	private static Element findBean(final Document doc, final String id) {
		final NodeList beans = doc.getElementsByTagName("bean");
		for (int i = 0; i < beans.getLength(); i++) {
			final Element bean = (Element) beans.item(i);
			if (id.equals(bean.getAttribute("id"))) {
				return bean;
			}
		}
		return null;
	}

	@Test
	public void unknownFallsBackToSpacesStore() {
		assertEquals(LIVE, fileIds.resolve(ID));
	}

	@Test
	public void rejectsInvalidUuid() {
		try {
			fileIds.resolve("not-a-uuid");
			fail("expected WebScriptException");
		} catch (WebScriptException e) {
			assertEquals(Status.STATUS_BAD_REQUEST, e.getStatus());
		}
	}

	@Test
	public void isVersionNode() {
		assertTrue(WopiFileIds.isVersionNode(VERSION));
		assertTrue(WopiFileIds.isVersionNode(FROZEN_STATE));
		assertFalse(WopiFileIds.isVersionNode(LIVE));
	}
}
