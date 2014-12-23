package org.scalatra.spring;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * @author Stephen Samuel
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Path {
    String value();
}
