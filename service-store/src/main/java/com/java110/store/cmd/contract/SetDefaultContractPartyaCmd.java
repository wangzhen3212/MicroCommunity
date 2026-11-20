package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.contract.ContractPartyaDto;
import com.java110.intf.store.IContractPartyaInnerServiceSMO;
import com.java110.po.contract.ContractPartyaPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 设置默认甲方主体（唯一性保证：先清除其它默认再设置当前）
 */
@Java110Cmd(serviceCode = "contract.setDefaultContractPartya")
public class SetDefaultContractPartyaCmd extends Cmd {

    @Autowired
    private IContractPartyaInnerServiceSMO contractPartyaInnerServiceSMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "partyaId", "请求报文中未包含甲方主体ID");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String partyaId = reqJson.getString("partyaId");
        String communityId = reqJson.getString("communityId");

        ContractPartyaDto partyaDto = new ContractPartyaDto();
        partyaDto.setPartyaId(partyaId);
        partyaDto.setStoreId(storeId);
        partyaDto.setCommunityId(communityId);
        partyaDto.setStatusCd("0");
        List<ContractPartyaDto> partyaDtos = contractPartyaInnerServiceSMOImpl.queryContractPartyas(partyaDto);
        Assert.listOnlyOne(partyaDtos, "未查询到甲方主体");

        ContractPartyaPo clearPo = new ContractPartyaPo();
        clearPo.setPartyaId(partyaId);
        clearPo.setStoreId(storeId);
        clearPo.setCommunityId(communityId);
        contractPartyaInnerServiceSMOImpl.clearDefaultContractPartya(clearPo);

        ContractPartyaPo partyaPo = new ContractPartyaPo();
        partyaPo.setPartyaId(partyaId);
        partyaPo.setStoreId(storeId);
        partyaPo.setCommunityId(communityId);
        partyaPo.setDefaultFlag("1");
        partyaPo.setStatusCd("0");
        int flag = contractPartyaInnerServiceSMOImpl.updateContractPartya(partyaPo);
        if (flag < 1) {
            throw new CmdException("设置默认主体失败");
        }
        context.setResponseEntity(ResultVo.success());
    }
}
