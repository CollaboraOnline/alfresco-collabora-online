// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;
import java.io.InputStream;

public class WopiGetFileWebScript extends AbstractWopiWebScript {
	private static final Logger logger = LoggerFactory.getLogger(WopiGetFileWebScript.class);

	/**
	 * The default buffer size 4k
	 */
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	@Override
	public void executeAsUser(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef) {
		final ContentData contentProp = (ContentData) nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT);
		res.setContentType(contentProp.getMimetype());
		res.setContentEncoding(contentProp.getEncoding());

		final ContentReader reader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);

		if (reader == null) {
			logger.error("No content reader for node={}", nodeRef);
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "No content reader for node=" + nodeRef);
		}

		try (InputStream inputStream = reader.getContentInputStream();) {
			// We don't want to close the outputStream, this is done by Tomcat
			long copied = IOUtils.copyLarge(inputStream, res.getOutputStream(), new byte[DEFAULT_BUFFER_SIZE]);

			logger.debug("Stream copied {} bytes", copied);
		} catch (IOException e) {
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Failed to copy content stream", e);
		}
	}
}
