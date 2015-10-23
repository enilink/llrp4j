package net.enilink.llrp4j.impl;

import net.enilink.llrp4j.LlrpException;
import net.enilink.llrp4j.annotations.LlrpProperties;

public class BaseType {
	public final Class<?> typeClass;
	public final int reservedBits;
	private Property[] properties;

	public BaseType(Class<?> typeClass, int reservedBits) {
		this.typeClass = typeClass;
		this.reservedBits = reservedBits;
	}

	public synchronized Property[] properties() {
		if (properties == null) {
			LlrpProperties annotation = typeClass.getAnnotation(LlrpProperties.class);
			properties = new Property[annotation.value().length];
			int i = 0;
			for (String name : annotation.value()) {
				try {
					properties[i++] = new Property(typeClass.getDeclaredField(name));
				} catch (Exception e) {
					throw new LlrpException(e);
				}
			}
		}
		return properties;
	}
}