package com.java110.fee.bmo.fee.impl;

import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.PayFeeDto;
import com.java110.dto.fee.PreGenerateFeePeriodDto;
import com.java110.dto.payFee.PayFeeRuleDto;
import com.java110.fee.bill.ICycleConvertOnceFee;
import com.java110.fee.bmo.fee.IPreGenerateFeeBMO;
import com.java110.intf.fee.IPayFeeConfigV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeRuleV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.po.payFee.PayFeeRulePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 预生成费用账单
 */
@Service("preGenerateFeeBMOImpl")
public class PreGenerateFeeBMOImpl implements IPreGenerateFeeBMO {

    /**
     * 最多试算期数
     */
    private static final int MAX_PERIOD_COUNT = 12;

    @Autowired
    private IPayFeeRuleV1InnerServiceSMO payFeeRuleV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeConfigV1InnerServiceSMO payFeeConfigV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private ICycleConvertOnceFee cycleConvertOnceFeeImpl;

    @Override
    public Map<String, Object> queryPeriods(String communityId, String ruleId) {

        PayFeeRuleDto payFeeRuleDto = queryRule(communityId, ruleId);
        FeeConfigDto feeConfigDto = queryFeeConfig(payFeeRuleDto);

        List<PreGenerateFeePeriodDto> periods = computePeriods(payFeeRuleDto, feeConfigDto);

        Map<String, Object> result = new HashMap<>();
        result.put("ruleId", payFeeRuleDto.getRuleId());
        result.put("feeFlag", feeConfigDto.getFeeFlag());
        result.put("feeName", feeConfigDto.getFeeName());
        result.put("paymentCycle", feeConfigDto.getPaymentCycle());
        result.put("endTime", payFeeRuleDto.getEndTime());
        result.put("periods", periods);

        return result;
    }

    @Override
    public void validatePeriods(List<PreGenerateFeePeriodDto> allPeriods, List<PreGenerateFeePeriodDto> submitPeriods, String ruleId) {

        if (ListUtil.isNull(submitPeriods)) {
            throw new CmdException("请选择要预生成的期次");
        }

        if (ListUtil.isNull(allPeriods)) {
            throw new CmdException("暂无可预生成期次");
        }

        Map<String, PreGenerateFeePeriodDto> periodMap = new HashMap<>();
        for (PreGenerateFeePeriodDto period : allPeriods) {
            periodMap.put(period.getStartTime(), period);
        }

        List<PreGenerateFeePeriodDto> sortedPeriods = sortPeriods(submitPeriods);
        for (PreGenerateFeePeriodDto period : sortedPeriods) {
            if (!periodMap.containsKey(period.getStartTime())) {
                throw new CmdException("预生成期次不合法，请刷新后重试");
            }
        }

        //todo 不允许掐头，必须从第 1 期开始
        if (!allPeriods.get(0).getStartTime().equals(sortedPeriods.get(0).getStartTime())) {
            throw new CmdException("预生成必须从第 1 期开始连续勾选");
        }

        //todo 不允许跳期，相邻期次必须首尾相接
        for (int i = 1; i < sortedPeriods.size(); i++) {
            Date preEndTime = DateUtil.getDateFromStringA(sortedPeriods.get(i - 1).getEndTime());
            String curStartTime = DateUtil.getFormatTimeStringA(DateUtil.getNextSecDateTime(preEndTime));
            if (!curStartTime.equals(sortedPeriods.get(i).getStartTime())) {
                throw new CmdException("预生成期次必须连续勾选，不能跳过中间的期次");
            }
        }

        //todo 兜底校验，防止并发或异常数据重复出账
        for (int i = 0; i < sortedPeriods.size(); i++) {
            PayFeeDto payFeeDto = new PayFeeDto();
            payFeeDto.setRuleId(ruleId);
            payFeeDto.setStartTime(DateUtil.getFormatTimeStringB(DateUtil.getDateFromStringA(sortedPeriods.get(i).getStartTime())));
            List<PayFeeDto> payFeeDtos = payFeeV1InnerServiceSMOImpl.queryPayFees(payFeeDto);
            if (!ListUtil.isNull(payFeeDtos)) {
                throw new CmdException("第 " + (i + 1) + " 期已生成账单，请刷新后重试");
            }
        }
    }

