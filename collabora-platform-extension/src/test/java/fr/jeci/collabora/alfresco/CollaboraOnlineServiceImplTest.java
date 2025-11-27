// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.permissions.impl.AllowPermissionServiceImpl;
import org.alfresco.repo.version.NodeServiceImpl;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import fr.jeci.collabora.alfresco.WopiDiscovery.DiscoveryAction;

@Transactional
public class CollaboraOnlineServiceImplTest {
	/**
	 * version store node service
	 */
	protected CollaboraOnlineServiceImpl collaboraOnlineService = null;

	NodeRef nodeRef = null;
	NodeService nodeService = null;

	static String LOCALHOST_SERVER = "http://localhost:8080/";
	static String PUBLICHOST_SERVER = "https://my.server.demo.com/";

	@Before
	public void setUp() throws Exception {
		nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "test-collabora");

		this.collaboraOnlineService = new CollaboraOnlineServiceImpl();

		nodeService = mock(NodeServiceImpl.class);
		this.collaboraOnlineService.setNodeService(nodeService);
		this.collaboraOnlineService.setPermissionService(mock(AllowPermissionServiceImpl.class));

		this.collaboraOnlineService.setCollaboraPublicUrl(new URL(PUBLICHOST_SERVER));
		this.collaboraOnlineService.setTokenTtlMs(1000);
		this.collaboraOnlineService.setAlfrescoPrivateURL(new URL(LOCALHOST_SERVER));
		this.collaboraOnlineService.setAlfrescoPublicURL(new URL(PUBLICHOST_SERVER));

	}

	@After
	public void tearDown() throws Exception {
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
		assertTrue("Token should only contain Base64 URL-safe characters: " + token,
				BASE64_URL_SAFE_PATTERN.matcher(token).matches());

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

}
