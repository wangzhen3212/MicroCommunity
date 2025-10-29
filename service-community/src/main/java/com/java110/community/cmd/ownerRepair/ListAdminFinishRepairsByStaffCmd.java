/**
 * 业主报修管理命令类 - 员工已办报修列表查询
 * 
 * 该命令类用于处理员工已完成的报修工单查询请求，支持PC端和移动端的维修已办功能。
 * 通过调用维修服务接口获取员工相关的已完成报修记录，并返回分页查询结果。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.repair.RepairDto;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.intf.community.IRepairUserInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Java110Cmd(serviceCode = "ownerRepair.listAdminFinishRepairsByStaff")
public class ListAdminFinishRepairsByStaffCmd extends Cmd {

    /**
     * 维修服务接口，用于查询维修相关信息
     */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /**
     * 维修人员服务接口，用于查询维修人员相关信息
     */
    @Autowired
    private IRepairUserInnerServiceSMO repairUserInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的合法性，包括分页信息验证、管理员权限验证和必要参数检查
     * 
     * @param event   命令事件对象，包含请求相关信息
     * @param context 数据流上下文对象，用于获取请求和响应数据
     * @param reqJson 请求报文JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数（页码、每页记录数）
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证员工ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "staffId", "请求中未包含员工信息");
    }

    /**
     * 执行员工已办报修查询命令
     * 
     * 根据员工ID查询其已完成的报修工单列表，支持按维修状态筛选和分页查询。
     * 主要用于PC端WEB维修已办功能，返回格式化的响应数据。
     * 
     * @param event   命令事件对象
     * @param context 数据流上下文对象，用于设置响应数据
     * @param reqJson 请求报文JSON对象，包含查询条件参数
     * @throws CmdException     当命令执行过程中出现业务异常时抛出
     * @throws ParseException   当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为RepairDto对象
        RepairDto ownerRepairDto = BeanConvertUtil.covertBean(reqJson, RepairDto.class);
        
        // 处理维修状态参数
        if (reqJson.containsKey("repairStates")) {
            // 如果请求中包含维修状态参数，按逗号分割并设置到DTO中
            String[] states = reqJson.getString("repairStates").split(",");
            ownerRepairDto.setStates(Arrays.asList(states));
        } else {
            // PC WEB维修已办功能：设置默认的已完成维修状态
            // 包括：退回、转单、已支付、支付错误、已评价、已回访、已完成等状态
            String[] states = {
                RepairDto.STATE_BACK, 
                RepairDto.STATE_TRANSFER,
                RepairDto.STATE_PAY, 
                RepairDto.STATE_PAY_ERROR, 
                RepairDto.STATE_APPRAISE, 
                RepairDto.STATE_RETURN_VISIT, 
                RepairDto.STATE_COMPLATE
            };
            ownerRepairDto.setStates(Arrays.asList(states));
        }
        
        // 查询员工已办报修记录总数
        int count = repairInnerServiceSMOImpl.queryStaffFinishRepairsCount(ownerRepairDto);
        List<RepairDto> ownerRepairs = null;
        
        // 根据查询结果数量处理数据
        if (count > 0) {
            // 如果有数据，查询具体的报修记录列表
            ownerRepairs = repairInnerServiceSMOImpl.queryStaffFinishRepairs(ownerRepairDto);
            // 注释掉的刷新员工姓名方法，可根据需要启用
            // refreshStaffName(ownerRepairs);
        } else {
            // 如果没有数据，返回空列表
            ownerRepairs = new ArrayList<>();
        }
        
        // 创建响应实体，包含分页信息和数据列表
        // 计算总页数：总记录数 / 每页记录数，向上取整
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(
            (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), 
            count, 
            ownerRepairs
        );
        
        // 设置响应数据到上下文
        context.setResponseEntity(responseEntity);
    }
}