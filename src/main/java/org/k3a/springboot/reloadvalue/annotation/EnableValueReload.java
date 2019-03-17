package org.k3a.springboot.reloadvalue.annotation;

import java.lang.annotation.*;

/**
 * Created by k3a
 * on 2019/1/24  AM 11:04
 * <p>
 * reload fields injected by @Value
 * notice that there are some side effect while fields reload(SpEL expressions will be evaluated),though this might not a big deal in common cases,
 * you should still take care of it and do not write "temporary" SpEL expressions
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableValueReload {
}
