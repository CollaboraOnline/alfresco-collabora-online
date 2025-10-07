// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

public enum WopiOverride {
	// Put with no Lock
	PUT,
	// Save As
	PUT_RELATIVE,
	// Ask for Lock
	LOCK,
	// Query Lock Key
	GET_LOCK,
	// Refresh Lock
	REFRESH_LOCK,
	// Remove Lock
	UNLOCK

}
