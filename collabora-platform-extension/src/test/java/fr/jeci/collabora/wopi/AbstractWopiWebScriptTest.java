// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.joda.time.LocalDateTime;
import org.junit.Test;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;

/**
 * Tests {@link AbstractWopiWebScript#sizeMismatchWarning(String, long)}, the integrity check against the optional,
 * informational {@code X-WOPI-Size} header. The WOPI spec treats the request body as authoritative, so a mismatch is
 * only worth a warning, never a rejected save.
 */
public class AbstractWopiWebScriptTest {

	@Test
	public void testSizeMismatchWarning_absentHeaderIsNoWarning() {
		assertNull(AbstractWopiWebScript.sizeMismatchWarning(null, 9497));
		assertNull(AbstractWopiWebScript.sizeMismatchWarning("", 9497));
		assertNull(AbstractWopiWebScript.sizeMismatchWarning("   ", 9497));
	}

	@Test
	public void testSizeMismatchWarning_matchingSizeIsNoWarning() {
		assertNull(AbstractWopiWebScript.sizeMismatchWarning("9497", 9497L));
		// surrounding whitespace must not break the comparison
		assertNull(AbstractWopiWebScript.sizeMismatchWarning(" 9497 ", 9497L));
	}

	@Test
	public void testSizeMismatchWarning_differentSizeWarnsWithBothValues() {
		String warning = AbstractWopiWebScript.sizeMismatchWarning("9497", 9000L);

		assertTrue(warning, warning != null && warning.contains("9497") && warning.contains("9000"));
	}

	@Test
	public void testSizeMismatchWarning_nonNumericHeaderWarns() {
		String warning = AbstractWopiWebScript.sizeMismatchWarning("not-a-number", 9497L);

		assertTrue(warning, warning != null && warning.contains("not-a-number"));
	}

	private static final NodeRef INPUT = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "input");
	private static final NodeRef OUTPUT = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "output");
	private static final LocalDateTime NOW = LocalDateTime.now();

	private static WOPIAccessTokenInfo scoped(String writeFileId) {
		return new WOPIAccessTokenInfo("t", NOW.minusMinutes(1), NOW.plusHours(1), "input", writeFileId, "alice", false);
	}

	@Test
	public void testCheckWriteScope_readIsAlwaysAllowed() {
		AbstractWopiWebScript.checkWriteScope(scoped(null), INPUT, false);
	}

	@Test
	public void testCheckWriteScope_readOnlyTokenCannotWrite() {
		try {
			AbstractWopiWebScript.checkWriteScope(scoped(null), INPUT, true);
			fail("expected WebScriptException");
		} catch (WebScriptException e) {
			assertEquals(Status.STATUS_UNAUTHORIZED, e.getStatus());
		}
	}

	@Test
	public void testCheckWriteScope_scopedTokenWritesItsOutput() {
		AbstractWopiWebScript.checkWriteScope(scoped("output"), OUTPUT, true);
	}

	@Test
	public void testCheckWriteScope_scopedTokenCannotWriteItsInput() {
		try {
			AbstractWopiWebScript.checkWriteScope(scoped("output"), INPUT, true);
			fail("expected WebScriptException");
		} catch (WebScriptException e) {
			assertEquals(Status.STATUS_UNAUTHORIZED, e.getStatus());
		}
	}

	@Test
	public void testCheckWriteScope_collaboraTokenWritesItsFile() {
		WOPIAccessTokenInfo collabora = new WOPIAccessTokenInfo("t", NOW.minusMinutes(1), NOW.plusHours(1), "input",
				"alice");

		AbstractWopiWebScript.checkWriteScope(collabora, INPUT, true);
	}

	@Test
	public void testWriteOperations() {
		assertFalse(new WopiGetFileWebScript().isWriteOperation());
		assertFalse(new WopiCheckFileInfoWebScript().isWriteOperation());
		assertTrue(new WopiPutFileWebScript().isWriteOperation());
		assertTrue(new WopiPutRelativeFileWebScript().isWriteOperation());
	}

	@Test
	public void testCheckPutRelativeAllowed_acceptsCollaboraToken() {
		LocalDateTime now = LocalDateTime.now();

		WopiPutRelativeFileWebScript.checkPutRelativeAllowed(new WOPIAccessTokenInfo("t", now, now.plusHours(1), "file-a",
				"alice"));
	}

	@Test
	public void testCheckPutRelativeAllowed_rejectsScopedToken() {
		LocalDateTime now = LocalDateTime.now();

		try {
			WopiPutRelativeFileWebScript.checkPutRelativeAllowed(new WOPIAccessTokenInfo("t", now, now.plusHours(1),
					"input", "input", "alice", false));
			fail("Expected WebScriptException for a non-renewable token");
		} catch (WebScriptException expectedException) {
			assertEquals(Status.STATUS_UNAUTHORIZED, expectedException.getStatus());
		}
	}
}
