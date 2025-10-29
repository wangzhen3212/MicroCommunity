package com.java110.community.cmd.index;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.repair.RepairDto;
import com.java110.dto.audit.AuditUser;
import com.java110.intf.common.IComplaintUserInnerServiceSMO;
import com.java110.intf.common.IGoodCollectionUserInnerServiceSMO;
import com.java110.intf.common.IResourceEntryStoreInnerServiceSMO;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRepairInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * 首页待办任务查询命令类
 * 
 * 该类用于查询用户在首页需要处理的各类待办任务数量，包括投诉、报修、采购、物品领用等模块的待办和已办任务统计
 * 通过调用各个业务模块的服务接口，汇总用户的待办任务信息并返回给前端展示
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode index.queryIndexTodoTask
 */
@Java110Cmd(serviceCode = "index.queryIndexTodoTask")
public class QueryIndexTodoTaskCmd extends Cmd {

    /** 投诉用户服务接口 */
    @Autowired
    private IComplaintUserInnerServiceSMO complaintUserInnerServiceSMOImpl;

    /** 报修服务接口 */
    @Autowired
    private IRepairInnerServiceSMO repairInnerServiceSMOImpl;

    /** 资源入库服务接口 */
    @Autowired
    private IResourceEntryStoreInnerServiceSMO resourceEntryStoreInnerServiceSMOImpl;

    /** 物品领用服务接口 */
    @Autowired
    private IGoodCollectionUserInnerServiceSMO goodCollectionUserInnerServiceSMOImpl;

    /** 停车位服务接口 */
    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    /** 菜单权限服务接口 */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，当前实现为空，可根据业务需求添加参数校验逻辑
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 参数验证逻辑，当前为空实现，可根据业务需求添加校验规则
    }

    /**
     * 命令执行方法
     * 
     * 主要业务逻辑处理方法，查询用户在各个业务模块的待办和已办任务数量
     * 包括投诉、报修、采购、物品领用等模块的任务统计
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象，包含storeId、userId、communityId等必要参数
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建审核用户对象，设置用户相关信息
        AuditUser auditUser = new AuditUser();
        auditUser.setStoreId(reqJson.getString("storeId"));      // 设置店铺ID
        auditUser.setUserId(reqJson.getString("userId"));        // 设置用户ID
        auditUser.setCommunityId(reqJson.getString("communityId")); // 设置社区ID

        // 查询投诉待办任务数量
        long complaintCount = complaintUserInnerServiceSMOImpl.getUserTaskCount(auditUser);

        // 查询投诉已办任务数量
        long complaintHisCount = complaintUserInnerServiceSMOImpl.getUserHistoryTaskCount(auditUser);

        // 创建报修查询对象和权限验证对象
        RepairDto ownerRepairDto = new RepairDto();
        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        
        // 检查用户是否具有查看报修列表的权限
        basePrivilegeDto.setResource("/viewListStaffRepairs"); // 设置权限资源路径
        basePrivilegeDto.setUserId(reqJson.getString("userId")); // 设置用户ID
        List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        
        // 如果用户没有查看所有报修的权限，则只查询该用户相关的报修
        if (privileges.size() == 0) {
            ownerRepairDto.setStaffId(reqJson.getString("userId")); // 设置员工ID，限定查询范围
        }
        ownerRepairDto.setCommunityId(reqJson.getString("communityId")); // 设置社区ID
        // 查询报修待办任务数量
        int repairCount = repairInnerServiceSMOImpl.queryStaffRepairsCount(ownerRepairDto);

        // 查询报修已办任务数量
        basePrivilegeDto.setResource("/listStaffFinishRepairs"); // 设置已完成报修列表权限资源
        List<Map> privileges2 = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);
        
        // 根据权限设置查询条件
        if (privileges2.size() == 0) {
            ownerRepairDto.setStaffId(reqJson.getString("userId")); // 无权限时只查询当前用户的已办报修
        } else {
            ownerRepairDto.setStaffId(""); // 有权限时查询所有已办报修
        }
        // 查询报修已办任务数量
        int repairHisCount = repairInnerServiceSMOImpl.queryStaffFinishRepairsCount(ownerRepairDto);

        // 查询采购待办任务数量
        long purchaseCount = resourceEntryStoreInnerServiceSMOImpl.getUserTaskCount(auditUser);

        // 查询采购已办任务数量
        long purchaseHisCount = resourceEntryStoreInnerServiceSMOImpl.getUserHistoryTaskCount(auditUser);

        // 查询物品领用待办任务数量
        long collectionCount = goodCollectionUserInnerServiceSMOImpl.getUserTaskCount(auditUser);

        // 查询物品领用已办任务数量
        long collectionHisCount = goodCollectionUserInnerServiceSMOImpl.getUserHistoryTaskCount(auditUser);

        // 构建返回结果对象
        JSONObject paramOut = new JSONObject();
        paramOut.put("complaintCount", complaintCount);          // 投诉待办数量
        paramOut.put("complaintHisCount", complaintHisCount);    // 投诉已办数量
        paramOut.put("repairCount", repairCount);                // 报修待办数量
        paramOut.put("repairHisCount", repairHisCount);          // 报修已办数量
        paramOut.put("purchaseCount", purchaseCount);            // 采购待办数量
        paramOut.put("purchaseHisCount", purchaseHisCount);      // 采购已办数量
        paramOut.put("collectionCount", collectionCount);        // 物品领用待办数量
        paramOut.put("collectionHisCount", collectionHisCount);  // 物品领用已办数量
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(paramOut));
    }
}