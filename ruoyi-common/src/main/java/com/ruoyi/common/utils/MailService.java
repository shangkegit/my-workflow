package com.ruoyi.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用邮件服务
 * 
 * @author ruoyi
 */
@Service
public class MailService
{
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private JavaMailSender mailSender;

    /**
     * 异步发送单个邮件（HTML格式）
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容（HTML格式）
     */
    @Async
    public void send(String to, String subject, String content)
    {
        try
        {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("邮件发送成功，收件人：{}", to);
        }
        catch (MessagingException e)
        {
            log.error("邮件发送失败，收件人：{}，错误信息：{}", to, e.getMessage(), e);
        }
        catch (Exception e)
        {
            log.error("邮件发送异常，收件人：{}，错误信息：{}", to, e.getMessage(), e);
        }
    }

    /**
     * 异步批量发送邮件（HTML格式）
     * 
     * @param toList 收件人邮箱列表
     * @param subject 邮件主题
     * @param content 邮件内容（HTML格式）
     */
    @Async
    public void send(List<String> toList, String subject, String content)
    {
        if (toList == null || toList.isEmpty())
        {
            log.warn("收件人列表为空，跳过邮件发送");
            return;
        }

        // 过滤空邮箱
        List<String> validEmails = toList.stream()
                .filter(email -> StringUtils.isNotEmpty(email))
                .collect(Collectors.toList());

        if (validEmails.isEmpty())
        {
            log.warn("没有有效的收件人邮箱，跳过邮件发送");
            return;
        }

        try
        {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(validEmails.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
            log.info("批量邮件发送成功，收件人数量：{}", validEmails.size());
        }
        catch (MessagingException e)
        {
            log.error("批量邮件发送失败，收件人：{}，错误信息：{}", validEmails, e.getMessage(), e);
        }
        catch (Exception e)
        {
            log.error("批量邮件发送异常，收件人：{}，错误信息：{}", validEmails, e.getMessage(), e);
        }
    }
}
