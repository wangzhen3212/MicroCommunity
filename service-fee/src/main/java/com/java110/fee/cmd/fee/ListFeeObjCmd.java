/**
 * 费用对象查询命令类
 * 
 * 该类用于处理费用对象列表查询请求，通过接收前端传入的参数，查询指定费用下的所有费用对象信息。
 * 主要功能包括参数验证和费用对象数据查询，支持按周期、自定义时间等条件进行筛选。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.fee.bmo.IQueryOweFee;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

@Java110Cmd(serviceCode = "/feeApi/listFeeObj")
public class ListFeeObjCmd extends Cmd {

    /**
     * 费用查询服务接口，用于查询欠费信息
     */
    @Autowired
    private IQueryOweFee queryOweFeeImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数中是否包含必要的小区信息和费用信息，确保后续业务逻辑能够正常执行。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象，包含前端传入的所有参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
        // 验证请求参数中必须包含费用ID
        Assert.hasKeyAndValue(reqJson, "feeId", "未包含费用信息");
    }

    /**
     * 执行费用对象查询命令
     * 
     * 根据请求参数构建费用查询条件，调用费用查询服务获取费用对象列表，并将结果设置到响应中。
     * 支持按费用周期、客户结束时间等条件进行筛选，对于特定周期(105)还支持自定义时间范围查询。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象，包含前端传入的所有参数
     * @throws CmdException 当命令执行过程中发生错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建费用数据传输对象
        FeeDto feeDto = new FeeDto();
        // 设置费用ID
        feeDto.setFeeId(reqJson.getString("feeId"));
        // 设置小区ID
        feeDto.setCommunityId(reqJson.getString("communityId"));
        
        // 如果请求参数中包含周期信息且不为空，则设置到费用对象中
        if (reqJson.containsKey("cycle") && !StringUtil.isEmpty(reqJson.getString("cycle"))) {
            feeDto.setCycle(reqJson.getString("cycle"));
        }
        
        // 如果请求参数中包含客户结束时间且不为空，则设置到费用对象中
        if (reqJson.containsKey("custEndTime") && !StringUtil.isEmpty(reqJson.getString("custEndTime"))) {
            feeDto.setCustEndTime(reqJson.getString("custEndTime"));
        }

        // 特殊处理：当周期为105时，需要设置自定义开始时间和结束时间
        if("105".equals(reqJson.getString("cycle"))){
            feeDto.setCustomStartTime(reqJson.getString("customStartTime"));
            feeDto.setCustomEndTime(reqJson.getString("customEndTime"));
        }

        // 调用费用查询服务，获取费用对象列表
        ResponseEntity<String> result = queryOweFeeImpl.listFeeObj(feeDto);
        // 将查询结果设置到响应上下文中
        context.setResponseEntity(result);
    }
}