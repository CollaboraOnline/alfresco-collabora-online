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

import org.joda.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;

import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;

public class WopiTokenStoreTest {

	private final LocalDateTime now = LocalDateTime.now();
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

	private WOPIAccessTokenInfo token(String value, LocalDateTime expiresAt) {
		return new WOPIAccessTokenInfo(value, now.minusMinutes(1), expiresAt, "input", "output", "alice", false);
	}

	@Test
	public void savedTokenIsReadBackWithAllFields() {
		WOPIAccessTokenInfo saved = token("token-1", now.plusHours(8));
		store.save(saved);

		WOPIAccessTokenInfo read = store.get("token-1");

		assertNotNull(read);
		assertEquals("token-1", read.getAccessToken());
		assertEquals(saved.getIssuedAt(), read.getIssuedAt());
		assertEquals(saved.getExpiresAt(), read.getExpiresAt());
		assertEquals("input", read.getFileId());
		assertEquals("output", read.getWriteFileId());
		assertEquals("alice", read.getUserName());
		assertFalse(read.isRenewable());
	}

	@Test
	public void readOnlyTokenKeepsNoWriteTarget() {
		store.save(new WOPIAccessTokenInfo("token-ro", now.minusMinutes(1), now.plusHours(1), "input", null, "alice",
				false));

		WOPIAccessTokenInfo read = store.get("token-ro");

		assertNull(read.getWriteFileId());
		assertFalse(read.canWrite("input"));
	}

	@Test
	public void tokenSurvivesANewStoreInstance() {
		store.save(token("token-1", now.plusHours(8)));

		assertNotNull(newStore().get("token-1"));
	}

	@Test
	public void attributeNeverHoldsTheClearToken() {
		store.save(token("clear-token-value", now.plusHours(8)));

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
		store.save(token("expired", now.minusMinutes(1)));
		store.save(token("valid", now.plusHours(1)));

		int purged = store.purgeExpired(now);

		assertEquals(1, purged);
		assertNull(store.get("expired"));
		assertNotNull(store.get("valid"));
	}

	@Test
	public void hashIsStableHexSha256() {
		assertEquals(64, WopiTokenStore.hash("x")
				.length());
		assertEquals(WopiTokenStore.hash("x"), WopiTokenStore.hash("x"));
		assertNotEquals(WopiTokenStore.hash("x"), WopiTokenStore.hash("y"));
	}
}
