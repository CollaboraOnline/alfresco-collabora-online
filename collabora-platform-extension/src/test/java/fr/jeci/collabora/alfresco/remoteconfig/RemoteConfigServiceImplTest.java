// SPDX-FileCopyrightText: 2026 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for RemoteConfigServiceImpl
 */
public class RemoteConfigServiceImplTest {

	// ========== sanitizeFontName Tests ==========

	@Test
	public void testSanitizeFontName_safeNameUnchanged() {
		assertEquals("OpenSans-Regular.ttf", RemoteConfigServiceImpl.sanitizeFontName("OpenSans-Regular.ttf"));
	}

	@Test
	public void testSanitizeFontName_variableFontBrackets() {
		// Square brackets are rejected by Tomcat even when percent-encoded
		assertEquals("MonaspaceRadonVarVF_wght_wdth_slnt_.ttf", RemoteConfigServiceImpl.sanitizeFontName(
				"MonaspaceRadonVarVF[wght,wdth,slnt].ttf"));
	}

	@Test
	public void testSanitizeFontName_spaces() {
		assertEquals("My_Font.otf", RemoteConfigServiceImpl.sanitizeFontName("My Font.otf"));
	}

	@Test
	public void testSanitizeFontName_accentedCharacters() {
		assertEquals("Trait__d_uni_n.woff2", RemoteConfigServiceImpl.sanitizeFontName("Traité d'unión.woff2"));
	}
}
