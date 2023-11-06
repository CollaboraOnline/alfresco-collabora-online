package fr.jeci.collabora.alfresco;

import org.alfresco.service.namespace.QName;

/**
 * Content Model Constants
 * 
 * @author jlesage
 *
 * @Deprecated We use LockService now
 */
public interface CollaboraOnlineModel {

	/** Content Model URI */
	static final String COLLABORA_MODEL_1_0_URI = "http://www.collaboraoffice.com/model/online/1.0";

	/** Content Model Prefix */
	static final String COLLABORA_MODEL_PREFIX = "collabora";

	/** @Deprecated Adding Aspect change node, that is not what we want */
	static final QName ASPECT_COLLABORA_ONLINE = QName.createQName(COLLABORA_MODEL_1_0_URI, "collaboraOnline");

	static final QName PROP_AUTOSAVE = QName.createQName(COLLABORA_MODEL_1_0_URI, "autosave");

	/** @Deprecated Use LockService */
	static final QName PROP_LOCK_ID = QName.createQName(COLLABORA_MODEL_1_0_URI, "lockId");
	/** @Deprecated Use LockService */
	static final QName PROP_LOCK_EXPIRATION = QName.createQName(COLLABORA_MODEL_1_0_URI, "lockExpiration");
	
}