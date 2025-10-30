/**
 * 查询即将到期费用命令类
 * 
 * 该命令类用于处理查询即将到期的费用统计信息，主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 查询指定小区内未来7天内到期的费用统计
 * 3. 计算各项费用的剩余天数
 * 4. 返回分页查询结果
 * 
 * @serviceCode reportFeeMonthStatistics.queryDeadlineFee
 * @version 1.0
 * @since 2024
 */
package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.reportFee.ReportFeeMonthStatisticsDto;
import com.java110.intf.report.IReportFeeMonthStatisticsInnerServiceSMO;
import com.java110.report.bmo.reportFeeMonthStatistics.impl.GetReportFeeMonthStatisticsBMOImpl;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "reportFeeMonthStatistics.queryDeadlineFee")
public class QueryDeadlineFeeCmd extends Cmd {

    private static final Logger logger = LoggerFactory.getLogger(GetReportFeeMonthStatisticsBMOImpl.class);

    @Autowired
    private IReportFeeMonthStatisticsInnerServiceSMO reportFeeMonthStatisticsInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括：
     * 1. 分页参数验证
     * 2. 必要参数验证（小区ID）
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当参数解析失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 调用父类方法验证分页参数
        super.validatePageInfo(reqJson);
        // 验证请求中必须包含小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 命令执行方法
     * 
     * 主要执行逻辑：
     * 1. 设置查询时间范围（当前时间到未来7天）
     * 2. 查询符合条件的费用统计数量
     * 3. 根据查询结果获取费用统计详情
     * 4. 刷新各项费用的到期天数
     * 5. 构建并返回分页响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当时间解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为数据传输对象
        ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto = BeanConvertUtil.covertBean(reqJson, ReportFeeMonthStatisticsDto.class);
        
        // 设置查询开始时间为当前时间
        reportFeeMonthStatisticsDto.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        
        // 设置查询结束时间为当前时间加7天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        reportFeeMonthStatisticsDto.setEndTime(DateUtil.getFormatTimeString(calendar.getTime(), DateUtil.DATE_FORMATE_STRING_A));

        // 查询符合条件的费用统计数量
        int count = reportFeeMonthStatisticsInnerServiceSMOImpl.queryDeadlineFeeCount(reportFeeMonthStatisticsDto);

        List<ReportFeeMonthStatisticsDto> reportFeeMonthStatisticsDtos = null;
        
        // 根据查询结果数量决定是否查询详情
        if (count > 0) {
            // 查询费用统计详情列表
            reportFeeMonthStatisticsDtos = reportFeeMonthStatisticsInnerServiceSMOImpl.queryDeadlineFee(reportFeeMonthStatisticsDto);

            // 刷新各项费用的到期天数
            freshReportDeadlineDay(reportFeeMonthStatisticsDtos);
        } else {
            // 如果没有查询到数据，返回空列表
            reportFeeMonthStatisticsDtos = new ArrayList<>();
        }

        // 计算总页数并构建结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reportFeeMonthStatisticsDto.getRow()), count, reportFeeMonthStatisticsDtos);

        // 设置HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新费用到期天数方法
     * 
     * 遍历费用统计列表，计算每个费用项的剩余天数：
     * 1. 获取当前系统时间
     * 2. 对每个费用项计算到期时间与当前时间的差值
     * 3. 设置欠费天数到对应DTO对象中
     * 
     * @param reportFeeMonthStatisticsDtos 费用统计数据传输对象列表
     */
    private void freshReportDeadlineDay(List<ReportFeeMonthStatisticsDto> reportFeeMonthStatisticsDtos) {
        // 获取当前系统时间
        Date nowDate = DateUtil.getCurrentDate();

        // 遍历所有费用统计项
        for (ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto : reportFeeMonthStatisticsDtos) {
            try {
                // 计算到期时间与当前时间的天数差
                int day = DateUtil.daysBetween(DateUtil.getDateFromString(reportFeeMonthStatisticsDto.getDeadlineTime(),
                        DateUtil.DATE_FORMATE_STRING_A), nowDate);
                // 设置欠费天数
                reportFeeMonthStatisticsDto.setOweDay(day);
            } catch (Exception e) {
                // 记录计算欠费天数失败的错误日志
                logger.error("计算欠费天数失败", e);
            }
        }
    }
}