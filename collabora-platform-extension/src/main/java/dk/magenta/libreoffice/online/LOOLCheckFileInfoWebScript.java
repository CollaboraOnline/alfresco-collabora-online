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
package dk.magenta.libreoffice.online;

import java.io.IOException;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import dk.magenta.libreoffice.online.service.LOOLService;
import dk.magenta.libreoffice.online.service.WOPIAccessTokenInfo;

public class LOOLCheckFileInfoWebScript extends DeclarativeWebScript {
    private static final Log logger = LogFactory.getLog(LOOLCheckFileInfoWebScript.class);

    private final static String ENABLE_OWNER_TERMINATION = "EnableOwnerTermination";
    private final static String POST_MESSAGE_ORIGIN = "PostMessageOrigin";
    private final static String VERSION = "Version";
    private final static String USER_FRIENDLY_NAME = "UserFriendlyName";
    private final static String USER_CAN_WRITE = "UserCanWrite";
    private final static String USER_ID = "UserId";
    private final static String SIZE = "Size";
    private final static String OWNER_ID = "OwnerId";
    private final static String LAST_MODIFIED_TIME = "LastModifiedTime";
    private final static String HIDE_PRINT_OPTION = "HidePrintOption";
    private final static String HIDE_SAVE_OPTION = "HideSaveOption";
    private final static String HIDE_EXPORT_OPTION = "HideExportOption";
    private final static String DISABLE_EXPORT = "DisableExport";
    private final static String DISABLE_PRINT = "DisablePrint";
    private final static String DISABLE_COPY = "DisableCopy";
    private final static String BASE_FILE_NAME = "BaseFileName";

    private LOOLService loolService;
    private NodeService nodeService;
    private VersionService versionService;
    private PermissionService permissionService;

    /**
     * https://msdn.microsoft.com/en-us/library/hh622920(v=office.12).aspx search
     * for "optional": false to see mandatory parameters. (As of 29/11/2016 when
     * this was modified, SHA is no longer needed) Also return all values defined
     * here:
     * https://github.com/LibreOffice/online/blob/3ce8c3158a6b9375d4b8ca862ea5b50490af4c35/wsd/Storage.cpp#L403
     * because LOOL uses them internally to determine permission on rendering of
     * certain elements. Well I assume given the variable name(s), one should be
     * able to semantically derive their relevance
     * 
     * @param req
     * @param status
     * @param cache
     * @return
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        // can't extends two classes in java
        LOOLAbstractWebScript dws = new LOOLAbstractWebScript() {
            @Override
            public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
                // NOP
            }
        };
        dws.setNodeService(this.nodeService);
        dws.setLoolService(this.loolService);
        final WOPIAccessTokenInfo wopiToken = dws.wopiToken(req);
        final NodeRef nodeRef = dws.getFileNodeRef(wopiToken);

        if (logger.isDebugEnabled()) {
            logger.debug("Check File for user '" + wopiToken.getUserName() + "' and nodeRef '" + nodeRef + "'");
        }

        ensureVersioningEnabled(wopiToken, nodeRef);

        Map<String, Object> model = new HashMap<>();
        Map<QName, Serializable> properties = dws.runAsGetProperties(wopiToken, nodeRef);

        final Date lastModifiedDate = (Date) properties.get(ContentModel.PROP_MODIFIED);
        // Convert lastModifiedTime to ISO 8601 according to:
        // https://github.com/LibreOffice/online/blob/master/wsd/Storage.cpp#L460 or
        // look in the
        // std::unique_ptr<WopiStorage::WOPIFileInfo> WopiStorage::getWOPIFileInfo
        final String dte = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochMilli(lastModifiedDate.getTime()));
        // TODO Some properties are hard coded for now but we should look into making
        // them sysadmin configurable

        // BaseFileName need extension, else Lool load it in read-only mode (since
        // 2.1.4)
        model.put(BASE_FILE_NAME, (String) properties.get(ContentModel.PROP_NAME));
        // We need to enable this if we want to be able to insert image into the
        // documents
        model.put(DISABLE_COPY, false);
        model.put(DISABLE_PRINT, false);
        model.put(DISABLE_EXPORT, false);
        model.put(HIDE_EXPORT_OPTION, false);
        model.put(HIDE_SAVE_OPTION, false);
        model.put(HIDE_PRINT_OPTION, false);
        model.put(LAST_MODIFIED_TIME, dte);
        model.put(OWNER_ID, properties.get(ContentModel.PROP_CREATOR).toString());
        final ContentData contentData = (ContentData) properties.get(ContentModel.PROP_CONTENT);
        model.put(SIZE, contentData.getSize());
        model.put(USER_ID, wopiToken.getUserName());
        model.put(USER_CAN_WRITE, userCanWrite(wopiToken, nodeRef));
        model.put(USER_FRIENDLY_NAME, wopiToken.getUserName());
        model.put(VERSION, (String) properties.get(ContentModel.PROP_VERSION_LABEL));
        // Host from which token generation request originated
        model.put(POST_MESSAGE_ORIGIN, loolService.getAlfExternalHost().toString());
        // Search https://www.collaboraoffice.com/category/community-en/ for
        // EnableOwnerTermination
        model.put(ENABLE_OWNER_TERMINATION, false);

        if (logger.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Check File details : {");
            sb.append("BASE_FILE_NAME: ").append(model.get(BASE_FILE_NAME)).append(", ");
            sb.append("OWNER_ID: ").append(model.get(OWNER_ID)).append(", ");
            sb.append("USER_ID: ").append(model.get(USER_ID)).append(", ");
            sb.append("USER_CAN_WRITE: ").append(model.get(USER_CAN_WRITE)).append(", ");
            sb.append("SIZE: ").append(model.get(SIZE)).append(", ");
            sb.append("VERSION: ").append(model.get(VERSION)).append(", ");
            sb.append("POST_MESSAGE_ORIGIN: ").append(model.get(POST_MESSAGE_ORIGIN));
            sb.append("LAST_MODIFIED_TIME: ").append(model.get(LAST_MODIFIED_TIME));
            sb.append("}");

            logger.debug(sb.toString());
        }

        return model;
    }

    private void ensureVersioningEnabled(final WOPIAccessTokenInfo wopiToken, final NodeRef nodeRef) {
        AuthenticationUtil.pushAuthentication();
        try {
            AuthenticationUtil.setRunAsUser(wopiToken.getUserName());

            // Force Versionning
            if (!nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE)) {
                Map<QName, Serializable> initialVersionProps = new HashMap<QName, Serializable>(1, 1.0f);
                versionService.ensureVersioningEnabled(nodeRef, initialVersionProps);
            }
        } finally {
            AuthenticationUtil.popAuthentication();
        }
    }

    private boolean userCanWrite(final WOPIAccessTokenInfo wopiToken, final NodeRef nodeRef) {
        AuthenticationUtil.pushAuthentication();
        try {
            AuthenticationUtil.setRunAsUser(wopiToken.getUserName());
            AccessStatus perm = permissionService.hasPermission(nodeRef, PermissionService.WRITE);

            return AccessStatus.ALLOWED == perm;
        } finally {
            AuthenticationUtil.popAuthentication();
        }
    }

    public void setLoolService(LOOLService loolService) {
        this.loolService = loolService;
    }

    public void setVersionService(VersionService versionService) {
        this.versionService = versionService;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
}
