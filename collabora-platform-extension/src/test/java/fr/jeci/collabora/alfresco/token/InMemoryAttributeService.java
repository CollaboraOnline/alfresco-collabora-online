// SPDX-FileCopyrightText: 2026 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco.token;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.cmr.attributes.AttributeService;

public class InMemoryAttributeService implements AttributeService {

	private final Map<List<Serializable>, Serializable> attributes = new LinkedHashMap<>();

	@Override
	public boolean exists(Serializable... keys) {
		return attributes.containsKey(List.of(keys));
	}

	@Override
	public Serializable getAttribute(Serializable... keys) {
		return attributes.get(List.of(keys));
	}

	@Override
	public void getAttributes(AttributeQueryCallback callback, Serializable... keys) {
		List<Serializable> prefix = List.of(keys);
		long id = 0;
		for (Map.Entry<List<Serializable>, Serializable> entry : new ArrayList<>(attributes.entrySet())) {
			if (startsWith(entry.getKey(), prefix)) {
				id++;
				if (!callback.handleAttribute(id, entry.getValue(), entry.getKey()
						.toArray(new Serializable[0]))) {
					return;
				}
			}
		}
	}

	@Override
	public void setAttribute(Serializable value, Serializable... keys) {
		attributes.put(List.of(keys), value);
	}

	@Override
	public void createAttribute(Serializable value, Serializable... keys) {
		if (exists(keys)) {
			throw new IllegalStateException("Attribute exists: " + List.of(keys));
		}
		setAttribute(value, keys);
	}

	@Override
	public void updateOrCreateAttribute(Serializable keyBefore1, Serializable keyBefore2, Serializable keyBefore3,
			Serializable keyAfter1, Serializable keyAfter2, Serializable keyAfter3) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeAttribute(Serializable... keys) {
		attributes.remove(List.of(keys));
	}

	@Override
	public void removeAttributes(Serializable... keys) {
		List<Serializable> prefix = List.of(keys);
		attributes.keySet()
				.removeIf(key -> startsWith(key, prefix));
	}

	public String dump() {
		return attributes.toString();
	}

	private static boolean startsWith(List<Serializable> key, List<Serializable> prefix) {
		return key.size() >= prefix.size() && key.subList(0, prefix.size())
				.equals(prefix);
	}
}
