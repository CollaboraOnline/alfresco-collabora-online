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

	public WOPIAccessTokenInfo(String accessToken, LocalDateTime issuedAt, LocalDateTime expiresAt, String fileId,
			String userName) {
		this.accessToken = accessToken;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
		this.fileId = fileId;
		this.userName = userName;
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
		return when.isAfter(issuedAt) && when.isBefore(expiresAt);
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

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("accessToken", accessToken).append("issuedAt", issuedAt)
				.append("expiresAt", expiresAt).append("fileId", fileId).append("userName", userName).toString();
	}
}
