// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.restriction;

import java.util.Set;

/**
 * Manages Collabora Online feature restriction based on group membership.
 * <p>
 * When enabled, only users belonging to a configurable Alfresco group are considered "licensed".
 * Unlicensed users receive {@code IsUserLocked=true} in the WOPI CheckFileInfo response,
 * which combined with {@code feature_lock.is_lock_readonly=true} in coolwsd.xml forces them
 * into read-only mode.
 *
 * @see <a href="https://sdk.collaboraonline.com/docs/installation/Configuration.html#feature-restriction">
 *      Collabora Online Feature Restriction</a>
 */
public interface FeatureRestrictionService {
	boolean isFeatureRestrictionEnabled();

	boolean hasLicense(String userName);

	int getLicenseCount();

	Set<String> getLicensedUsers();

	void grantLicense(String userName);

	void revokeLicense(String userName);

	int getMaxLicenses();

	void setMaxLicenses(int maxLicenses);
}
