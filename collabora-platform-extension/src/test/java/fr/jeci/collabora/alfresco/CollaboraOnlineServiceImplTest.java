// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.version.NodeServiceImpl;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import fr.jeci.collabora.alfresco.WopiDiscovery.DiscoveryAction;
import fr.jeci.collabora.alfresco.token.InMemoryAttributeService;
import fr.jeci.collabora.alfresco.token.WopiTokenStore;

@Transactional
public class CollaboraOnlineServiceImplTest {
	/**
	 * version store node service
	 */
	protected CollaboraOnlineServiceImpl collaboraOnlineService = null;

	NodeRef nodeRef = null;
	NodeService nodeService = null;
	WopiTokenStore tokenStore = null;
	PermissionService permissionService = null;
	NodeRef outputRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "test-output");

	static String LOCALHOST_SERVER = "http://localhost:8080/";
	static String PUBLICHOST_SERVER = "https://my.server.demo.com/";

	@Before
	public void setUp() throws Exception {
		AuthenticationUtil authenticationUtil = new AuthenticationUtil();
		authenticationUtil.setDefaultAdminUserName("admin");
		authenticationUtil.setDefaultGuestUserName("guest");
		authenticationUtil.afterPropertiesSet();

		nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "aaaaaaaa-0000-0000-0000-000000000001");

		this.collaboraOnlineService = new CollaboraOnlineServiceImpl();

		nodeService = mock(NodeServiceImpl.class);
		this.collaboraOnlineService.setNodeService(nodeService);
		permissionService = mock(PermissionService.class);
		this.collaboraOnlineService.setPermissionService(permissionService);
		this.collaboraOnlineService.setWopiFileIds(new WopiFileIds(nodeService));

		this.collaboraOnlineService.setCollaboraPublicUrl(new URL(PUBLICHOST_SERVER));
		this.collaboraOnlineService.setTokenTtlMs(1000);
		this.collaboraOnlineService.setAlfrescoPrivateURL(new URL(LOCALHOST_SERVER));
		this.collaboraOnlineService.setAlfrescoPublicURL(new URL(PUBLICHOST_SERVER));

		this.tokenStore = new WopiTokenStore();
		this.tokenStore.setAttributeService(new InMemoryAttributeService());
		this.collaboraOnlineService.setTokenStore(this.tokenStore);

		when(nodeService.exists(nodeRef)).thenReturn(true);
		when(nodeService.exists(outputRef)).thenReturn(true);
	}

	@After
	public void tearDown() throws Exception {
		AuthenticationUtil.clearCurrentSecurityContext();
	}

	@Test
	public void testGetWopiSrcURL() throws IOException {
		when(nodeService.getProperty(nodeRef, ContentModel.PROP_NAME)).thenReturn("toto.ods");

		WopiDiscovery wopiDiscovery = mock(WopiDiscovery.class);
		wopiDiscovery.setCollaboraPrivateUrl(new URL(LOCALHOST_SERVER));

		List<DiscoveryAction> actions = new ArrayList<>();
		String urlsrc = PUBLICHOST_SERVER + "/loleaflet/1430151/loleaflet.html?";
		DiscoveryAction actionOds = new DiscoveryAction("ods", "edit", urlsrc);
		actions.add(actionOds);
		when(wopiDiscovery.getAction("ods")).thenReturn(actions);
		this.collaboraOnlineService.setWopiDiscovery(wopiDiscovery);

		when(wopiDiscovery.hasCollaboraOnline()).thenReturn(true);

		String wopiSrcURL = this.collaboraOnlineService.getWopiSrcURL(nodeRef, "edit");
		assertEquals(urlsrc, wopiSrcURL);
	}

	// ========== Extension Resolution Tests ==========

	private static final String PDF_MIMETYPE = "application/pdf";
	private static final String PDF_URLSRC = PUBLICHOST_SERVER + "/browser/1430151/cool.html?";

	private WopiDiscovery onlineDiscovery() {
		WopiDiscovery wopiDiscovery = mock(WopiDiscovery.class);
		when(wopiDiscovery.hasCollaboraOnline()).thenReturn(true);
		this.collaboraOnlineService.setWopiDiscovery(wopiDiscovery);
		return wopiDiscovery;
	}

	private void givenNode(String name, String mimetype) {
		when(nodeService.getProperty(nodeRef, ContentModel.PROP_NAME)).thenReturn(name);
		when(nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT)).thenReturn(new ContentData("store://any.bin",
				mimetype, 1024L, "UTF-8"));
	}

	@Test
	public void testGetWopiSrcURL_dotInVersionNumberFallsBackToMimetype() throws IOException {
		givenNode("Alfresco 7.4 - Installation et exploitation", PDF_MIMETYPE);

		WopiDiscovery wopiDiscovery = onlineDiscovery();
		when(wopiDiscovery.getLegacyAction(PDF_MIMETYPE)).thenReturn(List.of(new DiscoveryAction("", "edit",
				PDF_URLSRC)));

		assertEquals(PDF_URLSRC, this.collaboraOnlineService.getWopiSrcURL(nodeRef, "edit"));
	}

	@Test
	public void testGetWopiSrcURL_numericSuffixFallsBackToMimetype() throws IOException {
		givenNode("Budget 2024.11", PDF_MIMETYPE);

		WopiDiscovery wopiDiscovery = onlineDiscovery();
		when(wopiDiscovery.getLegacyAction(PDF_MIMETYPE)).thenReturn(List.of(new DiscoveryAction("", "edit",
				PDF_URLSRC)));

		assertEquals(PDF_URLSRC, this.collaboraOnlineService.getWopiSrcURL(nodeRef, "edit"));
	}

	@Test
	public void testGetWopiSrcURL_trailingDotFallsBackToMimetype() throws IOException {
		givenNode("toto.", PDF_MIMETYPE);

		WopiDiscovery wopiDiscovery = onlineDiscovery();
		when(wopiDiscovery.getLegacyAction(PDF_MIMETYPE)).thenReturn(List.of(new DiscoveryAction("", "edit",
				PDF_URLSRC)));

		assertEquals(PDF_URLSRC, this.collaboraOnlineService.getWopiSrcURL(nodeRef, "edit"));
	}

	@Test
	public void testGetWopiSrcURL_uppercaseExtensionStillResolved() throws IOException {
		givenNode("toto.ODS", "application/vnd.oasis.opendocument.spreadsheet");

		WopiDiscovery wopiDiscovery = onlineDiscovery();
		String urlsrc = PUBLICHOST_SERVER + "/browser/1430151/cool.html?ods";
		when(wopiDiscovery.getAction("ods")).thenReturn(List.of(new DiscoveryAction("ods", "edit", urlsrc)));

		assertEquals(urlsrc, this.collaboraOnlineService.getWopiSrcURL(nodeRef, "edit"));
	}

	@Test
	public void testGetWopiSrcURL_mimetypeFallbackDegradesToAvailableAction() throws IOException {
		givenNode("Alfresco 7.4 - Installation et exploitation", PDF_MIMETYPE);

		WopiDiscovery wopiDiscovery = onlineDiscovery();
		when(wopiDiscovery.getLegacyAction(PDF_MIMETYPE)).thenReturn(List.of(new DiscoveryAction("", "view_comment",
				PDF_URLSRC)));

		assertEquals(PDF_URLSRC, this.collaboraOnlineService.getWopiSrcURL(nodeRef, "edit"));
	}

	@Test(expected = WebScriptException.class)
	public void testGetWopiSrcURL_unknownExtensionAndMimetype() throws IOException {
		givenNode("archive.dwg", "image/vnd.dwg");

		onlineDiscovery();

		this.collaboraOnlineService.getWopiSrcURL(nodeRef, "edit");
	}

	// ========== Token Generation Tests ==========

	/** Base64 URL-safe pattern: only A-Z, a-z, 0-9, - and _ (no padding =) */
	private static final Pattern BASE64_URL_SAFE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

	/** Expected token length: 32 bytes = 256 bits -> 43 Base64 characters (without padding) */
	private static final int EXPECTED_TOKEN_LENGTH = 43;

	/**
	 * Helper method to invoke the private generateAccessToken() method via reflection
	 */
	private String invokeGenerateAccessToken() throws Exception {
		Method method = CollaboraOnlineServiceImpl.class.getDeclaredMethod("generateAccessToken");
		method.setAccessible(true);
		return (String) method.invoke(this.collaboraOnlineService);
	}

	@Test
	public void testGenerateAccessToken_returnsNonNullToken() throws Exception {
		// Execute
		String token = invokeGenerateAccessToken();

		// Verify
		assertNotNull("Access token should not be null", token);
		assertFalse("Access token should not be empty", token.isEmpty());
	}

	@Test
	public void testGenerateAccessToken_hasCorrectLength() throws Exception {
		// Execute
		String token = invokeGenerateAccessToken();

		// Verify: 256 bits (32 bytes) in Base64 = 43 characters (without padding)
		assertEquals("Token should be " + EXPECTED_TOKEN_LENGTH + " characters (256 bits in Base64)",
				EXPECTED_TOKEN_LENGTH, token.length());
	}

	@Test
	public void testGenerateAccessToken_isBase64UrlSafe() throws Exception {
		// Execute
		String token = invokeGenerateAccessToken();

		// Verify: only URL-safe Base64 characters (A-Z, a-z, 0-9, -, _)
		assertTrue("Token should only contain Base64 URL-safe characters: " + token, BASE64_URL_SAFE_PATTERN.matcher(
				token)
				.matches());

		// Verify: no padding characters
		assertFalse("Token should not contain padding characters", token.contains("="));

		// Verify: no standard Base64 characters that are not URL-safe
		assertFalse("Token should not contain '+' character", token.contains("+"));
		assertFalse("Token should not contain '/' character", token.contains("/"));
	}

	@Test
	public void testGenerateAccessToken_generatesUniqueTokens() throws Exception {
		// Execute: generate multiple tokens
		Set<String> tokens = new HashSet<>();
		int tokenCount = 100;
		for (int i = 0; i < tokenCount; i++) {
			String token = invokeGenerateAccessToken();
			tokens.add(token);
		}

		// Verify: all tokens should be unique
		assertEquals("All " + tokenCount + " tokens should be unique", tokenCount, tokens.size());
	}

	@Test
	public void testGenerateAccessToken_twoConsecutiveTokensAreDifferent() throws Exception {
		// Execute
		String token1 = invokeGenerateAccessToken();
		String token2 = invokeGenerateAccessToken();

		// Verify
		assertNotEquals("Two consecutive tokens should be different", token1, token2);
	}

	// ========== Token Masking Tests ==========

	@Test
	public void testMaskToken_normalToken() {
		String token = "abcdefghijklmnopqrstuvwxyz1234567890ABC";
		String masked = WOPIAccessTokenInfo.maskToken(token);

		assertEquals("Masked token should show only last 4 chars", "****0ABC", masked);
		assertFalse("Masked token should not contain original token", masked.contains("abcdef"));
	}

	@Test
	public void testMaskToken_nullToken() {
		String masked = WOPIAccessTokenInfo.maskToken(null);
		assertEquals("Null token should return [null]", "[null]", masked);
	}

	@Test
	public void testMaskToken_shortToken() {
		String masked = WOPIAccessTokenInfo.maskToken("abc");
		assertEquals("Short token should be fully masked", "****", masked);
	}

	@Test
	public void testMaskToken_exactlyFourChars() {
		String masked = WOPIAccessTokenInfo.maskToken("abcd");
		assertEquals("4-char token should be fully masked", "****", masked);
	}

	@Test
	public void testMaskToken_fiveChars() {
		String masked = WOPIAccessTokenInfo.maskToken("abcde");
		assertEquals("5-char token should show last 4", "****bcde", masked);
	}

	// ========== Settings token Tests ==========

	@Test(expected = WebScriptException.class)
	public void testCreateSettingsAccessToken_rejectsAnonymous() {
		// No authenticated user in this lightweight harness
		this.collaboraOnlineService.createSettingsAccessToken();
	}

	// ========== resolveToken (user-only, no file binding) ==========

	@Test
	public void testResolveToken_returnsStoredTokenWithoutFileCheck() {
		Instant now = Instant.now();
		WOPIAccessTokenInfo stored = new WOPIAccessTokenInfo("settings-token", now.minus(Duration.ofMinutes(1)), now.plus(
				Duration.ofHours(1)), CollaboraOnlineService.SETTINGS_FILE_ID, "alice");
		this.tokenStore.save(stored);

		WOPIAccessTokenInfo resolved = this.collaboraOnlineService.resolveToken("settings-token");

		assertEquals("settings-token", resolved.getAccessToken());
		assertEquals("alice", resolved.getUserName());
		assertEquals(CollaboraOnlineService.SETTINGS_FILE_ID, resolved.getFileId());
	}

	@Test(expected = WebScriptException.class)
	public void testResolveToken_rejectsNull() {
		this.collaboraOnlineService.resolveToken(null);
	}

	@Test(expected = WebScriptException.class)
	public void testResolveToken_rejectsUnknownToken() {
		this.collaboraOnlineService.resolveToken("does-not-exist");
	}

	@Test
	public void testResolveToken_rejectsExpiredToken() {
		Instant past = Instant.now()
				.minus(Duration.ofHours(2));
		WOPIAccessTokenInfo expired = new WOPIAccessTokenInfo("expired-token", past, past.plus(Duration.ofHours(1)),
				CollaboraOnlineService.SETTINGS_FILE_ID, "alice");
		this.tokenStore.save(expired);

		try {
			this.collaboraOnlineService.resolveToken("expired-token");
			fail("Expected WebScriptException for expired token");
		} catch (WebScriptException expectedException) {
			// expected
		}
	}

	// ========== Persistent and scoped tokens ==========

	private void allow(NodeRef node, String permission) {
		when(permissionService.hasPermission(node, permission)).thenReturn(AccessStatus.ALLOWED);
	}

	@Test
	public void testCreateAccessToken_isPersistedAndRenewable() {
		AuthenticationUtil.setRunAsUser("alice");
		allow(nodeRef, PermissionService.READ);

		WOPIAccessTokenInfo created = this.collaboraOnlineService.createAccessToken(nodeRef);

		WOPIAccessTokenInfo stored = this.tokenStore.get(created.getAccessToken());
		assertNotNull(stored);
		assertTrue(stored.isRenewable());
		assertTrue(stored.canWrite(nodeRef.getId()));
	}

	@Test
	public void testCreateScopedAccessToken_readsInputWritesOutput() {
		AuthenticationUtil.setRunAsUser("alice");
		allow(nodeRef, PermissionService.READ);
		allow(outputRef, PermissionService.WRITE);

		WOPIAccessTokenInfo created = this.collaboraOnlineService.createScopedAccessToken(nodeRef, outputRef, Duration
				.ofHours(8));

		WOPIAccessTokenInfo stored = this.tokenStore.get(created.getAccessToken());
		assertEquals("alice", stored.getUserName());
		assertTrue(stored.canRead(nodeRef.getId()));
		assertFalse(stored.canWrite(nodeRef.getId()));
		assertTrue(stored.canWrite(outputRef.getId()));
		assertFalse(stored.isRenewable());
		assertTrue(stored.getExpiresAt()
				.isAfter(Instant.now()
						.plus(Duration.ofHours(7))));
	}

	@Test
	public void testCreateScopedAccessToken_readOnlyWhenNoOutput() {
		AuthenticationUtil.setRunAsUser("alice");
		allow(nodeRef, PermissionService.READ);

		WOPIAccessTokenInfo created = this.collaboraOnlineService.createScopedAccessToken(nodeRef, null, Duration.ofHours(
				8));

		assertFalse(this.tokenStore.get(created.getAccessToken())
				.canWrite(nodeRef.getId()));
	}

	@Test
	public void testCreateScopedAccessToken_acceptsTtlLongerThanIntMillis() {
		AuthenticationUtil.setRunAsUser("alice");
		allow(nodeRef, PermissionService.READ);

		WOPIAccessTokenInfo created = this.collaboraOnlineService.createScopedAccessToken(nodeRef, null, Duration.ofDays(
				30));

		assertEquals(created.getIssuedAt()
				.plus(Duration.ofDays(30)), created.getExpiresAt());
		assertEquals(WopiTokenKind.TASK, created.getKind());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateScopedAccessToken_rejectsMissingInput() {
		AuthenticationUtil.setRunAsUser("alice");
		NodeRef missing = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "missing");
		allow(missing, PermissionService.READ);

		this.collaboraOnlineService.createScopedAccessToken(missing, null, Duration.ofHours(8));
	}

	@Test(expected = AccessDeniedException.class)
	public void testCreateScopedAccessToken_rejectsSystemUser() {
		AuthenticationUtil.setRunAsUserSystem();

		this.collaboraOnlineService.createScopedAccessToken(nodeRef, null, Duration.ofHours(8));
	}

	@Test(expected = AccessDeniedException.class)
	public void testCreateScopedAccessToken_rejectsMissingWriteOnOutput() {
		AuthenticationUtil.setRunAsUser("alice");
		allow(nodeRef, PermissionService.READ);

		this.collaboraOnlineService.createScopedAccessToken(nodeRef, outputRef, Duration.ofHours(8));
	}

	private static final NodeRef VERSION_REF = new NodeRef(new StoreRef("workspace", "version2Store"),
			"bbbbbbbb-0000-0000-0000-000000000002");

	private void versionOf(final NodeRef liveRef, final boolean liveExists) {
		when(nodeService.exists(VERSION_REF)).thenReturn(true);
		when(nodeService.getProperty(VERSION_REF, Version2Model.PROP_QNAME_FROZEN_NODE_REF)).thenReturn(liveRef);
		when(nodeService.exists(liveRef)).thenReturn(liveExists);
	}

	private static final NodeRef TRASHED_REF = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
			"cccccccc-0000-0000-0000-000000000003");

	private static void assertNotFound(final Runnable call) {
		try {
			call.run();
			fail("expected WebScriptException");
		} catch (WebScriptException e) {
			assertEquals(Status.STATUS_NOT_FOUND, e.getStatus());
		}
	}

	@Test
	public void testCreateAccessToken_acceptsVersionOfLiveDocument() {
		AuthenticationUtil.setRunAsUser("alice");
		versionOf(nodeRef, true);
		allow(VERSION_REF, PermissionService.READ);

		WOPIAccessTokenInfo created = this.collaboraOnlineService.createAccessToken(VERSION_REF);

		assertTrue(this.tokenStore.get(created.getAccessToken())
				.canRead(VERSION_REF.getId()));
	}

	@Test
	public void testCreateAccessToken_rejectsVersionOfTrashedDocument() {
		AuthenticationUtil.setRunAsUser("alice");
		versionOf(TRASHED_REF, false);
		allow(VERSION_REF, PermissionService.READ);

		assertNotFound(() -> this.collaboraOnlineService.createAccessToken(VERSION_REF));
	}

	@Test
	public void testCreateAccessToken_rejectsMissingNode() {
		AuthenticationUtil.setRunAsUser("alice");
		allow(TRASHED_REF, PermissionService.READ);

		assertNotFound(() -> this.collaboraOnlineService.createAccessToken(TRASHED_REF));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateScopedAccessToken_rejectsVersionOfTrashedDocument() {
		AuthenticationUtil.setRunAsUser("alice");
		versionOf(TRASHED_REF, false);
		allow(VERSION_REF, PermissionService.READ);

		this.collaboraOnlineService.createScopedAccessToken(VERSION_REF, null, Duration.ofHours(8));
	}

	@Test
	public void testRenewAccessToken_rejectsVersionOfTrashedDocument() {
		versionOf(TRASHED_REF, false);
		allow(VERSION_REF, PermissionService.READ);
		allow(new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, VERSION_REF.getId()), PermissionService.READ);
		Instant past = Instant.now()
				.minus(Duration.ofHours(2));
		WOPIAccessTokenInfo expired = new WOPIAccessTokenInfo("trashed", past, past.plus(Duration.ofHours(1)), VERSION_REF
				.getId(), "alice");
		this.tokenStore.save(expired);

		assertNotFound(() -> this.collaboraOnlineService.renewAccessToken(expired));
	}

	@Test
	public void testCreateScopedAccessToken_acceptsVersionNodeAsInput() {
		AuthenticationUtil.setRunAsUser("alice");
		versionOf(nodeRef, true);
		allow(VERSION_REF, PermissionService.READ);

		WOPIAccessTokenInfo created = this.collaboraOnlineService.createScopedAccessToken(VERSION_REF, null, Duration
				.ofHours(8));

		assertTrue(this.tokenStore.get(created.getAccessToken())
				.canRead(VERSION_REF.getId()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateScopedAccessToken_rejectsVersionNodeAsOutput() {
		AuthenticationUtil.setRunAsUser("alice");
		when(nodeService.exists(VERSION_REF)).thenReturn(true);
		allow(nodeRef, PermissionService.READ);
		allow(VERSION_REF, PermissionService.WRITE);

		this.collaboraOnlineService.createScopedAccessToken(nodeRef, VERSION_REF, Duration.ofHours(8));
	}

	@Test
	public void testRenewAccessToken_checksReadOnVersionNode() {
		versionOf(nodeRef, true);
		allow(VERSION_REF, PermissionService.READ);
		Instant past = Instant.now()
				.minus(Duration.ofHours(2));
		WOPIAccessTokenInfo expired = new WOPIAccessTokenInfo("collabora", past, past.plus(Duration.ofHours(1)),
				VERSION_REF.getId(), "alice");
		this.tokenStore.save(expired);

		WOPIAccessTokenInfo renewed = this.collaboraOnlineService.renewAccessToken(expired);

		assertTrue(renewed.getExpiresAt()
				.isAfter(Instant.now()
						.minus(Duration.ofSeconds(1))));
	}

	@Test
	public void testCheckAccessToken_acceptsOutputOfScopedToken() {
		Instant now = Instant.now();
		this.tokenStore.save(new WOPIAccessTokenInfo("scoped", now.minus(Duration.ofMinutes(1)), now.plus(Duration
				.ofHours(1)), nodeRef.getId(), outputRef.getId(), "alice", WopiTokenKind.TASK));

		WOPIAccessTokenInfo checked = this.collaboraOnlineService.checkAccessToken("scoped", outputRef);

		assertEquals("scoped", checked.getAccessToken());
	}

	@Test(expected = WebScriptException.class)
	public void testCheckAccessToken_rejectsNodeOutsideScope() {
		Instant now = Instant.now();
		this.tokenStore.save(new WOPIAccessTokenInfo("scoped", now.minus(Duration.ofMinutes(1)), now.plus(Duration
				.ofHours(1)), nodeRef.getId(), null, "alice", WopiTokenKind.TASK));

		this.collaboraOnlineService.checkAccessToken("scoped", outputRef);
	}

	@Test
	public void testRenewAccessToken_extendsRenewableToken() {
		Instant past = Instant.now()
				.minus(Duration.ofHours(2));
		WOPIAccessTokenInfo expired = new WOPIAccessTokenInfo("collabora", past, past.plus(Duration.ofHours(1)), nodeRef
				.getId(), "alice");
		this.tokenStore.save(expired);
		allow(nodeRef, PermissionService.READ);

		WOPIAccessTokenInfo renewed = this.collaboraOnlineService.renewAccessToken(expired);

		assertEquals("collabora", renewed.getAccessToken());
		assertTrue(renewed.getExpiresAt()
				.isAfter(Instant.now()
						.minus(Duration.ofSeconds(1))));
		assertEquals(renewed.getExpiresAt(), this.tokenStore.get("collabora")
				.getExpiresAt());
	}

	@Test
	public void testRenewAccessToken_capsExpiryAtMaxLifetime() {
		this.collaboraOnlineService.setTokenMaxLifetimeMs(Duration.ofHours(3)
				.toMillis());
		this.collaboraOnlineService.setTokenTtlMs((int) Duration.ofHours(24)
				.toMillis());
		Instant issuedAt = Instant.now()
				.minus(Duration.ofHours(2));
		WOPIAccessTokenInfo expired = new WOPIAccessTokenInfo("collabora", issuedAt, issuedAt.plus(Duration.ofHours(1)),
				nodeRef.getId(), "alice");
		this.tokenStore.save(expired);
		allow(nodeRef, PermissionService.READ);

		WOPIAccessTokenInfo renewed = this.collaboraOnlineService.renewAccessToken(expired);

		assertEquals(renewed.getIssuedAt()
				.plus(Duration.ofHours(3)), renewed.getExpiresAt());
	}

	@Test
	public void testRenewAccessToken_rejectsTokenPastMaxLifetime() {
		Instant issuedAt = Instant.now()
				.minus(Duration.ofDays(8));
		WOPIAccessTokenInfo expired = new WOPIAccessTokenInfo("collabora", issuedAt, Instant.now()
				.minus(Duration.ofHours(1)), nodeRef.getId(), "alice");
		this.tokenStore.save(expired);
		allow(nodeRef, PermissionService.READ);

		try {
			this.collaboraOnlineService.renewAccessToken(expired);
			fail("Expected WebScriptException for a token past its maximum lifetime");
		} catch (WebScriptException expectedException) {
			assertEquals(401, expectedException.getStatus());
		}
	}

	@Test
	public void testRenewAccessToken_rejectsScopedToken() {
		Instant past = Instant.now()
				.minus(Duration.ofHours(2));
		WOPIAccessTokenInfo expired = new WOPIAccessTokenInfo("scoped", past, past.plus(Duration.ofHours(1)), nodeRef
				.getId(), null, "alice", WopiTokenKind.TASK);
		this.tokenStore.save(expired);
		allow(nodeRef, PermissionService.READ);

		try {
			this.collaboraOnlineService.renewAccessToken(expired);
			fail("Expected WebScriptException for a non-renewable token");
		} catch (WebScriptException expectedException) {
			assertEquals(401, expectedException.getStatus());
			assertEquals(expired.getExpiresAt(), this.tokenStore.get("scoped")
					.getExpiresAt());
		}
	}

	@Test
	public void testRenewAccessToken_rejectsUserWithoutRead() {
		Instant past = Instant.now()
				.minus(Duration.ofHours(2));
		WOPIAccessTokenInfo expired = new WOPIAccessTokenInfo("collabora", past, past.plus(Duration.ofHours(1)), nodeRef
				.getId(), "alice");
		this.tokenStore.save(expired);

		try {
			this.collaboraOnlineService.renewAccessToken(expired);
			fail("Expected WebScriptException when the user cannot read the file");
		} catch (WebScriptException expectedException) {
			assertEquals(expired.getExpiresAt(), this.tokenStore.get("collabora")
					.getExpiresAt());
		}
	}

	@Test
	public void testRenewAccessToken_rejectsUnknownToken() {
		Instant past = Instant.now()
				.minus(Duration.ofHours(2));
		WOPIAccessTokenInfo unknown = new WOPIAccessTokenInfo("never-saved", past, past.plus(Duration.ofHours(1)), nodeRef
				.getId(), "alice");
		allow(nodeRef, PermissionService.READ);

		try {
			this.collaboraOnlineService.renewAccessToken(unknown);
			fail("Expected WebScriptException for an unknown token");
		} catch (WebScriptException expectedException) {
			assertEquals(401, expectedException.getStatus());
		}
	}

	@Test
	public void testRenewAccessToken_rejectsSettingsToken() {
		Instant past = Instant.now()
				.minus(Duration.ofHours(2));
		WOPIAccessTokenInfo settings = new WOPIAccessTokenInfo("settings-token", past, past.plus(Duration.ofHours(1)),
				CollaboraOnlineService.SETTINGS_FILE_ID, CollaboraOnlineService.SETTINGS_FILE_ID, "alice",
				WopiTokenKind.SETTINGS);
		this.tokenStore.save(settings);

		try {
			this.collaboraOnlineService.renewAccessToken(settings);
			fail("Expected WebScriptException for a settings token");
		} catch (WebScriptException expectedException) {
			assertEquals(401, expectedException.getStatus());
		}
	}

}
