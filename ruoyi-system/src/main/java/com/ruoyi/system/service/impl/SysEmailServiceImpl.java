package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysEmailLog;
import com.ruoyi.system.mapper.SysEmailLogMapper;
import com.ruoyi.system.service.ISysEmailService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 邮件服务实现
 *
 * @author ruoyi
 */
@Service
public class SysEmailServiceImpl implements ISysEmailService
{
    private static final Logger log = LoggerFactory.getLogger(SysEmailServiceImpl.class);

    /** 邮件类型：流程挂起通知 */
    private static final String EMAIL_TYPE_SUSPEND = "SUSPEND_NOTIFY";

    /** 状态：待发送 */
    private static final String STATUS_PENDING = "0";
    /** 状态：已发送 */
    private static final String STATUS_SENT = "1";
    /** 状态：发送失败 */
    private static final String STATUS_FAILED = "2";

    @Autowired
    private SysEmailLogMapper emailLogMapper;

    @Autowired
    private ISysUserService userService;

    /**
     * 发送流程挂起通知
     * 异步发送，不阻塞主流程
     *
     * @param processName 流程名称
     * @param applicantId 申请人ID
     */
    @Override
    @Async
    public void sendSuspendNotification(String processName, Long applicantId)
    {
        log.info("开始发送流程挂起通知，流程名称: {}, 申请人ID: {}", processName, applicantId);

        try
        {
            // 1. 通知管理员
            notifyAdmins(processName);

            // 2. 通知申请人
            notifyApplicant(processName, applicantId);

            log.info("流程挂起通知发送完成");
        }
        catch (Exception e)
        {
            log.error("发送流程挂起通知异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 通知管理员
     *
     * @param processName 流程名称
     */
    private void notifyAdmins(String processName)
    {
        // 获取管理员用户列表（角色为 admin 的用户）
        SysUser queryUser = new SysUser();
        queryUser.setUserName("admin");
        List<SysUser> admins = userService.selectAllocatedList(queryUser);

        if (admins == null || admins.isEmpty())
        {
            log.warn("未找到管理员用户，跳过管理员通知");
            return;
        }

        String subject = "【流程挂起通知】流程已暂停";
        String content = String.format("尊敬的管理员，流程【%s】已被挂起，请及时处理。", processName);

        for (SysUser admin : admins)
        {
            if (StringUtils.isNotEmpty(admin.getEmail()))
            {
                sendEmailWithLog(admin.getEmail(), admin.getNickName(), subject, content, processName);
            }
            else
            {
                log.warn("管理员 {} 邮箱为空，跳过发送", admin.getUserName());
            }
        }
    }

    /**
     * 通知申请人
     *
     * @param processName 流程名称
     * @param applicantId 申请人ID
     */
    private void notifyApplicant(String processName, Long applicantId)
    {
        if (applicantId == null)
        {
            log.warn("申请人ID为空，跳过申请人通知");
            return;
        }

        SysUser applicant = userService.selectUserById(applicantId);
        if (applicant == null)
        {
            log.warn("未找到申请人，申请人ID: {}", applicantId);
            return;
        }

        if (StringUtils.isEmpty(applicant.getEmail()))
        {
            log.warn("申请人 {} 邮箱为空，跳过发送", applicant.getUserName());
            return;
        }

        String subject = "【流程挂起通知】您的流程已暂停";
        String content = String.format("尊敬的 %s，您提交的流程【%s】已被挂起，如有疑问请联系管理员。",
                applicant.getNickName(), processName);

        sendEmailWithLog(applicant.getEmail(), applicant.getNickName(), subject, content, processName);
    }

    /**
     * 发送邮件并记录日志
     * Mock 发送：暂时只记录日志，不真实发送
     *
     * @param recipient 收件人邮箱
     * @param recipientName 收件人姓名
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param processName 流程名称
     */
    private void sendEmailWithLog(String recipient, String recipientName, String subject,
                                   String content, String processName)
    {
        // 创建邮件日志（待发送状态）
        SysEmailLog emailLog = new SysEmailLog();
        emailLog.setEmailType(EMAIL_TYPE_SUSPEND);
        emailLog.setRecipient(recipient);
        emailLog.setRecipientName(recipientName);
        emailLog.setSubject(subject);
        emailLog.setContent(content);
        emailLog.setProcessName(processName);
        emailLog.setStatus(STATUS_PENDING);

        // 插入日志
        emailLogMapper.insertEmailLog(emailLog);

        try
        {
            // Mock 发送：暂时只记录日志，不真实发送
            // TODO: 配置 SMTP 后，使用 JavaMailSender 发送真实邮件
            log.info("[Mock发送] 邮件已发送: 收件人={}, 主题={}", recipient, subject);

            // 模拟发送延迟
            Thread.sleep(100);

            // 更新日志状态为已发送
            emailLogMapper.updateEmailLogStatus(emailLog.getId(), STATUS_SENT, null, new Date());
        }
        catch (Exception e)
        {
            log.error("邮件发送失败: 收件人={}, 错误={}", recipient, e.getMessage());

            // 更新日志状态为发送失败
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500)
            {
                errorMsg = errorMsg.substring(0, 500);
            }
            emailLogMapper.updateEmailLogStatus(emailLog.getId(), STATUS_FAILED, errorMsg, new Date());
        }
    }
}
