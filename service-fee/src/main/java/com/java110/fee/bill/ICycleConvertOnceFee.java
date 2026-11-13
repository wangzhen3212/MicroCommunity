package com.java110.fee.bill;

import com.java110.dto.fee.PreGenerateFeePeriodDto;
import com.java110.po.fee.PayFeePo;
import com.java110.po.payFee.PayFeeRulePo;

import java.util.List;

/**
 * 周期性费用 间接费用转换 一次性费用处理
 */
public interface ICycleConvertOnceFee {

    /**
     * 批量将 周期性 间接性费用 转换一次性
     *
     * @param payFeePos 费用集合
     * @return 大于1 转换成功 0 转换失败
     */
    int convertPayFees(List<PayFeePo> payFeePos);


    /**
     * 将 周期性 间接性费用 转换一次性
     *
     * @param payFeePo 单个费用
     * @return 大于1 转换成功 0 转换失败
     */
    int convertPayFee(PayFeePo payFeePo);

    /**
     * 将小区里的费用批量转换为一次性费用
     *
     * @param communityId 小区ID
     * @return 大于1 转换成功 0 转换失败
     */
    int covertCommunityPayFee(String communityId);

    /**
     * 根据 费用规则转换
     *
     * @param ruleIds 费用规则
     * @return 大于1 转换成功 0 转换失败
     */
    int covertRuleIdsPayFee(List<String> ruleIds);

    /**
     * 根据规则生成 一次性费用
     *
     * @param payFeeRulePos
     * @return
     */
    int rulesGeneratePayFees(List<PayFeeRulePo> payFeeRulePos);

    /**
     * 根据规则生成 一次性费用
     *
     * @param payFeeRulePo
     * @return
     */
    int ruleGeneratePayFee(PayFeeRulePo payFeeRulePo);

    /**
     * 保存费用规则并直接生成一次性账单（不做账单日校验）
     * 适用场景：月卡、预付类费用等账期起点可能为未来日期的业务
     *
     * @param payFeeRulePo 费用规则（ruleId/startTime/maxTime/amount 等必须已赋值）
     * @return 生成的费用ID集合，为空表示未生成
     */
    List<String> saveRuleAndGenerateOncePayFee(PayFeeRulePo payFeeRulePo);

    /**
     * 预生成周期性费用账单（不做账单日校验，按指定期次逐期出账）
     *
     * @param payFeeRulePo 费用规则
     * @param periods      需要出账的期次，按 seq 升序且连续
     * @return 生成的账单条数
     */
    int preGeneratePayFee(PayFeeRulePo payFeeRulePo, List<PreGenerateFeePeriodDto> periods);

    /**
     * 预生成一次性费用账单（不做账单日校验，生成后规则置为结束）
     *
     * @param payFeeRulePo 费用规则
     * @return 生成的账单条数
     */
    int generateOncePayFeeByRule(PayFeeRulePo payFeeRulePo);
}
