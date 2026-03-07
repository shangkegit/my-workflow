package com.ruoyi.common.utils.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 邮件服务
 * 
 * @author ruoyi
 */
@Service
public class MailService
{
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Resource
    private JavaMailSender mailSender;

    /**
     * 发送简单邮件
     * 
     * @param to 收件人
     * @param subject 主题
     * @param content 内容
     */
    public void sendSimpleMail(String to, String subject, String content)
    {
        try
        {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        }
        catch (Exception e)
        {
            log.error("邮件发送失败: to={}, subject={}", to, subject, e);
        }
    }
}
