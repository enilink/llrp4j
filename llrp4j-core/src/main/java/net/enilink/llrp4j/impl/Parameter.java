package net.enilink.llrp4j.impl;

import net.enilink.llrp4j.annotations.LlrpParameterType;

public class Parameter extends BaseType {
	public final LlrpParameterType type;

	public Parameter(LlrpParameterType type, Class<?> typeClass) {
		super(typeClass, type.reserved());
		this.type = type;
	}
}