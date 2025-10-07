// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

public interface WopiHeader {
	String X_LOOL_WOPI_IS_AUTOSAVE = "X-LOOL-WOPI-IsAutosave";
	String X_LOOL_WOPI_TIMESTAMP = "X-LOOL-WOPI-Timestamp";
	String X_WOPI_OVERRIDE = "X-WOPI-Override";
	String X_WOPI_LOCK = "X-WOPI-Lock";
	String X_WOPI_OLD_LOCK = "X-WOPI-OldLock";
	String X_WOPI_LOCK_FAILURE_REASON = "X-WOPI-LockFailureReason";
	String X_WOPI_ITEM_VERSION = "X-WOPI-ItemVersion";

	String X_PRISTY_ADD_PROPERTY = "X-PRISTY-ADD-PROPERTY";
	String X_PRISTY_DEL_PROPERTY = "X-PRISTY-DEL-PROPERTY";
	String X_PRISTY_DEL_ASPECT = "X-PRISTY-DEL-ASPECT";
	String X_PRISTY_ADD_ASPECT = "X-PRISTY-ADD-ASPECT";
}
