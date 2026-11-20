package com.java110.store.bmo.contract.impl;

import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.contract.ContractContentDto;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.contract.ContractRoomDto;
import com.java110.intf.store.IContractContentInnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.intf.store.IContractRoomInnerServiceSMO;
import com.java110.po.contract.ContractContentPo;
import com.java110.store.bmo.contract.IContractContentBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.ContractContentUtil;
import com.java110.utils.util.Money2ChineseUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同正文读写实现（正文独立表，按 contract_id upsert）
 */
@Service("contractContentBMOImpl")
public class ContractContentBMOImpl implements IContractContentBMO {

    private static final String PREFIX_CONTENT_ID = "94";

    private static Logger logger = LoggerFactory.getLogger(ContractContentBMOImpl.class);

    @Autowired
    private IContractContentInnerServiceSMO contractContentInnerServiceSMOImpl;

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IContractRoomInnerServiceSMO contractRoomInnerServiceSMOImpl;

    @Override
    public void saveOrUpdateContent(String contractId, String communityId, String templateId, String content, String templateContent) {
        if (StringUtil.isEmpty(contractId)) {
            throw new CmdException("保存合同正文失败，合同ID为空");
        }
        ContractContentDto contentDto = getContent(contractId);
        if (contentDto == null) {
            ContractContentPo contentPo = new ContractContentPo();
            contentPo.setContentId(GenerateCodeFactory.getGeneratorId(PREFIX_CONTENT_ID));
            contentPo.setContractId(contractId);
            contentPo.setCommunityId(communityId);
            contentPo.setTemplateId(templateId);
            contentPo.setContent(content == null ? "" : content);
            contentPo.setTemplateContent(templateContent == null ? "" : templateContent);
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
        contentPo.setCommunityId(StringUtil.isEmpty(communityId) ? contentDto.getCommunityId() : communityId);
        contentPo.setTemplateId(templateId);
        contentPo.setContent(content == null ? "" : content);
        contentPo.setTemplateContent(templateContent == null ? "" : templateContent);
        contentPo.setStatusCd("0");
        int flag = contractContentInnerServiceSMOImpl.updateContractContent(contentPo);
        if (flag < 1) {
            throw new CmdException("更新合同正文失败");
        }
    }

    @Override
    public ContractContentDto getContent(String contractId) {
        return getContent(contractId, null);
    }

    @Override
    public ContractContentDto getContent(String contractId, String communityId) {
        if (StringUtil.isEmpty(contractId)) {
            return null;
        }
        ContractContentDto contentDto = new ContractContentDto();
        contentDto.setContractId(contractId);
        contentDto.setCommunityId(communityId);
        contentDto.setStatusCd("0");
        List<ContractContentDto> contentDtos = contractContentInnerServiceSMOImpl.queryContractContents(contentDto);
        if (contentDtos == null || contentDtos.isEmpty()) {
            return null;
        }
        return contentDtos.get(0);
    }

    @Override
    public Map<String, Object> buildVariables(ContractDto contractDto) {
        Map<String, Object> variables = new HashMap<>();
        if (contractDto == null) {
            return variables;
        }
        variables.put("contractName", contractDto.getContractName());
        variables.put("contractCode", contractDto.getContractCode());
        variables.put("amount", contractDto.getAmount());
        variables.put("amountText", amountToChinese(contractDto.getAmount()));
        variables.put("startTime", contractDto.getStartTime());
        variables.put("endTime", contractDto.getEndTime());
        variables.put("partyA", contractDto.getPartyA());
        variables.put("aContacts", contractDto.getaContacts());
        variables.put("aLink", contractDto.getaLink());
        variables.put("partyB", contractDto.getPartyB());
        variables.put("bContacts", contractDto.getbContacts());
        variables.put("bLink", contractDto.getbLink());
        variables.put("operator", contractDto.getOperator());
        variables.put("operatorLink", contractDto.getOperatorLink());
        variables.put("signingTime", contractDto.getSigningTime());
        variables.put("houses", buildHouses(contractDto));
        return variables;
    }

    @Override
    public String getPrintContent(String contractId, String storeId, String communityId) {
        ContractContentDto contentDto = getContent(contractId, communityId);
        if (contentDto == null) {
            return null;
        }
        if (!StringUtil.isEmpty(contentDto.getContent())) {
            return contentDto.getContent();
        }
        if (StringUtil.isEmpty(contentDto.getTemplateContent())) {
            return null;
        }
        ContractDto contractDto = queryContract(contractId, storeId, communityId);
        if (contractDto == null) {
            return null;
        }
        return ContractContentUtil.render(contentDto.getTemplateContent(), buildVariables(contractDto));
    }

    @Override
    public void refreshContent(String contractId, String storeId, String communityId) {
        if (StringUtil.isEmpty(contractId)) {
            return;
        }
        ContractContentDto contentDto = getContent(contractId, communityId);
        if (contentDto == null) {
            logger.warn("合同 {} 无正文记录，跳过正文刷新", contractId);
            return;
        }
        if (StringUtil.isEmpty(contentDto.getTemplateContent())) {
            logger.warn("合同 {} 正文无变量原文，跳过正文刷新", contractId);
            return;
        }
        ContractDto contractDto = queryContract(contractId, storeId, communityId);
        if (contractDto == null) {
            logger.warn("合同 {} 查询不到合同信息，跳过正文刷新", contractId);
            return;
        }
        String renderedContent = ContractContentUtil.render(contentDto.getTemplateContent(), buildVariables(contractDto));
        String templateId = StringUtil.isEmpty(contentDto.getTemplateId())
                ? contractDto.getTemplateId() : contentDto.getTemplateId();
        saveOrUpdateContent(contractId, communityId, templateId, renderedContent, contentDto.getTemplateContent());
    }

    private ContractDto queryContract(String contractId, String storeId, String communityId) {
        ContractDto contractQueryDto = new ContractDto();
        contractQueryDto.setContractId(contractId);
        contractQueryDto.setStoreId(storeId);
        contractQueryDto.setCommunityId(communityId);
        contractQueryDto.setStatusCd("0");
        List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractQueryDto);
        if (contractDtos == null || contractDtos.isEmpty()) {
            return null;
        }
        return contractDtos.get(0);
    }

    /**
     * 关联房屋名称（contract_room.room_name 拼接）
     */
    private String buildHouses(ContractDto contractDto) {
        ContractRoomDto contractRoomDto = new ContractRoomDto();
        contractRoomDto.setContractId(contractDto.getContractId());
        contractRoomDto.setStoreId(contractDto.getStoreId());
        contractRoomDto.setCommunityId(contractDto.getCommunityId());
        contractRoomDto.setStatusCd("0");
        List<ContractRoomDto> contractRoomDtos = contractRoomInnerServiceSMOImpl.queryContractRooms(contractRoomDto);
        if (contractRoomDtos == null || contractRoomDtos.isEmpty()) {
            return "";
        }
        StringBuilder houses = new StringBuilder();
        for (ContractRoomDto roomDto : contractRoomDtos) {
            if (StringUtil.isEmpty(roomDto.getRoomName())) {
                continue;
            }
            if (houses.length() > 0) {
                houses.append("、");
            }
            houses.append(roomDto.getRoomName());
        }
        return houses.toString();
    }

    private String amountToChinese(String amount) {
        if (StringUtil.isEmpty(amount)) {
            return "";
        }
        try {
            return Money2ChineseUtil.toChineseChar(Double.parseDouble(amount));
        } catch (NumberFormatException e) {
            return "";
        }
    }
}
