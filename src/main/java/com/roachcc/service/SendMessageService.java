package com.roachcc.service;

/**
 *  @Description: 发送信息的接口
 */

public interface SendMessageService {
    void sendMessage(String to, String subject, String text);
}
