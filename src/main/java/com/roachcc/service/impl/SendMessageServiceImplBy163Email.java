package com.roachcc.service.impl;

import com.roachcc.config.EMailConfig;
import com.roachcc.service.SendMessageService;
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
public class SendMessageServiceImplBy163Email implements SendMessageService {

    @Autowired
    private JavaMailSender mailSender; //邮件发送器

    @Autowired
    private EMailConfig EMailConfig; //邮件配置

    @Override
    public void sendMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom(EMailConfig.getEMAIL_ADDRESS()); // 发件人、收件人（自己发给自己）
            mailSender.send(message);
        } catch (MailSendException ex) {
            // 记录更多错误信息
            log.error("邮件发送失败: " + ex.getMessage());
        }
    }
}

