package com.cn.core.remote.andlinker.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Indicate a parameter is a remote callback type.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Callback {

}
