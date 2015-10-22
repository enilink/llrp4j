package net.enilink.llrp4j.xml;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;

public class ParseException extends Exception {
	private static final long serialVersionUID = 2873348872583794388L;

	Location location;
	QName name;

	public ParseException(String msg) {
		this(msg, null, null);
	}

	public ParseException(String msg, Location location, QName name) {
		super(msg);
		this.location = location;
		this.name = name;
	}

	public Location getLocation() {
		return location;
	}

	public QName getName() {
		return name;
	}

	public String toString() {
		if (location == null) {
			return getMessage();
		}

		StringBuffer sb = new StringBuffer(getMessage()).append(" at ")
				.append(location.getLineNumber()).append(":")
				.append(location.getColumnNumber()).append(" <");
		if (name != null) {
			if (name.getPrefix() != null && name.getPrefix().length() > 0) {
				sb.append(name.getPrefix()).append(":");
			}
			sb.append(name.getLocalPart()).append(">");
		}
		return sb.toString();
	}
}
