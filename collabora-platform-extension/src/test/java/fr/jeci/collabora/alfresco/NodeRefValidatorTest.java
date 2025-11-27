// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
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

	// ========== validateNodeRefFormat Tests ==========

	@Test
	public void testValidateNodeRefFormat_valid() {
		// Should not throw
		NodeRefValidator.validateNodeRefFormat("workspace://SpacesStore/7f948b3e-a23e-4676-8d6d-8a4ea0a01da6");
	}

	@Test(expected = WebScriptException.class)
	public void testValidateNodeRefFormat_null() {
		NodeRefValidator.validateNodeRefFormat(null);
	}

	@Test(expected = WebScriptException.class)
	public void testValidateNodeRefFormat_empty() {
		NodeRefValidator.validateNodeRefFormat("");
	}

	@Test(expected = WebScriptException.class)
	public void testValidateNodeRefFormat_invalid() {
		NodeRefValidator.validateNodeRefFormat("not-a-noderef");
	}

	@Test(expected = WebScriptException.class)
	public void testValidateNodeRefFormat_missingStore() {
		NodeRefValidator.validateNodeRefFormat("7f948b3e-a23e-4676-8d6d-8a4ea0a01da6");
	}

	// ========== validateNodeExists Tests ==========

	@Test
	public void testValidateNodeExists_exists() {
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "7f948b3e-a23e-4676-8d6d-8a4ea0a01da6");
		NodeService nodeService = mock(NodeService.class);
		when(nodeService.exists(nodeRef)).thenReturn(true);

		// Should not throw
		NodeRefValidator.validateNodeExists(nodeRef, nodeService);
	}

	@Test(expected = WebScriptException.class)
	public void testValidateNodeExists_notExists() {
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "7f948b3e-a23e-4676-8d6d-8a4ea0a01da6");
		NodeService nodeService = mock(NodeService.class);
		when(nodeService.exists(nodeRef)).thenReturn(false);

		NodeRefValidator.validateNodeExists(nodeRef, nodeService);
	}

	@Test
	public void testValidateNodeExists_notFoundStatus() {
		NodeRef nodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "7f948b3e-a23e-4676-8d6d-8a4ea0a01da6");
		NodeService nodeService = mock(NodeService.class);
		when(nodeService.exists(nodeRef)).thenReturn(false);

		try {
			NodeRefValidator.validateNodeExists(nodeRef, nodeService);
		} catch (WebScriptException e) {
			assertEquals(Status.STATUS_NOT_FOUND, e.getStatus());
		}
	}

	@Test(expected = WebScriptException.class)
	public void testValidateNodeExists_nullNodeRef() {
		NodeService nodeService = mock(NodeService.class);
		NodeRefValidator.validateNodeExists(null, nodeService);
	}

	// ========== createNodeRefFromFileId Tests ==========

	@Test
	public void testCreateNodeRefFromFileId_valid() {
		String fileId = "7f948b3e-a23e-4676-8d6d-8a4ea0a01da6";
		NodeRef nodeRef = NodeRefValidator.createNodeRefFromFileId(fileId);

		assertNotNull(nodeRef);
		assertEquals(fileId, nodeRef.getId());
		assertEquals(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, nodeRef.getStoreRef());
	}

	@Test(expected = WebScriptException.class)
	public void testCreateNodeRefFromFileId_invalid() {
		NodeRefValidator.createNodeRefFromFileId("invalid-uuid");
	}

	// ========== createValidatedNodeRef Tests ==========

	@Test
	public void testCreateValidatedNodeRef_valid() {
		String nodeRefStr = "workspace://SpacesStore/7f948b3e-a23e-4676-8d6d-8a4ea0a01da6";
		NodeService nodeService = mock(NodeService.class);
		when(nodeService.exists(new NodeRef(nodeRefStr))).thenReturn(true);

		NodeRef nodeRef = NodeRefValidator.createValidatedNodeRef(nodeRefStr, nodeService);

		assertNotNull(nodeRef);
		assertEquals("7f948b3e-a23e-4676-8d6d-8a4ea0a01da6", nodeRef.getId());
	}

	@Test(expected = WebScriptException.class)
	public void testCreateValidatedNodeRef_invalidFormat() {
		NodeService nodeService = mock(NodeService.class);
		NodeRefValidator.createValidatedNodeRef("invalid", nodeService);
	}

	@Test(expected = WebScriptException.class)
	public void testCreateValidatedNodeRef_notExists() {
		String nodeRefStr = "workspace://SpacesStore/7f948b3e-a23e-4676-8d6d-8a4ea0a01da6";
		NodeService nodeService = mock(NodeService.class);
		when(nodeService.exists(new NodeRef(nodeRefStr))).thenReturn(false);

		NodeRefValidator.createValidatedNodeRef(nodeRefStr, nodeService);
	}
}
