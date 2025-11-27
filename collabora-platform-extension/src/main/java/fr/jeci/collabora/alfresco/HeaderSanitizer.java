// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import java.util.regex.Pattern;

/**
 * Utility class to sanitize HTTP header values to prevent HTTP response splitting attacks.
 * <p>
 * HTTP response splitting occurs when an attacker injects CRLF (Carriage Return Line Feed)
 * sequences into header values, which can lead to:
 * <ul>
 *   <li>Cache poisoning</li>
 *   <li>Session fixation</li>
 *   <li>Cross-site scripting (XSS)</li>
 * </ul>
 *
 * @author jlesage
 */
public final class HeaderSanitizer {

	private static final Pattern CRLF_PATTERN = Pattern.compile("[\\r\\n]");

	private HeaderSanitizer() {
		// Utility class - prevent instantiation
	}

	/**
	 * Sanitizes a header value by removing CR and LF characters.
	 * <p>
	 * This prevents HTTP response splitting attacks where malicious input
	 * containing CRLF sequences could inject additional headers.
	 *
	 * @param value the header value to sanitize (may be null)
	 * @return the sanitized value with CR/LF removed, or null if input was null
	 */
	public static String sanitize(String value) {
		if (value == null) {
			return null;
		}
		return CRLF_PATTERN.matcher(value).replaceAll("");
	}
}
