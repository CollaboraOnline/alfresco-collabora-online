// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.restriction;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.MailActionExecuter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link FeatureRestrictionService} backed by Alfresco group membership.
 * <p>
 * License state is determined by membership in a configurable group (default {@code GROUP_COLLABORA_ONLINE}).
 * The maximum number of licenses is persisted via {@link AttributeService} and survives restarts.
 * The group is auto-created on first startup if it does not exist.
 */
public class FeatureRestrictionServiceImpl implements FeatureRestrictionService {

	private static final Logger logger = LoggerFactory.getLogger(FeatureRestrictionServiceImpl.class);

	private static final String ATTR_KEY_ROOT = "collabora";
	private static final String ATTR_KEY_FEATURE = "feature-restriction";
	private static final String ATTR_KEY_MAX = "max-licenses";
	private static final String NOTIFICATION_TO = "pristy@jeci.fr";

	private boolean enabled;
	private String groupName;
	private int initialMaxLicenses;
	private final AtomicInteger maxLicenses = new AtomicInteger(-1);

	private AuthorityService authorityService;
	private AttributeService attributeService;
	private ActionService actionService;
	private NodeService nodeService;
	private org.alfresco.service.cmr.security.PersonService personService;
	private org.alfresco.repo.admin.SysAdminParams sysAdminParams;
	private String notificationFrom;

	public void init() {
		if (!enabled) {
			logger.info("Collabora feature restriction is disabled");
			return;
		}

		AuthenticationUtil.runAsSystem(() -> {
			ensureGroupExists();
			loadPersistedMaxLicenses();
			return null;
		});

		logger.info("Collabora feature restriction enabled. Group: {}, Max licenses: {}", groupName, formatMax(maxLicenses
				.get()));
	}

	private void ensureGroupExists() {
		if (!authorityService.authorityExists(groupName)) {
			String shortName = groupName.startsWith(PermissionService.GROUP_PREFIX)
					? groupName.substring(PermissionService.GROUP_PREFIX.length())
					: groupName;
			authorityService.createAuthority(AuthorityType.GROUP, shortName, "Collabora Online Licensed Users", Collections
					.emptySet());
			logger.info("Created group '{}'", groupName);
		}
	}

	private void loadPersistedMaxLicenses() {
		Serializable stored = attributeService.getAttribute(ATTR_KEY_ROOT, ATTR_KEY_FEATURE, ATTR_KEY_MAX);
		if (stored != null) {
			maxLicenses.set((Integer) stored);
		} else {
			maxLicenses.set(initialMaxLicenses);
			attributeService.setAttribute(initialMaxLicenses, ATTR_KEY_ROOT, ATTR_KEY_FEATURE, ATTR_KEY_MAX);
		}
	}

	@Override
	public boolean isFeatureRestrictionEnabled() {
		return enabled;
	}

	@Override
	public boolean hasLicense(String userName) {
		if (!enabled) {
			return true;
		}
		return AuthenticationUtil.runAsSystem(() -> {
			Set<String> authorities = authorityService.getAuthoritiesForUser(userName);
			return authorities.contains(groupName);
		});
	}

	@Override
	public int getLicenseCount() {
		return getLicensedUsers().size();
	}

	@Override
	public Set<String> getLicensedUsers() {
		if (!authorityService.authorityExists(groupName)) {
			return Collections.emptySet();
		}
		return authorityService.getContainedAuthorities(AuthorityType.USER, groupName, false);
	}

	@Override
	public void grantLicense(String userName) {
		int max = maxLicenses.get();
		if (max != -1 && getLicenseCount() >= max) {
			throw new WebScriptException(Status.STATUS_CONFLICT, "Cannot grant license: maximum licenses reached (" + max
																				  + ")");
		}

		Set<String> current = getLicensedUsers();
		if (current.contains(userName)) {
			throw new WebScriptException(Status.STATUS_CONFLICT, "User '" + userName + "' already has a license");
		}

		authorityService.addAuthority(groupName, userName);
		int count = getLicenseCount();
		logger.info("Collabora license granted for user '{}'. Total licenses: {}/{}", userName, count, formatMax(max));
	}

