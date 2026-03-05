package com.ruoyi.web.service;

import com.ruoyi.common.utils.StringUtils;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 邮件服务实现类
 *
 * @author ruoyi
 */
@Service
public class MailServiceImpl implements MailService {

    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${mail.admin-email:}")
    private String adminEmail;

    @Value("${mail.from:}")
    private String from;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    /**
     * 发送流程停用通知给管理员
     */
    @Override
    public void sendSuspendNoticeToAdmin(ProcessDefinition processDefinition, List<ProcessInstance> instances) {
        if (!mailEnabled) {
            log.debug("邮件功能已关闭，跳过发送管理员通知");
            return;
        }
        if (StringUtils.isEmpty(adminEmail)) {
            log.warn("管理员邮箱未配置，跳过发送管理员通知");
            return;
        }

        String subject = "【流程停用通知】" + processDefinition.getName();
        String content = buildAdminMailContent(processDefinition, instances);
        sendMailAsync(adminEmail, subject, content);
    }

    /**
     * 发送流程停用通知给申请人
     */
    @Override
    public void sendSuspendNoticeToApplicant(String userName, String userEmail, String processDefinitionName, List<ProcessInstance> instances) {
        if (!mailEnabled) {
            log.debug("邮件功能已关闭，跳过发送申请人通知");
            return;
        }
        if (StringUtils.isEmpty(userEmail)) {
            log.debug("用户 {} 邮箱为空，跳过发送申请人通知", userName);
            return;
        }

        String subject = "【您的流程已被停用】" + processDefinitionName;
        String content = buildApplicantMailContent(userName, instances);
        sendMailAsync(userEmail, subject, content);
    }

