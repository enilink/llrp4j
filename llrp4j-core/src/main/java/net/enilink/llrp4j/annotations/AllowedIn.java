package net.enilink.llrp4j.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(value=RetentionPolicy.RUNTIME)
public @interface AllowedIn {
	Class<?>targetType();

	boolean multiple() default false;

	boolean required() default false;

	String name() default "";
}
