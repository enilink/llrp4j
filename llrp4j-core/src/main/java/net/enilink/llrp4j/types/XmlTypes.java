package net.enilink.llrp4j.types;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.llrp.ltk.schema.core.FieldFormat;
import org.llrp.ltk.schema.core.FieldType;

import net.enilink.llrp4j.LlrpException;

public class XmlTypes {
	protected static volatile DatatypeFactory datatypeFactory;

	protected static DatatypeFactory datatypeFactory() {
		if (datatypeFactory == null) {
			try {
				datatypeFactory = DatatypeFactory.newInstance();
			} catch (DatatypeConfigurationException e) {
				throw new LlrpException(e);
			}
		}
		return datatypeFactory;
	}

	public static String toString(Object value, FieldFormat format) {
		if (value instanceof String) {
			return (String) value;
		} else if (value instanceof Boolean) {
			return Boolean.TRUE.equals(value) ? "1" : "0";
		} else if (value instanceof BitList) {
			return toString((BitList) value, format);
		} else if (value instanceof Number) {
			Number n = (Number) value;
			switch (format) {
			case DATETIME:
				BigInteger bigint;
				if (n instanceof BigInteger) {
					bigint = (BigInteger) n;
				} else {
					bigint = BigInteger.valueOf(n.longValue());
				}
				GregorianCalendar cal = new GregorianCalendar();

				// initialize calendar after removing the last
				// three digits that represent microseconds
				final long milliseconds = bigint.divide(BigInteger.valueOf(1000)).longValue();
				cal.setTimeInMillis(milliseconds);

				StringBuilder sb = new StringBuilder();
				DatatypeFactory df = datatypeFactory();
				XMLGregorianCalendar xmlcal = df.newXMLGregorianCalendar(cal);
				sb.append(xmlcal.toXMLFormat());

				int indexOfT = sb.indexOf("T");
				int l = value.toString().length();
				String microseconds = value.toString().substring(l - 3, l);
				sb.insert(indexOfT + 13, microseconds);

				return sb.toString();
			case HEX:
				if (n instanceof BigInteger) {
					return ((BigInteger) n).toString(16);
				} else {
					return Long.toString(n.longValue(), 16);
				}
			default:
				return n.toString();
			}
		} else if (value.getClass().isArray()) {
			if (value instanceof int[]) {
				return toString((int[]) value, format);
			} else if (value instanceof long[]) {
				return toString((long[]) value, format);
			} else if (value instanceof boolean[]) {
				return toString((boolean[]) value, format);
			} else if (value instanceof Object[]) {
				return toString((Object[]) value, format);
			} else if (value instanceof byte[]) {
				return bytesToHex((byte[]) value);
			}
		}
		throw new IllegalArgumentException("Unknown type: " + value.getClass());
	}

	private static String toString(BitList bits, FieldFormat format) {
		StringBuilder sb = new StringBuilder();
		int length = bits.length();
		for (int i = 0; i < length; i++) {
			sb.append(bits.get(i) ? "1" : "0");
		}
		return sb.toString();
	}

