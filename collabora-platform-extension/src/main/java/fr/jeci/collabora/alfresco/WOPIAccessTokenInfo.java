// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * POJO to hold info about a WOPI access token.
 * <p>
 * Created by seth on 30/04/16.
 */
public class WOPIAccessTokenInfo implements Serializable {

	private static final long serialVersionUID = 8344283129580208330L;

	private String accessToken;
	private Instant issuedAt;
	private Instant expiresAt;
	private String fileId;
	private String userName;
	private String writeFileId;
	private WopiTokenKind kind;

	/**
	 * Collabora token reading and writing {@code fileId}.
	 */
	public WOPIAccessTokenInfo(String accessToken, Instant issuedAt, Instant expiresAt, String fileId, String userName) {
		this(accessToken, issuedAt, expiresAt, fileId, fileId, userName, WopiTokenKind.COLLABORA);
	}

	/**
	 * Token reading {@code fileId}, writing {@code writeFileId} (none when {@code null}).
	 *
	 * @param kind what the token may be used for
	 */
	public WOPIAccessTokenInfo(String accessToken, Instant issuedAt, Instant expiresAt, String fileId,
			String writeFileId, String userName, WopiTokenKind kind) {
		this.accessToken = accessToken;
		this.issuedAt = toMillis(issuedAt);
		this.expiresAt = toMillis(expiresAt);
		this.fileId = fileId;
		this.writeFileId = writeFileId;
		this.userName = userName;
		this.kind = kind;
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
		return isValid(Instant.now());
	}

	/**
	 * Return whether the access token is valid for the given date.
	 */
	public boolean isValid(Instant when) {
		return !when.isBefore(issuedAt) && when.isBefore(expiresAt);
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public void setIssuedAt(Instant issuedAt) {
		this.issuedAt = toMillis(issuedAt);
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = toMillis(expiresAt);
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
		return kind != null && kind.isRenewable();
	}

	/**
	 * @return what the token may be used for
	 */
	public WopiTokenKind getKind() {
		return kind;
	}

	/**
	 * @param kind what the token may be used for
	 */
	public void setKind(WopiTokenKind kind) {
		this.kind = kind;
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

	private static Instant toMillis(Instant instant) {
		return instant == null ? null : instant.truncatedTo(ChronoUnit.MILLIS);
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("accessToken", getMaskedToken())
				.append("issuedAt", issuedAt)
				.append("expiresAt", expiresAt)
				.append("fileId", fileId)
				.append("writeFileId", writeFileId)
				.append("userName", userName)
				.append("kind", kind)
				.toString();
	}
}
