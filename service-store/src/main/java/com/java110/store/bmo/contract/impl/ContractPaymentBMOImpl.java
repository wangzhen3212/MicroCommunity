package com.java110.store.bmo.contract.impl;

import com.alibaba.fastjson.JSONObject;
import com.java110.dto.contract.ContractCollectionPlanDto;
import com.java110.intf.store.IContractCollectionPlanInnerServiceSMO;
import com.java110.po.contract.ContractCollectionPlanPo;
import com.java110.store.bmo.contract.IContractPaymentBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 合同履约付款登记实现
 */
@Service("contractPaymentBMOImpl")
public class ContractPaymentBMOImpl implements IContractPaymentBMO {

    @Autowired
    private IContractCollectionPlanInnerServiceSMO contractCollectionPlanInnerServiceSMOImpl;

    @Override
    public void savePayment(JSONObject reqJson, String storeId, String userId) {
        String planId = reqJson.getString("planId");
        Assert.hasLength(planId, "请求报文中未包含收款计划期次");

        ContractCollectionPlanDto planDto = new ContractCollectionPlanDto();
        planDto.setPlanId(planId);
        planDto.setStatusCd("0");
        List<ContractCollectionPlanDto> planDtos = contractCollectionPlanInnerServiceSMOImpl.queryContractCollectionPlans(planDto);
        if (planDtos == null || planDtos.isEmpty()) {
            throw new CmdException("未查询到收款计划期次");
        }
        ContractCollectionPlanDto plan = planDtos.get(0);

        ContractCollectionPlanPo planPo = new ContractCollectionPlanPo();
        planPo.setPlanId(planId);
        planPo.setStoreId(storeId);
        planPo.setStatusCd("0");

        // 撤销付款：期次回到未到期
        if ("1".equals(reqJson.getString("cancel"))) {
            planPo.setPayState(ContractCollectionPlanDto.PAY_STATE_DOING);
            planPo.setRemark(buildRemark(plan, reqJson.getString("remark")));
            contractCollectionPlanInnerServiceSMOImpl.updateContractCollectionPlan(planPo);
            return;
        }

        String payAmount = reqJson.getString("payAmount");
        if (StringUtil.isEmpty(payAmount)) {
            throw new CmdException("请填写实付金额");
        }
        String payTime = reqJson.getString("payTime");
        planPo.setPayState(ContractCollectionPlanDto.PAY_STATE_PAID);
        planPo.setPayTime(StringUtil.isEmpty(payTime) ? DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A) : payTime);
        planPo.setPlanAmount(payAmount);
        planPo.setRemark(buildRemark(plan, reqJson.getString("remark")));
        contractCollectionPlanInnerServiceSMOImpl.updateContractCollectionPlan(planPo);
    }

    /**
     * 备注追加付款登记信息，保留原有备注
     */
    private String buildRemark(ContractCollectionPlanDto plan, String remark) {
        String oldRemark = plan.getRemark() == null ? "" : plan.getRemark();
        if (StringUtil.isEmpty(remark)) {
            return oldRemark;
        }
        String newRemark = "付款登记:" + remark;
        if (StringUtil.isEmpty(oldRemark)) {
            return newRemark;
        }
        if (oldRemark.length() + newRemark.length() > 200) {
            return oldRemark;
        }
        return oldRemark + "；" + newRemark;
    }
}
