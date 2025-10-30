package com.java110.report.cmd.dataReport;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询收款方式统计命令类
 * 
 * 该类负责处理收款方式统计查询请求，主要功能包括：
 * 1. 验证请求参数的完整性
 * 2. 格式化日期参数
 * 3. 调用统计服务获取收款方式统计数据
 * 4. 返回统计结果
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode dataReport.queryReceivedWayStatistics
 */
@Java110Cmd(serviceCode = "dataReport.queryReceivedWayStatistics")
public class QueryReceivedWayStatisticsCmd extends Cmd {

    /**
     * 费用统计服务接口，用于获取收款方式统计数据
     */
    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法主要完成以下验证和处理：
     * 1. 验证请求中是否包含必要的小区ID参数
     * 2. 对日期参数进行格式化处理，确保时间范围的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当日期解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        
        // 获取开始日期和结束日期参数
        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        
        // 如果开始日期不为空且不包含时间部分，则添加默认开始时间
        if (!StringUtil.isEmpty(startDate) && !startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        
        // 如果结束日期不为空且不包含时间部分，则添加默认结束时间
        if (!StringUtil.isEmpty(endDate) && !endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }
    }

    /**
     * 执行统计查询命令
     * 
     * 该方法主要完成以下操作：
     * 1. 构建统计查询参数对象
     * 2. 调用统计服务获取收款方式统计数据
     * 3. 将统计结果设置到响应中返回
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出命令异常
     * @throws ParseException 当日期解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建统计查询参数对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        // 设置小区ID
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        // 设置查询开始日期
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        // 设置查询结束日期
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));

        // 定义数据结果列表
        List<Map> datas = null;
        // 调用统计服务，根据缴费方式获取统计数据
        datas = feeStatisticsImpl.getReceivedFeeByPrimeRate(queryStatisticsDto);

        // 将统计结果设置到响应中返回
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }
}