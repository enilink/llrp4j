package net.enilink.llrp4j;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import org.llrp.ltk.schema.core.FieldType;

import net.enilink.llrp4j.annotations.LlrpCustomParameterType;
import net.enilink.llrp4j.annotations.LlrpParameterType;
import net.enilink.llrp4j.impl.CustomKey;
import net.enilink.llrp4j.impl.Property;
import net.enilink.llrp4j.types.LlrpEnum;
import net.enilink.llrp4j.types.Types;

public class EncodingUtil {
	static Annotation parameterType(Class<?> clazz) {
		if (clazz.isAnnotationPresent(LlrpParameterType.class)) {
			return clazz.getAnnotation(LlrpParameterType.class);
		} else {
			return clazz.getAnnotation(LlrpCustomParameterType.class);
		}
	}

	static boolean isCustom(Annotation parameterType) {
		return parameterType instanceof LlrpCustomParameterType;
	}

	static int typeNum(Annotation parameterType) {
		if (isCustom(parameterType)) {
			// custom parameter
			return 1023;
		} else {
			return ((LlrpParameterType) parameterType).typeNum();
		}
	}

	static Property[] properties(Annotation parameterType, LlrpContext context) {
		if (isCustom(parameterType)) {
			LlrpCustomParameterType customType = (LlrpCustomParameterType) parameterType;
			return context.customParameterTypes.get(new CustomKey(customType.vendor(), customType.subType()))
					.properties();
		} else {
			LlrpParameterType type = (LlrpParameterType) parameterType;
			return context.parameterTypes.get(type.typeNum()).properties();
		}
	}

	static String indent(int depth, String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			sb.append("  ");
		}
		return sb.append(s).toString();
	}

	static String firstUpper(String str) {
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	static String firstLower(String str) {
		return Character.toLowerCase(str.charAt(0)) + str.substring(1);
	}

	private static Object toValue(FieldType type, Object value) throws Exception {
		Object result = ((LlrpEnum) value).value();
		if (type == FieldType.U_1 && result instanceof Number) {
			result = ((Number) result).intValue() != 0;
		}
		return result;
	}

	static Object encodeEnum(FieldType type, Object value) throws Exception {
		if (value instanceof List<?>) {
			int length = ((List<?>) value).size();
			Class<?> typeClass = Types.javaType(type);
			if (int[].class.equals(typeClass)) {
				int[] values = new int[length];
				int i = 0;
				for (Object v : (List<?>) value) {
					values[i++] = ((Number) toValue(type, v)).intValue();
				}
				return values;
			} else if (long[].class.equals(typeClass)) {
				long[] values = new long[length];
				int i = 0;
				for (Object v : (List<?>) value) {
					values[i++] = ((Number) toValue(type, v)).intValue();
				}
				return values;
			}
			throw new LlrpException("Unsupported field type for enumerations: " + type);
		} else {
			return toValue(type, value);
		}
	}

	private static Object fromValue(Class<?> enumClass, Number value) throws Exception {
		return enumClass.getDeclaredMethod("fromValue", int.class).invoke(null, value.intValue());
	}

	static Object decodeEnum(Class<?> enumClass, Object value) throws Exception {
		if (value instanceof Boolean) {
			value = Boolean.TRUE.equals(value) ? 1 : 0;
		}
		if (value.getClass().isArray()) {
			List<Object> values = new ArrayList<>();
			if (value instanceof int[]) {
				for (int i = 0; i < ((int[]) value).length; i++) {
					values.add(fromValue(enumClass, ((int[]) value)[i]));
				}
			} else if (value instanceof long[]) {
				for (int i = 0; i < ((long[]) value).length; i++) {
					values.add(fromValue(enumClass, ((long[]) value)[i]));
				}
			}
			return values;
		} else {
			return fromValue(enumClass, (Number) value);
		}
	}

	static Class<?> propertyType(Field field) {
		Class<?> propertyType = field.getType();
		if (List.class.isAssignableFrom(propertyType)) {
			if (field.getGenericType() instanceof ParameterizedType) {
				ParameterizedType pType = (ParameterizedType) field.getGenericType();
				propertyType = (Class<?>) pType.getActualTypeArguments()[0];
			}
		}
		return propertyType;
	}
}
