package com.roachcc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Description: 定时任务、重试机制配置
 */
@Configuration
@EnableScheduling
@EnableRetry
public class AppConfig {
}
