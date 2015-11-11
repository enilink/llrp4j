package net.enilink.llrp4j.types;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.llrp.ltk.schema.core.FieldType;

import net.enilink.llrp4j.bitbuffer.BitBuffer;

public class Types {
	public static void encode(Object value, FieldType fieldType, BitBuffer buffer) {
		switch (fieldType) {
		case U_1:
			buffer.putBit((Boolean) value);
			return;
		case U_2:
			buffer.putInt(((Number) value).intValue(), 2);
			return;
		case U_1_V:
			encodeBits((BitList) value, FieldType.U_1, buffer);
			return;
		case U_8:
			buffer.putInt(((Number) value).intValue(), 8);
			return;
		case S_8:
			buffer.putInt(((Number) value).intValue(), 8);
			return;
		case U_8_V:
			encodeArray((int[]) value, FieldType.U_8, buffer);
			return;
		case S_8_V:
			encodeArray((int[]) value, FieldType.S_8, buffer);
			return;
		case UTF_8_V:
			byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
			buffer.putInt(bytes.length, 16);
			buffer.put(bytes);
			return;
		case U_16:
			buffer.putInt(((Number) value).intValue(), 16);
			return;
		case S_16:
			buffer.putInt(((Number) value).intValue(), 16);
			return;
		case U_16_V:
			encodeArray((int[]) value, FieldType.U_16, buffer);
			return;
		case S_16_V:
			encodeArray((int[]) value, FieldType.S_16, buffer);
			return;
		case U_32:
			buffer.putLong(((Number) value).longValue(), 32);
			return;
		case S_32:
			buffer.putLong(((Number) value).longValue(), 32);
			return;
		case U_32_V:
			encodeArray((long[]) value, FieldType.S_32, buffer);
			return;
		case S_32_V:
			encodeArray((int[]) value, FieldType.S_32, buffer);
			return;
		case U_64: {
			BigInteger l = (BigInteger) value;
			BigInteger mask = BigInteger.valueOf((1L << 32) - 1);
			buffer.putLong(l.shiftRight(32).and(mask).longValue(), 32);
			buffer.putLong(l.and(mask).longValue(), 32);
			return;
		}
		case S_64: {
			BigInteger l = (BigInteger) value;
			BigInteger mask = BigInteger.valueOf((1L << 32) - 1);
			buffer.putLong(l.shiftRight(32).and(mask).longValue(), 32);
			buffer.putLong(l.and(mask).longValue(), 32);
			return;
		}
		case U_64_V:
			encodeArray((BigInteger[]) value, FieldType.U_64, buffer);
			return;
		case S_64_V:
			encodeArray((BigInteger[]) value, FieldType.S_64, buffer);
			return;
		case U_96: {
			BigInteger l = (BigInteger) value;
			BigInteger mask = BigInteger.valueOf((1L << 32) - 1);
			buffer.putLong(l.shiftRight(64).and(mask).longValue(), 32);
			buffer.putLong(l.shiftRight(32).and(mask).longValue(), 32);
			buffer.putLong(l.and(mask).longValue(), 32);
			return;
		}
		case BYTES_TO_END:
			buffer.put((byte[]) value);
			return;
		}
		throw new IllegalArgumentException("Unknown type " + fieldType);
	}

	private static void encodeBits(BitList bits, FieldType fieldType, BitBuffer buffer) {
		int length = bits.length();
		buffer.putInt(length, 16);
		for (int i = 0; i < length; i++) {
			buffer.putBoolean(bits.get(i));
		}
		if ((length % 8) != 0) {
			int padding = 8 - (length % 8);
			while (padding-- > 0) {
				buffer.putBoolean(false);
			}
		}
	}

	private static void encodeArray(int[] elements, FieldType fieldType, BitBuffer buffer) {
		buffer.putInt(elements.length, 16);
		for (int i = 0; i < elements.length; i++) {
			encode(elements[i], fieldType, buffer);
		}
	}

	private static void encodeArray(long[] elements, FieldType fieldType, BitBuffer buffer) {
		buffer.putInt(elements.length, 16);
		for (int i = 0; i < elements.length; i++) {
			encode(elements[i], fieldType, buffer);
		}
	}

	private static void encodeArray(Object[] elements, FieldType fieldType, BitBuffer buffer) {
		buffer.putInt(elements.length, 16);
		for (int i = 0; i < elements.length; i++) {
			encode(elements[i], fieldType, buffer);
		}
	}

