package dk.magenta.libreoffice.online;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.QName;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import dk.magenta.libreoffice.online.service.LOOLService;
import dk.magenta.libreoffice.online.service.WOPIAccessTokenInfo;

public abstract class LOOLAbstractWebScript extends AbstractWebScript {
    final static String ACCESS_TOKEN = "access_token";
    final static String FILE_ID = "fileId";

    protected NodeService nodeService;
    protected LOOLService loolService;

    /**
     * Returns a NodeRef given a file Id. Note: Checks to see if the node exists
     * aren't performed
     * 
     * @param fileId
     * @return
     */
    protected NodeRef getFileNodeRef(String fileId) {
        return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, fileId);
    }

    /**
     * Will return a file nodeRef only il file exist and is accessible
     *
     * @param tokenInfo
     * @return
     */
    protected NodeRef getFileNodeRef(final WOPIAccessTokenInfo wopiToken) {

        AuthenticationUtil.pushAuthentication();
        try {
            AuthenticationUtil.setRunAsUser(wopiToken.getUserName());

            final NodeRef fileNodeRef = getFileNodeRef(wopiToken.getFileId());
            if (nodeService.exists(fileNodeRef)) {
                return fileNodeRef;
            }
            return null;

        } finally {
            AuthenticationUtil.popAuthentication();
        }
    }

    protected Map<QName, Serializable> runAsGetProperties(final WOPIAccessTokenInfo wopiToken, final NodeRef nodeRef) {
        AuthenticationUtil.pushAuthentication();
        try {
            AuthenticationUtil.setRunAsUser(wopiToken.getUserName());

            return nodeService.getProperties(nodeRef);
        } finally {
            AuthenticationUtil.popAuthentication();
        }
    }

    /**
     * Check and renew token if needed
     * 
     * @param req
     * @return
     */
    protected WOPIAccessTokenInfo wopiToken(WebScriptRequest req) {
        final String fileId = req.getServiceMatch().getTemplateVars().get(FILE_ID);
        final NodeRef nodeRef = getFileNodeRef(fileId);
        final String accessToken = req.getParameter(ACCESS_TOKEN);

        if (fileId == null) {
            throw new WebScriptException("No 'fileId' parameter supplied");
        }

        WOPIAccessTokenInfo wopiToken = loolService.checkAccessToken(accessToken, nodeRef);

        if (!wopiToken.isValid()) {
            // try to renew
            AuthenticationUtil.pushAuthentication();
            try {
                AuthenticationUtil.setRunAsUser(wopiToken.getUserName());
                return loolService.createAccessToken(nodeRef);
            } finally {
                AuthenticationUtil.popAuthentication();
            }
        }
        return wopiToken;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setLoolService(LOOLService loolService2) {
        this.loolService = loolService2;
    }

}
