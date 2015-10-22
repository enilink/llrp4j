package net.enilink.llrp4j.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.llrp.ltk.schema.core.FieldFormat;
import org.llrp.ltk.schema.core.FieldType;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LlrpField {
	FieldFormat format() default FieldFormat.DEC;

	FieldType type();

	int reservedBefore() default 0;

	int reservedAfter() default 0;
}
