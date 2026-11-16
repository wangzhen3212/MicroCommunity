package com.java110.store.bmo.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.dto.contract.ContractAuditConfigDto;
import com.java110.dto.contract.ContractAuditDto;

import java.util.List;
import java.util.Map;

/**
 * 合同两级审批业务接口（提交 / 审批 / 撤回 / 审核人配置校验）
 */
public interface IContractAuditBMO {

    /**
     * 提交审批：写审批实例 + 两条环节记录，单据置为待审核（state=11）
     *
     * @param reqJson     请求报文（auditType / bizId / submitStaffId / submitStaffName / legalAuditorId / leaderAuditorId）
     * @param storeId     商户ID
     * @return 审批实例
     */
    ContractAuditDto submitAudit(JSONObject reqJson, String storeId);

    /**
     * 审批通过 / 驳回：校验当前环节审核人，推进 current_step 或置为驳回
     *
     * @param reqJson  请求报文（auditId / result / opinion / auditorId）
     * @param storeId  商户ID
     * @param userId   当前登录用户
     */
    void audit(JSONObject reqJson, String storeId, String userId);

    /**
     * 提交人撤回（仅 current_step=1 且尚无法务处理结果）
     *
     * @param auditId 审批实例ID
     * @param storeId 商户ID
     * @param userId  当前登录用户
     */
    void cancelAudit(String auditId, String storeId, String userId);

    /**
     * 查询审核人配置（法务校对 / 领导审核）
     *
     * @param storeId 商户ID
     * @return 配置列表
     */
    List<ContractAuditConfigDto> queryAuditorConfigs(String storeId);

    /**
     * 查询单据在途审批实例
     *
     * @param auditType 单据类型
     * @param bizId     业务ID
     * @return 审批实例，无则返回 null
     */
    ContractAuditDto queryDoingAudit(String auditType, String bizId);

    /**
     * 组装审批实例与环节记录（待我审批 / 已通过 / 已驳回 列表共用）
     *
     * @param auditDtos 审批实例列表
     * @return 每项包含 audit（实例）与 records（环节记录）
     */
    List<Map> wrapAuditRecords(List<ContractAuditDto> auditDtos);
}
