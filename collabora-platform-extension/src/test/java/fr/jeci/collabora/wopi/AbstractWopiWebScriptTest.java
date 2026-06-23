// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
}
