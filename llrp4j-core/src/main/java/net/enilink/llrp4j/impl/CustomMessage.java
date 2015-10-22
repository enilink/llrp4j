package net.enilink.llrp4j.impl;

import net.enilink.llrp4j.annotations.LlrpCustomMessageType;

public class CustomMessage extends BaseType {
	public final CustomKey key;
	public final LlrpCustomMessageType type;

	public CustomMessage(CustomKey key, LlrpCustomMessageType type, Class<?> typeClass) {
		super(typeClass, type.reserved());
		this.key = key;
		this.type = type;
	}
}