// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.evaluator.doclib.indicator;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.evaluator.BaseEvaluator;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditingEvaluator extends BaseEvaluator {

	private static final Logger logger = LoggerFactory.getLogger(EditingEvaluator.class);
	private static final String LOCK_WRITE = "LOCK_WRITE";

	@Override
	public boolean evaluate(JSONObject jsonObject) {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("EditingEvaluator - jsonObject" + jsonObject.toString());
			}
			String lockType = (String) getProperty(jsonObject, "cm:lockType");
			return LOCK_WRITE.equals(lockType);
		} catch (Exception err) {
			throw new AlfrescoRuntimeException("Failed to run UI evaluator: " + err.getMessage());
		}
	}
}
