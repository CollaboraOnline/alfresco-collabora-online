// SPDX-FileCopyrightText: 2026 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;
import fr.jeci.collabora.alfresco.WopiTokenKind;

public class WopiTokenStoreTest {

	private final Instant now = Instant.now();
	private InMemoryAttributeService attributeService;
	private WopiTokenStore store;

	@Before
	public void setUp() {
		attributeService = new InMemoryAttributeService();
		store = newStore();
	}

	private WopiTokenStore newStore() {
		WopiTokenStore tokenStore = new WopiTokenStore();
		tokenStore.setAttributeService(attributeService);
		return tokenStore;
	}

	private WOPIAccessTokenInfo token(String value, Instant expiresAt) {
		return new WOPIAccessTokenInfo(value, now.minus(Duration.ofMinutes(1)), expiresAt, "input", "output", "alice",
				WopiTokenKind.TASK);
	}

	@Test
	public void savedTokenIsReadBackWithAllFields() {
		WOPIAccessTokenInfo saved = token("token-1", now.plus(Duration.ofHours(8)));
		store.save(saved);

		WOPIAccessTokenInfo read = store.get("token-1");

		assertNotNull(read);
		assertEquals("token-1", read.getAccessToken());
		assertEquals(saved.getIssuedAt(), read.getIssuedAt());
		assertEquals(saved.getExpiresAt(), read.getExpiresAt());
		assertEquals("input", read.getFileId());
		assertEquals("output", read.getWriteFileId());
		assertEquals("alice", read.getUserName());
		assertEquals(WopiTokenKind.TASK, read.getKind());
		assertFalse(read.isRenewable());
	}

	@Test
	public void tokenStoredWithoutKindIsAbsent() {
		HashMap<String, Serializable> attribute = new HashMap<>();
		attribute.put(WopiTokenStore.ISSUED_AT, now.toEpochMilli());
		attribute.put(WopiTokenStore.EXPIRES_AT, now.plus(Duration.ofHours(1))
				.toEpochMilli());
		attribute.put(WopiTokenStore.FILE_ID, "input");
		attribute.put(WopiTokenStore.USER_NAME, "alice");
		attributeService.setAttribute(attribute, WopiTokenStore.ATTR_KEY_ROOT, WopiTokenStore.ATTR_KEY_TOKENS,
				WopiTokenStore.hash("no-kind"));

		assertNull(store.get("no-kind"));
	}

	@Test
	public void readOnlyTokenKeepsNoWriteTarget() {
		store.save(new WOPIAccessTokenInfo("token-ro", now.minus(Duration.ofMinutes(1)), now.plus(Duration.ofHours(1)),
				"input", null, "alice", WopiTokenKind.TASK));

		WOPIAccessTokenInfo read = store.get("token-ro");

		assertNull(read.getWriteFileId());
		assertFalse(read.canWrite("input"));
	}

	@Test
	public void tokenSurvivesANewStoreInstance() {
		store.save(token("token-1", now.plus(Duration.ofHours(8))));

		assertNotNull(newStore().get("token-1"));
	}

	@Test
	public void attributeNeverHoldsTheClearToken() {
		store.save(token("clear-token-value", now.plus(Duration.ofHours(8))));

		assertFalse(attributeService.dump()
				.contains("clear-token-value"));
		assertTrue(attributeService.dump()
				.contains(WopiTokenStore.hash("clear-token-value")));
	}

	@Test
	public void unknownOrNullTokenIsAbsent() {
		assertNull(store.get("does-not-exist"));
		assertNull(store.get(null));
	}

	@Test
	public void purgeRemovesExpiredTokensOnly() {
		store.save(token("expired", now.minus(Duration.ofMinutes(1))));
		store.save(token("valid", now.plus(Duration.ofHours(1))));

		int purged = store.purgeExpired(now);

		assertEquals(1, purged);
		assertNull(store.get("expired"));
		assertNotNull(store.get("valid"));
	}

	@Test
	public void purgeKeepsExpiredCollaboraTokensDuringRetention() {
		store.setRenewalRetentionMs(Duration.ofHours(1)
				.toMillis());
		store.save(new WOPIAccessTokenInfo("collabora-recent", now.minus(Duration.ofHours(2)), now.minus(Duration
				.ofMinutes(30)), "file", "alice"));
		store.save(new WOPIAccessTokenInfo("collabora-old", now.minus(Duration.ofHours(3)), now.minus(Duration.ofHours(
				2)), "file", "alice"));
		store.save(token("task-recent", now.minus(Duration.ofMinutes(30))));

		int purged = store.purgeExpired(now);

		assertEquals(2, purged);
		assertNotNull(store.get("collabora-recent"));
		assertNull(store.get("collabora-old"));
		assertNull(store.get("task-recent"));
	}

	@Test
	public void hashIsStableHexSha256() {
		assertEquals(64, WopiTokenStore.hash("x")
				.length());
		assertEquals(WopiTokenStore.hash("x"), WopiTokenStore.hash("x"));
		assertNotEquals(WopiTokenStore.hash("x"), WopiTokenStore.hash("y"));
	}
}
