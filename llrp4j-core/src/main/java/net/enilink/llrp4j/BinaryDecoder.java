package net.enilink.llrp4j;

import static net.enilink.llrp4j.EncodingUtil.decodeEnum;
import static net.enilink.llrp4j.EncodingUtil.indent;
import static net.enilink.llrp4j.EncodingUtil.properties;
import static net.enilink.llrp4j.EncodingUtil.propertyType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.llrp.ltk.schema.core.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.llrp4j.annotations.LlrpField;
import net.enilink.llrp4j.bitbuffer.BitBuffer;
import net.enilink.llrp4j.bitbuffer.SimpleBitBuffer;
import net.enilink.llrp4j.impl.BaseType;
import net.enilink.llrp4j.impl.CustomKey;
import net.enilink.llrp4j.impl.CustomParameter;
import net.enilink.llrp4j.impl.Parameter;
import net.enilink.llrp4j.impl.Property;
import net.enilink.llrp4j.types.LlrpEnum;
import net.enilink.llrp4j.types.LlrpMessage;
import net.enilink.llrp4j.types.Types;

public class BinaryDecoder {
	final static Logger logger = LoggerFactory.getLogger(BinaryDecoder.class);

	final static int RESERVED_LENGTH = 3;
	final static int VERSION_LENGTH = 3;
	final static int TYPE_LENGTH = 10;

	LlrpContext context;

	public BinaryDecoder(LlrpContext context) {
		this.context = context;
	}

	protected void decodeReserved(BaseType type, BitBuffer buffer) throws Exception {
		int reserved = type.reservedBits;
		while (reserved-- > 0) {
			buffer.getBoolean();
		}
	}

	public LlrpMessage decodeMessage(BitBuffer buffer) throws Exception {
		// Rsvd 3 bits (0 .. 2)
		buffer.position(buffer.position() + RESERVED_LENGTH);
		// Version 3 bits (3 .. 5)
		int version = buffer.getInt(VERSION_LENGTH);
		// Message Type 10 bits (6 .. 15)
		int typeNum = buffer.getIntUnsigned(TYPE_LENGTH);
		// Message Length 32 bits (16 .. 47)
		long length = buffer.getLongUnsigned(32);
		// Message ID 32 bits (48 .. 79)
		long messageID = buffer.getLongUnsigned(32);

		BaseType messageType;
		if (typeNum == 1023) {
			// custom message

			// Vendor ID 32 bits
			long vendor = buffer.getLongUnsigned(32);
			// Subtype 8 bit
			int subtype = buffer.getIntUnsigned(8);

			messageType = context.customMessageTypes.get(new CustomKey(vendor, subtype));
		} else {
			messageType = context.messageTypes.get(typeNum);
		}
		LlrpMessage message = (LlrpMessage) messageType.typeClass.newInstance();
		message.setMessageID(messageID);

		// remove reserved bits
		decodeReserved(messageType, buffer);

		// call the message specific encode function
		decodeProperties(message, messageType.properties(), buffer);
		return (LlrpMessage) message;
	}

	int depth = 0;

	private void decodeProperties(Object o, Property[] properties, BitBuffer buffer) throws Exception {
		for (Property property : properties) {
			int pos = buffer.position();
			if (pos >= buffer.size()) {
				break;
			}
			if (logger.isDebugEnabled()) {
				logger.debug(indent(depth,
						"decode " + property.field + " pos=" + (pos + ((SimpleBitBuffer) buffer).offset())));
				depth++;
			}
			Object fieldValue;
			if (property.isField) {
				fieldValue = decodeField(property.field, buffer);
			} else {
				fieldValue = decodeParameter(propertyType(property.field),
						List.class.isAssignableFrom(property.field.getType()), property.required, buffer);
			}
			property.field.set(o, fieldValue);
			if (logger.isDebugEnabled()) {
				depth--;
				logger.debug(indent(depth, "decoded " + property.field + " length=" + (buffer.position() - pos)));
			}
		}
	}

