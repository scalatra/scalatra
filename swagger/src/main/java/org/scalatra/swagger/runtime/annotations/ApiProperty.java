package org.scalatra.swagger.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** ApiProperty can be put on a Method to allow swagger to understand the json fields datatype and more. */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiProperty {
	/** Provide a human readable synopsis of this property */
    String value() default "";

	/**
	 * If the values that can be set are restricted, they can be set here. In the form of a comma separated list
	 * <code>registered, active, closed</code>.
	 *
	 * @return the allowable values
	 */
    String allowableValues() default "";
    String access() default "";
	/** Provide any extra information */
    String notes() default "";

	/**
	 * The dataType. See the documentation for the supported datatypes. If the data type is a custom object, set
	 * it's name, or nothing. In case of an enum use 'string' and allowableValues for the enum constants.
     */
    String dataType() default "";

	/**
	 * Whether or not the property is required, defaults to false.
	 *
	 * @return true if required, false otherwise
	 */
	boolean required() default true;
}
