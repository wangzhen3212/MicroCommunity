package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.reportFee.ReportFeeMonthStatisticsDto;
import com.java110.intf.report.IReportFeeMonthStatisticsInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 楼栋费用表明细查询命令类
 * 
 * 该类用于处理楼栋单元费用月度统计明细查询请求，负责参数验证和业务逻辑处理
 * 通过注解@Java110Cmd指定服务访问路径为/reportFeeMonthStatistics/queryReportFloorUnitFeeSummaryDetail
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "/reportFeeMonthStatistics/queryReportFloorUnitFeeSummaryDetail")
public class QueryReportFloorUnitFeeSummaryDetailCmd extends Cmd {

    /**
     * 费用月统计内部服务接口，用于查询费用统计相关数据
     */
    @Autowired
    private IReportFeeMonthStatisticsInnerServiceSMO reportFeeMonthStatisticsInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数是否完整，包括必填字段和分页信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填参数：小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未传入小区信息");
        // 验证必填参数：费用年份
        Assert.hasKeyAndValue(reqJson, "feeYear", "未传入年份");
        // 验证必填参数：费用月份
        Assert.hasKeyAndValue(reqJson, "feeMonth", "未传入月份");
        // 验证必填参数：楼栋号
        Assert.hasKeyAndValue(reqJson, "floorNum", "未传入楼栋");
        // 验证必填参数：单元号
        Assert.hasKeyAndValue(reqJson, "unitNum", "未传入单元");
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理楼栋单元费用明细查询业务逻辑，包括参数处理、数据查询和结果返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String[] configIds = null;
        // 处理配置ID参数，如果存在且不为空则进行分割处理
        if (reqJson.containsKey("configIds") && !StringUtil.isEmpty(reqJson.getString("configIds"))) {
            configIds = reqJson.getString("configIds").split(",");
            // 从请求JSON中移除configIds字段，避免后续转换冲突
            reqJson.remove("configIds");
        }
        
        // 将请求JSON对象转换为费用月统计DTO对象
        ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto = BeanConvertUtil.covertBean(reqJson, ReportFeeMonthStatisticsDto.class);
        // 设置配置ID数组到DTO对象中
        reportFeeMonthStatisticsDto.setConfigIds(configIds);

        // 查询符合条件的记录总数
        int count = reportFeeMonthStatisticsInnerServiceSMOImpl.queryReportFloorUnitFeeSummaryDetailCount(reportFeeMonthStatisticsDto);
        
        List<ReportFeeMonthStatisticsDto> reportFeeMonthStatisticsDtos = null;
        // 根据查询结果数量决定是否查询详细数据
        if (count > 0) {
            // 查询费用明细汇总数据
            reportFeeMonthStatisticsDtos = reportFeeMonthStatisticsInnerServiceSMOImpl.queryReportFloorUnitFeeDetailSummary(reportFeeMonthStatisticsDto);
        } else {
            // 如果没有数据，返回空列表
            reportFeeMonthStatisticsDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reportFeeMonthStatisticsDto.getRow()), count, reportFeeMonthStatisticsDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}