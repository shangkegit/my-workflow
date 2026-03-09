package com.ruoyi.system.service;

import java.util.List;

/**
 * 邮件服务接口
 */
public interface IMailService {
    /**
     * 发送邮件
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendMail(String to, String subject, String content);

    /**
     * 批量发送邮件
     * @param toList 收件人列表
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendMail(List<String> toList, String subject, String content);
}
