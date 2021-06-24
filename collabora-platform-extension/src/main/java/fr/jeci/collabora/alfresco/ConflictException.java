package fr.jeci.collabora.alfresco;

/**
 * Specific exception use to implement wopi "409" error. Mainly for Lock
 * problem.
 * 
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.10
 * 
 * @author jlesage
 *
 */
public class ConflictException extends Exception {
	private static final long serialVersionUID = -5116720784148049930L;

	private final String currentLockId;

	private final String lockFailureReason;

	/**
	 * 
	 * @param currentLockId     A string value identifying the current lock on the
	 *                          file;
	 * @param lockFailureReason An string value indicating the cause of a lock
	 *                          failure.
	 */
	public ConflictException(String currentLockId, String lockFailureReason) {
		this.currentLockId = currentLockId;
		this.lockFailureReason = lockFailureReason;
	}

	public String getCurrentLockId() {
		return currentLockId;
	}

	public String getLockFailureReason() {
		return lockFailureReason;
	}

}
