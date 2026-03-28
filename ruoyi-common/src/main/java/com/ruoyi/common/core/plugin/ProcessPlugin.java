package com.ruoyi.common.core.plugin;

import java.util.List;
import java.util.Map;

/**
 * 流程插件接口 - 所有流程插件必须实现此接口
 */
public interface ProcessPlugin {

    /** 获取流程类型标识 */
    String getProcessType();

    /** 获取流程名称 */
    String getProcessName();

    /** 获取表单数据（根据 businessKey） */
    Object getFormData(String businessKey);

    /** 提交表单（发起流程） */
    String submitForm(Map<String, Object> formData, String username);

    /** 获取待办列表数据 */
    List<?> getTodoList(String username);

    /** 初始化插件（可选） */
    default void initialize() {}

    /** 销毁插件（可选） */
    default void destroy() {}
}
