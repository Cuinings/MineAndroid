package com.cn.library.remote.andlinker.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Specify the interface as remote service interface.
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface RemoteInterface {
    
}
