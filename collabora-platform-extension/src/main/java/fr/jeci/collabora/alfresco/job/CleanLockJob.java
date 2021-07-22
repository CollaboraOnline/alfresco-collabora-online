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
package fr.jeci.collabora.alfresco.job;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.schedule.AbstractScheduledLockedJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;

/**
 * @author Cindy Piassale
 *
 */
public class CleanLockJob extends AbstractScheduledLockedJob implements StatefulJob {

	@Override
	public void executeJob(JobExecutionContext context) throws JobExecutionException {
		JobDataMap jobData = context.getJobDetail().getJobDataMap();

		// Extract the Job executer to use
		Object executerObj = jobData.get("jobExecuter");
		if (!(executerObj instanceof CleanLockJobExecuter)) {
			throw new AlfrescoRuntimeException(
					"CleanLockJob data must contain valid 'Executer' reference");
		}

		final CleanLockJobExecuter cleanLockjobExecuter = (CleanLockJobExecuter) executerObj;

		AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>() {
			public Object doWork() throws Exception {
				cleanLockjobExecuter.execute();
				return null;
			}
		}, AuthenticationUtil.getSystemUserName());
	}

}