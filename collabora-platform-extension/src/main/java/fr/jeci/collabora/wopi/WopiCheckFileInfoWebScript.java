// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.wopi;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;

import fr.jeci.collabora.alfresco.restriction.FeatureRestrictionService;
import fr.jeci.collabora.alfresco.settings.CollaboraSettingsService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.joda.time.LocalDateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <a href="https://msdn.microsoft.com/en-us/library/hh622920(v=office.12).aspx">...</a> search for "optional": false to
 * see mandatory
 * parameters. (As of 29/11/2016 when this was modified, SHA is no longer needed) Also return all values defined here:
 * <a
 * href="https://github.com/LibreOffice/online/blob/3ce8c3158a6b9375d4b8ca862ea5b50490af4c35/wsd/Storage.cpp#L403">...</a>
 * because LOOL
 * uses them internally to determine permission on rendering of certain elements. Well I assume given the variable
 * name(s), one should be able to semantically derive their relevance
 */
public class WopiCheckFileInfoWebScript extends AbstractWopiWebScript {
	private static final String VERSION = "Version";
	private static final String USER_FRIENDLY_NAME = "UserFriendlyName";
	private static final String USER_CAN_WRITE = "UserCanWrite";
	private static final String IS_ADMIN_USER = "IsAdminUser";
	private static final String IS_ANONYMOUS_USER = "IsAnonymousUser";
	private static final String IS_USER_LOCKED = "IsUserLocked";

	private static final String USER_ID = "UserId";
	private static final String SIZE = "Size";
	private static final String OWNER_ID = "OwnerId";

	private static final String BASE_FILE_NAME = "BaseFileName";

	// WOPI Settings references: tell Collabora where to fetch user/shared settings when a document is opened.
	// See https://sdk.collaboraonline.com/docs/advanced_integration.html#usersettings
	private static final String USER_SETTINGS = "UserSettings";
	private static final String SHARED_SETTINGS = "SharedSettings";
	// Collabora expects the settings reference key to be "uri" (wsd/RequestVettingStation.cpp,
	// wsd/wopi/WopiStorage.cpp); a "url" key is rejected by its JSON misspelling check.
	private static final String URI = "uri";
	private static final String STAMP = "stamp";

	private AuthorityService authorityService;
	private PermissionService permissionService;
	private PersonService personService;
	private FeatureRestrictionService featureRestrictionService;
	private CollaboraSettingsService collaboraSettingsService;

	@Override
	public void executeAsUser(final WebScriptRequest req, final WebScriptResponse res, final NodeRef nodeRef)
			throws IOException {
		final Map<String, String> model = this.collaboraOnlineService.serverInfo();
		final Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);

		final Version currentVersion = getCurrentVersion(nodeRef);

		if (currentVersion != null) {
			Date lastModifiedDate = currentVersion.getFrozenModifiedDate();
			LocalDateTime modifiedDatetime = new LocalDateTime(lastModifiedDate);
			model.put(LAST_MODIFIED_TIME, ISODateTimeFormat.dateTime()
					.print(modifiedDatetime));
			model.put(VERSION, currentVersion.getVersionLabel());
		} else if (userCanWrite(nodeRef)) {
			ensureVersioningEnabled(nodeRef);
		}

		// BaseFileName need extension, else COL load it in read-only mode
		model.put(BASE_FILE_NAME, (String) properties.get(ContentModel.PROP_NAME));

		model.put(OWNER_ID, properties.get(ContentModel.PROP_CREATOR)
				.toString());
		final ContentData contentData = (ContentData) properties.get(ContentModel.PROP_CONTENT);
		model.put(SIZE, Long.toString(contentData.getSize()));

		String userName = AuthenticationUtil.getRunAsUser();
		model.put(USER_ID, userName);
		model.put(USER_CAN_WRITE, Boolean.toString(userCanWrite(nodeRef)));
		model.put(USER_FRIENDLY_NAME, resolveUserFriendlyName(userName));
		boolean isAdmin = authorityService.isAdminAuthority(userName);
		model.put(IS_ADMIN_USER, Boolean.toString(isAdmin));
		boolean isGuest = authorityService.isGuestAuthority(userName);
		model.put(IS_ANONYMOUS_USER, Boolean.toString(isGuest));

		if (featureRestrictionService.isFeatureRestrictionEnabled()) {
			boolean isLocked = !featureRestrictionService.hasLicense(userName);
			model.put(IS_USER_LOCKED, Boolean.toString(isLocked));
		}

		// CheckFileInfo carries string values only; copy into an Object map to add the nested settings references.
		final Map<String, Object> response = new LinkedHashMap<>(model);
		addSettingsReferences(req, response);

		jsonResponse(res, 200, response);
	}

	/**
	 * Advertise the WOPI Settings fetch URLs so Collabora loads the user's and the shared settings when the document
	 * opens. The document access token is reused; the stamp lets Collabora cache and only re-fetch on change.
	 */
	private void addSettingsReferences(final WebScriptRequest req, final Map<String, Object> response) {
		final String accessToken = req.getParameter(ACCESS_TOKEN);
		response.put(USER_SETTINGS, settingsReference(CollaboraSettingsService.TYPE_USERCONFIG, accessToken));
		response.put(SHARED_SETTINGS, settingsReference(CollaboraSettingsService.TYPE_SYSTEMCONFIG, accessToken));
	}

	private Map<String, String> settingsReference(final String type, final String accessToken) {
		final Map<String, String> ref = new LinkedHashMap<>(2);
		ref.put(URI, collaboraSettingsService.settingsUrl(type, accessToken));
		ref.put(STAMP, collaboraSettingsService.settingsStamp(type));
		return ref;
	}

	private void ensureVersioningEnabled(final NodeRef nodeRef) {
		// Force Versioning
		if (!nodeService.hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE)) {
			Map<QName, Serializable> props = new HashMap<>(1, 1.0f);

			// should auto versioning be requested?
			props.put(ContentModel.PROP_AUTO_VERSION, true);

			// should auto versioning of properties be requested?
			props.put(ContentModel.PROP_AUTO_VERSION_PROPS, false);

			versionService.ensureVersioningEnabled(nodeRef, props);
		}
	}

	private boolean userCanWrite(final NodeRef nodeRef) {
		AccessStatus perm = permissionService.hasPermission(nodeRef, PermissionService.WRITE);
		return AccessStatus.ALLOWED == perm;
	}

	/**
	 * Builds the display name shown in the Collabora UI (cursor labels, avatars, change attribution) from the person's
	 * first and last name. Falls back to the login when neither is set, so the field is never blank.
	 */
	private String resolveUserFriendlyName(final String userName) {
		if (personService.personExists(userName)) {
			NodeRef personRef = personService.getPerson(userName);
			Map<QName, Serializable> personProps = nodeService.getProperties(personRef);
			String firstName = (String) personProps.get(ContentModel.PROP_FIRSTNAME);
			String lastName = (String) personProps.get(ContentModel.PROP_LASTNAME);
			String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
			if (!fullName.isEmpty()) {
				return fullName;
			}
		}
		return userName;
	}

	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}

	public void setPermissionService(PermissionService permissionService) {
		this.permissionService = permissionService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setFeatureRestrictionService(FeatureRestrictionService featureRestrictionService) {
		this.featureRestrictionService = featureRestrictionService;
	}

	public void setCollaboraSettingsService(CollaboraSettingsService collaboraSettingsService) {
		this.collaboraSettingsService = collaboraSettingsService;
	}
}
