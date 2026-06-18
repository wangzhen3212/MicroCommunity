package com.java110.report.cmd.dataReport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.report.statistics.IOrderStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.MoneyUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

/**
 * 费用数据报表查询命令类
 * 
 * 该类用于处理费用相关的数据统计查询请求，包括实收金额、欠费金额、优惠金额、
 * 滞纳金、账户预存、账户扣款、临时车收入、押金退款、退款订单数、退款金额、
 * 充电金额和月卡实收等多种费用统计数据的查询和汇总
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode dataReport.queryFeeDataReport
 */
@Java110Cmd(serviceCode = "dataReport.queryFeeDataReport")
public class QueryFeeDataReportCmd extends Cmd {

    /**
     * 费用统计服务接口，用于获取各类费用统计数据
     */
    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    /**
     * 订单统计服务接口，用于获取订单相关的统计数据
     */
    @Autowired
    private IOrderStatistics orderStatisticsImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，并对日期参数进行格式化处理
     * 如果日期参数不包含时间部分，自动添加默认时间
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当日期解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "startDate", "未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "endDate", "未包含结束时间");
        
        // 获取日期参数
        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        
        // 如果开始日期不包含时间部分，添加默认开始时间 00:00:00
        if (!startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        
        // 如果结束日期不包含时间部分，添加默认结束时间 23:59:59
        if (!endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }
    }

    /**
     * 执行费用数据报表查询命令
     * 
     * 根据查询条件获取各类费用统计数据，包括实收金额、欠费金额、优惠金额、
     * 滞纳金、账户预存、账户扣款、临时车收入、押金退款、退款订单数、退款金额、
     * 充电金额和月卡实收等，并将结果封装为JSON格式返回
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于设置响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当日期解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建查询统计数据传输对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));

        // 初始化数据数组，用于存储所有统计结果
        JSONArray datas = new JSONArray();
        JSONObject data = null;

        // 查询实收金额统计数据
        double receivedFee = feeStatisticsImpl.getReceivedFee(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "实收金额");
        data.put("value", MoneyUtil.computePriceScale(receivedFee));
        datas.add(data);

        // 查询欠费金额统计数据
        double oweFee = feeStatisticsImpl.getOweFee(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "欠费金额");
        data.put("value", MoneyUtil.computePriceScale(oweFee));
        datas.add(data);

        // 查询优惠金额统计数据
        double discountFee = feeStatisticsImpl.getDiscountFee(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "优惠金额");
        data.put("value", MoneyUtil.computePriceScale(discountFee));
        datas.add(data);

        // 查询滞纳金统计数据
        double lateFee = feeStatisticsImpl.getLateFee(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "滞纳金");
        data.put("value", MoneyUtil.computePriceScale(lateFee));
        datas.add(data);

        // 查询账户预存统计数据
        double prestoreAccount = feeStatisticsImpl.getPrestoreAccount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "账户预存");
        data.put("value", MoneyUtil.computePriceScale(prestoreAccount));
        datas.add(data);

        // 查询账户扣款统计数据
        double withholdAccount = feeStatisticsImpl.getWithholdAccount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "账户扣款");
        data.put("value", MoneyUtil.computePriceScale(withholdAccount));
        datas.add(data);

        // 查询临时车收入统计数据
        double tempCarFee = feeStatisticsImpl.getTempCarFee(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "临时车收入");
        data.put("value", MoneyUtil.computePriceScale(tempCarFee));
        datas.add(data);

        // 查询押金退款统计数据
        double refundDeposit = feeStatisticsImpl.geRefundDeposit(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "押金退款");
        data.put("value", MoneyUtil.computePriceScale(refundDeposit));
        datas.add(data);

        // 查询退款订单数统计数据
        double refundOrderCount = feeStatisticsImpl.geRefundOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "退款订单数");
        data.put("value", MoneyUtil.computePriceScale(refundOrderCount));
        datas.add(data);

        // 查询退款金额统计数据
        double refundFee = feeStatisticsImpl.geRefundFee(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "退款金额");
        data.put("value", MoneyUtil.computePriceScale(refundFee));
        datas.add(data);

        // 查询充电金额统计数据
        double chargeFee = feeStatisticsImpl.getChargeFee(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "充电金额");
        data.put("value", MoneyUtil.computePriceScale(chargeFee));
        datas.add(data);

        // 查询月卡实收统计数据（使用订单统计服务）
        double chargeMonthOrderMoney = orderStatisticsImpl.getChargeMonthOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name", "月卡实收");
        data.put("value", MoneyUtil.computePriceScale(chargeMonthOrderMoney));
        datas.add(data);

        // 将统计结果数据设置到响应中
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }
}