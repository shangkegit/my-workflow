package com.ruoyi.system.service.impl;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.IMailService;
import com.ruoyi.system.service.ISysUserService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 邮件服务实现类
 * 
 * @author ruoyi
 */
@Service
public class MailServiceImpl implements IMailService
{
    private static final Logger log = LoggerFactory.getLogger(MailServiceImpl.class);
    @Resource
    private JavaMailSender mailSender;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private HistoryService historyService;

    @Resource
    private TaskService taskService;

    @Resource
    private ISysUserService sysUserService;

    /**
     * 发送流程挂起通知邮件
     * 
     * @param processInstanceId 流程实例ID
     */
    @Override
    @Async
    public void sendProcessSuspendNotice(String processInstanceId)
    {
        try
        {
            // 获取流程实例
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();

            if (processInstance == null)
            {
                log.warn("流程实例不存在: {}", processInstanceId);
                return;
            }

            String processName = processInstance.getProcessDefinitionName();
            String startUserId = processInstance.getStartUserId();

            // 获取流程申请人信息
            SysUser applicant = null;
            if (StringUtils.isNotEmpty(startUserId))
            {
                applicant = sysUserService.selectUserByUserName(startUserId);
            }

            // 获取当前任务名称
            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            String currentTaskName = "";
            if (tasks.size() > 0)
            {
                currentTaskName = tasks.stream()
                        .map(Task::getName)
                        .collect(Collectors.joining(","));
            }

            // 获取管理员邮箱列表
            List<String> adminEmails = getAdminEmails();

            // 收件人列表（申请人 + 管理员）
            List<String> recipients = new ArrayList<>();
            
            // 添加申请人邮箱
            if (applicant != null && StringUtils.isNotEmpty(applicant.getEmail()))
            {
                recipients.add(applicant.getEmail());
            }

            // 添加管理员邮箱
            recipients.addAll(adminEmails);

            if (recipients.isEmpty())
            {
                log.warn("没有有效的收件人邮箱，跳过邮件发送");
                return;
            }

            // 构建邮件内容
            String subject = "【流程挂起通知】" + processName;
            String applicantName = applicant != null ? applicant.getNickName() : startUserId;
            String content = buildEmailContent(processName, applicantName, currentTaskName);

            // 发送邮件
            sendEmail(recipients, subject, content);

            log.info("流程挂起通知邮件发送成功，流程实例ID: {}, 收件人: {}", processInstanceId, recipients);
        }
        catch (Exception e)
        {
            log.error("发送流程挂起通知邮件失败，流程实例ID: {}", processInstanceId, e);
        }
    }

    /**
     * 获取管理员邮箱列表
     */
    private List<String> getAdminEmails()
    {
        List<String> emails = new ArrayList<>();
        try
        {
            // 查询有角色的用户（管理员）
            SysUser queryUser = new SysUser();
            List<SysUser> adminUsers = sysUserService.selectAllocatedList(queryUser);
            
            if (adminUsers != null && !adminUsers.isEmpty())
            {
                emails = adminUsers.stream()
                        .map(SysUser::getEmail)
                        .filter(StringUtils::isNotEmpty)
                        .collect(Collectors.toList());
            }
        }
        catch (Exception e)
        {
            log.error("获取管理员邮箱列表失败", e);
        }
        return emails;
    }

    /**
     * 构建邮件内容
     */
    private String buildEmailContent(String processName, String applicantName, String currentTaskName)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String currentTime = sdf.format(new Date());

        StringBuilder content = new StringBuilder();
        content.append("尊敬的用户：\n");
        content.append("  您好！流程【").append(processName).append("】已被挂起。\n\n");
        content.append("流程信息：\n");
        content.append("  - 流程名称：").append(processName).append("\n");
        content.append("  - 申请人：").append(applicantName).append("\n");
        content.append("  - 挂起时间：").append(currentTime).append("\n");
        content.append("  - 当前节点：").append(currentTaskName).append("\n\n");
        content.append("请及时处理。");

        return content.toString();
    }

    /**
     * 发送邮件
     */
    private void sendEmail(List<String> recipients, String subject, String content)
    {
        try
        {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipients.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
        }
        catch (Exception e)
        {
            log.error("邮件发送失败，收件人: {}, 主题: {}", recipients, subject, e);
            throw e;
        }
    }
}
