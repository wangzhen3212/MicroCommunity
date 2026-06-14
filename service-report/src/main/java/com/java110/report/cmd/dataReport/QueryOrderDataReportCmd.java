package com.java110.report.cmd.dataReport;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.report.QueryStatisticsDto;
import com.java110.report.statistics.IOrderStatistics;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

/**
 * 数据统计工单类统计命令类
 * 
 * 该类用于处理数据报表查询请求，统计各类工单的数量信息
 * 包括投诉单、报修单、巡检单、保养单、业主反馈和充电订单等
 * 支持按小区和时间范围进行统计查询
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "dataReport.queryOrderDataReport")
public class QueryOrderDataReportCmd extends Cmd {

    /**
     * 订单统计服务接口，用于获取各类工单的统计数据
     */
    @Autowired
    private IOrderStatistics orderStatisticsImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数是否完整，并对时间参数进行格式化处理
     * 如果时间参数不包含时分秒，自动补充默认时间
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当时间参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "startDate", "未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "endDate", "未包含结束时间");
        
        // 获取时间参数
        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        
        // 如果开始时间不包含时分秒，补充默认开始时间 00:00:00
        if (!startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        
        // 如果结束时间不包含时分秒，补充默认结束时间 23:59:59
        if (!endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }
    }

    /**
     * 执行数据统计命令
     * 
     * 根据请求参数查询各类工单的统计数据，包括总数、未完成数和完成数
     * 将统计结果封装成JSON数组返回给前端
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行失败时抛出命令异常
     * @throws ParseException 当时间参数解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建统计查询数据传输对象
        QueryStatisticsDto queryStatisticsDto = new QueryStatisticsDto();
        queryStatisticsDto.setStartDate(reqJson.getString("startDate"));
        queryStatisticsDto.setEndDate(reqJson.getString("endDate"));
        queryStatisticsDto.setCommunityId(reqJson.getString("communityId"));
        
        // 初始化数据数组，用于存储所有统计结果
        JSONArray datas = new JSONArray();
        JSONObject data = null;
        
        // 查询投诉单总数
        double complaintOrderCount = orderStatisticsImpl.getComplaintOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","投诉单");
        data.put("value", complaintOrderCount);
        datas.add(data);

        // 查询未完成投诉单数量
        double undoComplaintOrderCount = orderStatisticsImpl.getUndoComplaintOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","未完成投诉单");
        data.put("value", undoComplaintOrderCount);
        datas.add(data);

        // 查询已完成投诉单数量
        double finishComplaintOrderCount = orderStatisticsImpl.getFinishComplaintOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","完成投诉单");
        data.put("value", finishComplaintOrderCount);
        datas.add(data);

        // 查询报修单总数
        double repairOrderCount = orderStatisticsImpl.getRepairOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","报修单");
        data.put("value", repairOrderCount);
        datas.add(data);

        // 查询未完成报修单数量
        double undoRepairOrderCount = orderStatisticsImpl.getUndoRepairOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","未完成报修单");
        data.put("value", undoRepairOrderCount);
        datas.add(data);

        // 查询已完成报修单数量
        double finishRepairOrderCount = orderStatisticsImpl.getFinishRepairOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","完成报修单");
        data.put("value", finishRepairOrderCount);
        datas.add(data);

        // 查询巡检单总数
        double inspectionOrderCount = orderStatisticsImpl.getInspectionOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","巡检");
        data.put("value", inspectionOrderCount);
        datas.add(data);

        // 查询未完成巡检单数量
        double undoInspectionOrderCount = orderStatisticsImpl.getUndoInspectionOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","未完成巡检");
        data.put("value", undoInspectionOrderCount);
        datas.add(data);

        // 查询已完成巡检单数量
        double finishInspectionOrderCount = orderStatisticsImpl.getFinishInspectionOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","完成巡检");
        data.put("value", finishInspectionOrderCount);
        datas.add(data);

        // 查询保养单总数
        double maintainanceOrderCount = orderStatisticsImpl.getMaintainanceOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","保养");
        data.put("value", maintainanceOrderCount);
        datas.add(data);

        // 查询未完成保养单数量
        double undoMaintainanceOrderCount = orderStatisticsImpl.getUndoMaintainanceOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","未完成保养");
        data.put("value", undoMaintainanceOrderCount);
        datas.add(data);

        // 查询已完成保养单数量
        double finishMaintainanceOrderCount = orderStatisticsImpl.getFinishMaintainanceOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","完成保养");
        data.put("value", finishMaintainanceOrderCount);
        datas.add(data);

        // 查询业主反馈数量
        double notepadOrderCount = orderStatisticsImpl.getNotepadOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","业主反馈");
        data.put("value", notepadOrderCount);
        datas.add(data);

        // 查询充电订单数量
        double chargeMachineOrderCount = orderStatisticsImpl.getChargeMachineOrderCount(queryStatisticsDto);
        data = new JSONObject();
        data.put("name","充电订单");
        data.put("value", chargeMachineOrderCount);
        datas.add(data);

        // 将统计结果设置到响应中返回给前端
        context.setResponseEntity(ResultVo.createResponseEntity(datas));
    }
}