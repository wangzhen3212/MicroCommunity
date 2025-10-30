package com.java110.report.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerDto;
import com.java110.intf.report.IReportOrderStatisticsInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询业主就餐情况命令类
 * 
 * 该命令类用于处理业主就餐情况的查询请求，包括参数验证、数据查询和结果返回等功能。
 * 主要功能包括验证查询参数、查询业主就餐统计数据、格式化返回结果等。
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode owner.queryOwnerReserveGoods
 */
@Java110Cmd(serviceCode = "owner.queryOwnerReserveGoods")
public class QueryOwnerReserveGoodsCmd extends Cmd {

    /**
     * 报表订单统计内部服务接口
     */
    @Autowired
    private IReportOrderStatisticsInnerServiceSMO reportOrderStatisticsInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证请求参数是否完整和有效，包括必填字段检查和日期格式处理。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当日期解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区");
        Assert.hasKeyAndValue(reqJson,"startDate","未包含开始时间");
        Assert.hasKeyAndValue(reqJson,"endDate","未包含结束时间");
        
        // 处理开始时间格式，如果没有包含时间部分则添加默认时间
        String startDate = reqJson.getString("startDate");
        String endDate = reqJson.getString("endDate");
        if (!startDate.contains(":")) {
            startDate += " 00:00:00";
            reqJson.put("startDate", startDate);
        }
        // 处理结束时间格式，如果没有包含时间部分则添加默认时间
        if (!endDate.contains(":")) {
            endDate += " 23:59:59";
            reqJson.put("endDate", endDate);
        }
    }

    /**
     * 执行查询业主就餐情况命令
     * 
     * 该方法负责查询业主就餐统计数据，包括数据总数和详细信息，并返回格式化后的响应结果。
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行失败时抛出命令异常
     * @throws ParseException 当日期解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取分页行数
        int row = reqJson.getInteger("row");
        // 将请求参数转换为OwnerDto对象
        OwnerDto ownerDto = BeanConvertUtil.covertBean(reqJson, OwnerDto.class);

        // 查询业主就餐统计总数
        int total = reportOrderStatisticsInnerServiceSMOImpl.getOwnerReserveGoodsCount(ownerDto);
        
        List<Map> infos = null;
        // 根据总数判断是否需要查询详细信息
        if (total > 0) {
            // 查询业主就餐统计详细信息
            infos = reportOrderStatisticsInnerServiceSMOImpl.getOwnerReserveGoods(ownerDto);
        } else {
            // 如果没有数据，返回空列表
            infos = new ArrayList<>();
        }

        // 刷新开始时间和结束时间到结果中
        freshStartDateAndEndDate(infos, ownerDto);

        // 计算总页数并创建响应实体
        // 使用Math.ceil确保向上取整，避免数据丢失
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, infos);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新开始时间和结束时间到结果列表中
     * 
     * 该方法遍历查询结果列表，为每个结果项添加开始时间和结束时间字段。
     * 
     * @param infos 查询结果列表，每个元素为Map类型
     * @param ownerDto 业主数据传输对象，包含开始时间和结束时间信息
     */
    private void freshStartDateAndEndDate(List<Map> infos, OwnerDto ownerDto) {
        // 检查结果列表是否为空
        if(infos == null || infos.size() < 1){
            return ;
        }

        // 遍历所有结果项，添加开始时间和结束时间
        for(Map info : infos){
            info.put("startDate", ownerDto.getStartDate());
            info.put("endDate", ownerDto.getEndDate());
        }
    }
}