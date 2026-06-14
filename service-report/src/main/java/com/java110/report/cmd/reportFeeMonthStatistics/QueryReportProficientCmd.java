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
import com.java110.utils.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 查询报表专家图表数据命令类
 * 
 * 该类负责处理报表专家图表的查询请求，主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 构建查询条件并调用服务层获取数据
 * 3. 将查询结果封装为HTTP响应返回
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "/reportFeeMonthStatistics/queryReportProficient")
public class QueryReportProficientCmd extends Cmd {

    /**
     * 报表费用月统计服务接口，用于调用相关的业务逻辑
     */
    @Autowired
    private IReportFeeMonthStatisticsInnerServiceSMO reportFeeMonthStatisticsInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证HTTP请求中的必要参数是否完整有效
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区");
    }

    /**
     * 命令执行方法
     * 
     * 该方法负责处理具体的业务逻辑，包括：
     * 1. 构建查询条件对象
     * 2. 设置当前年月作为查询条件
     * 3. 调用服务层获取报表专家统计数据
     * 4. 将结果封装为HTTP响应
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建报表费用月统计数据传输对象
        ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto = new ReportFeeMonthStatisticsDto();
        // 设置小区ID查询条件
        reportFeeMonthStatisticsDto.setCommunityId(reqJson.getString("communityId"));

        // 设置查询年份为当前年份
        reportFeeMonthStatisticsDto.setFeeYear(DateUtil.getYear()+"");
        // 设置查询月份为当前月份
        reportFeeMonthStatisticsDto.setFeeMonth(DateUtil.getMonth()+"");

        // 调用服务层接口，获取报表专家统计数据
        JSONObject result = reportFeeMonthStatisticsInnerServiceSMOImpl.queryReportProficientCount(reportFeeMonthStatisticsDto);
        
        // 创建HTTP响应实体，将查询结果转换为字符串并设置HTTP状态码为200
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(result.toString(), HttpStatus.OK);

        // 将响应实体设置到上下文中，供框架后续处理
        context.setResponseEntity(responseEntity);
    }
}