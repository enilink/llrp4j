package net.enilink.llrp4j;

import static net.enilink.llrp4j.EncodingUtil.firstUpper;
import static net.enilink.llrp4j.EncodingUtil.indent;
import static net.enilink.llrp4j.EncodingUtil.parameterType;
import static net.enilink.llrp4j.EncodingUtil.properties;
import static net.enilink.llrp4j.EncodingUtil.typeNum;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

				if (logger.isDebugEnabled()) {
					logger.debug(indent(depth, "decode " + property.field));
					depth++;
				}

				boolean propertyWasRead = false;
				if (property.isField) {
					// this is a simple scalar field
					String expectedName = firstUpper(property.field.getName());
					if (name != null && name.getLocalPart().equals(expectedName)) {
						Object fieldValue = parseField(property.field, null);
						if (property.required && fieldValue == null) {
							throw new ParseException("Missing content in element " + name);
						}
						property.field.set(o, fieldValue);
						propertyWasRead = true;
					} else if (property.required) {
						unexpected(name);
					}
				} else {
					// this is a parameter object
					boolean isList = List.class.isAssignableFrom(property.field.getType());
					List<Object> valueList = null;
					boolean required = property.required;
					Class<?> expectedClass = propertyType(property.field);

					Object fieldValue = null;
					while (true) {
						Class<?> elementClass = context.qnameToClass.get(name);
						if (elementClass == null || !expectedClass.isAssignableFrom(elementClass)) {
							// reset read flag in case of lists
							propertyWasRead = false;
							if (required) {
								unexpected(name);
							}
							break;
						}

						propertyWasRead = true;
						fieldValue = parseParameter(elementClass, isList, required);
						if (required && (fieldValue == null
								|| fieldValue instanceof List && ((List<?>) fieldValue).isEmpty())) {
							unexpected(name);
						}
						if (isList) {
							if (valueList == null) {
								valueList = new ArrayList<>();
							}
							if (fieldValue instanceof List) {
								valueList.addAll((List<?>) fieldValue);
							} else {
								valueList.add(fieldValue);
							}
							// at least one parameter value was already read
							required = false;

							end();
							name = next();
						} else {
							// read only one parameter
							break;
						}
					}
					if (isList) {
						fieldValue = valueList;
					}
					if (fieldValue != null) {
						property.field.set(o, fieldValue);
					}
				}

				if (logger.isDebugEnabled()) {
					depth--;
					logger.debug(indent(depth, "decoded " + property.field));
				}

				if (propertyWasRead) {
					end();
					name = next();
				}
			}
			if (name != null) {
				useCurrentAsNext();
			}
		}

		private Object stringToEnum(Class<?> enumClass, boolean isList, String value) throws Exception {
			Method valueOf = enumClass.getDeclaredMethod("valueOf", String.class);
			if (isList) {
				String[] elements = value.split("\\s*,\\s*");
				List<Object> enumValues = new ArrayList<>(elements.length);
				for (int i = 0; i < elements.length; i++) {
					enumValues.add(valueOf.invoke(null, elements[i]));
				}
				return enumValues;
			} else {
				return valueOf.invoke(null, value);
			}
		}

		private Object parseField(Field field, String value) throws Exception {
			LlrpField annotation = field.getAnnotation(LlrpField.class);
			FieldType type = annotation.type();

			if (value == null) {
				value = parseStringValue();
			}
			if (value.length() > 0) {
				Class<?> elementType = propertyType(field);
				Object javaValue;
				if (LlrpEnum.class.isAssignableFrom(elementType)) {
					javaValue = stringToEnum(elementType, List.class.isAssignableFrom(field.getType()), value);
				} else {
					javaValue = XmlTypes.fromString(type, annotation.format(), value);
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