	private Object decodeField(Field field, BitBuffer buffer) throws Exception {
		LlrpField annotation = field.getAnnotation(LlrpField.class);
		FieldType type = annotation.type();
		for (int i = 0; i < annotation.reservedBefore(); i++) {
			buffer.getBoolean();
		}
		Object value = Types.decode(type, buffer);
		Class<?> elementType = propertyType(field);
		if (LlrpEnum.class.isAssignableFrom(elementType)) {
			value = decodeEnum(elementType, value);
		}
		for (int i = 0; i < annotation.reservedAfter(); i++) {
			buffer.getBoolean();
		}
		return value;
	}

	private Object decodeParameter(Class<?> expectedType, boolean list, boolean required, BitBuffer buffer)
			throws Exception {
		int bufferSize = buffer.size();

		Object parameter = null;
		List<Object> elements = null;
		int count = 0;
		while (buffer.position() + 8 < bufferSize) {
			boolean tvParameter = buffer.getBoolean();
			buffer.position(buffer.position() - 1);
			if (tvParameter) {
				parameter = decodeTVParameter(expectedType, buffer);
			} else {
				parameter = decodeTLVParameter(expectedType, buffer);
			}
			if (parameter == null) {
				if (count == 0 && required) {
					throw new LlrpException(
							"Missing required parameter of type '" + expectedType.getName() + "'.");
				}
				break;
			}
			if (list) {
				if (elements == null) {
					elements = new ArrayList<>();
				}
				elements.add(parameter);
			} else {
				break;
			}
			++count;
		}
		if (list) {
			return elements;
		} else {
			return parameter;
		}
	}

	private Object decodeTVParameter(Class<?> expectedType, BitBuffer buffer) throws Exception {
		int start = buffer.position();

		// first bit is always 1
		buffer.getBoolean();
		// decode type number
		int typeNum = buffer.getIntUnsigned(7);

		Parameter parameterType = context.parameterTypes.get(typeNum);
		if (parameterType == null) {
			throw new LlrpException("Unknown parameter with type=" + typeNum);
		}
		if (!expectedType.isAssignableFrom(parameterType.typeClass)) {
			buffer.position(start);
			return null;
		}

		Object parameter = parameterType.typeClass.newInstance();
		decodeProperties(parameter, properties(parameterType.type, context), buffer);
		return parameter;
	}

	private Object decodeTLVParameter(Class<?> expectedType, BitBuffer buffer) throws Exception {
		int start = buffer.position();

		// Reserved 6 bits (0 .. 5)
		buffer.position(start + 6);
		// Parameter Type 10 bits (6 .. 15)
		int typeNum = buffer.getIntUnsigned(10);

		Annotation parameterType;
		Class<?> typeClass;
		Parameter p = context.parameterTypes.get(typeNum);
		if (p == null) {
			throw new LlrpException("Unknown parameter with type=" + typeNum);
		}
		typeClass = p.typeClass;
		parameterType = p.type;
		boolean isCustom = typeNum == 1023;
		// Parameter Length 16 bits (16 .. 31)
		int length = buffer.getIntUnsigned(16);

		Object parameter = null;
		if (isCustom) {
			// Vendor ID 32 bits
			long vendor = buffer.getLongUnsigned(32);
			// Subtype 32 bits
			long subtype = buffer.getLongUnsigned(32);

			CustomParameter customParameter = context.customParameterTypes.get(new CustomKey(vendor, subtype));
			if (customParameter != null) {
				parameterType = customParameter.type;
				typeClass = customParameter.typeClass;
				// TODO check allowedIn
			} else {
				// rewind vendor and subtype
				buffer.position(buffer.position() - 64);
			}
		}
		if (!expectedType.isAssignableFrom(typeClass)) {
			buffer.position(start);
			return null;
		}
		parameter = typeClass.newInstance();
		int pos = buffer.position();
		int paramContentLength = length * 8 - (pos - start);
		if (paramContentLength > 0) {
			decodeProperties(parameter, properties(parameterType, context), buffer.slice(pos, paramContentLength));
		}
		buffer.position(pos + paramContentLength);
		return parameter;
	}
}
