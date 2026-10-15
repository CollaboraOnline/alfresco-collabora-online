// SPDX-FileCopyrightText: 2026 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.job;

import java.time.Instant;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.schedule.AbstractScheduledLockedJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.jeci.collabora.alfresco.token.WopiTokenStore;

/**
 * Scheduled job removing the expired WOPI access tokens from the {@link WopiTokenStore} given as {@code tokenStore} in
 * its job data.
 */
public class PurgeWopiTokensJob extends AbstractScheduledLockedJob implements StatefulJob {

	private static final Logger logger = LoggerFactory.getLogger(PurgeWopiTokensJob.class);

	@Override
	public void executeJob(JobExecutionContext context) throws JobExecutionException {
		final Object store = context.getJobDetail()
				.getJobDataMap()
				.get("tokenStore");
		if (!(store instanceof WopiTokenStore tokenStore)) {
			throw new AlfrescoRuntimeException("PurgeWopiTokensJob data must contain a valid 'tokenStore' reference");
		}
		final int purged = AuthenticationUtil.runAsSystem(() -> tokenStore.purgeExpired(Instant.now()));
		logger.debug("Purged {} expired WOPI tokens", purged);
	}
}
