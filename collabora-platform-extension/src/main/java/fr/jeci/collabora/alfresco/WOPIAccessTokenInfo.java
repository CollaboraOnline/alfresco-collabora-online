// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.LocalDateTime;

/**
 * POJO to hold info about a WOPI access token.
 * <p>
 * Created by seth on 30/04/16.
 */
public class WOPIAccessTokenInfo implements Serializable {

	private static final long serialVersionUID = 8344283129580208330L;

	private String accessToken;
	private LocalDateTime issuedAt;
	private LocalDateTime expiresAt;
	private String fileId;
	private String userName;
	private String writeFileId;
	private boolean renewable;

	public WOPIAccessTokenInfo(String accessToken, LocalDateTime issuedAt, LocalDateTime expiresAt, String fileId,
			String userName) {
		this(accessToken, issuedAt, expiresAt, fileId, fileId, userName, true);
	}

	/**
	 * Token reading {@code fileId}, writing {@code writeFileId} (none when {@code null}).
	 *
	 * @param renewable whether an expired token may be extended instead of rejected
	 */
	public WOPIAccessTokenInfo(String accessToken, LocalDateTime issuedAt, LocalDateTime expiresAt, String fileId,
			String writeFileId, String userName, boolean renewable) {
		this.accessToken = accessToken;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
		this.fileId = fileId;
		this.writeFileId = writeFileId;
		this.userName = userName;
		this.renewable = renewable;
	}

	/**
	 * @param id file id
	 * @return whether this token may read the given file
	 */
	public boolean canRead(String id) {
		return id != null && (id.equals(fileId) || id.equals(writeFileId));
	}

	/**
	 * @param id file id
	 * @return whether this token may write the given file
	 */
	public boolean canWrite(String id) {
		return id != null && id.equals(writeFileId);
	}

	/**
	 * Return whether the access token has been issued and not expired at the current time.
	 */
	public boolean isValid() {
		return isValid(LocalDateTime.now());
	}

	/**
	 * Return whether the access token is valid for the given date.
	 */
	public boolean isValid(LocalDateTime when) {
		return !when.isBefore(issuedAt) && when.isBefore(expiresAt);
	}

	public LocalDateTime getIssuedAt() {
		return issuedAt;
	}

	public void setIssuedAt(LocalDateTime issuedAt) {
		this.issuedAt = issuedAt;
	}

	public LocalDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(LocalDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * @return the id of the only file this token may write, or {@code null}
	 */
	public String getWriteFileId() {
		return writeFileId;
	}

	/**
	 * @param writeFileId the id of the only file this token may write, or {@code null}
	 */
	public void setWriteFileId(String writeFileId) {
		this.writeFileId = writeFileId;
	}

	/**
	 * @return whether an expired token may be extended instead of rejected
	 */
	public boolean isRenewable() {
		return renewable;
	}

	/**
	 * @param renewable whether an expired token may be extended instead of rejected
	 */
	public void setRenewable(boolean renewable) {
		this.renewable = renewable;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	/**
	 * Returns a masked version of the token for safe logging.
	 * Shows only the last 4 characters preceded by asterisks.
	 */
	public String getMaskedToken() {
		return maskToken(this.accessToken);
	}

	/**
	 * Masks a token for safe logging, showing only the last 4 characters.
	 * 
	 * @param token the token to mask
	 * @return masked token or "[null]" if token is null
	 */
	public static String maskToken(String token) {
		if (token == null) {
			return "[null]";
		}
		if (token.length() <= 4) {
			return "****";
		}
		return "****" + token.substring(token.length() - 4);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("accessToken", getMaskedToken())
				.append("issuedAt", issuedAt)
				.append("expiresAt", expiresAt)
				.append("fileId", fileId)
				.append("writeFileId", writeFileId)
				.append("userName", userName)
				.append("renewable", renewable)
				.toString();
	}
}
