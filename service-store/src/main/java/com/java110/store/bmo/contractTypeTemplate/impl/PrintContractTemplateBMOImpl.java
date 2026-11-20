package com.java110.store.bmo.contractTypeTemplate.impl;

import com.java110.dto.contract.ContractContentDto;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.contract.ContractAttrDto;
import com.java110.dto.contract.ContractTypeTemplateDto;
import com.java110.dto.mapping.Mapping;
import com.java110.intf.store.IContractAttrInnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.intf.store.IContractTypeTemplateInnerServiceSMO;

import com.java110.store.bmo.contract.IContractContentBMO;
import com.java110.store.bmo.contractTypeTemplate.IPrintContractTemplateBMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ContractContentUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同打印：打印内容以库中合同正文（contract_content.content）为准，
 * 依次回退 render(template_content)、模板 context
 */
@Service("printContractTemplateBMOImpl")
public class PrintContractTemplateBMOImpl implements IPrintContractTemplateBMO {

    /**
     * 正文来源：库中正文
     */
    public static final String CONTENT_SOURCE_CONTENT = "content";
    /**
     * 正文来源：含变量原文重渲染
     */
    public static final String CONTENT_SOURCE_RENDERED = "rendered";
    /**
     * 正文来源：模板兜底渲染
     */
    public static final String CONTENT_SOURCE_TEMPLATE = "template";

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IContractAttrInnerServiceSMO contractAttrInnerServiceSMOImpl;

    @Autowired
    private IContractTypeTemplateInnerServiceSMO contractTypeTemplateInnerServiceSMOImpl;

    @Autowired
    private IContractContentBMO contractContentBMOImpl;

    //域
    public static final String STORE_CONTRACT = "STORE.CONTRACT";