    /**
     * 异步发送邮件
     */
    @Override
    @Async
    public void sendMailAsync(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 优先使用配置的 from，否则使用 mail.username
            String fromAddress = StringUtils.isNotEmpty(from) ? from : mailUsername;
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true); // true表示HTML格式

            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        } catch (MessagingException e) {
            log.error("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
        } catch (Exception e) {
            log.error("邮件发送异常: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
        }
    }

    /**
     * 构建管理员邮件内容
     */
    private String buildAdminMailContent(ProcessDefinition pd, List<ProcessInstance> instances) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html><head><meta charset='UTF-8'></head><body>");
        sb.append("<div style='font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto;'>");

        sb.append("<h2 style='color: #e74c3c;'>⚠️ 流程停用通知</h2>");
        sb.append("<p>尊敬的管理员，您好！</p>");
        sb.append("<p>以下流程已被停用：</p>");

        sb.append("<table style='border-collapse: collapse; width: 100%; margin: 20px 0;'>");
        sb.append("<tr><td style='padding: 10px; border: 1px solid #ddd; background: #f5f5f5; width: 120px;'><strong>流程名称</strong></td>");
        sb.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(pd.getName()).append("</td></tr>");
        sb.append("<tr><td style='padding: 10px; border: 1px solid #ddd; background: #f5f5f5;'><strong>流程Key</strong></td>");
        sb.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(pd.getKey()).append("</td></tr>");
        sb.append("<tr><td style='padding: 10px; border: 1px solid #ddd; background: #f5f5f5;'><strong>流程版本</strong></td>");
        sb.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(pd.getVersion()).append("</td></tr>");
        sb.append("<tr><td style='padding: 10px; border: 1px solid #ddd; background: #f5f5f5;'><strong>停用时间</strong></td>");
        sb.append("<td style='padding: 10px; border: 1px solid #ddd;'>").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("</td></tr>");
        sb.append("<tr><td style='padding: 10px; border: 1px solid #ddd; background: #f5f5f5;'><strong>受影响实例</strong></td>");
        sb.append("<td style='padding: 10px; border: 1px solid #ddd; color: #e74c3c; font-weight: bold;'>").append(instances.size()).append(" 个</td></tr>");
        sb.append("</table>");

        if (!instances.isEmpty()) {
            sb.append("<h3 style='color: #333; margin-top: 30px;'>受影响的流程实例详情：</h3>");
            sb.append("<table style='border-collapse: collapse; width: 100%;'>");
            sb.append("<tr style='background: #3498db; color: white;'>");
            sb.append("<th style='padding: 10px; border: 1px solid #ddd;'>序号</th>");
            sb.append("<th style='padding: 10px; border: 1px solid #ddd;'>实例ID</th>");
            sb.append("<th style='padding: 10px; border: 1px solid #ddd;'>发起人</th>");
            sb.append("<th style='padding: 10px; border: 1px solid #ddd;'>发起时间</th>");
            sb.append("<th style='padding: 10px; border: 1px solid #ddd;'>业务Key</th>");
            sb.append("</tr>");

            int index = 1;
            for (ProcessInstance pi : instances) {
                sb.append("<tr>");
                sb.append("<td style='padding: 8px; border: 1px solid #ddd; text-align: center;'>").append(index++).append("</td>");
                sb.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(pi.getId()).append("</td>");
                sb.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(pi.getStartUserId() != null ? pi.getStartUserId() : "-").append("</td>");
                sb.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(pi.getStartTime() != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm").format(pi.getStartTime()) : "-").append("</td>");
                sb.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(pi.getBusinessKey() != null ? pi.getBusinessKey() : "-").append("</td>");
                sb.append("</tr>");
            }
            sb.append("</table>");
        } else {
            sb.append("<p style='color: #27ae60; margin-top: 20px;'>✅ 该流程没有在途实例，无需通知申请人。</p>");
        }

        sb.append("<hr style='margin-top: 30px; border: none; border-top: 1px solid #eee;'>");
        sb.append("<p style='color: #999; font-size: 12px;'>此邮件由系统自动发送，请勿回复。</p>");
        sb.append("</div></body></html>");

        return sb.toString();
    }

    /**
     * 构建申请人邮件内容
     */
    private String buildApplicantMailContent(String userName, List<ProcessInstance> instances) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>");
        sb.append("<html><head><meta charset='UTF-8'></head><body>");
        sb.append("<div style='font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto;'>");

        sb.append("<h2 style='color: #e74c3c;'>⚠️ 流程停用通知</h2>");
        sb.append("<p>").append(userName).append(" 您好，</p>");
        sb.append("<p>您发起的以下流程已被管理员停用：</p>");

        sb.append("<table style='border-collapse: collapse; width: 100%; margin: 20px 0;'>");
        sb.append("<tr style='background: #3498db; color: white;'>");
        sb.append("<th style='padding: 10px; border: 1px solid #ddd;'>序号</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #ddd;'>流程名称</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #ddd;'>发起时间</th>");
        sb.append("<th style='padding: 10px; border: 1px solid #ddd;'>实例ID</th>");
        sb.append("</tr>");

        int index = 1;
        for (ProcessInstance pi : instances) {
            sb.append("<tr>");
            sb.append("<td style='padding: 8px; border: 1px solid #ddd; text-align: center;'>").append(index++).append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(pi.getProcessDefinitionName() != null ? pi.getProcessDefinitionName() : "-").append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(pi.getStartTime() != null ? new SimpleDateFormat("yyyy-MM-dd HH:mm").format(pi.getStartTime()) : "-").append("</td>");
            sb.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(pi.getId()).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");

        sb.append("<p style='color: #e74c3c; margin-top: 20px;'>⚠️ 您的流程实例已被暂停处理，如有疑问请联系管理员。</p>");

        sb.append("<hr style='margin-top: 30px; border: none; border-top: 1px solid #eee;'>");
        sb.append("<p style='color: #999; font-size: 12px;'>此邮件由系统自动发送，请勿回复。</p>");
        sb.append("</div></body></html>");

        return sb.toString();
    }
}
