package com.ruoyi.web.service.impl;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.web.service.IProcessNotificationService;
import com.ruoyi.web.service.MailService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 流程通知服务实现类
 * 
 * @author ruoyi
 */
@Service
public class ProcessNotificationServiceImpl implements IProcessNotificationService
{
    private static final Logger log = LoggerFactory.getLogger(ProcessNotificationServiceImpl.class);

    private static final Long ADMIN_ROLE_ID = 1L; // 管理员角色ID

    @Autowired
    private HistoryService historyService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private MailService mailService;

    /**
     * 异步通知流程挂起
     * 发送邮件通知给管理员和流程申请人
     * 
     * @param processInstanceId 流程实例ID
     */
    @Override
    @Async
    public void notifyProcessSuspended(String processInstanceId)
    {
        try
        {
            // 检查邮件服务是否可用
            if (!mailService.isMailServiceAvailable())
            {
                log.info("邮件服务未配置，跳过流程挂起通知");
                return;
            }

            // 获取流程实例信息
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance == null)
            {
                log.warn("未找到流程实例: {}", processInstanceId);
                return;
            }

            String processName = processInstance.getProcessDefinitionName();
            String startUserId = processInstance.getStartUserId();
            String suspendedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            // 构建邮件内容
            String subject = "流程挂起通知";
            String content = buildEmailContent(processName, processInstanceId, suspendedTime);

            // 1. 发送邮件给管理员
            sendNotificationToAdmins(subject, content);

            // 2. 发送邮件给流程申请人
            if (startUserId != null && !startUserId.isEmpty())
            {
                sendNotificationToApplicant(startUserId, subject, content);
            }
            else
            {
                log.info("流程实例 {} 无申请人信息，跳过申请人通知", processInstanceId);
            }
        }
        catch (Exception e)
        {
            log.error("发送流程挂起通知失败，流程实例ID: {}, 错误: {}", processInstanceId, e.getMessage(), e);
        }
    }

    /**
     * 发送通知给所有管理员
     */
    private void sendNotificationToAdmins(String subject, String content)
    {
        try
        {
            // 查询具有管理员角色的用户
            SysUser queryUser = new SysUser();
            queryUser.setRoleId(ADMIN_ROLE_ID);
            List<SysUser> adminUsers = userService.selectAllocatedList(queryUser);

            if (adminUsers == null || adminUsers.isEmpty())
            {
                log.warn("未找到管理员用户");
                return;
            }

            for (SysUser admin : adminUsers)
            {
                if (admin.getEmail() != null && !admin.getEmail().isEmpty())
                {
                    mailService.sendSimpleMail(admin.getEmail(), subject, content);
                }
            }
        }
        catch (Exception e)
        {
            log.error("发送管理员通知失败: {}", e.getMessage());
        }
    }

    /**
     * 发送通知给流程申请人
     */
    private void sendNotificationToApplicant(String startUserId, String subject, String content)
    {
        try
        {
            SysUser applicant = userService.selectUserByUserName(startUserId);
            if (applicant != null && applicant.getEmail() != null && !applicant.getEmail().isEmpty())
            {
                mailService.sendSimpleMail(applicant.getEmail(), subject, content);
            }
            else
            {
                log.info("申请人 {} 没有配置邮箱地址", startUserId);
            }
        }
        catch (Exception e)
        {
            log.error("发送申请人通知失败: {}", e.getMessage());
        }
    }

    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String processName, String processInstanceId, String suspendedTime)
    {
        StringBuilder content = new StringBuilder();
        content.append("您好，\n\n");
        content.append("以下流程已被挂起：\n\n");
        content.append("流程名称：").append(processName != null ? processName : "未知").append("\n");
        content.append("流程实例ID：").append(processInstanceId).append("\n");
        content.append("挂起时间：").append(suspendedTime).append("\n\n");
        content.append("请及时处理。\n\n");
        content.append("此邮件由系统自动发送，请勿直接回复。");
        return content.toString();
    }
}