    /**
     * @param contractTypeTemplateDto
     * @return 订单服务能够接受的报文
     */
    public ResponseEntity<String> get(ContractTypeTemplateDto contractTypeTemplateDto, ContractDto contractDto) {

        int count = contractInnerServiceSMOImpl.queryContractsCount(contractDto);
        Map<String, Object> result = new HashMap<String, Object>();
        List<ContractDto> contractDtos = null;
        if (count > 0) {
            contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
            refreshAttr(contractDtos);
        } else {
            contractDtos = new ArrayList<>();
        }
        result.put("contract", contractDtos);

        ContractDto printContractDto = contractDtos.isEmpty() ? null : contractDtos.get(0);
        applyContractTemplateId(contractTypeTemplateDto, printContractDto);

        int count2 = contractTypeTemplateInnerServiceSMOImpl.queryContractTypeTemplatesCount(contractTypeTemplateDto);
        List<ContractTypeTemplateDto> contractTypeTemplateDtos = null;
        if (count2 > 0) {
            contractTypeTemplateDtos = contractTypeTemplateInnerServiceSMOImpl.queryContractTypeTemplates(contractTypeTemplateDto);
        } else {
            contractTypeTemplateDtos = new ArrayList<>();
        }
        result.put("contractTypeTemplate", contractTypeTemplateDtos);
        List<Mapping> baseRepalce = MappingCache.getValueByDomain(STORE_CONTRACT);

        result.put("baseRepalce", baseRepalce);

        Map<String, Object> printInfo = buildPrintInfo(printContractDto, contractTypeTemplateDtos, baseRepalce);
        result.putAll(printInfo);

        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) contractTypeTemplateDto.getRow()), count, result);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        return responseEntity;
    }

    /**
     * 模板选择：入参 templateId > contract.templateId > 首条
     */
    private void applyContractTemplateId(ContractTypeTemplateDto contractTypeTemplateDto, ContractDto contractDto) {
        if (!StringUtil.isEmpty(contractTypeTemplateDto.getTemplateId()) || contractDto == null) {
            return;
        }
        if (!StringUtil.isEmpty(contractDto.getTemplateId())) {
            contractTypeTemplateDto.setTemplateId(contractDto.getTemplateId());
        }
    }

    /**
     * 打印正文：①库中正文 ②含变量原文重渲染 ③模板 context 渲染（兼容旧 #中文名#）
     */
    private Map<String, Object> buildPrintInfo(ContractDto contractDto,
                                               List<ContractTypeTemplateDto> contractTypeTemplateDtos,
                                               List<Mapping> baseRepalce) {
        Map<String, Object> printInfo = new HashMap<>();
        printInfo.put("printContent", "");
        printInfo.put("contentSource", "");
        printInfo.put("contentVersion", 0);
        if (contractDto == null) {
            return printInfo;
        }
        String contractId = contractDto.getContractId();
        ContractContentDto contentDto = contractContentBMOImpl.getContent(contractId);
        int contentVersion = contentDto == null ? 0 : contentDto.getContentVersion();
        printInfo.put("contentVersion", contentVersion);

        if (contentDto != null && !StringUtil.isEmpty(contentDto.getContent())) {
            printInfo.put("printContent", contentDto.getContent());
            printInfo.put("contentSource", CONTENT_SOURCE_CONTENT);
            return printInfo;
        }

        String renderedContent = contractContentBMOImpl.getPrintContent(contractId, contractDto.getStoreId());
        if (!StringUtil.isEmpty(renderedContent)) {
            printInfo.put("printContent", renderedContent);
            printInfo.put("contentSource", CONTENT_SOURCE_RENDERED);
            return printInfo;
        }

        if (contractTypeTemplateDtos.isEmpty() || StringUtil.isEmpty(contractTypeTemplateDtos.get(0).getContext())) {
            return printInfo;
        }
        Map<String, Object> variables = contractContentBMOImpl.buildVariables(contractDto);
        String templateContent = ContractContentUtil.render(contractTypeTemplateDtos.get(0).getContext(), variables);
        printInfo.put("printContent", applyLegacyPlaceholders(templateContent, baseRepalce, contractDto));
        printInfo.put("contentSource", CONTENT_SOURCE_TEMPLATE);
        return printInfo;
    }

    /**
     * 兼容旧模板占位符（#中文名#），仅兜底路径使用
     */
    private String applyLegacyPlaceholders(String content, List<Mapping> baseRepalce, ContractDto contractDto) {
        if (StringUtil.isEmpty(content) || baseRepalce == null || baseRepalce.isEmpty()) {
            return content;
        }
        Map<String, Object> contractMap = BeanConvertUtil.beanCovertMap(contractDto);
        String result = content;
        for (Mapping mapping : baseRepalce) {
            if (StringUtil.isEmpty(mapping.getName()) || StringUtil.isEmpty(mapping.getKey())) {
                continue;
            }
            if (!contractMap.containsKey(mapping.getKey())) {
                continue;
            }
            Object value = contractMap.get(mapping.getKey());
            result = result.replace("#" + mapping.getName() + "#", value == null ? "" : value.toString());
        }
        return result;
    }

    /**
     * 属性属性
     *
     * @param contractDtos
     */
    private void refreshAttr(List<ContractDto> contractDtos) {
        List<String> contractIds = new ArrayList<>();
        for (ContractDto contractDto : contractDtos) {
            contractIds.add(contractDto.getContractId());
        }


        if (contractIds.size() < 1) {
            return;
        }

        ContractAttrDto contractAttrDto = new ContractAttrDto();
        contractAttrDto.setContractIds(contractIds.toArray(new String[contractIds.size()]));
        List<ContractAttrDto> contractAttrDtos = contractAttrInnerServiceSMOImpl.queryContractAttrs(contractAttrDto);

        List<ContractAttrDto> attrs = null;
        for (ContractDto contractDto : contractDtos) {
            attrs = new ArrayList<>();
            for (ContractAttrDto tmpContractAttrDto : contractAttrDtos) {
                if (contractDto.getContractId().equals(tmpContractAttrDto.getContractId())) {
                    attrs.add(tmpContractAttrDto);
                }
            }
            contractDto.setAttrs(attrs);
        }


    }
}
