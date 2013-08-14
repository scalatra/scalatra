package org.scalatra.swagger.runtime.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A bean class used in the REST-api.
 * Suppose you have an interface
 * <code>@PUT @ApiOperation(...) void foo(FooBean fooBean)</code>, there is
 * no direct way to see what fields <code>FooBean</code> would have. This
 * annotation is meant to give a description of <code>FooBean</code> and
 * then have the fields of it be annotated with
 * <code>@ApiModelProperty</code>.
 *
 * @author Heiko W. Rupp
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiModel {
  /** Provide a synopsis of this class */
  String value() default "";
  /** Provide a longer description of the class */
  String description() default "";
  /** Provide a superclass for the model to allow describing inheritence */
  Class<?> parent() default Void.class;
  /** for models with a base class, a discriminator can be provided for polymorphic use cases */
  String discriminator() default "";
}
