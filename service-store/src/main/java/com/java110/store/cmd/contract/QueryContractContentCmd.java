package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractContentDto;
import com.java110.store.bmo.contract.IContractContentBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 按合同查询正文（详情抽屉、编辑页、预览/打印按需拉取，列表接口不返回正文）
 */
@Java110Cmd(serviceCode = "contract.queryContractContent")
public class QueryContractContentCmd extends Cmd {

    @Autowired
    private IContractContentBMO contractContentBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "contractId", "请求报文中未包含合同ID");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ContractContentDto contentDto = contractContentBMOImpl.getContent(reqJson.getString("contractId"));
        Map<String, Object> data = new HashMap<>();
        if (contentDto == null) {
            data.put("contentId", "");
            data.put("contractId", reqJson.getString("contractId"));
            data.put("templateId", "");
            data.put("content", "");
            data.put("contentVersion", 0);
        } else {
            data.put("contentId", contentDto.getContentId());
            data.put("contractId", contentDto.getContractId());
            data.put("templateId", contentDto.getTemplateId());
            data.put("content", contentDto.getContent());
            data.put("contentVersion", contentDto.getContentVersion());
        }
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}
