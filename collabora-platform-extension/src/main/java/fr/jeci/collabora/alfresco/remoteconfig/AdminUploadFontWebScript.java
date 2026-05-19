// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.remoteconfig;

import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Admin webscript that uploads a font file to the fonts folder.
 * Only allowed font MIME types are accepted; rejects unknown formats.
 * An optional {@code mimeType} parameter overrides extension-based detection.
 * <p>
 * POST /collabora/admin/fonts?name={name}&amp;mimeType={mimeType} (admin authentication)
 */
public class AdminUploadFontWebScript extends AbstractWebScript {

	private static final Set<String> ALLOWED_MIME_TYPES = Set.of("font/ttf", "font/otf", "font/woff", "font/woff2");

	private RemoteConfigService remoteConfigService;

	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		String name = req.getParameter("name");
		if (name == null || name.isBlank()) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Parameter 'name' is required");
		}

		InputStream content = req.getContent()
				.getInputStream();
		if (content == null) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Request body is required");
		}

		String explicitMimeType = req.getParameter("mimeType");
		String mimeType = (explicitMimeType != null && !explicitMimeType.isBlank())
				? explicitMimeType
				: guessMimeType(name);

		if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
			throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Unsupported font format: " + mimeType + ". Allowed: "
																					  + ALLOWED_MIME_TYPES);
		}

		remoteConfigService.uploadFont(name, content, mimeType);

		res.setContentType("application/json;charset=UTF-8");
		res.getWriter()
				.write("{\"success\":true,\"font\":\"" + name + "\"}");
	}

	private static String guessMimeType(String fileName) {
		int dot = fileName.lastIndexOf('.');
		if (dot >= 0) {
			String ext = fileName.substring(dot + 1)
					.toLowerCase();
			switch (ext) {
			case "ttf":
				return "font/ttf";
			case "otf":
				return "font/otf";
			case "woff":
				return "font/woff";
			case "woff2":
				return "font/woff2";
			default:
				break;
			}
		}
		return "unknown";
	}

	public void setRemoteConfigService(RemoteConfigService remoteConfigService) {
		this.remoteConfigService = remoteConfigService;
	}
}
