package com.java110.store.cmd.contract;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.store.bmo.contract.IContractPaymentBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 登记 / 撤销合同履约付款（回写期次付款状态与实付金额）
 */
@Java110Cmd(serviceCode = "contract.saveContractPayment")
public class SaveContractPaymentCmd extends Cmd {

    @Autowired
    private IContractPaymentBMO contractPaymentBMOImpl;

    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "planId", "请求报文中未包含收款计划期次");
    }

    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String storeId = CmdContextUtils.getStoreId(context);
        String userId = CmdContextUtils.getUserId(context);
        contractPaymentBMOImpl.savePayment(reqJson, storeId, userId);
        context.setResponseEntity(ResultVo.success());
    }
}
