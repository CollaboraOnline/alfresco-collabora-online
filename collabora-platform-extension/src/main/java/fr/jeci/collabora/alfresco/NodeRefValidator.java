// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

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

}
