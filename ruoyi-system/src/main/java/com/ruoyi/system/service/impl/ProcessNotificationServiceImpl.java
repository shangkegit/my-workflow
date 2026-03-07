package com.ruoyi.system.service.impl;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.MailService;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.IProcessNotificationService;
import com.ruoyi.system.service.ISysUserService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private HistoryService historyService;

    @Autowired
    private ISysUserService sysUserService;

    @Autowired
    private MailService mailService;

    /**
     * 通知流程已被挂起
     * 
     * @param processInstanceId 流程实例ID
     */
    @Override
    public void notifyProcessSuspended(String processInstanceId)
    {
        try
        {
            // 1. 获取流程实例信息
            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance == null)
            {
                log.warn("流程实例不存在，processInstanceId: {}", processInstanceId);
                return;
            }

            String processName = processInstance.getProcessDefinitionName();
            String startUserId = processInstance.getStartUserId();

            // 2. 获取管理员邮箱列表
            List<String> adminEmails = getAdminEmails();

            // 3. 获取申请人邮箱
            String applicantEmail = getApplicantEmail(startUserId);

            // 4. 构建邮件内容
            String subject = "流程挂起通知：" + processName;
            String content = buildEmailContent(processName, processInstanceId, startUserId);

            // 5. 发送邮件给管理员
            if (!adminEmails.isEmpty())
            {
                mailService.send(adminEmails, subject, content);
                log.info("已发送流程挂起通知给管理员，流程实例ID: {}", processInstanceId);
            }

            // 6. 发送邮件给申请人（如果与管理员不同）
            if (StringUtils.isNotEmpty(applicantEmail) && !adminEmails.contains(applicantEmail))
            {
                mailService.send(applicantEmail, subject, content);
                log.info("已发送流程挂起通知给申请人，邮箱: {}", applicantEmail);
            }
        }
        catch (Exception e)
        {
            // 邮件发送失败不应影响流程挂起操作
            log.error("发送流程挂起通知失败，processInstanceId: {}，错误信息: {}", processInstanceId, e.getMessage(), e);
        }
    }

    /**
     * 获取管理员邮箱列表
     */
    private List<String> getAdminEmails()
    {
        try
        {
            // 通过角色查询已分配的管理员用户
            SysUser queryUser = new SysUser();
            queryUser.setRoleId(1L); // admin角色ID通常为1
            List<SysUser> adminUsers = sysUserService.selectAllocatedList(queryUser);

            // 过滤出有效邮箱
            return adminUsers.stream()
                    .map(SysUser::getEmail)
                    .filter(email -> StringUtils.isNotEmpty(email))
                    .collect(Collectors.toList());
        }
        catch (Exception e)
        {
            log.error("获取管理员邮箱列表失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 获取申请人邮箱
     */
    private String getApplicantEmail(String startUserId)
    {
        if (StringUtils.isEmpty(startUserId))
        {
            return null;
        }

        try
        {
            // 尝试作为用户ID查询
            SysUser user = sysUserService.selectUserById(Long.parseLong(startUserId));
            if (user != null)
            {
                return user.getEmail();
            }
        }
        catch (NumberFormatException e)
        {
            // 如果不是数字，尝试作为用户名查询
            try
            {
                SysUser user = sysUserService.selectUserByUserName(startUserId);
                if (user != null)
                {
                    return user.getEmail();
                }
            }
            catch (Exception ex)
            {
                log.error("根据用户名查询申请人邮箱失败: {}", ex.getMessage(), ex);
            }
        }
        catch (Exception e)
        {
            log.error("获取申请人邮箱失败: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * 构建邮件内容（HTML格式）
     */
    private String buildEmailContent(String processName, String processInstanceId, String startUserId)
    {
        StringBuilder content = new StringBuilder();
        content.append("<!DOCTYPE html>");
        content.append("<html>");
        content.append("<head>");
        content.append("<meta charset='UTF-8'>");
        content.append("<style>");
        content.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        content.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        content.append(".header { background-color: #f56c6c; color: white; padding: 15px; text-align: center; }");
        content.append(".content { background-color: #f9f9f9; padding: 20px; border: 1px solid #ddd; }");
        content.append(".info-item { margin-bottom: 10px; }");
        content.append(".label { font-weight: bold; }");
        content.append(".footer { text-align: center; color: #999; padding: 15px; font-size: 12px; }");
        content.append("</style>");
        content.append("</head>");
        content.append("<body>");
        content.append("<div class='container'>");
        content.append("<div class='header'>");
        content.append("<h2>流程挂起通知</h2>");
        content.append("</div>");
        content.append("<div class='content'>");
        content.append("<p>您好，以下流程已被管理员挂起：</p>");
        content.append("<div class='info-item'><span class='label'>流程名称：</span>").append(processName).append("</div>");
        content.append("<div class='info-item'><span class='label'>流程实例ID：</span>").append(processInstanceId).append("</div>");
        if (StringUtils.isNotEmpty(startUserId))
        {
            content.append("<div class='info-item'><span class='label'>申请人：</span>").append(startUserId).append("</div>");
        }
        content.append("<p>请及时关注并处理相关事宜。</p>");
        content.append("</div>");
        content.append("<div class='footer'>");
        content.append("此邮件由系统自动发送，请勿直接回复。");
        content.append("</div>");
        content.append("</div>");
        content.append("</body>");
        content.append("</html>");
        return content.toString();
    }
}
