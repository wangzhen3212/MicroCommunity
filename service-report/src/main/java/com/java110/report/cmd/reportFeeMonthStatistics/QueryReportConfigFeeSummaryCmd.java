package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * 费用项费用统计命令类
 * 负责处理费用项月度统计查询请求，计算各项费用统计指标
 * 包括户收费率、费用收取率等关键业务指标
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode reportFeeMonthStatistics.queryReportConfigFeeSummary
 */
@Java110Cmd(serviceCode = "reportFeeMonthStatistics.queryReportConfigFeeSummary")
public class QueryReportConfigFeeSummaryCmd extends Cmd {

    /**
     * 费用统计服务接口，用于获取费用统计数据
     */
    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    /**
     * 请求参数验证方法
     * 验证请求参数是否包含必要的字段
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当日期解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证开始日期参数
        Assert.hasKeyAndValue(reqJson, "startDate", "未包含开始日期");
        // 验证结束日期参数
        Assert.hasKeyAndValue(reqJson, "endDate", "未包含结束日期");
        // 验证小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 执行费用统计查询命令
     * 获取费用统计数据并计算相关统计指标
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出命令异常
     * @throws ParseException 当日期解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建查询统计数据传输对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        // 设置小区ID
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        // 设置开始日期
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        // 设置结束日期
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
        
        // 处理结束日期格式，如果结束日期不包含时间部分，则添加结束时间
        if(reqJson.containsKey("endDate") && !reqJson.getString("endDate").contains(":")) {
            queryStatisticsDto.setEndDate(reqJson.getString("endDate") + " 23:59:59");
        }
        
        // 设置楼层ID（可选参数）
        queryStatisticsDto.setFloorId(reqJson.getString("floorId"));
        // 设置费用配置ID列表（可选参数）
        if (reqJson.containsKey("configIds")) {
            queryStatisticsDto.setConfigIds(reqJson.getString("configIds").split(","));
        }

        // 调用费用统计服务获取配置费用汇总数据
        List<Map> datas = feeStatisticsImpl.getConfigFeeSummary(queryStatisticsDto);

        // 如果查询结果为空，直接返回空结果
        if (datas == null || datas.size() < 1) {
            context.setResponseEntity(ResultVo.createResponseEntity(datas));
            return;
        }
        
        // 定义统计计算相关变量
        BigDecimal feeRoomCountDec = null;
        BigDecimal oweRoomCountDec = null;
        BigDecimal feeRoomRate = null;
        BigDecimal curReceivedFee = null;
        BigDecimal curReceivableFee = null;
        
        // 遍历每条统计数据，计算相关指标
        for (Map data : datas) {
            // 计算户收费率：已收费户数占总应收户数的比例
            if (Double.parseDouble(data.get("feeRoomCount").toString()) > 0) {
                // 将收费户数转换为BigDecimal类型
                feeRoomCountDec = new BigDecimal(Double.parseDouble(data.get("feeRoomCount").toString()));
                // 将欠费户数转换为BigDecimal类型
                oweRoomCountDec = new BigDecimal(Double.parseDouble(data.get("oweRoomCount").toString()));
                // 计算户收费率公式：(收费户数 - 欠费户数) / 收费户数 * 100，保留2位小数
                feeRoomRate = feeRoomCountDec.subtract(oweRoomCountDec)
                        .divide(feeRoomCountDec, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal(100))
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                // 将计算结果设置到数据中
                data.put("feeRoomRate", feeRoomRate.doubleValue());
            } else {
                // 如果收费户数为0，则户收费率为0
                data.put("feeRoomRate", 0.0);
            }

            // 计算费用收取率：已收费用占应收费用的比例
            // 将当前已收费用转换为BigDecimal类型
            curReceivedFee = new BigDecimal(Double.parseDouble(data.get("curReceivedFee").toString()));
            // 将当前应收费用转换为BigDecimal类型
            curReceivableFee = new BigDecimal(Double.parseDouble(data.get("curReceivableFee").toString()));

            // 计算费用收取率
            if (curReceivableFee.doubleValue() > 0) {
                // 计算费用收取率公式：已收费用 / 应收费用 * 100，保留2位小数
                feeRoomRate = curReceivedFee.divide(curReceivableFee, 4, BigDecimal.ROUND_HALF_UP)
                        .multiply(new BigDecimal(100))
                        .setScale(2, BigDecimal.ROUND_HALF_UP);
                // 将计算结果设置到数据中
                data.put("feeRate", feeRoomRate.doubleValue());
            } else {
                // 如果应收费用为0，则费用收取率为0
                data.put("feeRate", 0.0);
            }
        }

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }
}