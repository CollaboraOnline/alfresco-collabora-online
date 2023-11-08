/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package fr.jeci.collabora.alfresco;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.WebScriptException;

public interface CollaboraOnlineService {
	static final String HIDE_PRINT_OPTION = "HidePrintOption";
	static final String HIDE_SAVE_OPTION = "HideSaveOption";
	static final String HIDE_EXPORT_OPTION = "HideExportOption";
	static final String DISABLE_EXPORT = "DisableExport";
	static final String DISABLE_PRINT = "DisablePrint";
	static final String DISABLE_COPY = "DisableCopy";
	static final String POST_MESSAGE_ORIGIN = "PostMessageOrigin";
	static final String ENABLE_OWNER_TERMINATION = "EnableOwnerTermination";
	static final String USER_CAN_NOT_WRITE_RELATIVE = "UserCanNotWriteRelative";
	static final String SUPPORTS_LOCKS = "SupportsLocks";

	static final String LOOL_AUTOSAVE = "collabora:autosave";
	static final String AUTOSAVE_DESCRIPTION = "Edit with Collabora";

	/**
	 * Generate and store an access token only valid for the current user/file id
	 * combination.
	 *
	 * If an existing access token exists for the user/file id combination, then
	 * extend its expiration date and return it.
	 * 
	 * @param nodeRef
	 * @return
	 */
	WOPIAccessTokenInfo createAccessToken(NodeRef nodeRef);

	/**
	 * Check if access token if valid and match nodeRef
	 *
	 *
	 * @param accessToken
	 * @param nodeRef
	 * @throws WebScriptException
	 * @return
	 */
	WOPIAccessTokenInfo checkAccessToken(final String accessToken, final NodeRef nodeRef);

	/**
	 * Returns the WOPI src URL for a given nodeRef and action.
	 *
	 * @param nodeRef
	 * @param action
	 * @return
	 * @throws IOException
	 */
	String getWopiSrcURL(NodeRef nodeRef, String action) throws IOException;

	/**
	 * Return a map with default value for WOPI CheckFileInfo
	 * 
	 * @return
	 */
	Map<String, String> serverInfo();

	/**
	 * URL use by Collabora Online to comunicate with Alfresco
	 * 
	 * @return
	 */
	URL getAlfrescoPrivateURL();

	/**
	 * https://wopi.readthedocs.io/projects/wopirest/en/latest/files/Lock.html
	 * 
	 * @param nodeRef
	 * @param lockId  New lock_id, or current lock_id for a refresh
	 * @return lockId on the node or null if lockId is blank
	 */
	String lock(NodeRef nodeRef, String lockId) throws ConflictException;

	/**
	 * https://wopi.readthedocs.io/projects/wopirest/en/latest/files/GetLock.html
	 * 
	 * @param nodeRef
	 * @return lockId on the node
	 */
	String lockGet(NodeRef nodeRef);

	/**
	 * https://wopi.readthedocs.io/projects/wopirest/en/latest/files/RefreshLock.html
	 * 
	 * @param nodeRef
	 * @param lock
	 */
	void lockRefresh(NodeRef nodeRef, String lockId) throws ConflictException;

	/**
	 * https://wopi.readthedocs.io/projects/wopirest/en/latest/files/Unlock.html
	 * 
	 * @param nodeRef
	 * @param lockId
	 * @return
	 */
	String lockUnlock(NodeRef nodeRef, String lockId) throws ConflictException;

	/**
	 * Remove lock without checking the current lockId but timestamp. (Use to clean
	 * old LOCK)
	 * 
	 * @deprecated
	 * 
	 * @param nodeRef
	 * @param force   if true, remove lock without checking timestamp
	 */
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
