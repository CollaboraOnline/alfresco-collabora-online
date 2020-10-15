package fr.jeci.collabora.alfresco;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import dk.magenta.libreoffice.online.LOOLPutFileWebScript;

/**
 * Remove automatic and explicit versions.
 * 
 * @author Jeremie Lesage
 *
 */
public class CleanVersionWebScript extends DeclarativeWebScript {
    private static final Log logger = LogFactory.getLog(CleanVersionWebScript.class);

    private static final String PARAM_STORE_TYPE = "store_type";
    private static final String PARAM_STORE_ID = "store_id";
    private static final String PARAM_ID = "id";
    private static final String PARAM_KEEP_EXP = "keep_exp";
    private static final String PARAM_KEEP_AUTO = "keep_auto";

    private VersionService versionService;

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        final Map<String, Object> model = new HashMap<>();

        try {
            final Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
            final String storeType = WebscriptHelper.getParam(templateArgs, PARAM_STORE_TYPE);
            final String storeId = WebscriptHelper.getParam(templateArgs, PARAM_STORE_ID);
            final String guid = WebscriptHelper.getParam(templateArgs, PARAM_ID);
            final NodeRef nodeRef = new NodeRef(storeType, storeId, guid);

            if (logger.isDebugEnabled()) {
                logger.error("Cleaning Noderef " + nodeRef);
            }

            // Number automatique version to keep
            Integer keepAuto = WebscriptHelper.intergerValue(req, PARAM_KEEP_AUTO);
            keepAuto = keepAuto == null ? -1 : keepAuto;

            if (logger.isDebugEnabled()) {
                logger.error("Keep " + keepAuto + " auto-save versions");
            }

            // Number explicit version to keep
            Integer keepExp = WebscriptHelper.intergerValue(req, PARAM_KEEP_EXP);
            keepExp = keepExp == null ? -1 : keepExp;

            if (logger.isDebugEnabled()) {
                logger.error("Keep " + keepExp + " explicit versions");
            }

            // Removing version by using Alfresco Java API
            final VersionHistory history = versionService.getVersionHistory(nodeRef);

            int countAuto = 0;
            int countExp = 0;
            for (Version version : history.getAllVersions()) {
                Serializable collaboraautosave = version.getVersionProperties().get(LOOLPutFileWebScript.LOOL_AUTOSAVE);
                if (collaboraautosave == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("v." + version.getVersionLabel() + " - not lool - keep");
                    }

                    // Not Lool Version, ignoring
                    continue;
                }

                Boolean autosave = (Boolean) collaboraautosave;
                if (autosave && keepAuto >= 0) {
                    // Removing old auto-save version
                    if (++countAuto > keepAuto) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("v." + version.getVersionLabel() + " - remove auto");
                        }

                        versionService.deleteVersion(nodeRef, version);
                    }
                }

                if (!autosave && keepExp >= 0) {
                    // Removing old save version (only from collabora)
                    if (++countExp > keepExp) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("v." + version.getVersionLabel() + " - remove explicit");
                        }

                        versionService.deleteVersion(nodeRef, version);
                    }
                }
            }

            model.put("success", "true");
        } catch (Exception e) {
            logger.error("CleanVersionWebScript Error ", e);
            model.put("success", "false");
        }
        return model;

    }

    public void setVersionService(VersionService versionService) {
        this.versionService = versionService;
    }

}
