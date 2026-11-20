package com.java110.fee.bmo.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.dto.contract.ContractFeeComputeDto;

/**
 * 合同费用试算
 */
public interface IComputeContractFeeBMO {

    /**
     * 按统一公式模块试算合同费用金额（只读，不产生费用/账单）
     *
     * @param reqJson 请求报文
     * @return 试算结果
     */
    ContractFeeComputeDto computeContractFeeAmount(JSONObject reqJson);
}