	private static String toString(int[] elements, FieldFormat format) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < elements.length; i++) {
			sb.append(toString(elements[i], format)).append(' ');
		}
		if (sb.length() > 0) {
			sb.replace(sb.length() - 1, sb.length(), "");
		}
		return sb.toString();
	}

	private static String toString(long[] elements, FieldFormat format) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < elements.length; i++) {
			sb.append(toString(elements[i], format)).append(' ');
		}
		if (sb.length() > 0) {
			sb.replace(sb.length() - 1, sb.length(), "");
		}
		return sb.toString();
	}

	private static String toString(Object[] elements, FieldFormat format) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < elements.length; i++) {
			sb.append(toString(elements[i], format)).append(' ');
		}
		if (sb.length() > 0) {
			sb.replace(sb.length() - 1, sb.length(), "");
		}
		return sb.toString();
	}

	public static Object fromString(FieldType fieldType, FieldFormat format, String s) {
		if (format == FieldFormat.DATETIME && fieldType == FieldType.U_64) {
			DatatypeFactory df = datatypeFactory();
			XMLGregorianCalendar cal = df.newXMLGregorianCalendar(s);
			// scale value to microseconds the last three digits
			// ("microseconds") are "000" at this stage
			BigInteger value = BigInteger.valueOf(cal.toGregorianCalendar().getTimeInMillis() * 1000);

			// compute microseconds value by subtracting milliseconds from
			// return value of XMLGregorianCalendar.getFractionalSecond()
			BigDecimal millisec = cal.getFractionalSecond().setScale(3, BigDecimal.ROUND_DOWN);
			BigDecimal microsec = (cal.getFractionalSecond().setScale(6, BigDecimal.ROUND_DOWN)).subtract(millisec);

			// add microseconds
			value = value.add(microsec.movePointRight(6).toBigInteger());
			return value;
		}
		int radix = format == FieldFormat.HEX ? 16 : 10;
		switch (fieldType) {
		case U_1:
			return "1".equals(s) ? Boolean.TRUE : Boolean.valueOf(s);
		case U_2:
			return Integer.valueOf(s, radix);
		case U_1_V: {
			int length = s.trim().length();
			BitList result = new BitList();
			for (int i = 0; i < length; i++) {
				result.set(i, s.charAt(i) != '0');
			}
			return result;
		}
		case U_8:
		case S_8:
			return Integer.valueOf(s, radix);
		case U_8_V: {
			String[] strings = split(s);
			return fromString(new int[strings.length], FieldType.U_8, format, strings);
		}
		case S_8_V: {
			String[] strings = split(s);
			return fromString(new int[strings.length], FieldType.S_8, format, strings);
		}
		case UTF_8_V:
			return s;
		case U_16:
		case S_16:
			return Integer.valueOf(s, radix);
		case U_16_V: {
			String[] strings = split(s);
			return fromString(new int[strings.length], FieldType.U_16, format, strings);
		}
		case S_16_V: {
			String[] strings = split(s);
			return fromString(new int[strings.length], FieldType.S_16, format, strings);
		}
		case U_32:
			return Long.valueOf(s, radix);
		case S_32:
			return Integer.valueOf(s, radix);
		case U_32_V: {
			String[] strings = split(s);
			return fromString(new long[strings.length], FieldType.U_32, format, strings);
		}
		case S_32_V: {
			String[] strings = split(s);
			return fromString(new long[strings.length], FieldType.S_32, format, strings);
		}
		case U_64:
		case S_64:
			return new BigInteger(s, radix);
		case U_64_V: {
			String[] strings = split(s);
			return fromString(new BigInteger[strings.length], FieldType.U_64, format, strings);
		}
		case S_64_V: {
			String[] strings = split(s);
			return fromString(new BigInteger[strings.length], FieldType.S_64, format, strings);
		}
		case U_96:
			return new BigInteger(s, radix);
		case BYTES_TO_END: {
			return hexStringToByteArray(s);
		}
		default:
		}
		throw new IllegalArgumentException("Unknown type " + fieldType);

	}

	private static String[] split(String s) {
		if (s.trim().length() == 0) {
			return new String[0];
		} else {
			return s.split("\\s+");
		}
	}

	private static int[] fromString(int[] elements, FieldType fieldType, FieldFormat format, String[] strings) {
		for (int i = 0; i < strings.length; i++) {
			elements[i] = (Integer) fromString(fieldType, format, strings[i]);
		}
		return elements;
	}

	private static long[] fromString(long[] elements, FieldType fieldType, FieldFormat format, String[] strings) {
		for (int i = 0; i < strings.length; i++) {
			elements[i] = (Long) fromString(fieldType, format, strings[i]);
		}
		return elements;
	}

	private static BigInteger[] fromString(BigInteger[] elements, FieldType fieldType, FieldFormat format,
			String[] strings) {
		for (int i = 0; i < strings.length; i++) {
			elements[i] = (BigInteger) fromString(fieldType, format, strings[i]);
		}
		return elements;
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] hexStringToByteArray(String s) {
		if ((s.length() % 2) != 0) {
			s = "0" + s;
		}
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
}
