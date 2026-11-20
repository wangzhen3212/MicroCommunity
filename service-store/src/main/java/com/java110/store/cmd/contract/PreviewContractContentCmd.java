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
import com.java110.utils.util.Assert;
import com.java110.utils.util.ContractContentUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同正文变量渲染（模板 + 表单变量 + 房屋清单），仅返回渲染结果不入库
 */
@Java110Cmd(serviceCode = "contract.previewContractContent")
public class PreviewContractContentCmd extends Cmd {

    @Autowired
    private IContractTypeTemplateInnerServiceSMO contractTypeTemplateInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String content = reqJson.getString("content");
        String templateId = reqJson.getString("templateId");

        if (StringUtil.isEmpty(content) && !StringUtil.isEmpty(templateId)) {
            content = queryTemplateContent(templateId, storeId, reqJson.getString("communityId"));
        }
        if (StringUtil.isEmpty(content)) {
            throw new CmdException("合同正文为空，无法预览");
        }

        JSONObject variables = reqJson.getJSONObject("variables");
        Map<String, Object> data = new HashMap<>();
        data.put("content", ContractContentUtil.render(content, variables));
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    private String queryTemplateContent(String templateId, String storeId, String communityId) {
        ContractTypeTemplateDto templateDto = new ContractTypeTemplateDto();
        templateDto.setTemplateId(templateId);
        templateDto.setStoreId(storeId);
        templateDto.setCommunityId(communityId);
        templateDto.setStatusCd("0");
        List<ContractTypeTemplateDto> templateDtos = contractTypeTemplateInnerServiceSMOImpl.queryContractTypeTemplates(templateDto);
        if (templateDtos == null || templateDtos.isEmpty()) {
            return null;
        }
        return templateDtos.get(0).getContext();
    }
}
