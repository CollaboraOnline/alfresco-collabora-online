package dk.magenta.libreoffice.online.service;

import java.io.IOException;
import java.net.URL;

import org.alfresco.service.cmr.repository.NodeRef;
import org.springframework.extensions.webscripts.WebScriptException;

/**
 * Created by seth on 30/04/16.
 */
public interface LOOLService {
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
     * In the case that Alfresco is behind a proxy and not using the proxy hostname
     * in the alfresco config section of the alfresco-global.properties file, then
     * we should be able to set a property in alfresco-global.properties for this
     * service to use.
     * 
     * @return
     */
    String getAlfrescoProxyDomain();

    /**
     * PostMessageOrigin need full URI.
     * 
     * @return
     */
    URL getAlfExternalHost();
}
