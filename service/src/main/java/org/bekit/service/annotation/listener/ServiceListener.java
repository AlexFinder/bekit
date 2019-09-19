/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2016-12-16 01:14 创建
 */
package org.bekit.service.annotation.listener;

import org.bekit.event.annotation.listener.Listener;
import org.bekit.service.listener.ServiceListenerType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 服务监听器
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Listener(type = ServiceListenerType.class)
public @interface ServiceListener {
    /**
     * 优先级
     * （具体执行顺序需要结合@Listen注解的priorityAsc属性共同决定）
     */
    @AliasFor(annotation = Listener.class, attribute = "priority")
    int priority() default Integer.MAX_VALUE;
}
