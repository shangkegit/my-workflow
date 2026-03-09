package com.ruoyi.system.service.impl;

import com.ruoyi.system.service.IMailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 邮件服务实现
 */
@Service
public class MailServiceImpl implements IMailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendMail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件发送成功: to={}", to);
        } catch (Exception e) {
            log.error("邮件发送失败: to={}", to, e);
            throw e;
        }
    }

    @Override
    public void sendMail(List<String> toList, String subject, String content) {
        for (String to : toList) {
            try {
                sendMail(to, subject, content);
            } catch (Exception e) {
                log.error("批量邮件发送失败: to={}", to, e);
                // 继续发送其他邮件
            }
        }
    }
}
