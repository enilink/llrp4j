package net.enilink.llrp4j;

import static net.enilink.llrp4j.EncodingUtil.decodeEnum;
import static net.enilink.llrp4j.EncodingUtil.firstUpper;
import static net.enilink.llrp4j.EncodingUtil.indent;
import static net.enilink.llrp4j.EncodingUtil.parameterType;
import static net.enilink.llrp4j.EncodingUtil.properties;
import static net.enilink.llrp4j.EncodingUtil.typeNum;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.llrp.ltk.schema.core.FieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.llrp4j.annotations.LlrpField;
import net.enilink.llrp4j.impl.BaseType;
import net.enilink.llrp4j.impl.Property;
import net.enilink.llrp4j.types.LlrpEnum;
import net.enilink.llrp4j.types.LlrpMessage;
import net.enilink.llrp4j.types.XmlTypes;
import net.enilink.llrp4j.xml.AbstractXMLParser;
import net.enilink.llrp4j.xml.ParseException;

public class XmlDecoder {
	class Parser extends AbstractXMLParser {
		Parser(XMLStreamReader reader) {
			super(reader);
		}

		LlrpMessage parseMessage() throws Exception {
			QName name = nextOrFail();
			Class<?> typeClass = context.qnameToClass.get(name);
			if (typeClass == null) {
				throw new ParseException("Unknown element: " + name);
			}

			LlrpMessage message = (LlrpMessage) typeClass.newInstance();
			BaseType messageType = context.messageType(typeClass);
			parseProperties(message, messageType.properties());
			return (LlrpMessage) message;
		}

		int depth = 0;

		void parseProperties(Object o, Property[] properties) throws Exception {
			QName name = next();

			for (int i = 0; i < properties.length; i++) {
				Property property = properties[i];

				boolean parseProperty = false;
				Class<?> elementClass = null;
				if (property.isField) {
					String expectedName = firstUpper(property.field.getName());
					if (name != null && name.getLocalPart().equals(expectedName)) {
						parseProperty = true;
					}
				} else {
					Class<?> expectedClass = propertyType(property.field);
					elementClass = context.qnameToClass.get(name);
					if (elementClass != null && expectedClass.isAssignableFrom(elementClass)) {
						parseProperty = true;
					}
				}

				if (parseProperty) {
					if (logger.isDebugEnabled()) {
						logger.debug(indent(depth, "decode " + property.field));
						depth++;
					}

					Object fieldValue;
					if (property.isField) {
						fieldValue = parseField(property.field, null);
						if (property.required && fieldValue == null) {
							throw new ParseException("Missing content in element " + name);
						}
						property.field.set(o, fieldValue);
					} else {
						boolean isList = List.class.isAssignableFrom(property.field.getType());
						fieldValue = parseParameter(elementClass, isList, property.required);
						if (property.required && (fieldValue == null
								|| fieldValue instanceof List && ((List<?>) fieldValue).isEmpty())) {
							unexpected(name);
						}
						if (isList && !(fieldValue instanceof List)) {
							@SuppressWarnings("unchecked")
							List<Object> list = (List<Object>) property.field.get(o);
							if (list == null) {
								list = new ArrayList<>();
								property.field.set(o, list);
							}
							list.add(fieldValue);
						} else {
							property.field.set(o, fieldValue);
						}
					}

					if (logger.isDebugEnabled()) {
						depth--;
						logger.debug(indent(depth, "decoded " + property.field));
					}

					end();
					name = next();
				} else if (property.required) {
					unexpected(name);
				}
			}
			if (name != null) {
				useCurrentAsNext();
			}
		}

		private Object parseField(Field field, String value) throws Exception {
			LlrpField annotation = field.getAnnotation(LlrpField.class);
			FieldType type = annotation.type();

			if (value == null) {
				value = parseStringValue();
			}
			if (value.length() > 0) {
				Object javaValue = XmlTypes.fromString(type, annotation.format(), value);
				Class<?> elementType = propertyType(field);
				if (LlrpEnum.class.isAssignableFrom(elementType)) {
					javaValue = decodeEnum(elementType, javaValue);
				}
				return javaValue;
			}
			return null;
		}

		protected Object parseParameter() throws Exception {
			QName name = nextOrFail();
			Class<?> elementClass = context.qnameToClass.get(name);
			return parseParameter(elementClass, false, false);
		}

		private Object parseParameter(Class<?> expectedType, boolean list, boolean required) throws Exception {
			Annotation parameterType = parameterType(expectedType);
			if (parameterType != null && typeNum(parameterType) < 128) {
				return parseTVParameter(parameterType, expectedType);
			} else {
				return parseTLVParameter(parameterType, expectedType, required);
			}
		}

		private Object parseTVParameter(Annotation parameterType, Class<?> expectedType) throws Exception {
			Object parameter = expectedType.newInstance();
			parseProperties(parameter, properties(parameterType, context));
			return parameter;
		}

		private Object parseTLVParameter(Annotation parameterType, Class<?> expectedType, boolean required)
				throws Exception {
			Object parameter = expectedType.newInstance();
			parseProperties(parameter, properties(parameterType, context));
			return parameter;
		}
	}

	final static Logger logger = LoggerFactory.getLogger(XmlDecoder.class);

	LlrpContext context;

	public XmlDecoder(LlrpContext context) {
		this.context = context;
	}

	private Class<?> propertyType(Field field) {
		Class<?> propertyType = field.getType();
		if (List.class.isAssignableFrom(propertyType)) {
			if (field.getGenericType() instanceof ParameterizedType) {
				ParameterizedType pType = (ParameterizedType) field.getGenericType();
				propertyType = (Class<?>) pType.getActualTypeArguments()[0];
			}
		}
		return propertyType;
	}

	public LlrpMessage decodeMessage(XMLStreamReader reader) throws Exception {
		return new Parser(reader).parseMessage();
	}

	public Object decodeParameter(XMLStreamReader reader) throws Exception {
		return new Parser(reader).parseParameter();
	}
}
