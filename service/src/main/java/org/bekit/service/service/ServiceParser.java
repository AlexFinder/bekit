/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2016-12-16 01:14 创建
 */
package org.bekit.service.service;

import org.apache.commons.lang3.StringUtils;
import org.bekit.common.transaction.TxExecutor;
import org.bekit.service.annotation.service.Service;
import org.bekit.service.annotation.service.ServiceExecute;
import org.bekit.service.engine.ServiceContext;
import org.bekit.service.service.ServiceExecutor.ServicePhaseExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.ResolvableType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 服务解析器
 */
public class ServiceParser {
    // 日志记录器
    private static final Logger logger = LoggerFactory.getLogger(ServiceParser.class);

    /**
     * 解析服务
     *
     * @param service            服务
     * @param transactionManager 事务管理器
     * @return 服务执行器
     */
    public static ServiceExecutor parseService(Object service, PlatformTransactionManager transactionManager) {
        // 获取目标class（应对AOP代理情况）
        Class<?> serviceClass = AopUtils.getTargetClass(service);
        logger.debug("解析服务：{}", ClassUtils.getQualifiedName(serviceClass));
        Service serviceAnnotation = serviceClass.getAnnotation(Service.class);
        // 获取服务名称
        String serviceName = serviceAnnotation.name();
        if (StringUtils.isEmpty(serviceName)) {
            serviceName = ClassUtils.getShortNameAsProperty(serviceClass);
        }
        // 创建服务执行器
        ServiceExecutor serviceExecutor = new ServiceExecutor(serviceName, serviceAnnotation.enableTx(), service);
        if (serviceAnnotation.enableTx()) {
            if (transactionManager == null) {
                throw new IllegalArgumentException("服务" + serviceAnnotation.name() + "的enableTx属性为开启状态，但不存在事务管理器（PlatformTransactionManager），请检查是否有配置spring事务管理器");
            }
            serviceExecutor.setTxExecutor(new TxExecutor(transactionManager, false));
        }
        for (Method method : serviceClass.getDeclaredMethods()) {
            for (Class clazz : ServiceExecutor.SERVICE_PHASE_ANNOTATIONS) {
                if (method.isAnnotationPresent(clazz)) {
                    // 设置服务阶段执行器
                    serviceExecutor.setPhaseExecutor(clazz, parseServicePhase(method));
                    break;
                }
            }
        }
        serviceExecutor.validate();

        return serviceExecutor;
    }

    // 解析服务阶段
    private static ServicePhaseExecutor parseServicePhase(Method method) {
        logger.debug("解析服务方法：{}", method);
        // 校验方法类型
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException("服务方法" + ClassUtils.getQualifiedMethodName(method) + "必须是public类型");
        }
        // 校验入参
        Class[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1 || parameterTypes[0] != ServiceContext.class) {
            throw new IllegalArgumentException("服务方法" + ClassUtils.getQualifiedMethodName(method) + "的入参必须是（ServiceContext）");
        }
        // 校验返回类型
        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("服务方法" + ClassUtils.getQualifiedMethodName(method) + "的返回类型必须是void");
        }
        // 获取ServiceContext中泛型O、R的真实类型
        ResolvableType resolvableType = ResolvableType.forMethodParameter(method, 0);
        Class orderClass = resolvableType.getGeneric(0).resolve(Object.class);
        Class resultClass = resolvableType.getGeneric(1).resolve(Object.class);
        // 校验result是否有默认构造函数
        if (method.isAnnotationPresent(ServiceExecute.class)) {
            if (!ClassUtils.hasConstructor(resultClass, new Class[]{})) {
                throw new IllegalArgumentException("@ServiceExecute服务方法" + ClassUtils.getQualifiedMethodName(method) + "参数ServiceContext的泛型" + ClassUtils.getShortName(resultClass) + "必须得有默认构造函数");
            }
        }

        return new ServicePhaseExecutor(method, orderClass, resultClass);
    }
}
