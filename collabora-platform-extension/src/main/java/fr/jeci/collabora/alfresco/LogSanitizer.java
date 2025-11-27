// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

/**
 * Utility class to sanitize values before logging to prevent log injection attacks.
 * <p>
 * Log injection occurs when an attacker injects newlines or control characters into
 * logged values, which can lead to:
 * <ul>
 * <li>Log falsification (injecting fake log entries)</li>
 * <li>Audit trail manipulation</li>
 * <li>Exploitation by log analysis tools</li>
 * </ul>
 *
 * @author jlesage
 */
public final class LogSanitizer {

	private LogSanitizer() {
		// Utility class - prevent instantiation
	}

	/**
	 * Sanitizes a value for safe logging by escaping control characters.
	 * <p>
	 * This prevents log injection attacks where malicious input containing
	 * newlines or control characters could falsify log entries.
	 *
	 * @param value the value to sanitize (may be null)
	 * @return the sanitized value with control characters escaped, or "[null]" if input was null
	 */
	public static String sanitize(String value) {
		if (value == null) {
			return "[null]";
		}
		return value.replace("\\", "\\\\")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t");
	}
}
