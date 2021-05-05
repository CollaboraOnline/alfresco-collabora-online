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
}
