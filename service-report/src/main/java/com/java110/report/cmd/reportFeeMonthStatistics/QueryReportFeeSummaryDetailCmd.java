package com.java110.report.cmd.reportFeeMonthStatistics;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.data.DataPrivilegeStaffDto;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.intf.community.IDataPrivilegeUnitV1InnerServiceSMO;
import com.java110.report.statistics.IFeeStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 费用汇总明细查询命令类
 * 
 * 该类负责处理费用汇总明细查询请求，包括参数验证、数据权限过滤、费用统计查询等功能
 * 通过IFeeStatistics接口获取费用统计数据，并返回分页查询结果
 * 
 * @author Java110
 * @version 1.0
 * @see Cmd
 * @see IFeeStatistics
 */
@Java110Cmd(serviceCode = "/reportFeeMonthStatistics.queryReportFeeSummaryDetail")
public class QueryReportFeeSummaryDetailCmd extends Cmd {

    /**
     * 费用统计服务接口，用于查询费用汇总数据
     */
    @Autowired
    private IFeeStatistics feeStatisticsImpl;

    /**
     * 数据权限单元服务接口，用于查询用户有权限的单元信息
     */
    @Autowired
    private IDataPrivilegeUnitV1InnerServiceSMO dataPrivilegeUnitV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 验证请求参数是否完整，包括分页信息、必填字段等
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证开始日期参数
        Assert.hasKeyAndValue(reqJson, "startDate", "未包含开始日期");
        // 验证结束日期参数
        Assert.hasKeyAndValue(reqJson, "endDate", "未包含结束日期");
        // 验证小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 执行费用汇总明细查询命令
     * 
     * 处理费用汇总明细查询请求，包括参数设置、数据权限过滤、统计查询和结果返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建查询统计数据传输对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        // 设置小区ID
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        // 设置开始日期
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        // 设置结束日期
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
        // 设置配置ID
        queryStatisticsDto.setConfigId(reqJson.getString("configId"));
        // 设置楼层ID
        queryStatisticsDto.setFloorId(reqJson.getString("floorId"));
        // 设置对象名称
        queryStatisticsDto.setObjName(reqJson.getString("objName"));
        // 设置费用类型编码
        queryStatisticsDto.setFeeTypeCd(reqJson.getString("feeTypeCd"));
        // 设置业主名称
        queryStatisticsDto.setOwnerName(reqJson.getString("ownerName"));
        // 设置链接信息
        queryStatisticsDto.setLink(reqJson.getString("link"));
        // 设置页码
        queryStatisticsDto.setPage(reqJson.getIntValue("page"));
        // 设置每页记录数
        queryStatisticsDto.setRow(reqJson.getIntValue("row"));
        
        // 处理配置ID数组参数
        if (reqJson.containsKey("configIds")) {
            queryStatisticsDto.setConfigIds(reqJson.getString("configIds").split(","));
        }

        // 获取当前用户ID
        String staffId = context.getReqHeaders().get("user-id");
        DataPrivilegeStaffDto dataPrivilegeStaffDto = new DataPrivilegeStaffDto();
        dataPrivilegeStaffDto.setStaffId(staffId);
        // 查询用户有权限的单元ID数组
        String[] unitIds = dataPrivilegeUnitV1InnerServiceSMOImpl.queryDataPrivilegeUnitsByStaff(dataPrivilegeStaffDto);

        // 如果用户有权限的单元不为空，则设置到查询条件中
        if (unitIds != null && unitIds.length > 0) {
            queryStatisticsDto.setUnitIds(unitIds);
        }

        // 查询费用汇总记录总数
        int count = feeStatisticsImpl.getObjFeeSummaryCount(queryStatisticsDto);

        List<Map> datas = null;
        // 如果有数据，则查询详细数据
        if (count > 0) {
            datas = feeStatisticsImpl.getObjFeeSummary(queryStatisticsDto);
        } else {
            // 如果没有数据，返回空列表
            datas = new ArrayList<>();
        }

        // 创建返回结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) queryStatisticsDto.getRow()), count, datas);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}