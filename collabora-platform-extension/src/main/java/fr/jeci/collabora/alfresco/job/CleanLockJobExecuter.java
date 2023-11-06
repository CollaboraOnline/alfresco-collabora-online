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

import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import fr.jeci.collabora.alfresco.CollaboraOnlineModel;

/**
 * @author Cindy Piassale 
 * Search for files for which the lock has expired
 * Removing locks
 *
 * @deprecated We use LockService now
 */
public class CleanLockJobExecuter {

	private static final Log logger = LogFactory.getLog(CleanLockJobExecuter.class);

	private SearchService searchService;
	private NodeService nodeService;

	public void execute() {
		if (logger.isInfoEnabled()) {
			logger.info("Running the clean lock job");
		}

		// Search content which are collabora:lockExpiration < NOW
		StringBuilder query = new StringBuilder();
		query.append("+ASPECT:\"").append(CollaboraOnlineModel.COLLABORA_MODEL_PREFIX).append(":")
				.append(CollaboraOnlineModel.ASPECT_COLLABORA_ONLINE.toPrefixString()).append("\"");
		query.append(" +@").append(CollaboraOnlineModel.COLLABORA_MODEL_PREFIX).append("\\:");
		query.append(CollaboraOnlineModel.PROP_LOCK_EXPIRATION.getLocalName()).append(":[MIN TO NOW}");

		if (logger.isDebugEnabled()) {
			logger.debug("CleanLockJobExecute - Query : " + query);
		}
		ResultSet result = this.searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
				SearchService.LANGUAGE_LUCENE, query.toString());
		try {
			List<ChildAssociationRef> nodes = result.getChildAssocRefs();
			// Remove the aspect collabora:collaboraOnline
			for (ChildAssociationRef node : nodes) {
				NodeRef nodeRef = node.getChildRef();
				this.nodeService.removeAspect(nodeRef, CollaboraOnlineModel.ASPECT_COLLABORA_ONLINE);
			}
		} catch (AlfrescoRuntimeException exception) {
			logger.error("Error to remove the collaboraOnline aspect", exception);
		} finally {
			result.close();
		}
	}

	/**
	 * @param searchService the searchService to set
	 */
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	/**
	 * @param nodeService the nodeService to set
	 */
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
}
