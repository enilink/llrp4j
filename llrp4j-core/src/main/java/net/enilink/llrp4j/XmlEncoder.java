package net.enilink.llrp4j;

import static net.enilink.llrp4j.EncodingUtil.firstUpper;
import static net.enilink.llrp4j.EncodingUtil.indent;
import static net.enilink.llrp4j.EncodingUtil.parameterType;
import static net.enilink.llrp4j.EncodingUtil.properties;
import static net.enilink.llrp4j.EncodingUtil.propertyType;
import static net.enilink.llrp4j.EncodingUtil.typeNum;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.enilink.llrp4j.annotations.LlrpField;
import net.enilink.llrp4j.impl.BaseType;
import net.enilink.llrp4j.impl.Property;
import net.enilink.llrp4j.types.LlrpEnum;
import net.enilink.llrp4j.types.LlrpMessage;
import net.enilink.llrp4j.types.XmlTypes;
import net.enilink.llrp4j.xml.IndentingXMLStreamWriter;

public class XmlEncoder {
	final static Logger logger = LoggerFactory.getLogger(XmlEncoder.class);

	protected LlrpContext context;
	protected final boolean indent;

	public XmlEncoder(LlrpContext context, boolean indent) {
		this.context = context;
		this.indent = indent;
	}

	protected String localName(Class<?> c) {
		return c.getSimpleName();
	}

	protected void setNamespaces(XMLStreamWriter writer) throws XMLStreamException {
		writer.setDefaultNamespace(LlrpContext.DEFAULT_NAMESPACE);
		for (Map.Entry<String, String> nsdecl : context.getNamespaces().entrySet()) {
			if (!LlrpContext.DEFAULT_NAMESPACE.equals(nsdecl.getValue())) {
				writer.setPrefix(nsdecl.getKey(), nsdecl.getValue());
			}
		}
	}

	protected void writeNamespaces(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeDefaultNamespace(LlrpContext.DEFAULT_NAMESPACE);
		for (Map.Entry<String, String> nsdecl : context.getNamespaces().entrySet()) {
			if (!LlrpContext.DEFAULT_NAMESPACE.equals(nsdecl.getValue())) {
				writer.writeNamespace(nsdecl.getKey(), nsdecl.getValue());
			}
		}
	}

	protected XMLStreamWriter indentingWriter(XMLStreamWriter writer) {
		if (indent) {
			return new IndentingXMLStreamWriter(writer);
		}
		return writer;
	}

	public void encodeMessage(LlrpMessage message, XMLStreamWriter writer) throws Exception {
		writer = indentingWriter(writer);
		writer.writeStartDocument("1.0");
		setNamespaces(writer);

		Class<?> c = message.getClass();
		String ns = context.xmlNamespace(c);
		writer.writeStartElement(ns, localName(c));
		writeNamespaces(writer);

		BaseType messageType = context.messageType(message.getClass());
		if (messageType == null) {
			messageType = context.customMessageType(message.getClass());
		}

		// call the message specific encode function
		encodeProperties(message, messageType.properties(), writer);

		writer.writeEndElement();
		writer.writeEndDocument();
	}

	public void encodeParameter(Object parameter, XMLStreamWriter writer) throws Exception {
		writer = indentingWriter(writer);
		writer.writeStartDocument("1.0");
		setNamespaces(writer);
		encodeParameterInternal(parameter, writer, true);
	}

	int depth = 0;

	private void encodeProperties(Object o, Property[] properties, XMLStreamWriter writer) throws Exception {
		String namespace = context.xmlNamespace(o.getClass());
		for (Property property : properties) {
			Object fieldValue = property.field.get(o);
			boolean list = List.class.isAssignableFrom(property.field.getType());
			boolean empty = fieldValue == null || list && ((List<?>) fieldValue).isEmpty();
			if (empty && property.required) {
				throw new LlrpException("Missing required " + (property.isField ? "field" : "parameter") + "' "
						+ property.field.getName() + "' in "
						+ (LlrpMessage.class.isAssignableFrom(o.getClass()) ? "message" : "parameter") + " of type '"
						+ o.getClass().getSimpleName() + "'");
			}
			if (empty) {
				continue;
			}

			if (logger.isDebugEnabled()) {
				logger.debug(indent(depth, "encode " + property.field));
				depth++;
			}
			if (property.isField) {
				encodeField(property.field, fieldValue, namespace, writer);
			} else {
				encodeParameterInternal(fieldValue, writer, false);
			}
			if (logger.isDebugEnabled()) {
				depth--;
				logger.debug(indent(depth, "encoded " + property.field));
			}
		}
	}

	private String enumToString(Object value) {
		if (value instanceof List<?>) {
			StringBuilder sb = new StringBuilder();
			for (Iterator<?> it = ((List<?>) value).iterator(); it.hasNext();) {
				LlrpEnum element = (LlrpEnum) it.next();
				sb.append(element.name());
				if (it.hasNext()) {
					sb.append(" ");
				}
			}
			return sb.toString();
		} else {
			return ((LlrpEnum) value).name();
		}
	}

	private String encodeField(Field field, Object value, String namespace, XMLStreamWriter writer) throws Exception {
		LlrpField annotation = field.getAnnotation(LlrpField.class);
		if (value instanceof LlrpEnum
				|| (value instanceof List<?> && LlrpEnum.class.isAssignableFrom(propertyType(field)))) {
			value = enumToString(value);
		}
		String fieldName = firstUpper(field.getName());
		String fieldValue = XmlTypes.toString(value, annotation.format());
		if (writer != null) {
			writer.writeStartElement(namespace, fieldName);
			writer.writeCharacters(fieldValue);
			writer.writeEndElement();
		}
		return fieldValue;
	}

	protected void encodeParameterInternal(Object parameter, XMLStreamWriter writer, boolean writeNamespaces)
			throws Exception {
		boolean isList = parameter instanceof List;
		List<?> elements = isList ? (List<?>) parameter : Arrays.asList(parameter);
		for (Object element : elements) {
			writer.writeStartElement(context.xmlNamespace(element.getClass()), localName(element.getClass()));
			if (writeNamespaces) {
				writeNamespaces(writer);
			}
			Annotation parameterType = parameterType(element.getClass());
			// TV Parameters have type number from 0 - 127, TLV from 128 -
			// 2047
			int typeNum = typeNum(parameterType);
			if (typeNum < 128) {
				encodeTVParameter(parameterType, element, writer);
			} else {
				encodeTLVParameter(parameterType, element, writer);
			}
			writer.writeEndElement();
		}
	}

	private void encodeTVParameter(Annotation parameterType, Object parameter, XMLStreamWriter writer)
			throws Exception {
		Property[] properties = properties(parameterType, context);
		// boolean inlineField = properties.length == 1;

		String namespace = context.xmlNamespace(parameter.getClass());
		for (Property property : properties) {
			Object fieldValue = property.field.get(parameter);

			// String encoded = encodeField(property.field, fieldValue,
			// namespace, inlineField ? null : writer);
			// writer.writeCharacters(encoded);

			encodeField(property.field, fieldValue, namespace, writer);
		}
	}

	private void encodeTLVParameter(Annotation parameterType, Object parameter, XMLStreamWriter writer)
			throws Exception {
		encodeProperties(parameter, properties(parameterType, context), writer);
	}
}
