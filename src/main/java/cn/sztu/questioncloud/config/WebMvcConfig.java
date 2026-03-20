package cn.sztu.questioncloud.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 异步请求配置类
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 创建一个正规的线程池
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);      // 核心线程数（日常保留的员工）
        executor.setMaxPoolSize(50);       // 最大线程数（最忙时雇佣的临时工）
        executor.setQueueCapacity(100);    // 队列容量（大厅等候区的座位数）
        executor.setThreadNamePrefix("ai-stream-"); // 给线程起个名字，方便以后看日志
        executor.initialize();             // 初始化线程池
        configurer.setTaskExecutor(executor);
        // 设置异步请求的超时时间（比如 60 秒），防止大模型卡死导致连接一直挂着
        configurer.setDefaultTimeout(60000L);
    }
}