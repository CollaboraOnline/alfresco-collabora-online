// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.restriction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.extensions.webscripts.WebScriptException;

public class FeatureRestrictionServiceImplTest {

	private FeatureRestrictionServiceImpl service;
	private AuthorityService authorityService;
	private AttributeService attributeService;

	private static final String GROUP_NAME = "GROUP_COLLABORA_ONLINE";

	@Before
	public void setUp() {
		service = new FeatureRestrictionServiceImpl();
		authorityService = mock(AuthorityService.class);
		attributeService = mock(AttributeService.class);

		service.setAuthorityService(authorityService);
		service.setAttributeService(attributeService);
		service.setGroupName(GROUP_NAME);
		service.setInitialMaxLicenses(-1);
	}

	@Test
	public void testDisabled_allUsersHaveLicense() {
		service.setEnabled(false);

		assertTrue(service.hasLicense("user1"));
		assertTrue(service.hasLicense("anyone"));
		assertFalse(service.isFeatureRestrictionEnabled());
	}

	@Test
	public void testEnabled_userInGroup_hasLicense() {
		service.setEnabled(true);

		Set<String> authorities = new HashSet<>();
		authorities.add(GROUP_NAME);
		authorities.add("GROUP_EVERYONE");
		when(authorityService.getAuthoritiesForUser("user1")).thenReturn(authorities);

		assertTrue(service.hasLicense("user1"));
	}

	@Test
	public void testEnabled_userNotInGroup_noLicense() {
		service.setEnabled(true);

		Set<String> authorities = new HashSet<>();
		authorities.add("GROUP_EVERYONE");
		when(authorityService.getAuthoritiesForUser("user2")).thenReturn(authorities);

		assertFalse(service.hasLicense("user2"));
	}

	@Test
	public void testGetLicensedUsers() {
		Set<String> users = new HashSet<>();
		users.add("user1");
		users.add("user2");
		when(authorityService.authorityExists(GROUP_NAME)).thenReturn(true);
		when(authorityService.getContainedAuthorities(AuthorityType.USER, GROUP_NAME, false)).thenReturn(users);

		Set<String> result = service.getLicensedUsers();
		assertEquals(2, result.size());
		assertTrue(result.contains("user1"));
		assertTrue(result.contains("user2"));
	}

	@Test
	public void testGetLicenseCount() {
		Set<String> users = new HashSet<>();
		users.add("user1");
		users.add("user2");
		users.add("user3");
		when(authorityService.authorityExists(GROUP_NAME)).thenReturn(true);
		when(authorityService.getContainedAuthorities(AuthorityType.USER, GROUP_NAME, false)).thenReturn(users);

		assertEquals(3, service.getLicenseCount());
	}

	@Test
	public void testGrantLicense_success() {
		service.setEnabled(true);

		when(authorityService.authorityExists(GROUP_NAME)).thenReturn(true);
		when(authorityService.getContainedAuthorities(AuthorityType.USER, GROUP_NAME, false)).thenReturn(new HashSet<>());

		service.grantLicense("newuser");

		verify(authorityService).addAuthority(GROUP_NAME, "newuser");
	}

	@Test(expected = WebScriptException.class)
	public void testGrantLicense_exceedsMax() {
		service.setEnabled(true);
		service.setMaxLicenses(2);

		Set<String> users = new HashSet<>();
		users.add("user1");
		users.add("user2");
		when(authorityService.authorityExists(GROUP_NAME)).thenReturn(true);
		when(authorityService.getContainedAuthorities(AuthorityType.USER, GROUP_NAME, false)).thenReturn(users);

		service.grantLicense("user3");
	}

	@Test
	public void testGrantLicense_unlimitedMax() {
		service.setEnabled(true);

		Set<String> users = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			users.add("user" + i);
		}
		when(authorityService.authorityExists(GROUP_NAME)).thenReturn(true);
		when(authorityService.getContainedAuthorities(AuthorityType.USER, GROUP_NAME, false)).thenReturn(users);

		Set<String> emptyForNewUser = new HashSet<>(users);
		service.grantLicense("newuser");

		verify(authorityService).addAuthority(GROUP_NAME, "newuser");
	}

	@Test(expected = WebScriptException.class)
	public void testGrantLicense_alreadyMember() {
		service.setEnabled(true);

		Set<String> users = new HashSet<>();
		users.add("user1");
		when(authorityService.authorityExists(GROUP_NAME)).thenReturn(true);
		when(authorityService.getContainedAuthorities(AuthorityType.USER, GROUP_NAME, false)).thenReturn(users);

		service.grantLicense("user1");
	}

	@Test
	public void testRevokeLicense_success() {
		Set<String> users = new HashSet<>();
		users.add("user1");
		when(authorityService.authorityExists(GROUP_NAME)).thenReturn(true);
		when(authorityService.getContainedAuthorities(AuthorityType.USER, GROUP_NAME, false)).thenReturn(users);

		service.revokeLicense("user1");

		verify(authorityService).removeAuthority(GROUP_NAME, "user1");
	}

	@Test(expected = WebScriptException.class)
	public void testRevokeLicense_notMember() {
		when(authorityService.authorityExists(GROUP_NAME)).thenReturn(true);
		when(authorityService.getContainedAuthorities(AuthorityType.USER, GROUP_NAME, false)).thenReturn(new HashSet<>());

		service.revokeLicense("unknown");
	}

	@Test
	public void testSetMaxLicenses_persists() {
		service.setMaxLicenses(50);

		assertEquals(50, service.getMaxLicenses());
		verify(attributeService).setAttribute(eq(50), eq("collabora"), eq("feature-restriction"), eq("max-licenses"));
	}

	@Test
	public void testSetMaxLicenses_unlimited() {
		service.setMaxLicenses(-1);

		assertEquals(-1, service.getMaxLicenses());
		verify(attributeService).setAttribute(eq(-1), eq("collabora"), eq("feature-restriction"), eq("max-licenses"));
	}

	@Test
	public void testGetLicensedUsers_groupNotExists() {
		when(authorityService.authorityExists(GROUP_NAME)).thenReturn(false);

		Set<String> result = service.getLicensedUsers();
		assertTrue(result.isEmpty());
		verify(authorityService, never()).getContainedAuthorities(any(), any(), eq(true));
	}
}
