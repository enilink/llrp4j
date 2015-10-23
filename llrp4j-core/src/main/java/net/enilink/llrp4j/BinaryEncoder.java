package net.enilink.llrp4j;

import static net.enilink.llrp4j.EncodingUtil.encodeEnum;
import static net.enilink.llrp4j.EncodingUtil.indent;
import static net.enilink.llrp4j.EncodingUtil.isCustom;
import static net.enilink.llrp4j.EncodingUtil.parameterType;
import static net.enilink.llrp4j.EncodingUtil.properties;
import static net.enilink.llrp4j.EncodingUtil.propertyType;
import static net.enilink.llrp4j.EncodingUtil.typeNum;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.llrp.ltk.schema.core.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.llrp4j.annotations.LlrpCustomParameterType;
import net.enilink.llrp4j.annotations.LlrpField;
import net.enilink.llrp4j.bitbuffer.BitBuffer;
import net.enilink.llrp4j.impl.BaseType;
import net.enilink.llrp4j.impl.CustomMessage;
import net.enilink.llrp4j.impl.Message;
import net.enilink.llrp4j.impl.Property;
import net.enilink.llrp4j.types.LlrpEnum;
import net.enilink.llrp4j.types.LlrpMessage;
import net.enilink.llrp4j.types.Types;

public class BinaryEncoder {
	final static Logger logger = LoggerFactory.getLogger(BinaryEncoder.class);

	protected LlrpContext context;

	public BinaryEncoder(LlrpContext context) {
		this.context = context;
	}

	protected void encodeReserved(BaseType type, BitBuffer buffer) {
		int reserved = type.reservedBits;
		while (reserved-- > 0) {
			buffer.putBoolean(false);
		}
	}

	public void encodeMessage(LlrpMessage message, BitBuffer buffer) {
		int typeNum;
		BaseType messageType = context.messageType(message.getClass());
		if (messageType == null) {
			throw new LlrpException("Unsupported message type: " + message.getClass());
		} else {
			typeNum = messageType instanceof CustomMessage ? 1023 : ((Message) messageType).type.typeNum();
		}
		// Rsvd 3 bits (0 .. 2)
		buffer.putInt(0, 3);
		// Version 3 bits (3 .. 5)
		buffer.putInt(1, 3);
		// Message Type 10 bits (6 .. 15)
		buffer.putInt(typeNum, 10);
		int messageLengthStart = buffer.position();
		// Message Length 32 bits (16 .. 47) - add as placeholder here
		buffer.putInt(0, 32);
		// Message ID 32 bits (48 .. 79)
		buffer.putLong(message.messageID(), 32);

		if (messageType instanceof CustomMessage) {
			// Vendor ID 32 bits
			buffer.putLong(((CustomMessage) messageType).type.vendor(), 32);
			// Subtype 8 bit
			buffer.putInt(((CustomMessage) messageType).type.subType(), 8);
		}

		// add reserved bits
		encodeReserved(messageType, buffer);

		// call the message specific encode function
		encodeProperties(message, messageType.properties(), buffer);

		int messageLength = (buffer.position() + 7) / 8;

		int mark = buffer.position();
		buffer.position(messageLengthStart);
		buffer.putInt(messageLength, 32);
		buffer.position(mark);
	}

	int depth = 0;

	private void encodeProperties(Object o, Property[] properties, BitBuffer buffer) {
		try {
			for (final Property property : properties) {
				Object fieldValue = property.field.get(o);
				boolean list = List.class.isAssignableFrom(property.field.getType());
				boolean empty = fieldValue == null || list && ((List<?>) fieldValue).isEmpty();
				if (empty && property.required) {
					throw new LlrpException("Missing required " + (property.isField ? "field" : "parameter") + "' "
							+ property.field.getName() + "' in "
							+ (LlrpMessage.class.isAssignableFrom(o.getClass()) ? "message" : "parameter")
							+ " of type '" + o.getClass().getSimpleName() + "'");
				}
				if (empty) {
					continue;
				}

				int pos = buffer.position();
				if (logger.isDebugEnabled()) {
					logger.debug(indent(depth, "encode " + property.field + " pos=" + pos));
					depth++;
				}
				if (property.isField) {
					encodeField(property.field, fieldValue, buffer);
				} else {
					encodeParameter(fieldValue, buffer);
				}
				if (logger.isDebugEnabled()) {
					depth--;
					logger.debug(indent(depth, "encoded " + property.field + " [" + pos + ", " + buffer.position()
							+ "], length=" + (buffer.position() - pos)));
				}
			}
		} catch (Exception e) {
			if (e instanceof LlrpException) {
				throw (LlrpException) e;
			}
			throw new LlrpException(e);
		}
	}

	private void encodeField(Field field, Object value, BitBuffer buffer) throws Exception {
		LlrpField annotation = field.getAnnotation(LlrpField.class);
		FieldType type = annotation.type();
		for (int i = 0; i < annotation.reservedBefore(); i++) {
			buffer.putBoolean(false);
		}
		if (value instanceof LlrpEnum
				|| (value instanceof List<?> && LlrpEnum.class.isAssignableFrom(propertyType(field)))) {
			value = encodeEnum(type, value);
		}
		Types.encode(value, type, buffer);
		for (int i = 0; i < annotation.reservedAfter(); i++) {
			buffer.putBoolean(false);
		}
	}

	private void encodeParameter(Object parameter, BitBuffer buffer) throws Exception {
		List<?> elements = (parameter instanceof List) ? (List<?>) parameter : Arrays.asList(parameter);
		for (Object element : elements) {
			Annotation parameterType = parameterType(element.getClass());
			// TV Parameters have type number from 0 - 127, TLV from 128 - 2047
			int typeNum = typeNum(parameterType);
			if (typeNum < 128) {
				encodeTVParameter(parameterType, element, buffer);
			} else {
				encodeTLVParameter(parameterType, element, buffer);
			}
		}
	}

	private void encodeTVParameter(Annotation parameterType, Object parameter, BitBuffer buffer) throws Exception {
		// first bit must always be set to 1
		buffer.put(true);
		// encode type number
		buffer.putInt(typeNum(parameterType), 7);

		for (Property property : properties(parameterType, context)) {
			Object fieldValue = property.field.get(parameter);
			encodeField(property.field, fieldValue, buffer);
		}
	}

	private void encodeTLVParameter(Annotation parameterType, Object parameter, BitBuffer buffer) throws Exception {
		int start = buffer.position();

		// Reserved 6 bits (0 .. 5)
		buffer.putInt(0, 6);
		// Parameter Type 10 bits (6 .. 15)
		buffer.putInt(typeNum(parameterType), 10);
		int lengthStart = buffer.position();
		// Parameter Length 16 bits (16 .. 31) - add as placeholder here
		buffer.putInt(0, 16);

		Property[] properties = properties(parameterType, context);
		if (isCustom(parameterType)) {
			LlrpCustomParameterType customType = (LlrpCustomParameterType) parameterType;
			// Vendor ID 32 bits
			buffer.putLong(customType.vendor(), 32);
			// Subtype 32 bits
			buffer.putLong(customType.subType(), 32);
		}
		encodeProperties(parameter, properties, buffer);

		int parameterLength = (buffer.position() - start + 7) / 8;
		int padding = parameterLength * 8 - buffer.position();
		while (padding-- > 0) {
			buffer.putBoolean(false);
		}

		int mark = buffer.position();
		buffer.position(lengthStart);
		buffer.putInt(parameterLength, 16);
		buffer.position(mark);
	}
}
