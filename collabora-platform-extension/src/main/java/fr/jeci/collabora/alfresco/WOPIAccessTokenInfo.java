/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package fr.jeci.collabora.alfresco;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.LocalDateTime;

/**
 * POJO to hold info about a WOPI access token.
 *
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
	 * Return whether the access token has been issued and not expired at the
	 * current time.
	 *
	 * @return
	 */
	public boolean isValid() {
		return isValid(LocalDateTime.now());
	}

	/**
	 * Return whether the access token is valid for the given date.
	 * 
	 * @return
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
