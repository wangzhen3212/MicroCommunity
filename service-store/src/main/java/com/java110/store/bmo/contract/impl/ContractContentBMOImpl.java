package com.java110.store.bmo.contract.impl;

import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.contract.ContractContentDto;
import com.java110.intf.store.IContractContentInnerServiceSMO;
import com.java110.po.contract.ContractContentPo;
import com.java110.store.bmo.contract.IContractContentBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 合同正文读写实现（正文独立表，按 contract_id upsert）
 */
@Service("contractContentBMOImpl")
public class ContractContentBMOImpl implements IContractContentBMO {

    private static final String PREFIX_CONTENT_ID = "94";

    @Autowired
    private IContractContentInnerServiceSMO contractContentInnerServiceSMOImpl;

    @Override
    public void saveOrUpdateContent(String contractId, String templateId, String content) {
        if (StringUtil.isEmpty(contractId)) {
            throw new CmdException("保存合同正文失败，合同ID为空");
        }
        ContractContentDto contentDto = getContent(contractId);
        if (contentDto == null) {
            ContractContentPo contentPo = new ContractContentPo();
            contentPo.setContentId(GenerateCodeFactory.getGeneratorId(PREFIX_CONTENT_ID));
            contentPo.setContractId(contractId);
            contentPo.setTemplateId(templateId);
            contentPo.setContent(content == null ? "" : content);
            contentPo.setContentVersion("0");
            int flag = contractContentInnerServiceSMOImpl.saveContractContent(contentPo);
            if (flag < 1) {
                throw new CmdException("保存合同正文失败");
            }
            return;
        }
        ContractContentPo contentPo = new ContractContentPo();
        contentPo.setContentId(contentDto.getContentId());
        contentPo.setContractId(contractId);
        contentPo.setTemplateId(templateId);
        contentPo.setContent(content == null ? "" : content);
        contentPo.setStatusCd("0");
        int flag = contractContentInnerServiceSMOImpl.updateContractContent(contentPo);
        if (flag < 1) {
            throw new CmdException("更新合同正文失败");
        }
    }

    @Override
    public ContractContentDto getContent(String contractId) {
        if (StringUtil.isEmpty(contractId)) {
            return null;
        }
        ContractContentDto contentDto = new ContractContentDto();
        contentDto.setContractId(contractId);
        contentDto.setStatusCd("0");
        List<ContractContentDto> contentDtos = contractContentInnerServiceSMOImpl.queryContractContents(contentDto);
        if (contentDtos == null || contentDtos.isEmpty()) {
            return null;
        }
        return contentDtos.get(0);
    }
}
