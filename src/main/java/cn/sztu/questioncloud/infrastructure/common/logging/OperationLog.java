package cn.sztu.questioncloud.infrastructure.common.logging;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String module();

    String action();

    String targetType() default "";

    String targetId() default "";

    String targetName() default "";

    String content();

    String userId() default "";

    boolean recordFailure() default true;
}
