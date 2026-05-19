// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import fr.jeci.collabora.alfresco.ConflictException;
import fr.jeci.collabora.alfresco.HeaderSanitizer;
import fr.jeci.collabora.alfresco.LogSanitizer;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.version.Version;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;
import java.io.InputStream;
import java.time.DateTimeException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Put the binary content into Alfresco.
 * <br>
 * The X-LOOL-WOPI-Timestamp is compare with PROP_FROZEN_MODIFIED or PROP_CREATED_DATE from the current version of the
 * target file.
 * <br>
 * We can change aspect or properties with specific headers, but these changes will not trigger policy.
 * <ul>
 * <li>X-PRISTY-ADD-ASPECT</li>
 * <li>X-PRISTY-DEL-ASPECT</li>
 * <li>X-PRISTY-DEL-PROPERTY</li>
 * <li>X-PRISTY-ADD-PROPERTY</li>
 * </ul>
 * It is safer to upload the file, then change metadata or aspect in another call.
 *
 * @author jlesage
 */
public class WopiPutFileWebScript extends AbstractWopiWebScript {
	private static final Logger logger = LoggerFactory.getLogger(WopiPutFileWebScript.class);

	@Override
	public void executeAsUser(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef)
			throws IOException {
		final boolean isAutosave = hasAutosaveHeader(req);

		checkWopiTimestamp(req, res, nodeRef);

		final InputStream inputStream = req.getContent()
				.getInputStream();
		if (inputStream == null) {
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "No inputStream");
		}

		try {
			final String lockId = req.getHeader(X_WOPI_LOCK);
			collaboraOnlineService.lockSteal(nodeRef, lockId);
			final Version newVersion = writeFileToDisk(inputStream, isAutosave, nodeRef);

			final Map<String, String> model = new HashMap<>(1);
			if (newVersion == null) {
				logger.warn("No version create for {}", nodeRef);
				model.put("warn", "No version create for " + nodeRef);
			} else {
				// WARN: To policy trigger with these actions
				headerActions(req, nodeRef);

				putLastModifiedTime(nodeRef, newVersion, model);
			}

			jsonResponse(res, Status.STATUS_OK, model);

			// Ask rendition only at last
			if (!isAutosave) {
				askForRendition(nodeRef);
			}

		} catch (ContentIOException we) {
			final String msg = "Error writing to file";
			logger.error(msg, we);
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, msg);
		} catch (ConflictException e) {
			logger.debug("ConflictException {}={};{}={}", X_WOPI_LOCK, LogSanitizer.sanitize(e.getCurrentLockId()),
					X_WOPI_LOCK_FAILURE_REASON, LogSanitizer.sanitize(e.getLockFailureReason()));

			res.setHeader(X_WOPI_LOCK, HeaderSanitizer.sanitize(e.getCurrentLockId()));
			res.setHeader(X_WOPI_LOCK_FAILURE_REASON, HeaderSanitizer.sanitize(e.getLockFailureReason()));
			jsonResponse(res, STATUS_CONFLICT, e.getLockFailureReason());
		}

	}

	private void putLastModifiedTime(final NodeRef nodeRef, final Version newVersion, final Map<String, String> model) {
		logger.debug("Modifier for the above nodeRef [{}] is: {}", nodeRef, newVersion.getFrozenModifier());

		Date newModified = newVersion.getFrozenModifiedDate();
		LocalDateTime modifiedDatetime = new LocalDateTime(newModified);
		model.put(LAST_MODIFIED_TIME, ISODateTimeFormat.dateTime()
				.print(modifiedDatetime));
	}

	private boolean hasAutosaveHeader(final WebScriptRequest req) {
		/*
		 * will have the value 'true' when the PutFile is triggered by autosave, and 'false' when triggered by explicit
		 * user operation (Save button or menu entry).
		 */
		final String hdrAutosave = req.getHeader(X_LOOL_WOPI_IS_AUTOSAVE);
		final boolean isAutosave = hdrAutosave != null && Boolean.parseBoolean(hdrAutosave.trim());

		if (logger.isDebugEnabled()) {
			logger.debug("- Request {} AUTOSAVE", isAutosave ? "is" : "is not");
		}
		return isAutosave;
	}

	/**
	 * Check the creation/modification date for current version. No check is there is no version, because the cm:modified
	 * is change for any change of a properties.
	 */
	private void checkWopiTimestamp(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef)
			throws IOException {
		final Version currentVersion = versionService.getCurrentVersion(nodeRef);

		if (currentVersion != null) {
			// Check if X-LOOL-WOPI-Timestamp
			final String hdrTimestamp = req.getHeader(X_LOOL_WOPI_TIMESTAMP);
			final Date modified = currentVersion.getFrozenModifiedDate();

			logger.debug("{}='{}'", X_LOOL_WOPI_TIMESTAMP, LogSanitizer.sanitize(hdrTimestamp));

			if (!checkTimestamp(hdrTimestamp, modified)) {
				final Map<String, String> model = new HashMap<>(1);
				model.put("LOOLStatusCode", "1010");
				jsonResponse(res, STATUS_CONFLICT, model);
			}
		}
	}

	/**
	 * Check if X-LOOL-WOPI-Timestamp is equal to PROP_FROZEN_MODIFIED
	 *
	 * @param hdrTimestamp "X-LOOL-WOPI-Timestamp"
	 * @param modified     PROP_FROZEN_MODIFIED
	 * @return true if timestamps are equal
	 */
	private boolean checkTimestamp(final String hdrTimestamp, final Date modified) {

		if (hdrTimestamp == null) {
			// Ignore if no X-LOOL-WOPI-Timestamp
			return true;
		}
		LocalDateTime loolTimestamp;
		try {
			// 2011-02-24T16:16:37.300000Z or 2022-04-08T08:29:01.355
			loolTimestamp = LocalDateTime.parse(hdrTimestamp, ISODateTimeFormat.dateTimeParser());
		} catch (DateTimeException | IllegalArgumentException e) {
			logger.error("checkTimestamp Error : {}", e.getMessage());
			return false;
		}

		// Check X_LOOL_WOPI_TIMESTAMP header
		final LocalDateTime localDate = new LocalDateTime(modified);

		if (loolTimestamp.compareTo(localDate) != 0) {
			logger.debug("PROP_FROZEN_MODIFIED : {}", modified);
			logger.debug("{} : {}", X_LOOL_WOPI_TIMESTAMP, LogSanitizer.sanitize(hdrTimestamp));
			logger.error("checkTimestamp Error : {} is different than PROP_MODIFIED", X_LOOL_WOPI_TIMESTAMP);
			return false;
		}

		return true;
	}
}
