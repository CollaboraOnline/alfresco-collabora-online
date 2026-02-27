// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

/**
 * Unit tests for NodeRefValidator
 */
public class NodeRefValidatorTest {

	// ========== validateUUID Tests ==========

	@Test
	public void testValidateUUID_validUUID() {
		// Should not throw any exception
		NodeRefValidator.validateUUID("7f948b3e-a23e-4676-8d6d-8a4ea0a01da6");
	}

	@Test
	public void testValidateUUID_validUUID_uppercase() {
		// Should not throw - UUID can be uppercase
		NodeRefValidator.validateUUID("7F948B3E-A23E-4676-8D6D-8A4EA0A01DA6");
	}

	@Test
	public void testValidateUUID_validUUID_mixed() {
		// Should not throw - UUID can be mixed case
		NodeRefValidator.validateUUID("7f948B3e-A23e-4676-8D6d-8a4eA0a01Da6");
	}

	@Test(expected = WebScriptException.class)
	public void testValidateUUID_null() {
		NodeRefValidator.validateUUID(null);
	}

	@Test(expected = WebScriptException.class)
	public void testValidateUUID_empty() {
		NodeRefValidator.validateUUID("");
	}

	@Test(expected = WebScriptException.class)
	public void testValidateUUID_blank() {
		NodeRefValidator.validateUUID("   ");
	}

	@Test(expected = WebScriptException.class)
	public void testValidateUUID_invalidFormat() {
		NodeRefValidator.validateUUID("not-a-valid-uuid");
	}

	@Test(expected = WebScriptException.class)
	public void testValidateUUID_tooShort() {
		NodeRefValidator.validateUUID("7f948b3e-a23e-4676-8d6d");
	}

	@Test(expected = WebScriptException.class)
	public void testValidateUUID_tooLong() {
		NodeRefValidator.validateUUID("7f948b3e-a23e-4676-8d6d-8a4ea0a01da6-extra");
	}

	@Test(expected = WebScriptException.class)
	public void testValidateUUID_invalidChars() {
		NodeRefValidator.validateUUID("7f948b3e-a23e-4676-8d6d-8a4ea0a01dXZ");
	}

	@Test(expected = WebScriptException.class)
	public void testValidateUUID_sqlInjection() {
		NodeRefValidator.validateUUID("'; DROP TABLE nodes; --");
	}

	@Test(expected = WebScriptException.class)
	public void testValidateUUID_pathTraversal() {
		NodeRefValidator.validateUUID("../../../etc/passwd");
	}

	@Test
	public void testValidateUUID_errorStatus() {
		try {
			NodeRefValidator.validateUUID("invalid");
		} catch (WebScriptException e) {
			assertEquals(Status.STATUS_BAD_REQUEST, e.getStatus());
		}
	}
}
