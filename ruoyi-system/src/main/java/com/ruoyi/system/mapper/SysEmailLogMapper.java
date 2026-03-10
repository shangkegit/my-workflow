package com.ruoyi.system.mapper;

import java.util.Date;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.SysEmailLog;

/**
 * 邮件发送日志 数据层
 *
 * @author ruoyi
 */
public interface SysEmailLogMapper
{
    /**
     * 新增邮件日志
     *
     * @param log 邮件日志对象
     * @return 影响行数
     */
    int insertEmailLog(SysEmailLog log);

    /**
     * 更新邮件日志状态
     *
     * @param id 日志ID
     * @param status 状态（0待发送 1已发送 2发送失败）
     * @param errorMsg 错误信息
     * @param sendTime 发送时间
     * @return 影响行数
     */
    int updateEmailLogStatus(@Param("id") Long id, @Param("status") String status,
                             @Param("errorMsg") String errorMsg, @Param("sendTime") Date sendTime);
}
