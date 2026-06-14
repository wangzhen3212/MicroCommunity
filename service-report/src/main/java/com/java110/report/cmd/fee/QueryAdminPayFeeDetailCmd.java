/**
 * 费用查询命令类
 * 
 * 功能：处理管理员费用明细查询请求，包括参数验证和数据查询
 * 用途：作为CMD命令处理管理员对费用明细的查询操作，支持分页和权限验证
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.report.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.reportFee.ReportFeeMonthStatisticsDto;
import com.java110.intf.report.IQueryPayFeeDetailInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "fee.queryAdminPayFeeDetail")
public class QueryAdminPayFeeDetailCmd extends Cmd {
    
    /**
     * 费用明细查询服务接口
     */
    @Autowired
    private IQueryPayFeeDetailInnerServiceSMO queryPayFeeDetailInnerServiceSMOImpl;

    /**
     * 员工社区服务接口
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能：验证请求参数的有效性，包括分页参数、管理员权限和结束时间处理
     * 
     * @param event CMD事件对象，包含请求相关信息
     * @param context 数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当时间解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
        
        // 处理结束时间参数
        String endTime = reqJson.getString("endTime");
        // 如果结束时间不为空且不包含冒号（表示只有日期没有时间）
        if (!StringUtil.isEmpty(endTime) && !endTime.contains(":")) {
            // 将结束时间设置为当天的23:59:59，确保包含整天的数据
            endTime += " 23:59:59";
            reqJson.put("endTime", endTime);
        }
    }

    /**
     * 命令执行方法
     * 
     * 功能：执行费用明细查询业务逻辑，包括参数转换、数据查询和响应设置
     * 
     * @param event CMD事件对象，包含请求相关信息
     * @param context 数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行失败时抛出命令异常
     * @throws ParseException 当时间解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为统计DTO对象
        ReportFeeMonthStatisticsDto reportFeeMonthStatisticsDto = BeanConvertUtil.covertBean(reqJson, ReportFeeMonthStatisticsDto.class);

        // 设置当前年份和月份作为查询条件
        reportFeeMonthStatisticsDto.setFeeYear(DateUtil.getYear() + "");
        reportFeeMonthStatisticsDto.setFeeMonth(DateUtil.getMonth() + "");
        
        // 从上下文中获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询当前员工有权限的社区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果社区ID列表不为空，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            // 将List转换为数组格式，设置到查询条件中
            reportFeeMonthStatisticsDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 调用服务层进行费用明细查询
        ResultVo resultVo = queryPayFeeDetailInnerServiceSMOImpl.query(reportFeeMonthStatisticsDto);

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}