    @Override
    public int preGenerate(String communityId, String ruleId, List<PreGenerateFeePeriodDto> periods) {

        PayFeeRuleDto payFeeRuleDto = queryRule(communityId, ruleId);
        FeeConfigDto feeConfigDto = queryFeeConfig(payFeeRuleDto);

        if (FeeDto.STATE_FINISH.equals(payFeeRuleDto.getState())) {
            throw new CmdException("该费用已收费结束，不能预生成账单");
        }

        PayFeeRulePo payFeeRulePo = BeanConvertUtil.covertBean(payFeeRuleDto, PayFeeRulePo.class);

        //todo 一次性费用直接生成一张账单
        if (FeeDto.FEE_FLAG_ONCE.equals(feeConfigDto.getFeeFlag())) {
            return cycleConvertOnceFeeImpl.generateOncePayFeeByRule(payFeeRulePo);
        }

        //todo 周期性费用按重算期次出账，不使用前端传入的账期
        List<PreGenerateFeePeriodDto> allPeriods = computePeriods(payFeeRuleDto, feeConfigDto);
        List<PreGenerateFeePeriodDto> targetPeriods = matchPeriods(allPeriods, periods);
        return cycleConvertOnceFeeImpl.preGeneratePayFee(payFeeRulePo, targetPeriods);
    }

    /**
     * 查询费用规则
     *
     * @param communityId 小区ID
     * @param ruleId      费用规则ID
     * @return 费用规则
     */
    private PayFeeRuleDto queryRule(String communityId, String ruleId) {
        PayFeeRuleDto payFeeRuleDto = new PayFeeRuleDto();
        payFeeRuleDto.setCommunityId(communityId);
        payFeeRuleDto.setRuleId(ruleId);
        List<PayFeeRuleDto> payFeeRuleDtos = payFeeRuleV1InnerServiceSMOImpl.queryPayFeeRules(payFeeRuleDto);
        Assert.listOnlyOne(payFeeRuleDtos, "费用规则不存在，ruleId=" + ruleId);
        return payFeeRuleDtos.get(0);
    }

    /**
     * 查询费用项
     *
     * @param payFeeRuleDto 费用规则
     * @return 费用项
     */
    private FeeConfigDto queryFeeConfig(PayFeeRuleDto payFeeRuleDto) {
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigId(payFeeRuleDto.getConfigId());
        feeConfigDto.setCommunityId(payFeeRuleDto.getCommunityId());
        List<FeeConfigDto> feeConfigDtos = payFeeConfigV1InnerServiceSMOImpl.queryPayFeeConfigs(feeConfigDto);
        Assert.listOnlyOne(feeConfigDtos, "费用项不存在，configId=" + payFeeRuleDto.getConfigId());
        return feeConfigDtos.get(0);
    }

    /**
     * 试算期次：自规则账期终点起按缴费周期递推，最多 12 期，受规则最大时间约束
     *
     * @param payFeeRuleDto 费用规则
     * @param feeConfigDto  费用项
     * @return 期次集合
     */
    private List<PreGenerateFeePeriodDto> computePeriods(PayFeeRuleDto payFeeRuleDto, FeeConfigDto feeConfigDto) {

        List<PreGenerateFeePeriodDto> periods = new ArrayList<>();

        //todo 一次性费用不出期次
        if (FeeDto.FEE_FLAG_ONCE.equals(feeConfigDto.getFeeFlag())) {
            return periods;
        }

        int ruleCycle = computeRuleCycle(feeConfigDto);

        Date startTime = parseDateTime(payFeeRuleDto.getEndTime());
        if (startTime == null) {
            startTime = parseDateTime(payFeeRuleDto.getStartTime());
        }
        if (startTime == null) {
            return periods;
        }

        //todo 期次上限只取费用规则的最大时间，费用项应收结束时间不参与截断
        Date limitEnd = parseEndOfDay(payFeeRuleDto.getMaxTime());

        int seq = 0;
        while (seq < MAX_PERIOD_COUNT && (limitEnd == null || startTime.before(limitEnd))) {
            Date endTime = getAddMonthDate(startTime, ruleCycle);
            if (limitEnd != null && !endTime.before(limitEnd)) {
                endTime = limitEnd;
            }
            periods.add(new PreGenerateFeePeriodDto(seq + 1,
                    DateUtil.getFormatTimeStringA(startTime),
                    DateUtil.getPreSecTime(endTime)));
            startTime = endTime;
            seq++;
        }

        return periods;
    }

