// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora;

import static org.junit.Assert.assertTrue;

import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;
import org.springframework.beans.BeanUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ContextWiringTest {

	private static final String CONTEXT = "alfresco/module/collabora-platform-extension/context/";

	private static final String[] FILES = { "bootstrap-context.xml", "job-context.xml", "service-context.xml",
			"webscript-context.xml" };

	@Test
	public void everyInjectedPropertyHasASetter() throws Exception {
		final Map<String, Element> beans = new HashMap<>();
		for (String file : FILES) {
			final NodeList nodes = parse(file).getElementsByTagName("bean");
			for (int i = 0; i < nodes.getLength(); i++) {
				final Element bean = (Element) nodes.item(i);
				if (!bean.getAttribute("id")
						.isEmpty()) {
					beans.put(bean.getAttribute("id"), bean);
				}
			}
		}

		final List<String> missing = new ArrayList<>();
		for (Element bean : beans.values()) {
			final Class<?> type = beanClass(bean, beans);
			if (type == null) {
				continue;
			}
			final NodeList properties = bean.getElementsByTagName("property");
			for (int i = 0; i < properties.getLength(); i++) {
				final Element property = (Element) properties.item(i);
				if (property.getParentNode() != bean) {
					continue;
				}
				final String name = property.getAttribute("name");
				final PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(type, name);
				if (descriptor == null || descriptor.getWriteMethod() == null) {
					missing.add(bean.getAttribute("id") + "." + name + " on " + type.getName());
				}
			}
		}

		assertTrue("properties without a setter: " + missing, missing.isEmpty());
	}

	private static Class<?> beanClass(Element bean, Map<String, Element> beans) {
		Element current = bean;
		while (current != null) {
			final String className = current.getAttribute("class");
			if (!className.isEmpty()) {
				if (!className.startsWith("fr.jeci.")) {
					return null;
				}
				try {
					return Class.forName(className);
				} catch (ClassNotFoundException e) {
					throw new AssertionError("unknown bean class " + className, e);
				}
			}
			current = beans.get(current.getAttribute("parent"));
		}
		return null;
	}

	private static Document parse(String file) throws Exception {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		try (InputStream in = ContextWiringTest.class.getClassLoader()
				.getResourceAsStream(CONTEXT + file)) {
			return factory.newDocumentBuilder()
					.parse(in);
		}
	}
}
