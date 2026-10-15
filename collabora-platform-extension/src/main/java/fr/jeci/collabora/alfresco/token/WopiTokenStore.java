// SPDX-FileCopyrightText: 2026 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.token;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.attributes.AttributeService;
import org.joda.time.LocalDateTime;

import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;

/**
 * Persistent store of WOPI access tokens, backed by the {@link AttributeService}.
 * <p>
 * A token is stored under the SHA-256 of its value, so the database never holds a usable token. Tokens survive a
 * repository restart; expired ones are removed by {@link #purgeExpired(LocalDateTime)}.
 */
public class WopiTokenStore {

	static final String ATTR_KEY_ROOT = "collabora";
	static final String ATTR_KEY_TOKENS = "wopi-token";

	static final String ISSUED_AT = "issuedAt";
	static final String EXPIRES_AT = "expiresAt";
	static final String FILE_ID = "fileId";
	static final String WRITE_FILE_ID = "writeFileId";
	static final String USER_NAME = "userName";
	static final String RENEWABLE = "renewable";

	private AttributeService attributeService;

	/**
	 * Stores a token, replacing any previous state of the same token.
	 */
	public void save(final WOPIAccessTokenInfo info) {
		attributeService.setAttribute(toAttribute(info), ATTR_KEY_ROOT, ATTR_KEY_TOKENS, hash(info.getAccessToken()));
	}

	/**
	 * @return the token, or {@code null} when the value is {@code null} or unknown
	 */
	public WOPIAccessTokenInfo get(final String accessToken) {
		if (accessToken == null) {
			return null;
		}
		final Serializable value = attributeService.getAttribute(ATTR_KEY_ROOT, ATTR_KEY_TOKENS, hash(accessToken));
		if (value instanceof Map<?, ?> attribute) {
			return fromAttribute(accessToken, attribute);
		}
		return null;
	}

	/**
	 * Removes every token expired at the given date.
	 *
	 * @return the number of tokens removed
	 */
	public int purgeExpired(final LocalDateTime now) {
		final long limit = now.toDate()
				.getTime();
		final List<Serializable> expired = new ArrayList<>();
		attributeService.getAttributes((id, value, keys) -> {
			if (value instanceof Map<?, ?> attribute && millis(attribute, EXPIRES_AT) <= limit) {
				expired.add(keys[2]);
			}
			return true;
		}, ATTR_KEY_ROOT, ATTR_KEY_TOKENS);
		for (Serializable key : expired) {
			attributeService.removeAttribute(ATTR_KEY_ROOT, ATTR_KEY_TOKENS, key);
		}
		return expired.size();
	}

	static String hash(final String accessToken) {
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of()
					.formatHex(digest.digest(accessToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static HashMap<String, Serializable> toAttribute(final WOPIAccessTokenInfo info) {
		final HashMap<String, Serializable> attribute = new HashMap<>(8);
		attribute.put(ISSUED_AT, info.getIssuedAt()
				.toDate()
				.getTime());
		attribute.put(EXPIRES_AT, info.getExpiresAt()
				.toDate()
				.getTime());
		attribute.put(FILE_ID, info.getFileId());
		if (info.getWriteFileId() != null) {
			attribute.put(WRITE_FILE_ID, info.getWriteFileId());
		}
		attribute.put(USER_NAME, info.getUserName());
		attribute.put(RENEWABLE, info.isRenewable());
		return attribute;
	}

	private static WOPIAccessTokenInfo fromAttribute(final String accessToken, final Map<?, ?> attribute) {
		return new WOPIAccessTokenInfo(accessToken, new LocalDateTime(millis(attribute, ISSUED_AT)), new LocalDateTime(
				millis(attribute, EXPIRES_AT)), (String) attribute.get(FILE_ID), (String) attribute.get(WRITE_FILE_ID),
				(String) attribute.get(USER_NAME), Boolean.TRUE.equals(attribute.get(RENEWABLE)));
	}

	private static long millis(final Map<?, ?> attribute, final String key) {
		final Object value = attribute.get(key);
		if (value instanceof Number number) {
			return number.longValue();
		}
		return 0L;
	}

	/**
	 * @param attributeService the service persisting the token attributes
	 */
	public void setAttributeService(AttributeService attributeService) {
		this.attributeService = attributeService;
	}
}
