package fr.jeci.collabora.alfresco;

import org.alfresco.service.namespace.QName;

/**
 * Content Model Costants
 * 
 * @author jlesage
 *
 */
public interface CollaboraOnlineModel {

	/** Content Model URI */
	static final String COLLABORA_MODEL_1_0_URI = "http://www.collaboraoffice.com/model/online/1.0";

	/** Content Model Prefix */
	static final String COLLABORA_MODEL_PREFIX = "collabora";

	static final QName ASPECT_COLLABORA_ONLINE = QName.createQName(COLLABORA_MODEL_1_0_URI, "collaboraOnline");
	static final QName PROP_AUTOSAVE = QName.createQName(COLLABORA_MODEL_1_0_URI, "autosave");
	static final QName PROP_LOCK_ID = QName.createQName(COLLABORA_MODEL_1_0_URI, "lockId");
	static final QName PROP_LOCK_EXPIRATION = QName.createQName(COLLABORA_MODEL_1_0_URI, "lockExpiration");
	
}