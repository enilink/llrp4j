package net.enilink.llrp4j;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import net.enilink.llrp4j.annotations.LlrpCustomMessageType;
import net.enilink.llrp4j.annotations.LlrpCustomParameterType;
import net.enilink.llrp4j.annotations.LlrpMessageType;
import net.enilink.llrp4j.annotations.LlrpNamespace;
import net.enilink.llrp4j.annotations.LlrpParameterType;
import net.enilink.llrp4j.impl.BaseType;
import net.enilink.llrp4j.impl.CustomKey;
import net.enilink.llrp4j.impl.CustomMessage;
import net.enilink.llrp4j.impl.CustomParameter;
import net.enilink.llrp4j.impl.Message;
import net.enilink.llrp4j.impl.Parameter;

public class LlrpContext {
	protected final Set<Class<?>> classes = new HashSet<>();
	protected final Map<String, String> namespaces = new HashMap<>();

	protected final Map<Integer, Message> messageTypes = new HashMap<>();
	protected final Map<Integer, Parameter> parameterTypes = new HashMap<>();
	protected final Map<CustomKey, CustomMessage> customMessageTypes = new HashMap<>();
	protected final Map<CustomKey, CustomParameter> customParameterTypes = new HashMap<>();

	protected final Map<AnnotationKey, Annotation> cachedAnnotations = new HashMap<>();

	protected final Map<QName, Class<?>> qnameToClass = new HashMap<>();

	static class AnnotationKey {
		final Class<?> targetClass;
		final Class<? extends Annotation> annotationClass;

		AnnotationKey(Class<?> targetClass, Class<? extends Annotation> annotationClass) {
			this.targetClass = targetClass;
			this.annotationClass = annotationClass;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((annotationClass == null) ? 0 : annotationClass.hashCode());
			result = prime * result + ((targetClass == null) ? 0 : targetClass.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof AnnotationKey))
				return false;
			AnnotationKey other = (AnnotationKey) obj;
			if (annotationClass == null) {
				if (other.annotationClass != null)
					return false;
			} else if (!annotationClass.equals(other.annotationClass))
				return false;
			if (targetClass == null) {
				if (other.targetClass != null)
					return false;
			} else if (!targetClass.equals(other.targetClass))
				return false;
			return true;
		}

	}

	protected LlrpContext(Module[] modules) {
		for (Module module : modules) {
			classes.addAll(module.getClasses());
			namespaces.putAll(module.getNamespaces());
		}
		Set<Package> packages = new HashSet<>();
		for (Class<?> c : classes) {
			addParameter(c);
			addMessage(c);
			addCustomMessage(c);

			addXmlType(c);

			packages.add(c.getPackage());
		}
	}

	private void addCustomMessage(Class<?> c) {
		LlrpCustomMessageType a = getAnnotation(c, LlrpCustomMessageType.class);
		if (a != null) {
			CustomKey key = new CustomKey(a.vendor(), a.subType());
			customMessageTypes.put(key, new CustomMessage(key, a, c));
		}

	}

	private void addMessage(Class<?> c) {
		LlrpMessageType a = getAnnotation(c, LlrpMessageType.class);
		if (a != null) {
			messageTypes.put(a.typeNum(), new Message(a, c));
		}
	}

	private void addParameter(Class<?> c) {
		LlrpCustomParameterType customAnnotation = getAnnotation(c, LlrpCustomParameterType.class);
		if (customAnnotation != null) {
			CustomKey key = new CustomKey(customAnnotation.vendor(), customAnnotation.subType());
			customParameterTypes.put(key, new CustomParameter(key, customAnnotation, c));
		} else {
			LlrpParameterType a = getAnnotation(c, LlrpParameterType.class);
			if (a != null) {
				parameterTypes.put(a.typeNum(), new Parameter(a, c));
			}
		}
	}

	private void addXmlType(Class<?> c) {
		LlrpNamespace a = getAnnotation(c, LlrpNamespace.class);
		if (a != null) {
			qnameToClass.put(new QName(a.value(), c.getSimpleName()), c);
		}

	}

	public static LlrpContext create(Module... modules) {
		return new LlrpContext(modules);
	}

	public BinaryEncoder createBinaryEncoder() {
		return new BinaryEncoder(this);
	}

	public BinaryDecoder createBinaryDecoder() {
		return new BinaryDecoder(this);
	}

	public XmlDecoder createXmlDecoder() {
		return new XmlDecoder(this);
	}

	public XmlEncoder createXmlEncoder() {
		return createXmlEncoder(false);
	}

	public XmlEncoder createXmlEncoder(boolean indent) {
		return new XmlEncoder(this, indent);
	}

	public Collection<Class<?>> getClasses() {
		return Collections.unmodifiableCollection(classes);
	}

	CustomMessage customMessageType(Class<?> target) {
		LlrpCustomMessageType a = getAnnotation(target, LlrpCustomMessageType.class);
		if (a != null) {
			return customMessageTypes.get(new CustomKey(a.vendor(), a.subType()));
		}
		return null;
	}

	BaseType messageType(Class<?> target) {
		LlrpMessageType a = getAnnotation(target, LlrpMessageType.class);
		if (a != null) {
			return messageTypes.get(a.typeNum());
		}
		return customMessageType(target);
	}

	CustomParameter customParameterType(Class<?> target) {
		LlrpCustomParameterType a = getAnnotation(target, LlrpCustomParameterType.class);
		if (a != null) {
			return customParameterTypes.get(new CustomKey(a.vendor(), a.subType()));
		}
		return null;
	}

	Parameter parameterType(Class<?> target) {
		LlrpParameterType a = getAnnotation(target, LlrpParameterType.class);
		if (a != null) {
			return parameterTypes.get(a.typeNum());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	protected <E extends Annotation> E getAnnotation(Class<?> target, Class<E> annotationClass) {
		if (target == null || Object.class.equals(target)) {
			return null;
		}
		AnnotationKey key = new AnnotationKey(target, annotationClass);
		Annotation cached = cachedAnnotations.get(key);
		if (cached != null) {
			return (E) cached;
		}
		E annotation;
		if (target.isAnnotationPresent(annotationClass)) {
			annotation = target.getAnnotation(annotationClass);
		} else {
			annotation = getAnnotation(target.getSuperclass(), annotationClass);
		}
		if (annotation != null) {
			cachedAnnotations.put(key, annotation);
		}
		return annotation;
	}

	static final String DEFAULT_NAMESPACE = "http://www.llrp.org/ltk/schema/core/encoding/xml/1.0";

	protected String xmlNamespace(Class<?> clazz) {
		LlrpNamespace ns = clazz.getAnnotation(LlrpNamespace.class);
		if (ns != null) {
			return ns.value();
		}
		return DEFAULT_NAMESPACE;
	}

	public Map<String, String> getNamespaces() {
		return namespaces;
	}
}
