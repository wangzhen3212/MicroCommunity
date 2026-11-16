package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractTypeTemplateDto;
import com.java110.intf.store.IContractTypeTemplateInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 合同模板卡片列表（含分类、版本、使用次数、正文摘要）
 */
@Java110Cmd(serviceCode = "contract.listContractTemplates")
public class ListContractTemplatesCmd extends Cmd {

    private static final int SUMMARY_LENGTH = 120;

    @Autowired
    private IContractTypeTemplateInnerServiceSMO contractTypeTemplateInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson);
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);

        ContractTypeTemplateDto templateDto = BeanConvertUtil.covertBean(reqJson, ContractTypeTemplateDto.class);
        templateDto.setStoreId(storeId);
        templateDto.setStatusCd("0");
        int count = contractTypeTemplateInnerServiceSMOImpl.queryContractTypeTemplatesCount(templateDto);

        List<ContractTypeTemplateDto> templateDtos = new ArrayList<>();
        if (count > 0) {
            templateDtos = contractTypeTemplateInnerServiceSMOImpl.queryContractTypeTemplates(templateDto);
            for (ContractTypeTemplateDto dto : templateDtos) {
                dto.setContext(summary(dto.getContext()));
            }
        }
        context.setResponseEntity(ResultVo.createResponseEntity(
                (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, templateDtos));
    }

    private String summary(String context) {
        if (context == null) {
            return "";
        }
        String text = context.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        if (text.length() <= SUMMARY_LENGTH) {
            return text;
        }
        return text.substring(0, SUMMARY_LENGTH) + "...";
    }
}
