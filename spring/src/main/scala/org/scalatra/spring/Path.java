package org.scalatra.spring;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * @author Stephen Samuel
 * @deprecated Spring integration has been no longer maintained since 2.6.0. It will be dropped in 2.7.0.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface Path {
    String value();
}