	public static Object decode(FieldType fieldType, BitBuffer buffer) {
		switch (fieldType) {
		case U_1:
			return buffer.getBoolean();
		case U_2:
			return buffer.getIntUnsigned(2);
		case U_1_V: {
			int length = buffer.getIntUnsigned(16);
			return decodeBits(length, FieldType.U_1, buffer);
		}
		case U_8:
			return buffer.getIntUnsigned(8);
		case S_8:
			return buffer.getInt(8);
		case U_8_V:
			return decodeArray(new int[buffer.getIntUnsigned(16)], FieldType.U_8, buffer);
		case S_8_V:
			return decodeArray(new int[buffer.getIntUnsigned(16)], FieldType.S_8, buffer);
		case UTF_8_V:
			int length = buffer.getIntUnsigned(16);
			return buffer.getString(length, StandardCharsets.UTF_8);
		case U_16:
			return buffer.getIntUnsigned(16);
		case S_16:
			return buffer.getInt(16);
		case U_16_V:
			return decodeArray(new int[buffer.getIntUnsigned(16)], FieldType.U_16, buffer);
		case S_16_V:
			return decodeArray(new int[buffer.getIntUnsigned(16)], FieldType.S_16, buffer);
		case U_32:
			return buffer.getLongUnsigned(32);
		case S_32:
			return buffer.getInt(32);
		case U_32_V:
			return decodeArray(new long[buffer.getIntUnsigned(16)], FieldType.U_32, buffer);
		case S_32_V:
			return decodeArray(new int[buffer.getIntUnsigned(16)], FieldType.S_32, buffer);
		case U_64: {
			return BigInteger.valueOf(buffer.getLongUnsigned(32)).shiftLeft(32)
					.add(BigInteger.valueOf(buffer.getLongUnsigned(32)));
		}
		case S_64: {
			// TODO check if this is correct
			return BigInteger.valueOf(buffer.getLongUnsigned(32)).shiftLeft(32)
					.add(BigInteger.valueOf(buffer.getLongUnsigned(32)));
		}
		case U_64_V:
			return decodeArray(new BigInteger[buffer.getIntUnsigned(16)], FieldType.U_64, buffer);
		case S_64_V:
			return decodeArray(new BigInteger[buffer.getIntUnsigned(16)], FieldType.S_64, buffer);
		case U_96: {
			BigInteger first = BigInteger.valueOf(buffer.getLongUnsigned(32));
			BigInteger second = BigInteger.valueOf(buffer.getLongUnsigned(32));
			BigInteger third = BigInteger.valueOf(buffer.getLongUnsigned(32));
			return first.shiftLeft(32).add(second).shiftLeft(32).add(third);
		}
		case BYTES_TO_END: {
			int byteLength = (buffer.size() - buffer.position()) / 8;
			byte[] bytes = new byte[byteLength];
			buffer.get(bytes);
			return bytes;
		}
		default:
		}
		throw new IllegalArgumentException("Unknown type " + fieldType);
	}

	private static BitList decodeBits(int length, FieldType fieldType, BitBuffer buffer) {
		BitList result = new BitList();
		for (int i = 0; i < length; i++) {
			result.set(i, buffer.getBoolean());
		}
		if ((length % 8) != 0) {
			int padding = 8 - (length % 8);
			while (padding-- > 0) {
				buffer.getBoolean();
			}
		}
		return result;
	}

	private static int[] decodeArray(int[] elements, FieldType fieldType, BitBuffer buffer) {
		for (int i = 0; i < elements.length; i++) {
			elements[i] = (Integer) decode(fieldType, buffer);
		}
		return elements;
	}

	private static long[] decodeArray(long[] elements, FieldType fieldType, BitBuffer buffer) {
		for (int i = 0; i < elements.length; i++) {
			elements[i] = (Long) decode(fieldType, buffer);
		}
		return elements;
	}

	private static Object[] decodeArray(Object[] elements, FieldType fieldType, BitBuffer buffer) {
		for (int i = 0; i < elements.length; i++) {
			elements[i] = decode(fieldType, buffer);
		}
		return elements;
	}

	public static Class<?> javaType(FieldType fieldType) {
		switch (fieldType) {
		case U_1:
			return boolean.class;
		case U_2:
			return int.class;
		case U_1_V:
			return BitList.class;
		case U_8:
			return int.class;
		case S_8:
			return int.class;
		case U_8_V:
			return int[].class;
		case S_8_V:
			return int[].class;
		case UTF_8_V:
			return String.class;
		case U_16:
			return int.class;
		case S_16:
			return int.class;
		case U_16_V:
			return int[].class;
		case S_16_V:
			return int[].class;
		case U_32:
			return long.class;
		case S_32:
			return int.class;
		case U_32_V:
			return long[].class;
		case S_32_V:
			return int[].class;
		case U_64:
			return BigInteger.class;
		case S_64:
			return long.class;
		case U_64_V:
			return BigInteger[].class;
		case S_64_V:
			return BigInteger[].class;
		case U_96:
			return BigInteger.class;
		case BYTES_TO_END:
			return byte[].class;
		default:
		}
		throw new IllegalArgumentException("Unknown type " + fieldType);
	}

	@SuppressWarnings("unchecked")
	public static <T> Class<T> wrap(Class<T> c) {
		return c.isPrimitive() ? (Class<T>) PRIMITIVES_TO_WRAPPERS.get(c) : c;
	}

	private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS;

	static {
		PRIMITIVES_TO_WRAPPERS = new HashMap<>();
		PRIMITIVES_TO_WRAPPERS.put(boolean.class, Boolean.class);
		PRIMITIVES_TO_WRAPPERS.put(byte.class, Byte.class);
		PRIMITIVES_TO_WRAPPERS.put(char.class, Character.class);
		PRIMITIVES_TO_WRAPPERS.put(double.class, Double.class);
		PRIMITIVES_TO_WRAPPERS.put(float.class, Float.class);
		PRIMITIVES_TO_WRAPPERS.put(int.class, Integer.class);
		PRIMITIVES_TO_WRAPPERS.put(long.class, Long.class);
		PRIMITIVES_TO_WRAPPERS.put(short.class, Short.class);
		PRIMITIVES_TO_WRAPPERS.put(void.class, Void.class);
	}
}
