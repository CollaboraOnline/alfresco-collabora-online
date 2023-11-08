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
package fr.jeci.collabora.wopi;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * https://msdn.microsoft.com/en-us/library/hh622920(v=office.12).aspx search for "optional": false to see mandatory
 * parameters. (As of 29/11/2016 when this was modified, SHA is no longer needed) Also return all values defined here:
 * https://github.com/LibreOffice/online/blob/3ce8c3158a6b9375d4b8ca862ea5b50490af4c35/wsd/Storage.cpp#L403 because LOOL
 * uses them internally to determine permission on rendering of certain elements. Well I assume given the variable
 * name(s), one should be able to semantically derive their relevance
 */
public class WopiCheckFileInfoWebScript extends AbstractWopiWebScript {
	private static final String VERSION = "Version";
	private static final String USER_FRIENDLY_NAME = "UserFriendlyName";
	private static final String USER_CAN_WRITE = "UserCanWrite";
	private static final String USER_ID = "UserId";
	private static final String SIZE = "Size";
	private static final String OWNER_ID = "OwnerId";

	private static final String BASE_FILE_NAME = "BaseFileName";

	private PermissionService permissionService;

	@Override
	public void executeAsUser(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef)
			throws IOException {
		ensureVersioningEnabled(nodeRef);

		final Map<String, String> model = this.collaboraOnlineService.serverInfo();
		final Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);

		final Version currentVersion = versionService.getCurrentVersion(nodeRef);

		if (currentVersion != null) {
			Date lastModifiedDate = currentVersion.getFrozenModifiedDate();
			LocalDateTime modifiedDatetime = new LocalDateTime(lastModifiedDate);
			model.put(LAST_MODIFIED_TIME, ISODateTimeFormat.dateTime().print(modifiedDatetime));
			model.put(VERSION, currentVersion.getVersionLabel());
		}

		// BaseFileName need extension, else COL load it in read-only mode
		model.put(BASE_FILE_NAME, (String) properties.get(ContentModel.PROP_NAME));

		model.put(OWNER_ID, properties.get(ContentModel.PROP_CREATOR).toString());
		final ContentData contentData = (ContentData) properties.get(ContentModel.PROP_CONTENT);
		model.put(SIZE, Long.toString(contentData.getSize()));

		String userName = AuthenticationUtil.getRunAsUser();
		model.put(USER_ID, userName);
		model.put(USER_CAN_WRITE, Boolean.toString(userCanWrite(nodeRef)));
		model.put(USER_FRIENDLY_NAME, userName);

		jsonResponse(res, 200, model);
	}

	private void ensureVersioningEnabled(final NodeRef nodeRef) {
		// Force Versioning
		if (!nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE)) {
			Map<QName, Serializable> initialVersionProps = new HashMap<>(1, 1.0f);
			versionService.ensureVersioningEnabled(nodeRef, initialVersionProps);
		}
	}

	private boolean userCanWrite(final NodeRef nodeRef) {
		AccessStatus perm = permissionService.hasPermission(nodeRef, PermissionService.WRITE);
		return AccessStatus.ALLOWED == perm;
	}

	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}
}
