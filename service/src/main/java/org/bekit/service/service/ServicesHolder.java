/* 
 * 作者：钟勋 (e-mail:zhongxunking@163.com)
 */

/*
 * 修订记录:
 * @author 钟勋 2016-12-16 01:14 创建
 */
package org.bekit.service.service;

import org.bekit.service.annotation.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 服务持有器
 */
@Component
public class ServicesHolder {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;
    // 服务执行器Map（key：服务名称）
    private final Map<String, ServiceExecutor> serviceExecutorMap = new HashMap<>();

    // 初始化（查询spring容器中所有的@Service服务并解析，spring自动执行）
    @PostConstruct
    public void init() {
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(Service.class);
        for (String beanName : beanNames) {
            // 解析服务
            ServiceExecutor serviceExecutor = ServiceParser.parseService(applicationContext.getBean(beanName), transactionManager);
            if (serviceExecutorMap.containsKey(serviceExecutor.getServiceName())) {
                throw new RuntimeException("存在重名的服务：" + serviceExecutor.getServiceName());
            }
            // 将执行器放入持有器中
            serviceExecutorMap.put(serviceExecutor.getServiceName(), serviceExecutor);
        }
    }

    /**
     * 获取所有服务名称
     */
    public Set<String> getServiceNames() {
        return serviceExecutorMap.keySet();
    }

    /**
     * 获取服务执行器
     *
     * @param service 服务名称
     * @throws IllegalArgumentException 如果不存在该服务
     */
    public ServiceExecutor getRequiredServiceExecutor(String service) {
        if (!serviceExecutorMap.containsKey(service)) {
            throw new IllegalArgumentException("不存在服务" + service);
        }
        return serviceExecutorMap.get(service);
    }
}
