package com.yzddmr6.prismspace.util.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Marks an integer as an Android multi-user user ID, not a Linux UID.
 */
@Retention(SOURCE)
@Target({ METHOD, PARAMETER, FIELD, LOCAL_VARIABLE, TYPE_USE })
public @interface UserIdInt {}
