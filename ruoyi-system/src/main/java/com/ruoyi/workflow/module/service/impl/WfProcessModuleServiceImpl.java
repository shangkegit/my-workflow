package com.ruoyi.workflow.module.service.impl;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.workflow.module.domain.WfProcessModule;
import com.ruoyi.workflow.module.mapper.WfProcessModuleMapper;
import com.ruoyi.workflow.module.service.IWfProcessModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程模块Service业务层处理
 * 
 * @author OpenClaw Agent
 * @date 2026-03-03
 */
@Service
public class WfProcessModuleServiceImpl implements IWfProcessModuleService {

    @Autowired
    private WfProcessModuleMapper wfProcessModuleMapper;

    /**
     * 查询流程模块
     */
    @Override
    public WfProcessModule selectWfProcessModuleById(Long id) {
        return wfProcessModuleMapper.selectWfProcessModuleById(id);
    }

    /**
     * 根据流程键查询模块
     */
    @Override
    public WfProcessModule selectWfProcessModuleByProcessKey(String processKey) {
        return wfProcessModuleMapper.selectWfProcessModuleByProcessKey(processKey);
    }

    /**
     * 查询流程模块列表
     */
    @Override
    public List<WfProcessModule> selectWfProcessModuleList(WfProcessModule wfProcessModule) {
        return wfProcessModuleMapper.selectWfProcessModuleList(wfProcessModule);
    }

    /**
     * 新增流程模块
     */
    @Override
    public int insertWfProcessModule(WfProcessModule wfProcessModule) {
        wfProcessModule.setDeployTime(DateUtils.getNowDate());
        return wfProcessModuleMapper.insertWfProcessModule(wfProcessModule);
    }

    /**
     * 修改流程模块
     */
    @Override
    public int updateWfProcessModule(WfProcessModule wfProcessModule) {
        return wfProcessModuleMapper.updateWfProcessModule(wfProcessModule);
    }

    /**
     * 批量删除流程模块
     */
    @Override
    public int deleteWfProcessModuleByIds(Long[] ids) {
        return wfProcessModuleMapper.deleteWfProcessModuleByIds(ids);
    }

    /**
     * 删除流程模块信息
     */
    @Override
    public int deleteWfProcessModuleById(Long id) {
        return wfProcessModuleMapper.deleteWfProcessModuleById(id);
    }
}
