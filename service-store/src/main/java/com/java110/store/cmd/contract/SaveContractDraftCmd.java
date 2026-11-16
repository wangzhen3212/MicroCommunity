package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.contract.ContractDto;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.po.contract.ContractPo;
import com.java110.store.bmo.contract.IContractContentBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 保存合同草稿（state=10，不发起审批）
 */
@Java110Cmd(serviceCode = "contract.saveContractDraft")
public class SaveContractDraftCmd extends Cmd {

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IContractContentBMO contractContentBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String contractId = reqJson.getString("contractId");
        ContractPo contractPo = BeanConvertUtil.covertBean(reqJson, ContractPo.class);
        contractPo.setStoreId(storeId);
        contractPo.setState(ContractDto.STATE_DRAFT);
        contractPo.setStatusCd("0");

        String contractCode = contractPo.getContractCode();
        if (StringUtil.isEmpty(contractId)) {
            if (StringUtil.isEmpty(contractCode)) {
                throw new CmdException("请先生成合同编号");
            }
            ContractDto contractDto = new ContractDto();
            contractDto.setStoreId(storeId);
            contractDto.setContractCode(contractCode);
            List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
            if (contractDtos != null && !contractDtos.isEmpty()) {
                throw new CmdException("合同编号[" + contractCode + "]已存在");
            }
            contractId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_contractId);
            contractPo.setContractId(contractId);
            if (StringUtil.isEmpty(contractPo.getStartUserId())) {
                contractPo.setStartUserId(CmdContextUtils.getUserId(context));
            }
            int flag = contractInnerServiceSMOImpl.saveContract(contractPo);
            if (flag < 1) {
                throw new CmdException("保存合同草稿失败");
            }
        } else {
            contractPo.setContractId(contractId);
            int flag = contractInnerServiceSMOImpl.updateContract(contractPo);
            if (flag < 1) {
                throw new CmdException("保存合同草稿失败");
            }
        }

        if (reqJson.containsKey("content")) {
            contractContentBMOImpl.saveOrUpdateContent(contractId, contractPo.getTemplateId(), reqJson.getString("content"));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("contractId", contractId);
        data.put("contractCode", contractCode);
        context.setResponseEntity(ResultVo.createResponseEntity(data));
    }
}
