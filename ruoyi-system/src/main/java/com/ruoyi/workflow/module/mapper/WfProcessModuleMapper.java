package com.ruoyi.workflow.module.mapper;

import com.ruoyi.workflow.module.domain.WfProcessModule;
import java.util.List;

/**
 * 流程模块Mapper接口
 * 
 * @author OpenClaw Agent
 * @date 2026-03-03
 */
public interface WfProcessModuleMapper {

    /**
     * 查询流程模块
     */
    public WfProcessModule selectWfProcessModuleById(Long id);

    /**
     * 根据流程键查询模块
     */
    public WfProcessModule selectWfProcessModuleByProcessKey(String processKey);

    /**
     * 查询流程模块列表
     */
    public List<WfProcessModule> selectWfProcessModuleList(WfProcessModule wfProcessModule);

    /**
     * 新增流程模块
     */
    public int insertWfProcessModule(WfProcessModule wfProcessModule);

    /**
     * 修改流程模块
     */
    public int updateWfProcessModule(WfProcessModule wfProcessModule);

    /**
     * 删除流程模块
     */
    public int deleteWfProcessModuleById(Long id);

    /**
     * 根据流程键删除模块
     */
    public int deleteWfProcessModuleByProcessKey(String processKey);

    /**
     * 批量删除流程模块
     */
    public int deleteWfProcessModuleByIds(Long[] ids);
}
