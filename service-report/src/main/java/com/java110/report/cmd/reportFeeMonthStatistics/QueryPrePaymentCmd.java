package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.reportFee.ReportFeeMonthStatisticsDto;
import com.java110.intf.report.IReportFeeMonthStatisticsInnerServiceSMO;
import com.java110.report.bmo.reportFeeMonthStatistics.impl.GetReportFeeMonthStatisticsBMOImpl;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 查询预交费命令类
 * 该命令类用于处理查询预交费统计信息的请求，主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 查询指定时间范围内的预交费统计数据
 * 3. 处理并格式化返回结果
 * 4. 计算费用的欠费天数
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "reportFeeMonthStatistics.queryPrePayment")
public class QueryPrePaymentCmd extends Cmd {

    private static final Logger logger = LoggerFactory.getLogger(GetReportFeeMonthStatisticsBMOImpl.class);

    @Autowired
    private IReportFeeMonthStatisticsInnerServiceSMO reportFeeMonthStatisticsInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 该方法用于验证传入的请求参数是否合法，包括分页信息和必要字段的检查
     *
     * @param event   命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   当命令执行出现异常时抛出
     * @throws ParseException 当日期解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证小区ID是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 执行查询预交费命令
     * 该方法主要功能：
     * 1. 设置查询时间范围（当前时间到7天后）
     * 2. 查询预交费统计数据总数
     * 3. 根据查询结果获取详细数据
     * 4. 处理对象名称和计算欠费天数
     * 5. 构建并返回响应结果
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException   当命令执行出现异常时抛出
     * @throws ParseException 当日期解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为统计DTO对象
        ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto = BeanConvertUtil.covertBean(reqJson, ReportFeeMonthStatisticsDto.class);
        
        // 设置查询开始时间为当前时间
        reportFeeMonthStatisticsDto.setStartTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        
        // 设置查询结束时间为7天后的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        reportFeeMonthStatisticsDto.setEndTime(DateUtil.getFormatTimeString(calendar.getTime(), DateUtil.DATE_FORMATE_STRING_A));
        
        // 查询预交费统计数据总数
        int count = reportFeeMonthStatisticsInnerServiceSMOImpl.queryPrePaymentNewCount(reportFeeMonthStatisticsDto);

        List<ReportFeeMonthStatisticsDto> reportFeeMonthStatisticsDtos = null;
        
        // 根据查询结果判断是否获取详细数据
        if (count > 0) {
            // 获取预交费详细统计数据
            reportFeeMonthStatisticsDtos = reportFeeMonthStatisticsInnerServiceSMOImpl.queryPrePayment(reportFeeMonthStatisticsDto);
            // 处理对象名称和计算欠费天数
            doObjNames(reportFeeMonthStatisticsDtos);
        } else {
            // 如果没有数据，返回空列表
            reportFeeMonthStatisticsDtos = new ArrayList<>();
        }

        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reportFeeMonthStatisticsDto.getRow()), count, reportFeeMonthStatisticsDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 处理对象名称和计算欠费天数
     * 该方法主要功能：
     * 1. 为统计对象设置显示名称（房屋或车位）
     * 2. 计算每个费用的欠费天数
     *
     * @param reportFeeMonthStatisticsDtos 预交费统计数据传输对象列表
     */
    private static void doObjNames(List<ReportFeeMonthStatisticsDto> reportFeeMonthStatisticsDtos) {
        // 第一遍循环：处理对象名称
        for (ReportFeeMonthStatisticsDto tmpReportFeeMonthStatisticsDto : reportFeeMonthStatisticsDtos) {
            // 如果对象名称已存在，跳过处理
            if(!StringUtil.isEmpty(tmpReportFeeMonthStatisticsDto.getObjName())){
                continue;
            }
            
            // 根据缴费对象类型设置不同的显示名称
            if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(tmpReportFeeMonthStatisticsDto.getPayerObjType())) {
                // 房屋对象：格式为"楼层-单元-房间号"
                tmpReportFeeMonthStatisticsDto.setObjName(tmpReportFeeMonthStatisticsDto.getFloorNum()
                        + "-" + tmpReportFeeMonthStatisticsDto.getUnitNum()
                        + "-" + tmpReportFeeMonthStatisticsDto.getRoomNum());
            } else {
                // 车位对象：使用车牌号作为显示名称
                tmpReportFeeMonthStatisticsDto.setObjName(tmpReportFeeMonthStatisticsDto.getCarNum());
            }
        }

        // 第二遍循环：计算欠费天数
        for (ReportFeeMonthStatisticsDto tmpReportFeeMonthStatisticsDto : reportFeeMonthStatisticsDtos) {
            try {
                // 计算费用结束时间与当前时间的间隔天数
                tmpReportFeeMonthStatisticsDto.setOweDay(DateUtil.daysBetween(
                    DateUtil.getDateFromString(tmpReportFeeMonthStatisticsDto.getEndTime(), DateUtil.DATE_FORMATE_STRING_A), 
                    DateUtil.getCurrentDate()));
            } catch (ParseException e) {
                // 记录日期解析异常日志
                logger.error("费用结束时间错误" + tmpReportFeeMonthStatisticsDto.getEndTime(), e);
            }
        }
    }
}