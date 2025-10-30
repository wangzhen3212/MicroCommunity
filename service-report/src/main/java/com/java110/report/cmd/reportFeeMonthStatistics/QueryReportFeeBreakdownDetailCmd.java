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
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 费用月度统计明细查询命令类
 * 
 * 该类用于处理费用月度统计明细查询请求，包括参数验证、数据查询和结果返回等功能
 * 主要提供费用明细的分页查询服务，支持按小区、费用项和时间范围等条件进行查询
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode /reportFeeMonthStatistics/queryReportFeeBreakdownDetail
 */
@Java110Cmd(serviceCode = "/reportFeeMonthStatistics/queryReportFeeBreakdownDetail")
public class QueryReportFeeBreakdownDetailCmd extends Cmd {

    /**
     * 费用月度统计内部服务接口
     * 用于调用费用月度统计相关的数据查询服务
     */
    @Autowired
    private IReportFeeMonthStatisticsInnerServiceSMO reportFeeMonthStatisticsInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证传入的请求参数是否完整和有效，确保必要的参数存在且符合要求
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未传入小区信息");
        // 验证费用项配置ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "configId", "未传入费用项");
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理费用明细查询业务逻辑，包括参数处理、数据查询和结果返回
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为数据传输对象
        ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto = BeanConvertUtil.covertBean(reqJson, ReportFeeMonthStatisticsDto.class);
        
        // 处理年月参数：如果未传入年月，则使用当前年月作为默认值
        if(StringUtil.isEmpty(reportFeeMonthStatisticsDto.getYearMonth())){
            reportFeeMonthStatisticsDto.setFeeYear(DateUtil.getYear()+"");
            reportFeeMonthStatisticsDto.setFeeMonth(DateUtil.getMonth()+"");
        }
        
        // 处理开始时间参数：如果存在开始时间，则补全天的时间部分
        if (!StringUtil.isEmpty(reportFeeMonthStatisticsDto.getStartTime())) {
            reportFeeMonthStatisticsDto.setStartTime(reportFeeMonthStatisticsDto.getStartTime() + " 00:00:00");
        }
        // 处理结束时间参数：如果存在结束时间，则补全天的时间部分
        if (!StringUtil.isEmpty(reportFeeMonthStatisticsDto.getEndTime())) {
            reportFeeMonthStatisticsDto.setEndTime(reportFeeMonthStatisticsDto.getEndTime() + " 23:59:59");
        }

        // 查询费用明细总记录数
        int count = reportFeeMonthStatisticsInnerServiceSMOImpl.queryFeeBreakdownDetailCount(reportFeeMonthStatisticsDto);

        List<ReportFeeMonthStatisticsDto> reportFeeMonthStatisticsDtos = null;
        // 根据查询结果数量决定是否查询明细数据
        if (count > 0) {
            // 查询费用明细数据列表
            reportFeeMonthStatisticsDtos = reportFeeMonthStatisticsInnerServiceSMOImpl.queryFeeBreakdownDetail(reportFeeMonthStatisticsDto);
        } else {
            // 如果没有数据，返回空列表
            reportFeeMonthStatisticsDtos = new ArrayList<>();
        }

        // 构建返回结果对象，包含分页信息和数据列表
        // 计算总页数：总记录数除以每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reportFeeMonthStatisticsDto.getRow()), count, reportFeeMonthStatisticsDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}