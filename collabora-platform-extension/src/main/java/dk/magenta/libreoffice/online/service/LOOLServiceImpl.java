package dk.magenta.libreoffice.online.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class LOOLServiceImpl implements LOOLService {
    private static final Log logger = LogFactory.getLog(LOOLServiceImpl.class);

    private static final long ONE_HOUR_MS = 1000 * 60 * 60;

    private static final long DEFAULT_TOKEN_TTL_MS = ONE_HOUR_MS * 24;
    private long tokenTtlMs = -1;

    private static final int DEFAULT_WOPI_PORT = 9980;

    private URL wopiBaseURL;
    // In case alfresco is behind a proxy then we need the proxy's host address
    private URL alfExternalHost;
    private URL wopiDiscoveryURL;
    private WOPILoader wopiLoader;
    private NodeService nodeService;
    private PermissionService permissionService;
    private SysAdminParams sysAdminParams;

    private SecureRandom random = new SecureRandom();

    /**
     * This holds a map of the the "token info(s)" mapped to a file. Each token info
     * is mapped to a user, so in essence a user may only have one token info per
     * file. <FileId, <userName, tokenInfo> >
     * <p>
     * { fileId: { <== The id of the nodeRef that refers to the file userName:
     * WOPIAccessTokenInfo } }
     *
     *
     * fileIdAccessTokenMap is an Hazelcast IMap see:
     * https://docs.hazelcast.org/docs/2.4/javadoc/com/hazelcast/core/IMap.html The
     * get(Object key) method returns a clone of original value, modifying the
     * returned value does not change the actual value in the map. One should put
     * modified value back to make changes visible to all nodes.
     */
    private SimpleCache<String, WOPIAccessTokenInfo> tokenMap;

    public void setTokenMap(SimpleCache<String, WOPIAccessTokenInfo> tokenMap) {
        this.tokenMap = tokenMap;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setWopiBaseURL(URL wopiBaseURL) {
        this.wopiBaseURL = wopiBaseURL;
    }

    public void setWopiDiscoveryURL(URL wopiDiscoveryURL) {
        this.wopiDiscoveryURL = wopiDiscoveryURL;
    }

    public void setAlfExternalHost(URL alfExternalHost) {
        this.alfExternalHost = alfExternalHost;
    }

    public void setTokenTtlMs(long tokenTtlMs) {
        this.tokenTtlMs = tokenTtlMs;
    }

    /**
     * Generate and store an access token only valid for the current user/file id
     * combination.
     *
     * We check if we have at least READ permission, so a user must be connected.
     * 
     * @param nodeRef
     * @return
     */
    @Override
    public WOPIAccessTokenInfo createAccessToken(NodeRef nodeRef) {
        if (AuthenticationUtil.isRunAsUserTheSystemUser()) {
            logger.warn("Create token for System user, it is not desirable");
        } else {
            AccessStatus perm = this.permissionService.hasPermission(nodeRef, PermissionService.READ);
            if (AccessStatus.ALLOWED != perm) {
                throw new WebScriptException(Status.STATUS_UNAUTHORIZED, "Not allow to READ " + nodeRef);
            }
        }

        final String userName = AuthenticationUtil.getRunAsUser();
        final String fileId = nodeRef.getId();
        final Date now = new Date();
        WOPIAccessTokenInfo tokenInfo = new WOPIAccessTokenInfo(generateAccessToken(), now, newExpiresAt(now), fileId,
                userName);
        this.tokenMap.put(tokenInfo.getAccessToken(), tokenInfo);

        if (logger.isDebugEnabled()) {
            logger.debug("Created Access Token for user '" + userName + "' and nodeRef '" + nodeRef + "'");
        }
        return tokenInfo;
    }

    /**
     * Compute token time to live
     * 
     * @return Now + tokenTtlMs
     */
    private Date newExpiresAt(final Date now) {
        if (this.tokenTtlMs < 1) {
            this.tokenTtlMs = DEFAULT_TOKEN_TTL_MS;
        } else if (this.tokenTtlMs < ONE_HOUR_MS) {
            logger.warn("Token TTL is short : " + this.tokenTtlMs + " ms");
        }
        return new Date(now.getTime() + tokenTtlMs);
    }

    /**
     * Generates a random access token.
     * 
     * @return
     */
    private String generateAccessToken() {
        return new BigInteger(130, random).toString(32);
    }

    /**
     * Check the access token given in the request and return the nodeRef
     * corresponding to the file id passed to the request.
     *
     * @param req
     * @throws WebScriptException
     * @return
     */
    @Override
    public WOPIAccessTokenInfo checkAccessToken(final String accessToken, final NodeRef nodeRef) {
        WOPIAccessTokenInfo tokenInfo = this.tokenMap.get(accessToken);

        if (tokenInfo == null) {
            throw new WebScriptException(Status.STATUS_UNAUTHORIZED, "No token access found for " + accessToken);
        }

        if (!tokenInfo.getFileId().equals(nodeRef.getId())) {
            throw new WebScriptException(Status.STATUS_UNAUTHORIZED,
                    "Tokens stored for " + accessToken + ", not match the given file" + nodeRef);
        }

        return tokenInfo;
    }

    /**
     * Returns the WOPI src URL for a given nodeRef and action.
     *
     * @param nodeRef
     * @param action
     * @return
     */
    @Override
    public String getWopiSrcURL(NodeRef nodeRef, String action) throws IOException {
        final ContentData contentData = (ContentData) nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
        return wopiLoader.getSrcURL(contentData.getMimetype(), action);
    }

    /**
     * In the case that Alfresco is behind a proxy and not using the proxy hostname
     * in the alfresco config section of the alfresco-global.properties file, then
     * we should be able to set a property in alfresco-global.properties for this
     * service to use.
     *
     * @return
     */
    @Override
    public String getAlfrescoProxyDomain() {
        return alfExternalHost.getHost();
    }

    @Override
    public URL getAlfExternalHost() {
        return alfExternalHost;
    }

    public void setSysAdminParams(SysAdminParams sysAdminParams) {
        this.sysAdminParams = sysAdminParams;
    }

    public void init() {
        if (wopiBaseURL == null) {
            try {
                logger.warn("The wopiBaseURL param wasn't found in alfresco-global.properties."
                        + "Assuming lool service is on the same host and setting url to match.");
                wopiBaseURL = new URL("https", sysAdminParams.getAlfrescoHost(), DEFAULT_WOPI_PORT, "/");
            } catch (MalformedURLException e) {
                throw new AlfrescoRuntimeException("Invalid WOPI Base URL: " + this.wopiBaseURL, e);
            }
        }

        // We should actually never throw an exception here unless of course.......
        if (wopiDiscoveryURL == null) {
            try {
                wopiDiscoveryURL = new URL(
                        wopiBaseURL.getProtocol() + wopiBaseURL.getHost() + wopiBaseURL.getPort() + "/discovery");
                logger.warn("The wopiDiscoveryURL param wasn't found in alfresco-global.properties. "
                        + "\nWe will assume that the discovery.xml file is hosted on this"
                        + "server and construct a url path based on this: " + wopiDiscoveryURL.toString());
            } catch (MalformedURLException mue) {
                logger.error("Unable to create discovery URL. (Should never be thrown so this is an "
                        + "interesting situation we find ourselves.. To the bat cave Robin!!)");
                throw new AlfrescoRuntimeException("Invalid WOPI Base URL: " + this.wopiBaseURL, mue);
            }
        }

        wopiLoader = new WOPILoader(wopiDiscoveryURL);
    }

    public class WOPILoader {
        private Document discoveryDoc;
        private URL wopiDiscoveryURL;

        public WOPILoader(URL wopiDiscoveryURL) {
            this.wopiDiscoveryURL = wopiDiscoveryURL;
        }

        /**
         * Return the srcurl for a given mimetype.
         *
         * @param mimeType
         * @return
         */
        public String getSrcURL(String mimeType, String action) throws IOException {
            // Attempt to reload discovery.xml from host if it isn't already
            // loaded.
            if (this.discoveryDoc == null) {
                try {
                    loadDiscoveryXML();
                } catch (IOException e) {
                    logger.error("Failed to fetch discovery.xml file from server (" + wopiDiscoveryURL.toString() + ")",
                            e);
                    throw e;
                }
            }

            final XPathFactory xPathFactory = XPathFactory.newInstance();
            final XPath xPath = xPathFactory.newXPath();
            final String xPathExpr = ("/wopi-discovery/net-zone/app[@name='${mimeType}']/action[@name='${action}']/@urlsrc")
                    .replace("${mimeType}", mimeType).replace("${action}", action);
            try {
                return xPath.evaluate(xPathExpr, this.discoveryDoc);
            } catch (XPathExpressionException e) {
                logger.error("XPath Error return null", e);
            }
            return null;
        }

        private void loadDiscoveryXML() throws IOException {
            this.discoveryDoc = parse(fetchDiscoveryXML());
        }

        /**
         * Parse a discovery.xml file input stream.
         *
         * @param discoveryInputStream
         * @return
         */
        private Document parse(InputStream discoveryInputStream) {
            final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            try {
                final DocumentBuilder builder = builderFactory.newDocumentBuilder();
                return builder.parse(discoveryInputStream);
            } catch (ParserConfigurationException | IOException | SAXException e) {
                logger.error("Parse Error return null", e);
            }
            return null;
        }

        private InputStream fetchDiscoveryXML() throws IOException {
            HttpURLConnection connection = (HttpURLConnection) this.wopiDiscoveryURL.openConnection();

            if (logger.isDebugEnabled()) {
                logger.debug("Http connection for discovery xml returned with a [" + connection.getResponseCode()
                        + "] response code.");
            }

            try {
                final byte[] conn = IOUtils.toByteArray(connection.getInputStream());
                return new ByteArrayInputStream(conn);
            } catch (IOException e) {
                logger.error("There was an error fetching discovery.xml", e);
            }
            return null;

        }
    }
}
