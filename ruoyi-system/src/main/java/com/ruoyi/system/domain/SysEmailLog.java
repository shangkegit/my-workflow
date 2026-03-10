package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 邮件发送日志表 sys_email_log
 *
 * @author ruoyi
 */
public class SysEmailLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @Excel(name = "日志ID", cellType = ColumnType.NUMERIC)
    private Long id;

    /** 邮件类型（SUSPEND_NOTIFY） */
    @Excel(name = "邮件类型")
    private String emailType;

    /** 收件人邮箱 */
    @Excel(name = "收件人邮箱")
    private String recipient;

    /** 收件人姓名 */
    @Excel(name = "收件人姓名")
    private String recipientName;

    /** 邮件主题 */
    @Excel(name = "邮件主题")
    private String subject;

    /** 邮件内容 */
    private String content;

    /** 流程实例ID */
    @Excel(name = "流程实例ID")
    private String processId;

    /** 流程名称 */
    @Excel(name = "流程名称")
    private String processName;

    /** 状态（0待发送 1已发送 2发送失败） */
    @Excel(name = "状态", readConverterExp = "0=待发送,1=已发送,2=发送失败")
    private String status;

    /** 错误信息 */
    @Excel(name = "错误信息")
    private String errorMsg;

    /** 发送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发送时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getEmailType()
    {
        return emailType;
    }

    public void setEmailType(String emailType)
    {
        this.emailType = emailType;
    }

    public String getRecipient()
    {
        return recipient;
    }

    public void setRecipient(String recipient)
    {
        this.recipient = recipient;
    }

    public String getRecipientName()
    {
        return recipientName;
    }

    public void setRecipientName(String recipientName)
    {
        this.recipientName = recipientName;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getProcessId()
    {
        return processId;
    }

    public void setProcessId(String processId)
    {
        this.processId = processId;
    }

    public String getProcessName()
    {
        return processName;
    }

    public void setProcessName(String processName)
    {
        this.processName = processName;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    public Date getSendTime()
    {
        return sendTime;
    }

    public void setSendTime(Date sendTime)
    {
        this.sendTime = sendTime;
    }
}
