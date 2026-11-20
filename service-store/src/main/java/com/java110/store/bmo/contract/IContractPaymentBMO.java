package com.java110.store.bmo.contract;

import com.alibaba.fastjson.JSONObject;

/**
 * 合同履约付款登记业务接口
 */
public interface IContractPaymentBMO {

    /**
     * 登记 / 撤销付款：回写期次付款状态与合同履约状态
     *
     * @param reqJson     请求报文（planId / payAmount / payTime / channel / remark / cancel）
     * @param storeId     商户ID
     * @param communityId 小区ID
     * @param userId      当前登录用户
     */
    void savePayment(JSONObject reqJson, String storeId, String communityId, String userId);
}
