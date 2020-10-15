package fr.jeci.collabora.evaluator.doclib.indicator;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.web.evaluator.BaseEvaluator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class EditingEvaluator extends BaseEvaluator {
	private static final String ASPECT_COLLABORA_ONLINE = "collabora:CollaboraOnline";
	private static final String PROP_START_AT = "collabora:started_at";
	private static final String PROP_EDITORS = "collabora:editors";

	@Override
	public boolean evaluate(JSONObject jsonObject) {
		try {
			JSONArray nodeAspects = getNodeAspects(jsonObject);
			if (nodeAspects == null) {
				return false;
			} else {
				return nodeAspects.contains(ASPECT_COLLABORA_ONLINE);
			}
		} catch (Exception err) {
			throw new AlfrescoRuntimeException("Failed to run UI evaluator: " + err.getMessage());
		}
	}
}
