// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;

import java.io.Serializable;

import org.alfresco.repo.version.Version2Model;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.version.VersionService;

/**
 * Resolves a WOPI file id to the node it designates: a live node of the SpacesStore, or a frozen version node of the
 * version store.
 */
public class WopiFileIds {

	private static final StoreRef VERSION_STORE = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, Version2Model.STORE_ID);

	private NodeService nodeService;

	public WopiFileIds() {
	}

	public WopiFileIds(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	/**
	 * @param nodeService the node service used to test node existence and read the frozen node reference of a version
	 */
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	/**
	 * @param fileId the WOPI file id, a node UUID
	 * @return the live node when it exists, else the version node when it and its live node exist, else the live node
	 *         reference so the caller answers not found
	 * @throws org.springframework.extensions.webscripts.WebScriptException if the id is not a valid UUID
	 */
	public NodeRef resolve(String fileId) {
		NodeRefValidator.validateUUID(fileId);
		final NodeRef live = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, fileId);
		if (this.nodeService.exists(live)) {
			return live;
		}
		final NodeRef version = new NodeRef(VERSION_STORE, fileId);
		if (exists(version)) {
			return version;
		}
		return live;
	}

	/**
	 * @param ref a node reference, a version node given with the {@code versionStore} protocol included
	 * @return whether the node exists and, for a version node, whether the live node it was frozen from still exists
	 */
	public boolean exists(NodeRef ref) {
		final NodeRef node = normalize(ref);
		if (node == null || !this.nodeService.exists(node)) {
			return false;
		}
		if (!isVersionNode(node)) {
			return true;
		}
		final Serializable frozen = this.nodeService.getProperty(node, Version2Model.PROP_QNAME_FROZEN_NODE_REF);
		return frozen instanceof NodeRef && this.nodeService.exists((NodeRef) frozen);
	}

	/**
	 * @param ref a node reference
	 * @return whether the node belongs to the version store
	 */
	public static boolean isVersionNode(NodeRef ref) {
		final NodeRef node = normalize(ref);
		return node != null && VERSION_STORE.equals(node.getStoreRef());
	}

	/**
	 * @param ref a node reference
	 * @return the reference with a {@code versionStore://version2Store} store turned into
	 *         {@code workspace://version2Store}, other references unchanged
	 */
	public static NodeRef normalize(NodeRef ref) {
		if (ref == null || !VersionService.VERSION_STORE_PROTOCOL.equals(ref.getStoreRef()
				.getProtocol()) || !Version2Model.STORE_ID.equals(ref.getStoreRef()
						.getIdentifier())) {
			return ref;
		}
		return new NodeRef(VERSION_STORE, ref.getId());
	}
}
