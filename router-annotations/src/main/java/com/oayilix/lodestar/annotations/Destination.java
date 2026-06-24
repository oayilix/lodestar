package com.oayilix.lodestar.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE}) // 元注解，说明当前注解可以修饰的元素，此处标识可以用于标记在类上面
@Retention(RetentionPolicy.CLASS) // 元注解，说明当前注解的生命周期。也就是可以保留的时间。保留到编译为 class 文件。
public @interface Destination {

    /**
     * 当前页面定义的 url，不能为空
     * @return 页面定义的 url
     */
    String url();

    /**
     * 定义当前页面的描述
     * @return 页面描述内容
     */
    String description() default "no description";
}
