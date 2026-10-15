// SPDX-FileCopyrightText: 2026 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.joda.time.LocalDateTime;
import org.junit.Test;

public class WOPIAccessTokenInfoTest {

	private static final LocalDateTime NOW = LocalDateTime.now();

	@Test
	public void legacyConstructorGrantsReadWriteOnFileAndIsRenewable() {
		WOPIAccessTokenInfo info = new WOPIAccessTokenInfo("t", NOW, NOW.plusHours(1), "file-a", "alice");

		assertTrue(info.canRead("file-a"));
		assertTrue(info.canWrite("file-a"));
		assertTrue(info.isRenewable());
		assertEquals("file-a", info.getWriteFileId());
	}

	@Test
	public void scopedTokenReadsInputAndWritesOutputOnly() {
		WOPIAccessTokenInfo info = new WOPIAccessTokenInfo("t", NOW, NOW.plusHours(1), "input", "output", "alice", false);

		assertTrue(info.canRead("input"));
		assertTrue(info.canRead("output"));
		assertFalse(info.canRead("other"));
		assertFalse(info.canWrite("input"));
		assertTrue(info.canWrite("output"));
		assertFalse(info.isRenewable());
	}

	@Test
	public void readOnlyTokenWritesNothing() {
		WOPIAccessTokenInfo info = new WOPIAccessTokenInfo("t", NOW, NOW.plusHours(1), "input", null, "alice", false);

		assertTrue(info.canRead("input"));
		assertFalse(info.canWrite("input"));
		assertFalse(info.canWrite(null));
		assertFalse(info.canRead(null));
	}

	@Test
	public void tokenIsValidAtItsIssueInstant() {
		WOPIAccessTokenInfo info = new WOPIAccessTokenInfo("t", NOW, NOW.plusHours(1), "file-a", "alice");

		assertTrue(info.isValid(NOW));
	}
}
