package net.enilink.llrp4j.impl;

import net.enilink.llrp4j.annotations.LlrpCustomParameterType;

public class CustomParameter extends BaseType {
	public final CustomKey key;
	public final LlrpCustomParameterType type;

	public CustomParameter(CustomKey key, LlrpCustomParameterType type, Class<?> typeClass) {
		super(typeClass, type.reserved());
		this.key = key;
		this.type = type;
	}
}