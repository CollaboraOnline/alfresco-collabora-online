// SPDX-FileCopyrightText: 2026 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

/**
 * Kind of a WOPI access token, deciding what it may be used for.
 */
public enum WopiTokenKind {

	/**
	 * Token given to Collabora to edit one file: renewable, allowed on the settings endpoints and on PutRelativeFile.
	 */
	COLLABORA,

	/**
	 * Token giving access to the user's Collabora settings only.
	 */
	SETTINGS,

	/**
	 * Token scoped to a processing task: reads an input file, writes an optional output file, never renewed.
	 */
	TASK;

	/**
	 * @return whether an expired token of this kind may be extended instead of rejected
	 */
	public boolean isRenewable() {
		return this == COLLABORA;
	}
}
