package net.enilink.llrp4j.impl;

import java.lang.reflect.Field;

import net.enilink.llrp4j.annotations.LlrpField;
import net.enilink.llrp4j.annotations.LlrpParam;

public class Property {
	public final Field field;
	public final boolean required;
	public final boolean isField;

	public Property(Field field) {
		this.field = field;
		field.setAccessible(true);
		this.isField = field.isAnnotationPresent(LlrpField.class);
		boolean required = isField;
		if (!required) {
			LlrpParam param = field.getAnnotation(LlrpParam.class);
			required = param.required();
		}
		this.required = required;
	}
}