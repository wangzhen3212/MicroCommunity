package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.reportFee.ReportFeeMonthStatisticsDto;
import com.java110.intf.fee.IFeeDetailInnerServiceSMO;
import com.java110.intf.report.IQueryPayFeeDetailInnerServiceSMO;
import com.java110.report.dao.IReportAttendanceServiceDao;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 缴费明细查询命令类
 * 
 * 该类用于处理缴费明细查询请求，继承自Cmd基类，通过注解标识服务接口地址
 * 主要功能包括参数验证和缴费明细数据查询
 * 
 * @author Java110
 * @version 1.0
 * @see Cmd
 */
@Java110Cmd(serviceCode = "/reportFeeMonthStatistics/queryPayFeeDetail")
public class QueryPayFeeDetailCmd extends Cmd {

    /**
     * 缴费明细查询服务接口
     */
    @Autowired
    private IQueryPayFeeDetailInnerServiceSMO queryPayFeeDetailInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息、必填字段和日期格式处理
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当日期解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证属性信息
        super.validateProperty(context);
        // 验证小区ID必填
        Assert.hasKeyAndValue(reqJson, "communityId", "为包含小区");
        
        // 处理结束时间，如果没有包含时分秒，则自动添加23:59:59
        String endTime = reqJson.getString("endTime");
        if (!StringUtil.isEmpty(endTime) && !endTime.contains(":")) {
            endTime += " 23:59:59";
            reqJson.put("endTime", endTime);
        }
    }

    /**
     * 执行命令方法
     * 
     * 处理缴费明细查询业务逻辑，将请求参数转换为DTO对象，设置当前年月，
     * 调用查询服务获取结果并设置响应
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行失败时抛出命令异常
     * @throws ParseException 当日期解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为数据传输对象
        ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto = BeanConvertUtil.covertBean(reqJson, ReportFeeMonthStatisticsDto.class);

        // 设置当前年份和月份
        reportFeeMonthStatisticsDto.setFeeYear(DateUtil.getYear() + "");
        reportFeeMonthStatisticsDto.setFeeMonth(DateUtil.getMonth() + "");

        // 调用查询服务获取缴费明细结果
        ResultVo resultVo = queryPayFeeDetailInnerServiceSMOImpl.query(reportFeeMonthStatisticsDto);

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }
}