// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.extensions.webscripts.WebScriptRequest;

/**
 * Tests {@link AbstractWopiSettingsWebScript#queryParam(WebScriptRequest, String)}, which reads parameters from the
 * raw query string so the request body (the uploaded file) is never consumed.
 */
public class AbstractWopiSettingsWebScriptTest {

	@Test
	public void testQueryParam_decodesValuesFromQueryString() {
		WebScriptRequest req = mock(WebScriptRequest.class);
		when(req.getQueryString()).thenReturn(
				"access_token=abc123&access_token_ttl=0&fileId=%2Fsettings%2Fuserconfig%2Fwordbook%2Fstandard.dic&type=userconfig");

		assertEquals("abc123", AbstractWopiSettingsWebScript.queryParam(req, "access_token"));
		assertEquals("/settings/userconfig/wordbook/standard.dic", AbstractWopiSettingsWebScript.queryParam(req,
				"fileId"));
		assertEquals("userconfig", AbstractWopiSettingsWebScript.queryParam(req, "type"));
	}

	@Test
	public void testQueryParam_doesNotConfusePrefixedNames() {
		// "access_token" must not match "access_token_ttl"
		WebScriptRequest req = mock(WebScriptRequest.class);
		when(req.getQueryString()).thenReturn("access_token_ttl=0&access_token=real-token");

		assertEquals("real-token", AbstractWopiSettingsWebScript.queryParam(req, "access_token"));
		assertEquals("0", AbstractWopiSettingsWebScript.queryParam(req, "access_token_ttl"));
	}

	@Test
	public void testQueryParam_missingReturnsNull() {
		WebScriptRequest req = mock(WebScriptRequest.class);
		when(req.getQueryString()).thenReturn("type=userconfig");

		assertNull(AbstractWopiSettingsWebScript.queryParam(req, "fileId"));
	}

	@Test
	public void testQueryParam_fallsBackToUrlWhenNoQueryString() {
		WebScriptRequest req = mock(WebScriptRequest.class);
		when(req.getQueryString()).thenReturn(null);
		when(req.getURL()).thenReturn("/wopi/settings/download?fileId=%2Fx&access_token=t");

		assertEquals("/x", AbstractWopiSettingsWebScript.queryParam(req, "fileId"));
		assertEquals("t", AbstractWopiSettingsWebScript.queryParam(req, "access_token"));
	}

	@Test
	public void testQueryParam_noQueryAtAllReturnsNull() {
		WebScriptRequest req = mock(WebScriptRequest.class);
		when(req.getQueryString()).thenReturn(null);
		when(req.getURL()).thenReturn("/wopi/settings/download");

		assertNull(AbstractWopiSettingsWebScript.queryParam(req, "fileId"));
	}

	@Test
	public void testQueryParam_flagWithoutValueIsEmptyString() {
		WebScriptRequest req = mock(WebScriptRequest.class);
		when(req.getQueryString()).thenReturn("flag&access_token=t");

		assertEquals("", AbstractWopiSettingsWebScript.queryParam(req, "flag"));
	}
}
