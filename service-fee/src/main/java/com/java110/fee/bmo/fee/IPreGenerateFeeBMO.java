package com.java110.fee.bmo.fee;

import com.java110.dto.fee.PreGenerateFeePeriodDto;

import java.util.List;
import java.util.Map;

/**
 * 预生成费用账单
 */
public interface IPreGenerateFeeBMO {

    /**
     * 试算可预生成的期次（自规则账期终点起递推，最多 12 期，不查账单）
     *
     * @param communityId 小区ID
     * @param ruleId      费用规则ID
     * @return feeFlag/feeName/paymentCycle/endTime/periods
     */
    Map<String, Object> queryPeriods(String communityId, String ruleId);

    /**
     * 校验提交的期次：连续性、不掐头、不重复出账
     *
     * @param allPeriods    重算出的全部期次
     * @param submitPeriods 前端提交的期次
     * @param ruleId        费用规则ID
     */
    void validatePeriods(List<PreGenerateFeePeriodDto> allPeriods, List<PreGenerateFeePeriodDto> submitPeriods, String ruleId);

    /**
     * 预生成账单
     *
     * @param communityId 小区ID
     * @param ruleId      费用规则ID
     * @param periods     需要出账的期次，一次性费用可为空
     * @return 生成的账单条数
     */
    int preGenerate(String communityId, String ruleId, List<PreGenerateFeePeriodDto> periods);
}
