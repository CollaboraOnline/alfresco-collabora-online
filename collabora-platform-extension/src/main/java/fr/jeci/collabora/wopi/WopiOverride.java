package fr.jeci.collabora.wopi;

public enum WopiOverride {
	// Put with no Lock
	PUT,
	// Save As
	PUT_RELATIVE,
	// Ask for Lock
	LOCK,
	// Query Lock Key
	GET_LOCK,
	// Refresh Lock
	REFRESH_LOCK,
	// Remove Lock
	UNLOCK
	
}
