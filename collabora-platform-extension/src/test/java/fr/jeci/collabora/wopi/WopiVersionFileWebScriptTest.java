// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.junit.Before;
import org.junit.Test;
import org.springframework.extensions.webscripts.Match;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.jeci.collabora.alfresco.CollaboraOnlineService;
import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;
import fr.jeci.collabora.alfresco.WopiFileIds;
import fr.jeci.collabora.alfresco.restriction.FeatureRestrictionService;
import fr.jeci.collabora.alfresco.settings.CollaboraSettingsService;

public class WopiVersionFileWebScriptTest {
	private static final NodeRef LIVE = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "live");
	private static final NodeRef FROZEN = new NodeRef(new StoreRef("workspace", "version2Store"), "frozen");
	private static final Date FROZEN_MODIFIED = new Date(1700000000000L);
	private static final byte[] BODY = "hello".getBytes();

	private NodeService nodeService;
	private VersionService versionService;
	private ContentService contentService;
	private CollaboraOnlineService collaboraOnlineService;
	private WopiFileIds wopiFileIds;
	private WebScriptRequest req;
	private WebScriptResponse res;
	private StringWriter jsonOut;
	private ByteArrayOutputStream bodyOut;
	private WOPIAccessTokenInfo token;

	@Before
	public void setUp() throws Exception {
		nodeService = mock(NodeService.class);
		versionService = mock(VersionService.class);
		contentService = mock(ContentService.class);
		collaboraOnlineService = mock(CollaboraOnlineService.class);
		wopiFileIds = mock(WopiFileIds.class);
		req = mock(WebScriptRequest.class);
		res = mock(WebScriptResponse.class);
		jsonOut = new StringWriter();
		bodyOut = new ByteArrayOutputStream();
		when(res.getWriter()).thenReturn(new PrintWriter(jsonOut));
		when(res.getOutputStream()).thenReturn(bodyOut);
		Instant now = Instant.now();
		token = new WOPIAccessTokenInfo("t", now.minus(Duration.ofMinutes(1)), now.plus(Duration.ofHours(1)), "live",
				"alice");
		when(collaboraOnlineService.serverInfo()).thenReturn(new HashMap<>());
		when(collaboraOnlineService.checkAccessToken(any(), any())).thenReturn(token);
		when(req.getServiceMatch()).thenReturn(new Match(null, Map.of("file_id", "x"), null));
		when(req.getParameter("access_token")).thenReturn("t");
		when(nodeService.exists(any(NodeRef.class))).thenReturn(true);
		when(wopiFileIds.resolve(any())).thenReturn(FROZEN);

		ContentReader reader = mock(ContentReader.class);
		when(reader.getContentInputStream()).thenReturn(new ByteArrayInputStream(BODY));
		when(contentService.getReader(any(NodeRef.class), eq(ContentModel.PROP_CONTENT))).thenReturn(reader);
		ContentData contentData = new ContentData("contentUrl", "text/plain", 5L, "UTF-8");
		when(nodeService.getProperty(any(NodeRef.class), eq(ContentModel.PROP_CONTENT))).thenReturn(contentData);

		Map<QName, Serializable> frozenProps = new HashMap<>();
		frozenProps.put(ContentModel.PROP_NAME, "frozen.docx");
		frozenProps.put(ContentModel.PROP_CONTENT, contentData);
		frozenProps.put(Version2Model.PROP_QNAME_FROZEN_CREATOR, "bob");
		frozenProps.put(Version2Model.PROP_QNAME_FROZEN_MODIFIED, FROZEN_MODIFIED);
		frozenProps.put(Version2Model.PROP_QNAME_VERSION_LABEL, "1.3");
		when(nodeService.getProperties(FROZEN)).thenReturn(frozenProps);
		when(nodeService.getProperty(FROZEN, Version2Model.PROP_QNAME_VERSION_LABEL)).thenReturn("1.3");

		Map<QName, Serializable> liveProps = new HashMap<>();
		liveProps.put(ContentModel.PROP_NAME, "live.docx");
		liveProps.put(ContentModel.PROP_CONTENT, contentData);
		liveProps.put(ContentModel.PROP_CREATOR, "carol");
		when(nodeService.getProperties(LIVE)).thenReturn(liveProps);
	}

	private WopiGetFileWebScript getFile() {
		WopiGetFileWebScript ws = new WopiGetFileWebScript();
		ws.nodeService = nodeService;
		ws.versionService = versionService;
		ws.contentService = contentService;
		ws.collaboraOnlineService = collaboraOnlineService;
		ws.wopiFileIds = wopiFileIds;
		return ws;
	}

	private WopiCheckFileInfoWebScript checkFileInfo() {
		WopiCheckFileInfoWebScript ws = new WopiCheckFileInfoWebScript();
		ws.nodeService = nodeService;
		ws.versionService = versionService;
		ws.collaboraOnlineService = collaboraOnlineService;
		ws.wopiFileIds = wopiFileIds;
		PermissionService permissionService = mock(PermissionService.class);
		when(permissionService.hasPermission(any(), any())).thenReturn(
				org.alfresco.service.cmr.security.AccessStatus.ALLOWED);
		ws.setPermissionService(permissionService);
		ws.setAuthorityService(mock(AuthorityService.class));
		ws.setPersonService(mock(PersonService.class));
		ws.setFeatureRestrictionService(mock(FeatureRestrictionService.class));
		ws.setCollaboraSettingsService(mock(CollaboraSettingsService.class));
		return ws;
	}

	private static Version versionLabelled(String label) {
		Version version = mock(Version.class);
		when(version.getVersionLabel()).thenReturn(label);
		when(version.getFrozenModifiedDate()).thenReturn(FROZEN_MODIFIED);
		return version;
	}

	@Test
	public void testGetFile_liveVersionedNodeSendsCurrentLabel() throws Exception {
		Version current = versionLabelled("2.1");
		when(versionService.getCurrentVersion(LIVE)).thenReturn(current);

		getFile().executeAsUser(req, res, LIVE, token);

		verify(res).setHeader(WopiHeader.X_WOPI_ITEM_VERSION, "2.1");
		assertEquals("hello", bodyOut.toString());
	}

	@Test
	public void testGetFile_versionNodeSendsItsOwnLabel() throws Exception {
		getFile().executeAsUser(req, res, FROZEN, token);

		verify(res).setHeader(WopiHeader.X_WOPI_ITEM_VERSION, "1.3");
		verify(versionService, never()).getCurrentVersion(any());
		assertEquals("hello", bodyOut.toString());
	}

	@Test
	public void testExecute_resolvesFileIdOnce() throws Exception {
		AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getSystemUserName());
		try {
			getFile().execute(req, res);
		} finally {
			AuthenticationUtil.clearCurrentSecurityContext();
		}

		verify(wopiFileIds, times(1)).resolve("x");
		verify(collaboraOnlineService).checkAccessToken("t", FROZEN);
		assertEquals("hello", bodyOut.toString());
	}

	@Test
	public void testGetFile_unversionedNodeSendsNoHeader() throws Exception {
		getFile().executeAsUser(req, res, LIVE, token);

		verify(res, never()).setHeader(eq(WopiHeader.X_WOPI_ITEM_VERSION), any());
		assertEquals("hello", bodyOut.toString());
	}

	@Test
	public void testCheckFileInfo_versionNodeIsFrozenAndReadOnly() throws Exception {
		checkFileInfo().executeAsUser(req, res, FROZEN, token);

		JsonNode json = new ObjectMapper().readTree(jsonOut.toString());
		assertEquals("1.3", json.get("Version")
				.asText());
		assertEquals("false", json.get("UserCanWrite")
				.asText());
		assertEquals("true", json.get("UserCanNotWriteRelative")
				.asText());
		assertEquals("frozen.docx", json.get("BaseFileName")
				.asText());
		assertEquals("bob", json.get("OwnerId")
				.asText());
		assertEquals("5", json.get("Size")
				.asText());
		assertEquals(org.joda.time.format.ISODateTimeFormat.dateTime()
				.print(new org.joda.time.LocalDateTime(FROZEN_MODIFIED)), json.get("LastModifiedTime")
						.asText());
		verify(versionService, never()).getCurrentVersion(any());
		verify(versionService, never()).ensureVersioningEnabled(any(), any());
	}

	@Test
	public void testCheckFileInfo_versionNodeWithoutAuditablePropsOmitsOwnerAndModified() throws Exception {
		Map<QName, Serializable> props = new HashMap<>(nodeService.getProperties(FROZEN));
		props.remove(Version2Model.PROP_QNAME_FROZEN_CREATOR);
		props.remove(Version2Model.PROP_QNAME_FROZEN_MODIFIED);
		when(nodeService.getProperties(FROZEN)).thenReturn(props);

		checkFileInfo().executeAsUser(req, res, FROZEN, token);

		JsonNode json = new ObjectMapper().readTree(jsonOut.toString());
		assertNull(json.get("OwnerId"));
		assertNull(json.get("LastModifiedTime"));
		assertEquals("1.3", json.get("Version")
				.asText());
	}

	@Test
	public void testCheckFileInfo_liveNodeKeepsItsCurrentVersion() throws Exception {
		Version current = versionLabelled("2.1");
		when(versionService.getCurrentVersion(LIVE)).thenReturn(current);

		checkFileInfo().executeAsUser(req, res, LIVE, token);

		JsonNode json = new ObjectMapper().readTree(jsonOut.toString());
		assertEquals("2.1", json.get("Version")
				.asText());
		assertEquals("true", json.get("UserCanWrite")
				.asText());
		assertNull(json.get("UserCanNotWriteRelative"));
	}

	@Test
	public void testCheckFileInfo_unversionedLiveNodeHasNoVersion() throws Exception {
		checkFileInfo().executeAsUser(req, res, LIVE, token);

		JsonNode json = new ObjectMapper().readTree(jsonOut.toString());
		assertNull(json.get("Version"));
	}

	@Test
	public void testPutFile_versionNodeIsRefusedWith401() throws Exception {
		WopiPutFileWebScript ws = new WopiPutFileWebScript();
		ws.nodeService = nodeService;
		ws.collaboraOnlineService = collaboraOnlineService;
		ws.wopiFileIds = wopiFileIds;

		try {
			ws.execute(req, res);
			fail("expected WebScriptException");
		} catch (WebScriptException e) {
			assertEquals(Status.STATUS_UNAUTHORIZED, e.getStatus());
		}
		verify(versionService, never()).createVersion(any(NodeRef.class), any());
	}
}
