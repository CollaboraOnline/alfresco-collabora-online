// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

import java.util.regex.Pattern;

/**
 * Utility class for validating NodeRef inputs to prevent invalid data
 * from causing exceptions or security issues.
 */
public final class NodeRefValidator {

	/** UUID pattern for validating file IDs */
	private static final Pattern UUID_PATTERN = Pattern.compile(
			"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	private NodeRefValidator() {
		// Utility class - prevent instantiation
	}

	/**
	 * Validates that the fileId is a valid UUID format.
	 *
	 * @param fileId the file ID to validate
	 * @throws WebScriptException with BAD_REQUEST status if validation fails
	 */
	public static void validateUUID(String fileId) {
		if (fileId == null || fileId.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "File ID is required");
		}
		if (!UUID_PATTERN.matcher(fileId)
				.matches()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid file ID format");
		}
	}

	/**
	 * Validates that a NodeRef string is properly formatted.
	 *
	 * @param nodeRefStr the NodeRef string to validate
	 * @throws WebScriptException with BAD_REQUEST status if validation fails
	 */
	public static void validateNodeRefFormat(String nodeRefStr) {
		if (nodeRefStr == null || nodeRefStr.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "NodeRef is required");
		}
		if (!NodeRef.isNodeRef(nodeRefStr)) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid NodeRef format");
		}
	}

	/**
	 * Validates that a node exists in the repository.
	 *
	 * @param nodeRef     the NodeRef to check
	 * @param nodeService the NodeService to use for existence check
	 * @throws WebScriptException with NOT_FOUND status if node does not exist
	 */
	public static void validateNodeExists(NodeRef nodeRef, NodeService nodeService) {
		if (nodeRef == null) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "NodeRef is required");
		}
		if (!nodeService.exists(nodeRef)) {
			throw new WebScriptException(Status.STATUS_NOT_FOUND, "Node not found: " + nodeRef.getId());
		}
	}

	/**
	 * Creates a validated NodeRef from a file ID (UUID).
	 * Only validates the UUID format, does not check node existence.
	 *
	 * @param fileId the file ID (UUID) to convert
	 * @return a NodeRef pointing to the workspace SpacesStore
	 * @throws WebScriptException with BAD_REQUEST status if fileId is invalid
	 */
	public static NodeRef createNodeRefFromFileId(String fileId) {
		validateUUID(fileId);
		return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, fileId);
	}

	/**
	 * Creates a validated NodeRef from a full nodeRef string.
	 * Validates both format and existence.
	 *
	 * @param nodeRefStr  the full NodeRef string
	 * @param nodeService the NodeService to use for existence check
	 * @return the validated NodeRef
	 * @throws WebScriptException with BAD_REQUEST or NOT_FOUND status if validation fails
	 */
	public static NodeRef createValidatedNodeRef(String nodeRefStr, NodeService nodeService) {
		validateNodeRefFormat(nodeRefStr);
		NodeRef nodeRef = new NodeRef(nodeRefStr);
		validateNodeExists(nodeRef, nodeService);
		return nodeRef;
	}
}