    /**
     * 用重算出的期次覆盖前端提交的期次，防止篡改账期
     *
     * @param allPeriods    重算出的期次
     * @param submitPeriods 前端提交的期次
     * @return 需出账的期次
     */
    private List<PreGenerateFeePeriodDto> matchPeriods(List<PreGenerateFeePeriodDto> allPeriods, List<PreGenerateFeePeriodDto> submitPeriods) {

        if (ListUtil.isNull(submitPeriods)) {
            throw new CmdException("请选择要预生成的期次");
        }

        Map<String, PreGenerateFeePeriodDto> periodMap = new LinkedHashMap<>();
        for (PreGenerateFeePeriodDto period : allPeriods) {
            periodMap.put(period.getStartTime(), period);
        }

        List<PreGenerateFeePeriodDto> targetPeriods = new ArrayList<>();
        for (PreGenerateFeePeriodDto period : sortPeriods(submitPeriods)) {
            PreGenerateFeePeriodDto targetPeriod = periodMap.get(period.getStartTime());
            if (targetPeriod == null) {
                throw new CmdException("预生成期次不合法，请刷新后重试");
            }
            targetPeriods.add(targetPeriod);
        }

        return targetPeriods;
    }

    /**
     * 按期次开始时间升序
     *
     * @param periods 期次集合
     * @return 排序后的期次集合
     */
    private List<PreGenerateFeePeriodDto> sortPeriods(List<PreGenerateFeePeriodDto> periods) {
        List<PreGenerateFeePeriodDto> sortedPeriods = new ArrayList<>(periods);
        sortedPeriods.sort(new Comparator<PreGenerateFeePeriodDto>() {
            @Override
            public int compare(PreGenerateFeePeriodDto o1, PreGenerateFeePeriodDto o2) {
                return o1.getStartTime().compareTo(o2.getStartTime());
            }
        });
        return sortedPeriods;
    }

    /**
     * 计算缴费周期，一次性费用按 1 处理
     *
     * @param feeConfigDto 费用项
     * @return 缴费周期
     */
    private int computeRuleCycle(FeeConfigDto feeConfigDto) {
        if (FeeDto.FEE_FLAG_ONCE.equals(feeConfigDto.getFeeFlag())) {
            return 1;
        }
        if (StringUtil.isEmpty(feeConfigDto.getPaymentCycle()) || !StringUtil.isNumber(feeConfigDto.getPaymentCycle())) {
            return 1;
        }
        int ruleCycle = Integer.parseInt(feeConfigDto.getPaymentCycle());
        return ruleCycle < 1 ? 1 : ruleCycle;
    }

    /**
     * 按月递推，保留时分秒
     *
     * @param date       时间
     * @param monthCount 月份数
     * @return 递推后的时间
     */
    private Date getAddMonthDate(Date date, int monthCount) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, monthCount);
        return calendar.getTime();
    }

    /**
     * 解析到当天结束，返回账期开区间终点
     *
     * @param time 时间字符串
     * @return 时间
     */
    private Date parseEndOfDay(String time) {
        if (StringUtil.isEmpty(time)) {
            return null;
        }
        String tmpTime = time;
        if (!tmpTime.contains(":")) {
            tmpTime = tmpTime + " 23:59:59";
        }
        Date date = DateUtil.getDateFromStringA(tmpTime);
        if (tmpTime.endsWith("23:59:59")) {
            date = DateUtil.getNextSecDateTime(date);
        }
        return date;
    }

    /**
     * 兼容 yyyy-MM-dd 与 yyyy-MM-dd HH:mm:ss 两种格式
     *
     * @param time 时间字符串
     * @return 时间
     */
    private Date parseDateTime(String time) {
        if (StringUtil.isEmpty(time)) {
            return null;
        }
        if (!time.contains(":")) {
            return DateUtil.getDateFromStringB(time);
        }
        return DateUtil.getDateFromStringA(time);
    }
}
