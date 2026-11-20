package com.java110.store.bmo.contract;

import com.java110.dto.contract.ContractContentDto;

/**
 * 合同正文读写业务接口（同事务写 contract_content，按 contract_id upsert）
 */
public interface IContractContentBMO {

    /**
     * 保存或更新合同正文，content_version 自增
     *
     * @param contractId      合同ID
     * @param templateId      模板ID
     * @param content         渲染后的正文HTML（详情/打印/预览使用）
     * @param templateContent 含变量占位符的正文原文（编辑回显使用）
     */
    void saveOrUpdateContent(String contractId, String templateId, String content, String templateContent);

    /**
     * 按合同查询正文
     *
     * @param contractId 合同ID
     * @return 正文，未配置返回 null
     */
    ContractContentDto getContent(String contractId);
}
