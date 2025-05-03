package com.cn.library.remote.andlinker.annotation;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Directional tag indicating which way the data goes, same as "in" tag in AIDL.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface In {
    
}
