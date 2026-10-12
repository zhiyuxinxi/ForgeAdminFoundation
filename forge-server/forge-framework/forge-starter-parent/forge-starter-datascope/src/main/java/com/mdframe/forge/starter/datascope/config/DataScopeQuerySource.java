package com.mdframe.forge.starter.datascope.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Retains a same-mapper policy source when a static query receives its own XML statement. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DataScopeQuerySource {
    String value();
}
