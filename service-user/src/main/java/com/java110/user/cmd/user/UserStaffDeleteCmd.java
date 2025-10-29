/**
 * 员工删除命令类
 * 
 * 该命令类用于处理删除员工的相关业务逻辑，包括删除员工基本信息、用户信息、
 * 以及员工相关的各种业务数据（如报修设置、排班数据、考勤组、巡检任务、权限等）
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.attendance.AttendanceClassesStaffDto;
import com.java110.dto.classes.ScheduleClassesStaffDto;
import com.java110.dto.data.DataPrivilegeStaffDto;
import com.java110.dto.inspection.InspectionPlanStaffDto;
import com.java110.dto.maintainance.MaintainancePlanStaffDto;
import com.java110.dto.privilege.PrivilegeUserDto;
import com.java110.dto.repair.RepairTypeUserDto;
import com.java110.dto.user.StaffAppAuthDto;
import com.java110.intf.community.IInspectionPlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IMaintainancePlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IRepairTypeUserInnerServiceSMO;
import com.java110.intf.community.IRepairTypeUserV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesStaffV1InnerServiceSMO;
import com.java110.intf.store.IStoreUserV1InnerServiceSMO;
import com.java110.intf.user.*;
import com.java110.po.attendance.AttendanceClassesStaffPo;
import com.java110.po.classes.ScheduleClassesStaffPo;
import com.java110.po.inspection.InspectionPlanStaffPo;
import com.java110.po.maintainance.MaintainancePlanStaffPo;
import com.java110.po.privilege.DataPrivilegeStaffPo;
import com.java110.po.privilege.PrivilegeUserPo;
import com.java110.po.repair.RepairTypeUserPo;
import com.java110.po.store.StoreUserPo;
import com.java110.po.user.StaffAppAuthPo;
import com.java110.po.user.UserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 删除员工接口文档注解
 * 提供外部系统删除员工功能的API文档信息
 */
@Java110CmdDoc(title = "删除员工",
        description = "外部系统通过删除员工接口 删除员工 注意需要物业管理员账号登录，因为不需要传storeId 是根据管理员登录信息获取的",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/user.staff.delete",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "user.staff.delete",
        seq = 5
)

/**
 * 请求参数文档注解
 * 定义接口的请求参数信息
 */
@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "userId", length = 30, remark = "员工ID"),
})

/**
 * 响应参数文档注解
 * 定义接口的响应参数信息
 */
@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

/**
 * 请求响应示例文档注解
 * 提供接口调用示例
 */
@Java110ExampleDoc(
        reqBody = "{\"userId\":\"123123\"}",
        resBody = "{'code':0,'msg':'成功'"
)

/**
 * 命令注解，标识该类的服务代码
 */
@Java110Cmd(serviceCode = "user.staff.delete")
public class UserStaffDeleteCmd extends Cmd {

    // 依赖注入各种服务接口
    @Autowired
    private IRepairTypeUserInnerServiceSMO repairTypeUserInnerServiceSMOImpl;

    @Autowired
    private IStoreUserV1InnerServiceSMO storeUserV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IPrivilegeUserV1InnerServiceSMO privilegeUserV1InnerServiceSMOImpl;

    @Autowired
    private IRepairTypeUserV1InnerServiceSMO repairTypeUserV1InnerServiceSMOImpl;

    @Autowired
    private IScheduleClassesStaffV1InnerServiceSMO scheduleClassesStaffV1InnerServiceSMOImpl;

    @Autowired
    private IAttendanceClassesStaffV1InnerServiceSMO attendanceClassesStaffV1InnerServiceSMOImpl;

    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;

    @Autowired
    private IStaffAppAuthInnerServiceSMO staffAppAuthInnerServiceSMOImpl;

    @Autowired
    private IDataPrivilegeStaffV1InnerServiceSMO dataPrivilegeStaffV1InnerServiceSMOImpl;

