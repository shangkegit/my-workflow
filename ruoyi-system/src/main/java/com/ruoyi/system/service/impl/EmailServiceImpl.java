package com.ruoyi.system.service.impl;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.IEmailService;
import com.ruoyi.system.service.ISysUserService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * 邮件服务实现类
 *
 * @author ruoyi
 */
@Service
public class EmailServiceImpl implements IEmailService
{
    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ISysUserService userService;

    @Resource
    private RuntimeService runtimeService;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 发送流程挂起通知邮件
     *
     * @param processInstanceId 流程实例ID
     * @param processName 流程名称
     * @param operator 操作人
     * @param reason 挂起原因
     */
    @Override
    @Async
    public void sendSuspendNotification(String processInstanceId, String processName,
                                         String operator, String reason)
    {
        try
        {
            List<String> recipients = new ArrayList<>();

            // 获取管理员邮箱（userId=1）
            SysUser admin = userService.selectUserById(1L);
            if (admin != null && StringUtils.isNotEmpty(admin.getEmail()))
            {
                recipients.add(admin.getEmail());
            }

            // 获取流程发起人邮箱
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (instance != null)
            {
                String startUserId = instance.getStartUserId();
                if (StringUtils.isNotEmpty(startUserId))
                {
                    SysUser applicant = userService.selectUserByUserName(startUserId);
                    if (applicant != null && StringUtils.isNotEmpty(applicant.getEmail()))
                    {
                        // 避免重复添加
                        if (!recipients.contains(applicant.getEmail()))
                        {
                            recipients.add(applicant.getEmail());
                        }
                    }
                }
            }

            if (recipients.isEmpty())
            {
                log.warn("流程挂起通知邮件：未找到收件人，processInstanceId={}", processInstanceId);
                return;
            }

            // 构建邮件内容
            Context context = new Context();
            context.setVariable("processName", processName != null ? processName : "未命名流程");
            context.setVariable("processInstanceId", processInstanceId);
            context.setVariable("operator", operator != null ? operator : "系统");
            context.setVariable("reason", reason != null ? reason : "无");
            String content = templateEngine.process("email/process-suspend", context);

            // 发送邮件
            sendEmail(recipients.toArray(new String[0]), "流程挂起通知：" + processName, content);

            log.info("流程挂起通知邮件发送成功，processInstanceId={}, recipients={}", processInstanceId, recipients);
        }
        catch (Exception e)
        {
            log.error("流程挂起通知邮件发送失败，processInstanceId={}", processInstanceId, e);
        }
    }

    /**
     * 发送邮件
     *
     * @param to 收件人列表
     * @param subject 主题
     * @param content 内容（HTML格式）
     */
    private void sendEmail(String[] to, String subject, String content) throws MessagingException
    {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(message);
    }
}
