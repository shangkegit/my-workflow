package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.mail.MailService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程停用通知服务
 * 当流程被停用时，发送邮件通知给管理员和相关申请人
 * 
 * @author OpenClaw Agent
 * @date 2026-03-05
 */
@Service
public class WorkflowSuspendNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowSuspendNotificationService.class);

    @Autowired(required = false)
    private MailService mailService;

    @Autowired(required = false)
    private RuntimeService runtimeService;

    @Autowired
    private ISysUserService userService;

    @Value("${mail.admin-emails:}")
    private String adminEmailsConfig;

    @Value("${mail.enabled:false}")
    private boolean mailEnabled;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 流程停用通知入口
     * 
     * @param processKey  流程标识
     * @param processName 流程名称
     * @param reason      停用原因（可选）
     */
    public void notifyOnSuspend(String processKey, String processName, String reason) {
        if (!isNotificationAvailable()) {
            log.info("邮件通知服务不可用，跳过流程停用通知: processKey={}", processKey);
            return;
        }

        log.info("开始处理流程停用通知: processKey={}, processName={}", processKey, processName);

        try {
            // 1. 获取运行中实例的申请人信息
            List<ProcessInstance> activeInstances = getActiveInstances(processKey);
            List<SysUser> applicants = getApplicants(activeInstances);
            
            // 2. 发送管理员通知
            sendAdminNotification(processKey, processName, reason, activeInstances, applicants);
            
            // 3. 发送申请人通知
            sendApplicantNotifications(processKey, processName, reason, activeInstances);
            
            log.info("流程停用通知发送完成: processKey={}, 申请人数量={}", processKey, applicants.size());
        } catch (Exception e) {
            log.error("流程停用通知发送失败: processKey={}, error={}", processKey, e.getMessage(), e);
        }
    }

    /**
     * 异步发送流程停用通知
     */
    @Async
    public void notifyOnSuspendAsync(String processKey, String processName, String reason) {
        notifyOnSuspend(processKey, processName, reason);
    }

    /**
     * 获取流程的运行中实例
     */
    private List<ProcessInstance> getActiveInstances(String processKey) {
        if (runtimeService == null) {
            return Collections.emptyList();
        }
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processKey)
                .active()
                .list();
    }

    /**
     * 从流程实例中提取申请人
     */
    private List<SysUser> getApplicants(List<ProcessInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> startUserIds = instances.stream()
                .map(ProcessInstance::getStartUserId)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());

        if (startUserIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询用户信息
        List<SysUser> users = new ArrayList<>();
        for (String userId : startUserIds) {
            try {
                // 尝试按用户名查询
                SysUser user = userService.selectUserByUserName(userId);
                if (user != null && StringUtils.isNotEmpty(user.getEmail())) {
                    users.add(user);
                }
            } catch (Exception e) {
                log.warn("查询用户信息失败: userId={}, error={}", userId, e.getMessage());
            }
        }
        return users;
    }

    /**
     * 发送管理员通知
     */
    private void sendAdminNotification(String processKey, String processName, String reason,
                                       List<ProcessInstance> instances, List<SysUser> applicants) {
        List<String> adminEmails = getAdminEmails();
        if (adminEmails.isEmpty()) {
            log.warn("未配置管理员邮箱，跳过管理员通知");
            return;
        }

        String subject = "【工作流系统】流程停用通知";
        String content = buildAdminEmailContent(processKey, processName, reason, instances, applicants);

        mailService.sendHtmlMailAsync(adminEmails, subject, content);
        log.info("已发送管理员通知: admins={}", adminEmails);
    }

    /**
     * 发送申请人通知
     */
    private void sendApplicantNotifications(String processKey, String processName, String reason,
                                            List<ProcessInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return;
        }

        String suspendTime = DATE_FORMAT.format(new Date());
        
        for (ProcessInstance instance : instances) {
            String startUserId = instance.getStartUserId();
            if (StringUtils.isEmpty(startUserId)) {
                continue;
            }

            try {
                SysUser user = userService.selectUserByUserName(startUserId);
                if (user == null || StringUtils.isEmpty(user.getEmail())) {
                    log.warn("申请人邮箱为空，跳过通知: userId={}", startUserId);
                    continue;
                }

                String subject = "【工作流系统】您申请的流程已被停用";
                String content = buildApplicantEmailContent(user, processName, instance, reason, suspendTime);

                mailService.sendHtmlMailAsync(user.getEmail(), subject, content);
                log.info("已发送申请人通知: userId={}, email={}", startUserId, user.getEmail());
            } catch (Exception e) {
                log.error("发送申请人通知失败: userId={}, error={}", startUserId, e.getMessage());
            }
        }
    }

    /**
     * 构建管理员邮件内容
     */
    private String buildAdminEmailContent(String processKey, String processName, String reason,
                                          List<ProcessInstance> instances, List<SysUser> applicants) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<p>尊敬的管理员：</p>");
        sb.append("<p>以下流程已被停用：</p>");
        sb.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse;'>");
        sb.append("<tr><td><b>流程名称</b></td><td>").append(processName).append("</td></tr>");
        sb.append("<tr><td><b>流程标识</b></td><td>").append(processKey).append("</td></tr>");
        sb.append("<tr><td><b>停用时间</b></td><td>").append(DATE_FORMAT.format(new Date())).append("</td></tr>");
        sb.append("<tr><td><b>停用原因</b></td><td>").append(StringUtils.isNotEmpty(reason) ? reason : "未说明").append("</td></tr>");
        sb.append("<tr><td><b>受影响实例数</b></td><td>").append(instances.size()).append("</td></tr>");
        sb.append("</table>");

        if (!applicants.isEmpty()) {
            sb.append("<p><b>涉及申请人：</b></p>");
            sb.append("<ul>");
            for (SysUser user : applicants) {
                sb.append("<li>").append(user.getNickName()).append(" (").append(user.getUserName()).append(")</li>");
            }
            sb.append("</ul>");
        }

        sb.append("<p>请及时处理相关事宜。</p>");
        sb.append("<hr/>");
        sb.append("<p style='color: gray; font-size: 12px;'>此邮件由系统自动发送，请勿回复。</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * 构建申请人邮件内容
     */
    private String buildApplicantEmailContent(SysUser user, String processName, 
                                              ProcessInstance instance, String reason, String suspendTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<p>尊敬的 ").append(user.getNickName()).append("：</p>");
        sb.append("<p>您好！</p>");
        sb.append("<p>您申请的流程 <b>\"").append(processName).append("\"</b> 已被系统管理员停用。</p>");
        sb.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse: collapse;'>");
        sb.append("<tr><td><b>流程实例ID</b></td><td>").append(instance.getId()).append("</td></tr>");
        sb.append("<tr><td><b>当前状态</b></td><td>已暂停</td></tr>");
        sb.append("<tr><td><b>停用时间</b></td><td>").append(suspendTime).append("</td></tr>");
        if (StringUtils.isNotEmpty(reason)) {
            sb.append("<tr><td><b>停用原因</b></td><td>").append(reason).append("</td></tr>");
        }
        sb.append("</table>");
        sb.append("<p>如有疑问，请联系系统管理员。</p>");
        sb.append("<hr/>");
        sb.append("<p style='color: gray; font-size: 12px;'>此邮件由系统自动发送，请勿回复。</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    /**
     * 获取管理员邮箱列表
     */
    private List<String> getAdminEmails() {
        if (StringUtils.isEmpty(adminEmailsConfig)) {
            return Collections.emptyList();
        }
        return Arrays.stream(adminEmailsConfig.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }

    /**
     * 检查通知服务是否可用
     */
    private boolean isNotificationAvailable() {
        return mailEnabled && mailService != null && mailService.isMailAvailable();
    }
}
