package com.java110.community.cmd.ownerRepair;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.repair.RepairDto;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询员工待办维修工单命令类
 * 
 * 该命令类用于处理查询员工待处理的维修工单请求，主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 查询员工待办维修工单的数量和详细信息
 * 3. 返回分页格式的查询结果
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode ownerRepair.listAdminUndoRepairsByStaff
 */
@Java110Cmd(serviceCode = "ownerRepair.listAdminUndoRepairsByStaff")
public class ListAdminUndoRepairsByStaffCmd extends Cmd {

    /**
     * 维修服务接口，用于调用维修相关的业务逻辑
     */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，包括：
     * 1. 验证分页参数
     * 2. 验证管理员权限
     * 3. 验证员工ID参数是否存在
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 数据流上下文对象，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数（页码、每页行数等）
        super.validatePageInfo(reqJson);
        // 验证当前用户是否具有管理员权限
        super.validateAdmin(context);
        // 验证请求中必须包含员工ID参数
        Assert.hasKeyAndValue(reqJson, "staffId", "请求中未包含员工信息");
    }

    /**
     * 执行查询员工待办维修工单命令
     * 
     * 该方法主要功能：
     * 1. 将请求参数转换为维修数据传输对象
     * 2. 查询员工待办维修工单总数
     * 3. 根据分页参数查询具体的维修工单列表
     * 4. 构建并返回分页响应结果
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 数据流上下文对象，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出
     * @throws ParseException 当数据解析出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为RepairDto对象，便于后续业务处理
        RepairDto ownerRepairDto = BeanConvertUtil.covertBean(reqJson, RepairDto.class);
        
        // 查询员工待办维修工单的总数量
        int count = repairInnerServiceSMOImpl.queryStaffRepairsCount(ownerRepairDto);
        
        // 定义维修工单列表
        List<RepairDto> ownerRepairs = null;
        
        // 如果存在待办工单，则查询具体的工单列表
        if (count > 0) {
            ownerRepairs = repairInnerServiceSMOImpl.queryStaffRepairs(ownerRepairDto);
        } else {
            // 如果没有待办工单，返回空列表
            ownerRepairs = new ArrayList<>();
        }
        
        // 计算总页数：总记录数 / 每页行数，向上取整
        // 创建包含分页信息的响应实体，包括总页数、总记录数和数据列表
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(
            (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), // 计算总页数
            count, // 总记录数
            ownerRepairs // 当前页数据列表
        );
        
        // 将响应实体设置到上下文中，供后续处理使用
        context.setResponseEntity(responseEntity);
    }
}