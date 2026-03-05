package com.ruoyi.common.utils.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;

/**
 * 邮件发送服务
 * 
 * @author OpenClaw Agent
 * @date 2026-03-05
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${mail.from-name:工作流系统}")
    private String fromName;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    /**
     * 发送 HTML 邮件
     * 
     * @param to      收件人
     * @param subject 主题
     * @param content HTML 内容
     */
    public boolean sendHtmlMail(String to, String subject, String content) {
        if (!isMailAvailable()) {
            log.warn("邮件服务未启用或未配置，跳过发送邮件给: {}", to);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
            return true;
        } catch (MessagingException e) {
            log.error("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送 HTML 邮件给多个收件人
     * 
     * @param toList  收件人列表
     * @param subject 主题
     * @param content HTML 内容
     */
    public boolean sendHtmlMail(List<String> toList, String subject, String content) {
        if (toList == null || toList.isEmpty()) {
            log.warn("收件人列表为空，跳过发送邮件");
            return false;
        }

        if (!isMailAvailable()) {
            log.warn("邮件服务未启用或未配置，跳过发送邮件给: {}", toList);
            return false;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toList.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", toList, subject);
            return true;
        } catch (MessagingException e) {
            log.error("邮件发送失败: to={}, subject={}, error={}", toList, subject, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 异步发送邮件
     * 
     * @param to      收件人
     * @param subject 主题
     * @param content HTML 内容
     */
    @Async
    public void sendHtmlMailAsync(String to, String subject, String content) {
        sendHtmlMail(to, subject, content);
    }

    /**
     * 异步批量发送邮件
     * 
     * @param toList  收件人列表
     * @param subject 主题
     * @param content HTML 内容
     */
    @Async
    public void sendHtmlMailAsync(List<String> toList, String subject, String content) {
        sendHtmlMail(toList, subject, content);
    }

    /**
     * 检查邮件服务是否可用
     */
    public boolean isMailAvailable() {
        return mailEnabled && mailSender != null && from != null && !from.isEmpty();
    }

    /**
     * 获取发件人名称
     */
    public String getFromName() {
        return fromName;
    }
}
