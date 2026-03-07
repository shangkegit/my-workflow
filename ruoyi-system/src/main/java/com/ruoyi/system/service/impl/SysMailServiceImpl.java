package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.service.ISysMailService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 邮件服务实现
 * 
 * @author ruoyi
 */
@Service
public class SysMailServiceImpl implements ISysMailService
{
    private static final Logger log = LoggerFactory.getLogger(SysMailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ISysUserService userService;

    @Value("${spring.mail.username:}")
    private String from;

    /**
     * 发送简单邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    @Override
    public void sendSimpleMail(String to, String subject, String content)
    {
        try
        {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("邮件发送成功: 收件人={}, 主题={}", to, subject);
        }
        catch (Exception e)
        {
            log.error("邮件发送失败: 收件人={}, 主题={}, 错误={}", to, subject, e.getMessage());
        }
    }

    /**
     * 发送流程停用通知邮件给管理员
     * 
     * @param processName 流程名称
     * @param processKey 流程Key
     * @param operator 操作人
     */
    @Override
    public void sendProcessSuspendNotification(String processName, String processKey, String operator)
    {
        // 获取所有管理员用户（角色ID为1的用户）
        SysUser queryUser = new SysUser();
        queryUser.setRoleId(1L);  // 设置管理员角色ID
        List<SysUser> adminUsers = userService.selectAllocatedList(queryUser);
        
        // 过滤出有邮箱的管理员
        List<String> adminEmails = adminUsers.stream()
                .filter(user -> user.getEmail() != null && !user.getEmail().isEmpty())
                .map(SysUser::getEmail)
                .collect(Collectors.toList());

        if (adminEmails.isEmpty())
        {
            log.warn("未找到管理员邮箱，无法发送流程停用通知");
            return;
        }

        String subject = "【工作流通知】流程已停用";
        String content = String.format(
                "尊敬的管理员：\n\n" +
                "流程已被停用，详情如下：\n" +
                "- 流程名称：%s\n" +
                "- 流程Key：%s\n" +
                "- 操作人：%s\n" +
                "- 操作时间：%s\n\n" +
                "请及时处理。\n\n" +
                "此邮件由系统自动发送，请勿回复。",
                processName != null ? processName : "未知",
                processKey != null ? processKey : "未知",
                operator != null ? operator : "系统",
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
        );

        // 发送给所有管理员
        for (String email : adminEmails)
        {
            sendSimpleMail(email, subject, content);
        }
        
        log.info("流程停用通知已发送给 {} 位管理员", adminEmails.size());
    }
}
