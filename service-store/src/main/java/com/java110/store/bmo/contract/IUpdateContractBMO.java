package com.java110.store.bmo.contract;
import com.alibaba.fastjson.JSONObject;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.contract.ContractChangePlanDto;
import com.java110.po.contract.ContractPo;
import org.springframework.http.ResponseEntity;

public interface IUpdateContractBMO {


    /**
     * 修改合同管理
     * add by wuxw
     * @param contractPo
     * @return
     */
    ResponseEntity<String> update(ContractPo contractPo, JSONObject reqJson);


    /**
     * 需要审核合同
     * @param contractDto
     * @param reqJson
     * @return
     */
    ResponseEntity<String> needAuditContract(ContractDto contractDto, JSONObject reqJson);

    /**
     * 变更审核
     * @param contractChangePlanDto
     * @param reqJson
     * @return
     */
    ResponseEntity<String> needAuditContractPlan(ContractChangePlanDto contractChangePlanDto, JSONObject reqJson);

    /**
     * 两级审批全部通过后回写变更到合同主表（租期/主体/资产），并同步合同房屋与业主关系
     *
     * @param planId      变更单ID
     * @param storeId     商户ID
     * @param communityId 小区ID
     */
    void applyChangePlan(String planId, String storeId, String communityId);
}
