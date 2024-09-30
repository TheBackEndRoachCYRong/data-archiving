package com.roachcc.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author TheBackEndRoachCYRong
 * @Description: 邮箱配置
 */
@Configuration
@Data
public class MailConfig {

    @Value("${spring.mail.username}")
    private String EMAIL_ADDRESS;// 收件人邮箱
}
