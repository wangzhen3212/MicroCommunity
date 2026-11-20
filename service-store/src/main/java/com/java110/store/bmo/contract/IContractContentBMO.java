package com.java110.store.bmo.contract;

import com.java110.dto.contract.ContractContentDto;
import com.java110.dto.contract.ContractDto;

import java.util.Map;

/**
 * 合同正文读写业务接口（同事务写 contract_content，按 contract_id upsert）
 */
public interface IContractContentBMO {

    /**
     * 保存或更新合同正文，content_version 自增
     *
     * @param contractId      合同ID
     * @param communityId     小区ID（子表继承主表值）
     * @param templateId      模板ID
     * @param content         渲染后的正文HTML（详情/打印/预览使用）
     * @param templateContent 含变量占位符的正文原文（编辑回显使用）
     */
    void saveOrUpdateContent(String contractId, String communityId, String templateId, String content, String templateContent);

    /**
     * 按合同查询正文
     *
     * @param contractId 合同ID
     * @return 正文，未配置返回 null
     */
    ContractContentDto getContent(String contractId);

    /**
     * 按合同 + 小区查询正文
     *
     * @param contractId  合同ID
     * @param communityId 小区ID
     * @return 正文，未配置返回 null
     */
    ContractContentDto getContent(String contractId, String communityId);

    /**
     * 构建正文渲染变量（合同字段 + 金额大写 + 关联房屋）
     *
     * @param contractDto 合同
     * @return 变量表
     */
    Map<String, Object> buildVariables(ContractDto contractDto);

    /**
     * 打印正文：库中正文优先，其次用含变量原文重渲染
     *
     * @param contractId  合同ID
     * @param storeId     商户ID
     * @param communityId 小区ID
     * @return 可打印正文，均无返回 null
     */
    String getPrintContent(String contractId, String storeId, String communityId);

    /**
     * 按最新合同字段重渲染含变量原文并写回库中正文（content_version 自增）
     *
     * @param contractId  合同ID
     * @param storeId     商户ID
     * @param communityId 小区ID
     */
    void refreshContent(String contractId, String storeId, String communityId);
}