    @Autowired
    private IMaintainancePlanStaffV1InnerServiceSMO maintainancePlanStaffV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否包含必要的字段，并检查环境配置
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 检查是否为开发环境
        Environment.isDevEnv();
        // 验证请求参数中必须包含storeId字段
        Assert.jsonObjectHaveKey(reqJson, "storeId", "请求参数中未包含storeId 节点，请确认");
        // 验证请求参数中必须包含userId字段
        Assert.jsonObjectHaveKey(reqJson, "userId", "请求参数中未包含userId 节点，请确认");
    }

    /**
     * 命令执行方法
     * 
     * 执行删除员工的核心业务逻辑，包括删除员工相关的所有数据
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当删除操作失败时抛出异常
     * @throws ParseException 当数据解析失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 删除员工基本信息
        deleteStaff(reqJson);

        // 删除用户信息
        deleteUser(reqJson);

        // 删除报修设置信息
        RepairTypeUserDto repairTypeUserDto = new RepairTypeUserDto();
        repairTypeUserDto.setStaffId(reqJson.getString("userId"));
        repairTypeUserDto.setStatusCd("0"); // 设置状态码为有效状态
        List<RepairTypeUserDto> repairTypeUserDtoList = repairTypeUserInnerServiceSMOImpl.queryRepairTypeUsers(repairTypeUserDto);
        // 如果存在报修设置数据，则逐个删除
        if (!ListUtil.isNull(repairTypeUserDtoList)) {
            for (RepairTypeUserDto tmpRepairTypeUserDto : repairTypeUserDtoList) {
                JSONObject typeUserJson1 = (JSONObject) JSONObject.toJSON(tmpRepairTypeUserDto);
                deleteRepairTypeUser(typeUserJson1);
            }
        }
        
        // 删除员工排班数据
        deleteScheduleClassesStaff(reqJson);

        // 删除员工考勤组信息
        deleteAttendanceClassesStaff(reqJson);

        // 删除巡检员工信息
        deleteInspectionPlanStaff(reqJson);

        // 删除用户权限信息
        deleteUserPrivilege(reqJson);

        // 删除员工认证信息
        deleteStaffAppAuth(reqJson);

        // 删除数据权限信息
        deleteStaffDataPri(reqJson);

        // 删除保养员工信息
        deleteMaintainanceStaff(reqJson);
    }

    /**
     * 删除员工数据权限信息
     * 
     * 根据员工ID删除该员工的所有数据权限配置
     * 
     * @param reqJson 请求的JSON数据，包含员工ID信息
     */
    private void deleteStaffDataPri(JSONObject reqJson) {
        // 构建查询条件
        DataPrivilegeStaffDto dataPrivilegeStaffDto = new DataPrivilegeStaffDto();
        dataPrivilegeStaffDto.setStaffId(reqJson.getString("userId"));
        // 查询员工的数据权限配置
        List<DataPrivilegeStaffDto> dataPrivilegeStaffDtos
                = dataPrivilegeStaffV1InnerServiceSMOImpl.queryDataPrivilegeStaffs(dataPrivilegeStaffDto);
        
        // 如果没有数据权限配置，直接返回
        if(ListUtil.isNull(dataPrivilegeStaffDtos)){
            return;
        }
        
        // 逐个删除数据权限配置
        for(DataPrivilegeStaffDto tmpDataPrivilegeStaffDto:dataPrivilegeStaffDtos){
            DataPrivilegeStaffPo dataPrivilegeStaffPo = new DataPrivilegeStaffPo();
            dataPrivilegeStaffPo.setDpsId(tmpDataPrivilegeStaffDto.getDpsId()); // 设置数据权限ID
            dataPrivilegeStaffV1InnerServiceSMOImpl.deleteDataPrivilegeStaff(dataPrivilegeStaffPo);
        }
    }

    /**
     * 删除员工应用认证信息
     * 
     * 根据员工ID删除该员工的所有应用认证信息
     * 
     * @param reqJson 请求的JSON数据，包含员工ID信息
     */
    private void deleteStaffAppAuth(JSONObject reqJson) {
        // 构建查询条件
        StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
        staffAppAuthDto.setStaffId(reqJson.getString("userId"));
        // 查询员工的应用认证信息
        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);
        
        // 如果没有应用认证信息，直接返回
        if(ListUtil.isNull(staffAppAuthDtos)){
            return;
        }
        
        // 逐个删除应用认证信息
        for(StaffAppAuthDto tmpStaffAppAuthDto:staffAppAuthDtos){
            StaffAppAuthPo staffAppAuthPo = new StaffAppAuthPo();
            staffAppAuthPo.setAuId(tmpStaffAppAuthDto.getAuId()); // 设置认证ID
            staffAppAuthInnerServiceSMOImpl.deleteStaffAppAuth(staffAppAuthPo);
        }
    }

    /**
     * 删除巡检员工信息
     * 
     * 根据员工ID删除该员工的所有巡检任务分配信息
     * 
     * @param reqJson 请求的JSON数据，包含员工ID信息
     */
    private void deleteInspectionPlanStaff(JSONObject reqJson) {
        // 构建查询条件
        InspectionPlanStaffDto inspectionPlanStaffDto = new InspectionPlanStaffDto();
        inspectionPlanStaffDto.setStaffId(reqJson.getString("userId"));
        // 查询员工的巡检任务分配信息
        List<InspectionPlanStaffDto> inspectionPlanStaffDtos
                = inspectionPlanStaffV1InnerServiceSMOImpl.queryInspectionPlanStaffs(inspectionPlanStaffDto);
        
        // 如果没有巡检任务分配信息，直接返回
        if(ListUtil.isNull(inspectionPlanStaffDtos)){
            return;
        }

        // 逐个删除巡检任务分配信息
        for(InspectionPlanStaffDto tmpInspectionPlanStaffDto:inspectionPlanStaffDtos){
            InspectionPlanStaffPo inspectionPlanStaffPo = new InspectionPlanStaffPo();
            inspectionPlanStaffPo.setIpStaffId(tmpInspectionPlanStaffDto.getIpStaffId()); // 设置巡检员工ID
            inspectionPlanStaffV1InnerServiceSMOImpl.deleteInspectionPlanStaff(inspectionPlanStaffPo);
        }
    }

    /**
     * 删除保养员工信息
     * 
     * 根据员工ID删除该员工的所有保养任务分配信息
     * 
     * @param reqJson 请求的JSON数据，包含员工ID信息
     */
    private void deleteMaintainanceStaff(JSONObject reqJson) {
        // 构建查询条件
        MaintainancePlanStaffDto maintainancePlanStaffDto = new MaintainancePlanStaffDto();
        maintainancePlanStaffDto.setStaffId(reqJson.getString("userId"));
        // 查询员工的保养任务分配信息
        List<MaintainancePlanStaffDto> maintainancePlanStaffDtos
                = maintainancePlanStaffV1InnerServiceSMOImpl.queryMaintainancePlanStaffs(maintainancePlanStaffDto);
        
        // 如果没有保养任务分配信息，直接返回
        if(ListUtil.isNull(maintainancePlanStaffDtos)){
            return;
        }

        // 逐个删除保养任务分配信息
        for(MaintainancePlanStaffDto tmpMaintainancePlanStaffDto:maintainancePlanStaffDtos){
            MaintainancePlanStaffPo maintainancePlanStaffPo = new MaintainancePlanStaffPo();
            maintainancePlanStaffPo.setMpsId(tmpMaintainancePlanStaffDto.getMpsId()); // 设置保养员工ID
            maintainancePlanStaffV1InnerServiceSMOImpl.deleteMaintainancePlanStaff(maintainancePlanStaffPo);
        }
    }

    /**
     * 删除员工考勤组信息
     * 
     * 根据员工ID删除该员工的所有考勤组分配信息
     * 
     * @param reqJson 请求的JSON数据，包含员工ID信息
     */
    private void deleteAttendanceClassesStaff(JSONObject reqJson) {
        // 构建查询条件
        AttendanceClassesStaffDto attendanceClassesStaffDto = new AttendanceClassesStaffDto();
        attendanceClassesStaffDto.setStaffId(reqJson.getString("userId"));
        // 查询员工的考勤组分配信息
        List<AttendanceClassesStaffDto> attendanceClassesStaffDtos
                = attendanceClassesStaffV1InnerServiceSMOImpl.queryAttendanceClassesStaffs(attendanceClassesStaffDto);
        
        // 如果没有考勤组分配信息，直接返回
        if(ListUtil.isNull(attendanceClassesStaffDtos)){
            return;
        }
        
        // 逐个删除考勤组分配信息
        for(AttendanceClassesStaffDto tmpAttendanceClassesStaffDto:attendanceClassesStaffDtos){
            AttendanceClassesStaffPo attendanceClassesStaffPo = new AttendanceClassesStaffPo();
            attendanceClassesStaffPo.setCsId(tmpAttendanceClassesStaffDto.getCsId()); // 设置考勤组ID
            attendanceClassesStaffV1InnerServiceSMOImpl.deleteAttendanceClassesStaff(attendanceClassesStaffPo);
        }
    }

    /**
     * 删除员工排班数据
     * 
     * 根据员工ID删除该员工的所有排班信息
     * 
     * @param reqJson 请求的JSON数据，包含员工ID信息
     */
    private void deleteScheduleClassesStaff(JSONObject reqJson) {
        // 构建查询条件
        ScheduleClassesStaffDto scheduleClassesStaffDto = new ScheduleClassesStaffDto();
        scheduleClassesStaffDto.setStaffId(reqJson.getString("userId"));
        // 查询员工的排班信息
        List<ScheduleClassesStaffDto> scheduleClassesStaffDtos
                = scheduleClassesStaffV1InnerServiceSMOImpl.queryScheduleClassesStaffs(scheduleClassesStaffDto);
        
        // 如果没有排班信息，直接返回
        if(ListUtil.isNull(scheduleClassesStaffDtos)){
            return;
        }
        
        // 逐个删除排班信息
        for (ScheduleClassesStaffDto tmpScheduleClassesStaffDto : scheduleClassesStaffDtos) {
            ScheduleClassesStaffPo scheduleClassesStaffPo = new ScheduleClassesStaffPo();
            scheduleClassesStaffPo.setScsId(tmpScheduleClassesStaffDto.getScsId()); // 设置排班ID
            scheduleClassesStaffV1InnerServiceSMOImpl.deleteScheduleClassesStaff(scheduleClassesStaffPo);
        }
    }

    /**
     * 删除用户权限信息
     * 
     * 根据用户ID删除该用户的所有权限配置
     * 
     * @param paramInJson 请求的JSON数据，包含用户ID信息
     * @throws CmdException 当删除权限失败时抛出异常
     */
    private void deleteUserPrivilege(JSONObject paramInJson) {
        // 构建查询条件
        PrivilegeUserDto privilegeUserDto = new PrivilegeUserDto();
        privilegeUserDto.setUserId(paramInJson.getString("userId"));
        // 查询用户的权限配置
        List<PrivilegeUserDto> privilegeUserDtos = privilegeUserV1InnerServiceSMOImpl.queryPrivilegeUsers(privilegeUserDto);

        // 如果没有权限配置，直接返回
        if (ListUtil.isNull(privilegeUserDtos)) {
            return;
        }

        // 逐个删除权限配置
        for (PrivilegeUserDto tmpPrivilegeUserDto : privilegeUserDtos) {
            PrivilegeUserPo privilegeUserPo = new PrivilegeUserPo();
            privilegeUserPo.setPuId(tmpPrivilegeUserDto.getPuId()); // 设置权限ID
            int flag = privilegeUserV1InnerServiceSMOImpl.deletePrivilegeUser(privilegeUserPo);
            // 检查删除操作是否成功
            if (flag < 1) {
                throw new CmdException("删除员工失败");
            }
        }
    }

    /**
     * 删除员工信息
     * 
     * 根据员工ID和商户ID删除员工的基本信息
     * 
     * @param paramInJson 请求的JSON数据，包含员工ID和商户ID信息
     * @throws CmdException 当删除员工失败时抛出异常
     */
    public void deleteStaff(JSONObject paramInJson) {
        // 构建删除参数
        JSONObject businessStoreUser = new JSONObject();
        businessStoreUser.put("storeId", paramInJson.getString("storeId")); // 设置商户ID
        businessStoreUser.put("userId", paramInJson.getString("userId"));   // 设置员工ID

        // 转换为PO对象
        StoreUserPo storeUserPo = BeanConvertUtil.covertBean(businessStoreUser, StoreUserPo.class);

        // 执行删除操作
        int flag = storeUserV1InnerServiceSMOImpl.deleteStoreUser(storeUserPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除员工失败");
        }
    }

    /**
     * 删除用户信息
     * 
     * 根据用户ID删除用户的基本信息
     * 
     * @param paramInJson 请求的JSON数据，包含用户ID信息
     * @throws CmdException 当删除用户失败时抛出异常
     */
    public void deleteUser(JSONObject paramInJson) {
        // 构建删除参数
        JSONObject businessStoreUser = new JSONObject();
        businessStoreUser.put("userId", paramInJson.getString("userId")); // 设置用户ID

        // 转换为PO对象
        UserPo userPo = BeanConvertUtil.covertBean(businessStoreUser, UserPo.class);
        // 执行删除操作
        int flag = userV1InnerServiceSMOImpl.deleteUser(userPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除员工失败");
        }
    }

    /**
     * 删除报修类型用户关联信息
     * 
     * 根据报修类型用户信息删除对应的关联关系
     * 
     * @param paramInJson 请求的JSON数据，包含报修类型用户信息
     * @throws CmdException 当删除报修类型用户失败时抛出异常
     */
    public void deleteRepairTypeUser(JSONObject paramInJson) {
        // 转换为PO对象
        RepairTypeUserPo repairTypeUserPo = BeanConvertUtil.covertBean(paramInJson, RepairTypeUserPo.class);
        // 执行删除操作
        int flag = repairTypeUserV1InnerServiceSMOImpl.deleteRepairTypeUser(repairTypeUserPo);
        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除员工失败");
        }
    }
}