	@Override
	public void revokeLicense(String userName) {
		Set<String> current = getLicensedUsers();
		if (!current.contains(userName)) {
			throw new WebScriptException(Status.STATUS_NOT_FOUND, "User '" + userName + "' does not have a license");
		}

		authorityService.removeAuthority(groupName, userName);
		int count = getLicenseCount();
		int max = maxLicenses.get();
		logger.info("Collabora license revoked for user '{}'. Total licenses: {}/{}", userName, count, formatMax(max));
	}

	@Override
	public int getMaxLicenses() {
		return maxLicenses.get();
	}

	@Override
	public void setMaxLicenses(int newMax) {
		int oldMax = maxLicenses.getAndSet(newMax);
		attributeService.setAttribute(newMax, ATTR_KEY_ROOT, ATTR_KEY_FEATURE, ATTR_KEY_MAX);
		logger.info("Collabora max licenses updated from {} to {}", formatMax(oldMax), formatMax(newMax));
		sendLicenseChangeNotification(oldMax, newMax);
	}

	private void sendLicenseChangeNotification(int oldMax, int newMax) {
		try {
			String userName = AuthenticationUtil.getRunAsUser();
			String from = resolveUserEmail(userName);
			String server = resolveServerName();
			String subject = "Collabora Online - License quota changed";
			String body = String.format("The Collabora Online license quota has been changed.\n\n" + "Server: %s\n"
												 + "Changed by: %s\n" + "Previous quota: %s\n" + "New quota: %s\n"
												 + "Current usage: %d licenses\n", server, userName, formatMax(oldMax), formatMax(
														 newMax), getLicenseCount());

			Map<String, Serializable> params = new HashMap<>();
			params.put(MailActionExecuter.PARAM_TO, NOTIFICATION_TO);
			params.put(MailActionExecuter.PARAM_FROM, from);
			params.put(MailActionExecuter.PARAM_SUBJECT, subject);
			params.put(MailActionExecuter.PARAM_TEXT, body);

			Action mailAction = actionService.createAction(MailActionExecuter.NAME, params);
			actionService.executeAction(mailAction, null, false, true);
			logger.info("License change notification sent to {}", NOTIFICATION_TO);
		} catch (Exception e) {
			logger.warn("Failed to send license change notification: {}", e.getMessage());
		}
	}

	private String resolveUserEmail(String userName) {
		try {
			if (personService.personExists(userName)) {
				NodeRef personRef = personService.getPerson(userName);
				String email = (String) nodeService.getProperty(personRef, ContentModel.PROP_EMAIL);
				if (email != null && !email.isBlank()) {
					return email;
				}
			}
		} catch (Exception e) {
			logger.debug("Could not resolve email for user '{}': {}", userName, e.getMessage());
		}
		return notificationFrom;
	}

	/**
	 * Resolves the configured Alfresco host (from {@code alfresco.host}) so the notification identifies which server the
	 * change was made on. Falls back to the local hostname when {@link org.alfresco.repo.admin.SysAdminParams} is unset.
	 */
	private String resolveServerName() {
		if (sysAdminParams != null) {
			String host = sysAdminParams.getAlfrescoHost();
			if (host != null && !host.isBlank()) {
				return host;
			}
		}
		try {
			return java.net.InetAddress.getLocalHost()
					.getHostName();
		} catch (Exception e) {
			return "unknown";
		}
	}

	private static String formatMax(int max) {
		return max == -1 ? "unlimited" : Integer.toString(max);
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public void setInitialMaxLicenses(int initialMaxLicenses) {
		this.initialMaxLicenses = initialMaxLicenses;
	}

	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}

	public void setAttributeService(AttributeService attributeService) {
		this.attributeService = attributeService;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setPersonService(org.alfresco.service.cmr.security.PersonService personService) {
		this.personService = personService;
	}

	public void setSysAdminParams(org.alfresco.repo.admin.SysAdminParams sysAdminParams) {
		this.sysAdminParams = sysAdminParams;
	}

	public void setNotificationFrom(String notificationFrom) {
		this.notificationFrom = notificationFrom;
	}
}
