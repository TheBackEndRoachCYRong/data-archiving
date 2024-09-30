package com.roachcc.service.tools;

import com.roachcc.config.MailConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件工具-服务类
 */
@Service
@Slf4j
public class MailServiceTool {

    @Autowired
    private JavaMailSender mailSender; //邮件发送器

    @Autowired
    private MailConfig mailConfig; //邮件配置

    public void sendErrorEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom(mailConfig.getEMAIL_ADDRESS()); // 发件人、收件人（自己发给自己）
            mailSender.send(message);
        } catch (MailSendException ex) {
            // 记录更多错误信息
            log.error("邮件发送失败: " + ex.getMessage());
        }
    }
}

