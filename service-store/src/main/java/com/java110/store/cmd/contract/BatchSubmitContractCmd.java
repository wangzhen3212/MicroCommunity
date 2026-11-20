package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractDto;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.po.contract.ContractPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量提交合同审核（草稿/已驳回 -> 法务校对，逐条校验并返回成功/失败明细）
 */
@Java110Cmd(serviceCode = "contract.batchSubmitContract")
public class BatchSubmitContractCmd extends Cmd {

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        if (!reqJson.containsKey("contractIds")) {
            throw new CmdException("请求报文中未包含合同");
        }
    }

    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        JSONArray contractIds = reqJson.getJSONArray("contractIds");
        if (contractIds == null || contractIds.isEmpty()) {
            throw new CmdException("请求报文中未包含合同");
        }

        int successCount = 0;
        List<Map<String, Object>> failList = new ArrayList<>();
        for (int i = 0; i < contractIds.size(); i++) {
            String contractId = contractIds.getString(i);
            try {
                submitContract(contractId, storeId);
                successCount++;
            } catch (Exception e) {
                failList.add(buildFail(contractId, e.getMessage()));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("successCount", successCount);
        data.put("failCount", failList.size());
        data.put("failList", failList);
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }

    /**
     * 单条提交：草稿/已驳回可提交，直接置为法务校对（state=11）
     */
    private void submitContract(String contractId, String storeId) {
        ContractDto contractDto = new ContractDto();
        contractDto.setContractId(contractId);
        contractDto.setStoreId(storeId);
        contractDto.setStatusCd("0");
        List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
        if (contractDtos == null || contractDtos.isEmpty()) {
            throw new CmdException("合同不存在");
        }
        ContractDto contract = contractDtos.get(0);
        if (!ContractDto.STATE_DRAFT.equals(contract.getState()) && !ContractDto.STATE_FAIL.equals(contract.getState())) {
            throw new CmdException("仅草稿或已驳回的合同可提交审核");
        }

        ContractPo contractPo = new ContractPo();
        contractPo.setContractId(contractId);
        contractPo.setState(ContractDto.STATE_AUDIT);
        contractPo.setStatusCd("0");
        int flag = contractInnerServiceSMOImpl.updateContract(contractPo);
        if (flag < 1) {
            throw new CmdException("提交审核失败");
        }
    }

    private Map<String, Object> buildFail(String contractId, String msg) {
        Map<String, Object> fail = new HashMap<>();
        fail.put("contractId", contractId);
        fail.put("msg", StringUtil.isEmpty(msg) ? "提交失败" : msg);
        return fail;
    }
}
