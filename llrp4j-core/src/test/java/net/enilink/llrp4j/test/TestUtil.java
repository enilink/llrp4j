package net.enilink.llrp4j.test;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.llrp.ltk.schema.core.FieldType;

import net.enilink.llrp4j.annotations.LlrpCustomMessageType;
import net.enilink.llrp4j.annotations.LlrpField;
import net.enilink.llrp4j.annotations.LlrpMessageType;
import net.enilink.llrp4j.annotations.LlrpParam;
import net.enilink.llrp4j.types.BitList;

public class TestUtil {
	public static <T> T mockObject(T o, Set<Class<?>> classesPool, Random rnd) throws Exception {
		if (o instanceof List<?>) {
			for (Object element : (List<?>) o) {
				mockObject(element, classesPool, rnd);
			}
		} else {
			for (Field f : o.getClass().getDeclaredFields()) {
				Object value = null;

				LlrpField fieldAnn = f.getAnnotation(LlrpField.class);
				if (fieldAnn != null) {
					if (f.getType().isPrimitive()) {
						if (fieldAnn.type() == FieldType.U_2) {
							value = rnd.nextInt(4);
						} else if (int.class.equals(f.getType())) {
							value = rnd.nextInt(128);
						} else if (long.class.equals(f.getType())) {
							value = (long) rnd.nextInt(1000);
						} else if (boolean.class.equals(f.getType())) {
							value = rnd.nextBoolean();
						}
						// TODO implement more types
					} else {
						value = createValue(f, classesPool, rnd);
						if (value instanceof BigInteger && fieldAnn.type().name().startsWith("U")) {
							value = ((BigInteger) value).abs();
						}
					}
				} else {
					LlrpParam paramAnn = f.getAnnotation(LlrpParam.class);
					if (paramAnn != null) {
						if (paramAnn.required() || rnd.nextBoolean()) {
							value = createValue(f, classesPool, rnd);
						}
					}
				}
				if (value != null) {
					f.setAccessible(true);
					f.set(o, value);
				}
			}
		}
		return o;
	}

	public static Object createValue(Field f, Set<Class<?>> classesPool, Random rnd) throws Exception {
		List<Object> list = null;
		Class<?> c = f.getType();
		if (List.class.isAssignableFrom(c)) {
			list = new ArrayList<>();
			if (f.getGenericType() instanceof ParameterizedType) {
				ParameterizedType pType = (ParameterizedType) f.getGenericType();
				c = (Class<?>) pType.getActualTypeArguments()[0];
			}
		}

		Object value = null;
		if (c.isEnum()) {
			Object[] constants = c.getEnumConstants();
			value = constants[rnd.nextInt(constants.length)];
		} else if (c.isInterface()) {
			for (Class<?> candidate : classesPool) {
				if (c.isAssignableFrom(candidate)) {
					value = candidate.newInstance();
					break;
				}
			}
		} else if (BitList.class.equals(c)) {
			byte[] bytes = new byte[rnd.nextInt(5)];
			rnd.nextBytes(bytes);
			value = new BitList(bytes);
		} else if (BigInteger.class.equals(c)) {
			value = BigInteger.valueOf(rnd.nextLong());
		} else if (c.isArray()) {
			if (byte[].class.equals(c)) {
				byte[] bytes = new byte[rnd.nextInt(5) + 1];
				rnd.nextBytes(bytes);
				value = bytes;
			} else {
				value = Array.newInstance(c.getComponentType(), 0);
			}
		} else {
			value = c.newInstance();
		}
		if (value == null) {
			throw new IllegalArgumentException("Cannot instantiate class: " + c);
		}
		if (!value.getClass().isArray()
				&& !(value instanceof Number || value instanceof String || value instanceof Boolean)) {
			mockObject(value, classesPool, rnd);
		}
		if (list != null) {
			list.add(value);
			return list;
		}
		return value;
	}

	public static Class<?> responseType(Class<?> msgClass) {
		if (msgClass.isAnnotationPresent(LlrpMessageType.class)) {
			return msgClass.getAnnotation(LlrpMessageType.class).responseType();
		} else if (msgClass.isAnnotationPresent(LlrpCustomMessageType.class)) {
			return msgClass.getAnnotation(LlrpCustomMessageType.class).responseType();
		}
		return void.class;
	}
}
