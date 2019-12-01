/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2016-12-16 01:14 创建
 */
package org.bekit.service.engine;

import org.bekit.event.EventPublisher;
import org.bekit.service.ServiceEngine;
import org.bekit.service.event.ServiceApplyEvent;
import org.bekit.service.event.ServiceExceptionEvent;
import org.bekit.service.event.ServiceFinishEvent;
import org.bekit.service.service.ServiceExecutor;
import org.bekit.service.service.ServicesHolder;
import org.springframework.cglib.core.ReflectUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务引擎默认实现类
 */
public class DefaultServiceEngine implements ServiceEngine {
    // 服务持有器
    private final ServicesHolder servicesHolder;
    // 服务事件发布器
    private final EventPublisher eventPublisher;

    public DefaultServiceEngine(ServicesHolder servicesHolder, EventPublisher eventPublisher) {
        this.servicesHolder = servicesHolder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public <O, R> R execute(String service, O order) {
        return execute(service, order, null);
    }

    @Override
    public <O, R> R execute(String service, O order, Map<Object, Object> attachment) {
        // 校验order类型
        checkOrderClass(order, service);
        // 构建服务上下文
        ServiceContext<O, R> serviceContext = new ServiceContext(order, newResult(service), reviseAttachment(attachment));
        // 执行服务
        executeService(service, serviceContext);

        return serviceContext.getResult();
    }

    // 校验入参order类型
    private void checkOrderClass(Object order, String service) {
        ServiceExecutor serviceExecutor = servicesHolder.getRequiredServiceExecutor(service);
        if (!serviceExecutor.getOrderClass().isAssignableFrom(order.getClass())) {
            throw new IllegalArgumentException("入参order的类型和服务" + serviceExecutor.getServiceName() + "期望的类型不匹配");
        }
    }

    // 创建result
    private Object newResult(String service) {
        ServiceExecutor serviceExecutor = servicesHolder.getRequiredServiceExecutor(service);
        return ReflectUtils.newInstance(serviceExecutor.getResultClass());
    }

    // 修正附件
    private Map<Object, Object> reviseAttachment(Map<Object, Object> attachment) {
        return attachment != null ? attachment : new HashMap<>();
    }

    // 执行服务
    private void executeService(String service, ServiceContext serviceContext) {
        // 获取服务执行器
        ServiceExecutor serviceExecutor = servicesHolder.getRequiredServiceExecutor(service);
        try {
            // 发布服务申请事件
            eventPublisher.publish(new ServiceApplyEvent(service, serviceContext));
            // 执行服务
            serviceExecutor.execute(serviceContext);
        } catch (Throwable e) {
            // 发布服务异常事件
            eventPublisher.publish(new ServiceExceptionEvent(service, serviceContext, e));
        } finally {
            // 发布服务结束事件
            eventPublisher.publish(new ServiceFinishEvent(service, serviceContext));
        }
    }
}
