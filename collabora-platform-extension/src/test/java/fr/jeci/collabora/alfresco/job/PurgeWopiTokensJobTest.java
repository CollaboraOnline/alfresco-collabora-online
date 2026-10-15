// SPDX-FileCopyrightText: 2026 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.job;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import fr.jeci.collabora.alfresco.WOPIAccessTokenInfo;
import fr.jeci.collabora.alfresco.token.InMemoryAttributeService;
import fr.jeci.collabora.alfresco.token.WopiTokenStore;

public class PurgeWopiTokensJobTest {

	@After
	public void tearDown() {
		AuthenticationUtil.clearCurrentSecurityContext();
	}

	@Test
	public void jobPurgesExpiredTokens() throws Exception {
		WopiTokenStore store = new WopiTokenStore();
		store.setAttributeService(new InMemoryAttributeService());
		LocalDateTime now = LocalDateTime.now();
		store.save(new WOPIAccessTokenInfo("expired", now.minusHours(2), now.minusHours(1), "file", "alice"));
		store.save(new WOPIAccessTokenInfo("valid", now.minusHours(1), now.plusHours(1), "file", "alice"));

		JobDetail jobDetail = mock(JobDetail.class);
		when(jobDetail.getJobDataMap()).thenReturn(new JobDataMap(Map.of("tokenStore", store)));
		JobExecutionContext context = mock(JobExecutionContext.class);
		when(context.getJobDetail()).thenReturn(jobDetail);

		new PurgeWopiTokensJob().executeJob(context);

		assertNull(store.get("expired"));
		assertNotNull(store.get("valid"));
	}
}
