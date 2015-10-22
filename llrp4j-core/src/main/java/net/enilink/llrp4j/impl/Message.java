package net.enilink.llrp4j.impl;

import net.enilink.llrp4j.annotations.LlrpMessageType;

public class Message extends BaseType {
	public final LlrpMessageType type;

	public Message(LlrpMessageType type, Class<?> typeClass) {
		super(typeClass, type.reserved());
		this.type = type;
	}
}