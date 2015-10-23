package net.enilink.llrp4j.types;

import net.enilink.llrp4j.annotations.LlrpCustomMessageType;
import net.enilink.llrp4j.annotations.LlrpMessageType;

/**
 * Represents an LLRP message.
 * 
 */
public abstract class LlrpMessage {
	protected long messageID;

	public long messageID() {
		return messageID;
	}

	public void messageID(long messageID) {
		this.messageID = messageID;
	}

	public Class<?> getResponseType() {
		if (getClass().isAnnotationPresent(LlrpMessageType.class)) {
			return getClass().getAnnotation(LlrpMessageType.class).responseType();
		} else if (getClass().isAnnotationPresent(LlrpCustomMessageType.class)) {
			return getClass().getAnnotation(LlrpCustomMessageType.class).responseType();
		}
		return void.class;
	}

	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);
}
