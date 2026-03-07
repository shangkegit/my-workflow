package com.ruoyi.system.service.impl;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.mail.MailService;
import com.ruoyi.system.service.IProcessNotificationService;
import com.ruoyi.system.service.ISysUserService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 流程通知服务实现
 * 
 * @author ruoyi
 */
@Service
public class ProcessNotificationServiceImpl implements IProcessNotificationService
{
    private static final Logger log = LoggerFactory.getLogger(ProcessNotificationServiceImpl.class);

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private MailService mailService;

    /**
     * 通知流程实例已被挂起
     * 
     * @param processInstanceId 流程实例ID
     */
    @Override
    public void notifyProcessSuspended(String processInstanceId)
    {
        try
        {
            // 获取流程实例信息
            ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (instance == null)
            {
                log.warn("流程实例不存在: {}", processInstanceId);
                return;
            }

            String processName = instance.getProcessDefinitionName();
            String startUserId = instance.getStartUserId();
            Date suspensionTime = new Date();

            // 1. 通知管理员
            notifyAdmins(processName, processInstanceId, startUserId, suspensionTime);

            // 2. 通知申请人
            notifyApplicant(processName, processInstanceId, startUserId, suspensionTime);
        }
        catch (Exception e)
        {
            log.error("发送流程挂起通知失败: processInstanceId={}, error={}", processInstanceId, e.getMessage(), e);
        }
    }

    /**
     * 通知管理员
     */
    private void notifyAdmins(String processName, String processInstanceId, String applicant, Date suspensionTime)
    {
        try
        {
            // 查询管理员列表（roleId=1 为管理员角色）
            SysUser query = new SysUser();
            query.setRoleId(1L);
            List<SysUser> admins = userService.selectAllocatedList(query);

            if (admins == null || admins.isEmpty())
            {
                log.warn("没有找到管理员用户");
                return;
            }

            String subject = String.format("【流程挂起通知】%s", processName);
            String content = buildEmailContent(processName, processInstanceId, applicant, suspensionTime, "管理员");

            for (SysUser admin : admins)
            {
                if (StringUtils.isNotEmpty(admin.getEmail()))
                {
                    mailService.sendMail(admin.getEmail(), subject, content);
                }
            }
        }
        catch (Exception e)
        {
            log.error("通知管理员失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 通知申请人
     */
    private void notifyApplicant(String processName, String processInstanceId, String applicant, Date suspensionTime)
    {
        try
        {
            if (StringUtils.isEmpty(applicant))
            {
                log.warn("申请人信息为空");
                return;
            }

            // 查询申请人信息
            SysUser applicantUser = userService.selectUserByUserName(applicant);
            if (applicantUser == null)
            {
                log.warn("申请人不存在: {}", applicant);
                return;
            }

            if (StringUtils.isEmpty(applicantUser.getEmail()))
            {
                log.warn("申请人邮箱为空: {}", applicant);
                return;
            }

            String subject = String.format("【流程挂起通知】%s", processName);
            String content = buildEmailContent(processName, processInstanceId, applicant, suspensionTime, "申请人");

            mailService.sendMail(applicantUser.getEmail(), subject, content);
        }
        catch (Exception e)
        {
            log.error("通知申请人失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String processName, String processInstanceId, String applicant,
                                     Date suspensionTime, String recipientType)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        StringBuilder content = new StringBuilder();
        content.append(String.format("尊敬的%s，\n\n", recipientType));
        content.append("流程实例已挂起，详情如下：\n\n");
        content.append(String.format("流程名称：%s\n", processName));
        content.append(String.format("实例 ID：%s\n", processInstanceId));
        content.append(String.format("申请人：%s\n", applicant));
        content.append(String.format("挂起时间：%s\n\n", sdf.format(suspensionTime)));
        content.append("请及时处理。\n\n");
        content.append("此致\n");
        content.append("工作流管理系统\n");

        return content.toString();
    }
}
