package com.java110.store.cmd.propertyIndex;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.repair.RepairDto;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 物业报修指标查询命令类
 * 
 * 该类用于查询物业报修相关的统计指标数据，包括不同状态的报修单数量统计
 * 通过serviceCode "propertyIndex.queryRepairIndex" 标识该命令
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "propertyIndex.queryRepairIndex")
public class QueryRepairIndexCmd extends Cmd {

    /**
     * 报修服务接口，用于查询报修相关数据
     */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的小区信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 命令执行方法
     * 
     * 查询不同状态的报修单数量统计，包括全部报修、待派单、处理中和已完成的数量
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建返回结果对象
        JSONObject paramOut = new JSONObject();

        // 查询全部报修单数量
        RepairDto repairDto = new RepairDto();
        repairDto.setCommunityId(reqJson.getString("communityId"));
        int allCount = repairInnerServiceSMOImpl.queryRepairsCount(repairDto);
        paramOut.put("allCount", allCount);

        // 查询待派单状态的报修单数量
        repairDto = new RepairDto();
        repairDto.setCommunityId(reqJson.getString("communityId"));
        repairDto.setState(RepairDto.STATE_WAIT); // 设置状态为待派单
        int waitCount = repairInnerServiceSMOImpl.queryRepairsCount(repairDto);
        paramOut.put("waitCount", waitCount);

        // 查询处理中状态的报修单数量
        repairDto = new RepairDto();
        repairDto.setCommunityId(reqJson.getString("communityId"));
        // 设置多个处理中状态：接单中、退回、转单、支付中、支付失败
        repairDto.setStatess(new String[]{
            RepairDto.STATE_TAKING, 
            RepairDto.STATE_BACK, 
            RepairDto.STATE_TRANSFER, 
            RepairDto.STATE_PAY, 
            RepairDto.STATE_PAY_ERROR
        });
        int doingCount = repairInnerServiceSMOImpl.queryRepairsCount(repairDto);
        paramOut.put("doingCount", doingCount);

        // 查询已完成状态的报修单数量
        repairDto = new RepairDto();
        repairDto.setCommunityId(reqJson.getString("communityId"));
        // 设置多个完成状态：待评价、待回访、已完成、未处理、已终止
        repairDto.setStatess(new String[]{
            RepairDto.STATE_APPRAISE, 
            RepairDto.STATE_RETURN_VISIT, 
            RepairDto.STATE_COMPLATE, 
            RepairDto.STATE_UNPROCESSED, 
            RepairDto.STATE_STOP
        });
        int finishCount = repairInnerServiceSMOImpl.queryRepairsCount(repairDto);
        paramOut.put("finishCount", finishCount);
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(paramOut));
    }
}