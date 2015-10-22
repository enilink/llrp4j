package net.enilink.llrp4j.xml;

import java.util.Arrays;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * This is an abstract implementation for a StAX-based XML parser.
 */
public abstract class AbstractXMLParser {
	private Stack<QName> context = new Stack<QName>();
	protected XMLStreamReader reader;
	private boolean useCurrentAsNext;

	public AbstractXMLParser(XMLStreamReader reader) {
		this.reader = reader;
	}

	protected QName[] combine(QName[] names1, QName... names2) {
		QName[] combined = Arrays.copyOf(names1, names1.length + names2.length);
		System.arraycopy(names2, 0, combined, names1.length, names2.length);
		return combined;
	}

	protected void start(QName name) {
		context.push(name);
	}

	protected void useCurrentAsNext() {
		this.useCurrentAsNext = true;
	}

	protected void end() throws XMLStreamException, ParseException {
		if (reader.isEndElement()) {
			popContext();

			// consume the end element
			reader.next();
			useCurrentAsNext = true;
			return;
		}
		loop: while (reader.hasNext()) {
			switch (reader.next()) {
			case XMLStreamConstants.START_ELEMENT:
				break loop;
			case XMLStreamConstants.END_ELEMENT:
				popContext();
				// consume the end element
				reader.next();
				useCurrentAsNext = true;
				return;
			}
		}
		throw newError("Expected closing tag: " + context.peek());
	}

	protected void expected(QName... names) throws ParseException {
		throw newError("Expected one of: " + toString(",\n", (Object[]) names));
	}

	protected String getAttribute(QName name) {
		String namespaceURI = name.getNamespaceURI();
		return reader.getAttributeValue(XMLConstants.NULL_NS_URI.equals(namespaceURI) ? null : namespaceURI,
				name.getLocalPart());
	}

	protected Boolean getBoolean(QName name) {
		String attribute = getAttribute(name);
		return attribute != null ? Boolean.valueOf(attribute) : null;
	}

	protected Integer getInteger(QName name) {
		String attribute = getAttribute(name);
		return attribute != null ? Integer.valueOf(attribute) : null;
	}

	protected String getString(QName name) {
		return getAttribute(name);
	}

	protected ParseException newError(String msg) throws ParseException {
		context.clear();
		return new ParseException(msg, reader.getLocation(), reader.hasName() ? reader.getName() : null);
	}

	protected QName next(QName... names) throws XMLStreamException, ParseException {
		while (useCurrentAsNext || reader.hasNext()) {
			if (!useCurrentAsNext) {
				reader.next();
			} else {
				useCurrentAsNext = false;
			}
			switch (reader.getEventType()) {
			case XMLStreamReader.START_ELEMENT:
				QName name = reader.getName();
				if (names.length == 0) {
					start(name);
					return name;
				} else {
					for (QName expectedName : names) {
						if (expectedName.equals(name)) {
							start(name);
							return expectedName;
						}
					}
				}
			case XMLStreamReader.END_ELEMENT:
				useCurrentAsNext = true;
				return null;
			}
		}
		return null;
	}

	protected QName nextOrFail(QName... names) throws XMLStreamException, ParseException {
		QName next = next(names);
		if (next == null) {
			expected(names);
		}
		return next;
	}

	protected String parseStringValue() throws XMLStreamException, ParseException {
		StringBuilder sb = new StringBuilder();
		if (reader.isCharacters()) {
			sb.append(reader.getText());
		}
		outer: while (reader.hasNext()) {
			switch (reader.next()) {
			case XMLStreamReader.START_ELEMENT:
				useCurrentAsNext = true;
				break outer;
			case XMLStreamReader.END_ELEMENT:
				break outer;
			case XMLStreamReader.CHARACTERS:
				sb.append(reader.getText());
			}
		}
		return sb.toString().trim();
	}

	private void popContext() throws ParseException {
		if (context.isEmpty()) {
			throw newError("Imbalance between opening and closing tags. Unexpected closing tag: " + reader.getName());
		}
		if (!context.peek().equals(reader.getName())) {
			throw newError("Unexpected closing tag: " + reader.getName() + ". Expected: " + context.peek());
		}
		context.pop();
	}

	protected void required(QName attributeName, Object value) throws ParseException {
		if (value == null || (value instanceof String && ((String) value).length() == 0)) {
			throw newError("Attribute " + attributeName + " is missing or has invalid value");
		}
	}

	protected String toString(String separator, Object... values) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			sb.append(values[i]);

			if (i < values.length - 1) {
				sb.append(separator);
			}
		}
		return sb.toString();
	}

	protected void unexpected(QName name) throws ParseException {
		throw newError("Unexpected element " + name);
	}

}
