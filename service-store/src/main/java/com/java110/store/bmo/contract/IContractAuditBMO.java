package com.java110.store.bmo.contract;

import com.alibaba.fastjson.JSONObject;

import java.util.Map;

/**
 * 合同审批业务接口（状态机：法务校对 11 -> 领导审批 56 -> 完成 22，驳回 33）
 */
public interface IContractAuditBMO {

    /**
     * 审核通过 / 驳回：校验当前状态与按钮权限，写审批记录并推进单据状态
     *
     * @param reqJson     请求报文（bizType 1 合同 / 2 变更，bizId，result 1 通过 / 2 驳回，opinion）
     * @param storeId     商户ID
     * @param communityId 小区ID
     * @param userId      当前登录用户
     */
    void audit(JSONObject reqJson, String storeId, String communityId, String userId);

    /**
     * 查询单据审批情况（合同状态 + 审批记录时间线）
     *
     * @param bizType     单据类型 1 合同 / 2 变更
     * @param bizId       合同ID / 变更单ID
     * @param storeId     商户ID
     * @param communityId 小区ID
     * @return state / stateName / currentStep / currentStepName / records
     */
    Map<String, Object> queryAuditFlow(String bizType, String bizId, String storeId, String communityId);

    /**
     * 批量查询单据最近一次审批记录（列表页「最近审批」列）
     *
     * @param bizType     单据类型 1 合同 / 2 变更
     * @param bizIds      单据ID集合
     * @param storeId     商户ID
     * @param communityId 小区ID
     * @return key = 单据ID，value = 最近一次审批记录
     */
    Map<String, Map<String, Object>> queryLatestAuditLogs(String bizType, String[] bizIds, String storeId, String communityId);
}
