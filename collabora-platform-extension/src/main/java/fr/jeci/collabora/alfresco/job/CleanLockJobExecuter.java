// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.job;

import java.util.List;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;

import fr.jeci.collabora.alfresco.CollaboraOnlineModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Cindy Piassale Search for files for which the lock has expired Removing locks
 * @deprecated We use LockService now
 */
@Deprecated
public class CleanLockJobExecuter {

	private static final Logger logger = LoggerFactory.getLogger(CleanLockJobExecuter.class);

	private SearchService searchService;
	private NodeService nodeService;

	public void execute() {
		if (logger.isInfoEnabled()) {
			logger.info("Running the clean lock job");
		}

		// Search content which are collabora:lockExpiration < NOW
		StringBuilder query = new StringBuilder();
		query.append("+ASPECT:\"")
				.append(CollaboraOnlineModel.COLLABORA_MODEL_PREFIX)
				.append(":")
				.append(CollaboraOnlineModel.ASPECT_COLLABORA_ONLINE.toPrefixString())
				.append("\"");
		query.append(" +@")
				.append(CollaboraOnlineModel.COLLABORA_MODEL_PREFIX)
				.append("\\:");
		query.append(CollaboraOnlineModel.PROP_LOCK_EXPIRATION.getLocalName())
				.append(":[MIN TO NOW}");

		logger.debug("CleanLockJobExecute - Query : {}", query);
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
