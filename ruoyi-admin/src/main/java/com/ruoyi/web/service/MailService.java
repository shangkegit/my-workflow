package com.ruoyi.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务
 * 
 * @author ruoyi
 */
@Service
public class MailService
{
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 异步发送简单邮件
     * 
     * @param to 收件人
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    @Async
    public void sendSimpleMail(String to, String subject, String content)
    {
        try
        {
            if (mailSender == null)
            {
                log.warn("邮件发送器未配置，跳过发送邮件到: {}", to);
                return;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件发送成功: {}", to);
        }
        catch (Exception e)
        {
            log.error("邮件发送失败: {}, 错误: {}", to, e.getMessage());
        }
    }

    /**
     * 检查邮件服务是否可用
     * 
     * @return true 如果邮件服务已配置
     */
    public boolean isMailServiceAvailable()
    {
        return mailSender != null;
    }
}
