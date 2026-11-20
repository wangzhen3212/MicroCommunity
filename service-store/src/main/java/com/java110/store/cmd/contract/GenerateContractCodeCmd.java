package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractDto;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 生成合同编号：HC-{yyyy}-{ZL|CG|WB}-{4位序号}
 */
@Java110Cmd(serviceCode = "contract.generateContractCode")
public class GenerateContractCodeCmd extends Cmd {

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String category = reqJson.getString("category");
        if (StringUtil.isEmpty(category)) {
            category = ContractDto.CATEGORY_PURCHASE;
        }
        String prefix = categoryPrefix(category);
        String year = DateUtil.getNow("yyyy");
        String codeHead = "HC-" + year + "-" + prefix + "-";

        ContractDto contractDto = new ContractDto();
        contractDto.setStoreId(storeId);
        contractDto.setContractCodeLike(codeHead);
        int count = contractInnerServiceSMOImpl.queryContractsCount(contractDto);

        Map<String, Object> data = new HashMap<>();
        data.put("contractCode", codeHead + String.format("%04d", count + 1));
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    private String categoryPrefix(String category) {
        if (ContractDto.CATEGORY_RENT.equals(category)) {
            return "ZL";
        }
        if (ContractDto.CATEGORY_MAINTAIN.equals(category)) {
            return "WB";
        }
        return "CG";
    }
}
