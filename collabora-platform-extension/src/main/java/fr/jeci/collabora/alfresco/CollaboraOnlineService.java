// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;

public interface CollaboraOnlineService {
	String HIDE_PRINT_OPTION = "HidePrintOption";
	String HIDE_SAVE_OPTION = "HideSaveOption";
	String HIDE_EXPORT_OPTION = "HideExportOption";
	String DISABLE_EXPORT = "DisableExport";
	String DISABLE_PRINT = "DisablePrint";
	String DISABLE_COPY = "DisableCopy";
	String POST_MESSAGE_ORIGIN = "PostMessageOrigin";
	String ENABLE_OWNER_TERMINATION = "EnableOwnerTermination";
	String USER_CAN_NOT_WRITE_RELATIVE = "UserCanNotWriteRelative";
	String SUPPORTS_LOCKS = "SupportsLocks";

	String LOOL_AUTOSAVE = "collabora:autosave";
	String AUTOSAVE_DESCRIPTION = "Edit with Collabora";

	/**
	 * Generate and store an access token only valid for the current user/file id combination.
	 * <p>
	 * If an existing access token exists for the user/file id combination, then extend its expiration date and return
	 * it.
	 *
	 * @param nodeRef Node to lock
	 * @return Access Token
	 */
	WOPIAccessTokenInfo createAccessToken(NodeRef nodeRef);

	/**
	 * Check if access token is valid and match nodeRef
	 *
	 * @param accessToken Current Access Token
	 * @param nodeRef     Node to lock
	 * @return Access Token
	 */
	WOPIAccessTokenInfo checkAccessToken(final String accessToken, final NodeRef nodeRef);

	/**
	 * Resolve an access token to its info without binding it to a file.
	 * <p>
	 * Used by the WOPI Settings endpoints, where the {@code fileId} is a virtual settings path (or {@code -1}) rather
	 * than a document nodeRef. Only the token's existence and expiry are validated; the resolved {@code userName}
	 * drives the runAs.
	 *
	 * @param accessToken Current Access Token
	 * @return Access Token info
	 * @throws org.springframework.extensions.webscripts.WebScriptException if the token is missing, unknown or expired
	 */
	WOPIAccessTokenInfo resolveToken(final String accessToken);

	/**
	 * Returns the WOPI src URL for a given nodeRef and action.
	 *
	 * @param nodeRef Node to lock
	 * @return WOPI src URL
	 */
	String getWopiSrcURL(NodeRef nodeRef, String action) throws IOException;

	/**
	 * Return a map with default value for WOPI CheckFileInfo
	 *
	 * @return default value for WOPI CheckFileInfo
	 */
	Map<String, String> serverInfo();

	/**
	 * URL use by Collabora Online to communicate with Alfresco
	 *
	 * @return Internal URL to Alfresco
	 */
	URL getAlfrescoPrivateURL();

	/**
	 * <a href="https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/rest/files/Lock">...</a>
	 *
	 * @param nodeRef Node to lock
	 * @param lockId  New lock_id, or current lock_id for a refresh
	 * @return lockId on the node or null if lockId is blank
	 */
	String lock(NodeRef nodeRef, String lockId) throws ConflictException;

	/**
	 * <a href="https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/rest/files/GetLock">...</a>
	 *
	 * @param nodeRef Node to lock
	 * @return lockId on the node
	 */
	String lockGet(NodeRef nodeRef);

	/**
	 * <a
	 * href="https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/rest/files/RefreshLock">...</a>
	 *
	 * @param nodeRef Node to lock
	 * @param lockId  current lock-id
	 */
	void lockRefresh(NodeRef nodeRef, String lockId) throws ConflictException;

	/**
	 * <a href="https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/rest/files/Unlock">...</a>
	 *
	 * @param nodeRef Node to lock
	 * @param lockId  current lock-id
	 * @return current lock-id
	 */
	String lockUnlock(NodeRef nodeRef, String lockId) throws ConflictException;

	/**
	 * Remove lock without checking the current lockId but timestamp. (Use to clean old LOCK)
	 *
	 * @param nodeRef Node to lock
	 * @param force   if true, remove lock without checking timestamp
	 */
	@Deprecated
	void unlock(NodeRef nodeRef, boolean force);

	/**
	 * Un-lock then Re-lock with new owner. (If current user is the owner, just renew the lock)
	 * <p/>
	 * lockId must match current lock-id
	 *
	 * @param nodeRef Node to lock
	 * @param lockId  current lock-id
	 */
	void lockSteal(NodeRef nodeRef, String lockId) throws ConflictException;
}
