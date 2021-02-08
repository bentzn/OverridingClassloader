package com.bentzn.products.appserver.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author bentzn
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface PublishedService {


    String entityName() default "";


    String methodName() default "";


    Class<?> returnClass() default Void.class;


    boolean exclude() default false;


    boolean isEntityClass() default false;
}
