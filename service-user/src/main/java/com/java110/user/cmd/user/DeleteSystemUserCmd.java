/**
 * 删除系统用户命令类
 * 
 * 该类负责处理删除系统用户的业务逻辑，包括删除用户基本信息、权限信息、关联数据等
 * 通过注解@Java110Cmd(serviceCode = "user.deleteSystemUser")标识为命令处理器
 * 继承自Cmd基类，实现validate和doCmd方法
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.attendance.AttendanceClassesStaffDto;
import com.java110.dto.classes.ScheduleClassesStaffDto;
import com.java110.dto.data.DataPrivilegeStaffDto;
import com.java110.dto.inspection.InspectionPlanStaffDto;
import com.java110.dto.maintainance.MaintainancePlanStaffDto;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.dto.privilege.PrivilegeUserDto;
import com.java110.dto.repair.RepairTypeUserDto;
import com.java110.dto.store.StoreUserDto;
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
import com.java110.po.owner.OwnerAppUserPo;
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

@Java110Cmd(serviceCode = "user.deleteSystemUser")
public class DeleteSystemUserCmd extends Cmd {

    // 注入各种服务接口
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

    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括管理员权限验证和必要参数检查
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证管理员权限
        super.validateAdmin(context);
        // 检查是否为开发环境
        Environment.isDevEnv();
        // 验证请求参数中是否包含staffId
        Assert.hasKeyAndValue(reqJson, "staffId", "未包含用户ID");
    }

    /**
     * 执行删除系统用户命令
     * 
     * 主要执行删除员工信息和用户信息的业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 删除员工信息
        deleteStaff(reqJson);

        //todo 删除用户
        deleteUser(reqJson);

        // 解绑业主关联
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setUserId(reqJson.getString("staffId"));
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);
        // 如果存在业主关联数据，则删除
        if (ListUtil.isNull(ownerAppUserDtos)) {
            return;
        }
        OwnerAppUserPo ownerAppUserPo = null;
        for (OwnerAppUserDto tmpOwnerAppUserDto : ownerAppUserDtos) {
            // 转换DTO为PO对象
            ownerAppUserPo = BeanConvertUtil.covertBean(tmpOwnerAppUserDto, OwnerAppUserPo.class);
            // 删除业主关联
            ownerAppUserV1InnerServiceSMOImpl.deleteOwnerAppUser(ownerAppUserPo);
        }
    }

    /**
     * 删除用户基本信息
     * 
     * 根据staffId删除用户表中的基本信息
     * 
     * @param paramInJson 请求参数JSON对象，包含staffId
     */
    public void deleteUser(JSONObject paramInJson) {
        UserPo userPo = new UserPo();
        userPo.setUserId(paramInJson.getString("staffId"));
        // 调用用户服务删除用户
        int flag = userV1InnerServiceSMOImpl.deleteUser(userPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除员工失败");
        }
    }

    /**
     * 删除员工关联信息
     * 
     * 删除员工相关的所有关联数据，包括商户关联、报修设置、排班数据等
     * 
     * @param reqJson 请求参数JSON对象，包含staffId
     */
    public void deleteStaff(JSONObject reqJson) {

        // 删除商户关联信息
        StoreUserDto storeUserDto = new StoreUserDto();
        storeUserDto.setUserId(reqJson.getString("staffId"));
        List<StoreUserDto> storeUserDtos = storeUserV1InnerServiceSMOImpl.queryStoreUsers(storeUserDto);
        // 如果存在商户关联数据，则删除
        if (ListUtil.isNull(storeUserDtos)) {
            return;
        }
        StoreUserPo storeUserPo = null;
        for (StoreUserDto tmpStoreUserDto : storeUserDtos) {
            storeUserPo = new StoreUserPo();
            storeUserPo.setStoreUserId(tmpStoreUserDto.getStoreUserId());
            storeUserPo.setUserId(tmpStoreUserDto.getUserId());
            storeUserPo.setStoreId(tmpStoreUserDto.getStoreId());
            // 删除商户关联
            storeUserV1InnerServiceSMOImpl.deleteStoreUser(storeUserPo);
        }

        //todo 删除报修设置
        RepairTypeUserDto repairTypeUserDto = new RepairTypeUserDto();
        repairTypeUserDto.setStaffId(reqJson.getString("staffId"));
        repairTypeUserDto.setStatusCd("0");
        List<RepairTypeUserDto> repairTypeUserDtoList = repairTypeUserInnerServiceSMOImpl.queryRepairTypeUsers(repairTypeUserDto);
        // 如果存在报修设置数据，则删除
        if (!ListUtil.isNull(repairTypeUserDtoList)) {
            for (RepairTypeUserDto tmpRepairTypeUserDto : repairTypeUserDtoList) {
                JSONObject typeUserJson1 = (JSONObject) JSONObject.toJSON(tmpRepairTypeUserDto);
                deleteRepairTypeUser(typeUserJson1);
            }
        }
        //todo 删除员工排班数据
        deleteScheduleClassesStaff(reqJson);

        //todo 删除员工考勤组
        deleteAttendanceClassesStaff(reqJson);

        // todo 删除巡检员工
        deleteInspectionPlanStaff(reqJson);

        //todo 删除用户权限
        deleteUserPrivilege(reqJson);

        // todo 删除员工认证
        deleteStaffAppAuth(reqJson);

        // todo 删除数据权限
        deleteStaffDataPri(reqJson);

        //todo 删除保养员工
        deleteMaintainanceStaff(reqJson);
    }

    /**
     * 删除员工数据权限
     * 
     * 根据staffId删除员工的数据权限配置
     * 
     * @param reqJson 请求参数JSON对象，包含staffId
     */
    private void deleteStaffDataPri(JSONObject reqJson) {

        DataPrivilegeStaffDto dataPrivilegeStaffDto = new DataPrivilegeStaffDto();
        dataPrivilegeStaffDto.setStaffId(reqJson.getString("staffId"));
        List<DataPrivilegeStaffDto> dataPrivilegeStaffDtos
                = dataPrivilegeStaffV1InnerServiceSMOImpl.queryDataPrivilegeStaffs(dataPrivilegeStaffDto);
        // 如果存在数据权限配置，则删除
        if (ListUtil.isNull(dataPrivilegeStaffDtos)) {
            return;
        }
        for (DataPrivilegeStaffDto tmpDataPrivilegeStaffDto : dataPrivilegeStaffDtos) {
            // 创建PO对象并设置主键
            DataPrivilegeStaffPo dataPrivilegeStaffPo = new DataPrivilegeStaffPo();
            dataPrivilegeStaffPo.setDpsId(tmpDataPrivilegeStaffDto.getDpsId());
            // 删除数据权限
            dataPrivilegeStaffV1InnerServiceSMOImpl.deleteDataPrivilegeStaff(dataPrivilegeStaffPo);
        }
    }

    /**
     * 删除员工应用认证
     * 
     * 根据staffId删除员工的应用认证信息
     * 
     * @param reqJson 请求参数JSON对象，包含staffId
     */
    private void deleteStaffAppAuth(JSONObject reqJson) {

        StaffAppAuthDto staffAppAuthDto = new StaffAppAuthDto();
        staffAppAuthDto.setStaffId(reqJson.getString("staffId"));
        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);
        // 如果存在应用认证数据，则删除
        if (ListUtil.isNull(staffAppAuthDtos)) {
            return;
        }
        for (StaffAppAuthDto tmpStaffAppAuthDto : staffAppAuthDtos) {
            StaffAppAuthPo staffAppAuthPo = new StaffAppAuthPo();
            staffAppAuthPo.setAuId(tmpStaffAppAuthDto.getAuId());
            // 删除应用认证
            staffAppAuthInnerServiceSMOImpl.deleteStaffAppAuth(staffAppAuthPo);
        }
    }

    /**
     * 删除巡检员工
     * 
     * 根据staffId删除员工的巡检计划关联
     * 
     * @param reqJson 请求参数JSON对象，包含staffId
     */
    private void deleteInspectionPlanStaff(JSONObject reqJson) {

        InspectionPlanStaffDto inspectionPlanStaffDto = new InspectionPlanStaffDto();
        inspectionPlanStaffDto.setStaffId(reqJson.getString("staffId"));
        List<InspectionPlanStaffDto> inspectionPlanStaffDtos
                = inspectionPlanStaffV1InnerServiceSMOImpl.queryInspectionPlanStaffs(inspectionPlanStaffDto);
        // 如果存在巡检计划关联，则删除
        if (ListUtil.isNull(inspectionPlanStaffDtos)) {
            return;
        }

        for (InspectionPlanStaffDto tmpInspectionPlanStaffDto : inspectionPlanStaffDtos) {
            InspectionPlanStaffPo inspectionPlanStaffPo = new InspectionPlanStaffPo();
            inspectionPlanStaffPo.setIpStaffId(tmpInspectionPlanStaffDto.getIpStaffId());
            // 删除巡检员工关联
            inspectionPlanStaffV1InnerServiceSMOImpl.deleteInspectionPlanStaff(inspectionPlanStaffPo);
        }
    }

    /**
     * 删除保养员工
     * 
     * 根据staffId删除员工的保养计划关联
     * 
     * @param reqJson 请求参数JSON对象，包含staffId
     */
    private void deleteMaintainanceStaff(JSONObject reqJson) {
        MaintainancePlanStaffDto maintainancePlanStaffDto = new MaintainancePlanStaffDto();
        maintainancePlanStaffDto.setStaffId(reqJson.getString("staffId"));
        List<MaintainancePlanStaffDto> maintainancePlanStaffDtos
                = maintainancePlanStaffV1InnerServiceSMOImpl.queryMaintainancePlanStaffs(maintainancePlanStaffDto);
        // 如果存在保养计划关联，则删除
        if (ListUtil.isNull(maintainancePlanStaffDtos)) {
            return;
        }

        for (MaintainancePlanStaffDto tmpMaintainancePlanStaffDto : maintainancePlanStaffDtos) {
            MaintainancePlanStaffPo maintainancePlanStaffPo = new MaintainancePlanStaffPo();
            maintainancePlanStaffPo.setMpsId(tmpMaintainancePlanStaffDto.getMpsId());
            // 删除保养员工关联
            maintainancePlanStaffV1InnerServiceSMOImpl.deleteMaintainancePlanStaff(maintainancePlanStaffPo);
        }
    }

    /**
     * 删除考勤组员工
     * 
     * 根据staffId删除员工的考勤组关联
     * 
     * @param reqJson 请求参数JSON对象，包含staffId
     */
    private void deleteAttendanceClassesStaff(JSONObject reqJson) {

        AttendanceClassesStaffDto attendanceClassesStaffDto = new AttendanceClassesStaffDto();
        attendanceClassesStaffDto.setStaffId(reqJson.getString("staffId"));
        List<AttendanceClassesStaffDto> attendanceClassesStaffDtos
                = attendanceClassesStaffV1InnerServiceSMOImpl.queryAttendanceClassesStaffs(attendanceClassesStaffDto);
        // 如果存在考勤组关联，则删除
        if (ListUtil.isNull(attendanceClassesStaffDtos)) {
            return;
        }
        for (AttendanceClassesStaffDto tmpAttendanceClassesStaffDto : attendanceClassesStaffDtos) {
            AttendanceClassesStaffPo attendanceClassesStaffPo = new AttendanceClassesStaffPo();
            attendanceClassesStaffPo.setCsId(tmpAttendanceClassesStaffDto.getCsId());
            // 删除考勤组员工关联
            attendanceClassesStaffV1InnerServiceSMOImpl.deleteAttendanceClassesStaff(attendanceClassesStaffPo);
        }
    }

    /**
     * 删除排班员工
     * 
     * 根据staffId删除员工的排班数据
     * 
     * @param reqJson 请求参数JSON对象，包含staffId
     */
    private void deleteScheduleClassesStaff(JSONObject reqJson) {
        ScheduleClassesStaffDto scheduleClassesStaffDto = new ScheduleClassesStaffDto();
        scheduleClassesStaffDto.setStaffId(reqJson.getString("staffId"));
        List<ScheduleClassesStaffDto> scheduleClassesStaffDtos
                = scheduleClassesStaffV1InnerServiceSMOImpl.queryScheduleClassesStaffs(scheduleClassesStaffDto);
        // 如果存在排班数据，则删除
        if (ListUtil.isNull(scheduleClassesStaffDtos)) {
            return;
        }
        for (ScheduleClassesStaffDto tmpScheduleClassesStaffDto : scheduleClassesStaffDtos) {
            ScheduleClassesStaffPo scheduleClassesStaffPo = new ScheduleClassesStaffPo();
            scheduleClassesStaffPo.setScsId(tmpScheduleClassesStaffDto.getScsId());
            // 删除排班员工关联
            scheduleClassesStaffV1InnerServiceSMOImpl.deleteScheduleClassesStaff(scheduleClassesStaffPo);
        }

    }

    /**
     * 删除用户权限
     * 
     * 根据staffId删除用户的权限配置
     * 
     * @param paramInJson 请求参数JSON对象，包含staffId
     */
    private void deleteUserPrivilege(JSONObject paramInJson) {

        PrivilegeUserDto privilegeUserDto = new PrivilegeUserDto();
        privilegeUserDto.setUserId(paramInJson.getString("staffId"));
        List<PrivilegeUserDto> privilegeUserDtos = privilegeUserV1InnerServiceSMOImpl.queryPrivilegeUsers(privilegeUserDto);

        // 如果存在权限配置，则删除
        if (ListUtil.isNull(privilegeUserDtos)) {
            return;
        }

        for (PrivilegeUserDto tmpPrivilegeUserDto : privilegeUserDtos) {
            PrivilegeUserPo privilegeUserPo = new PrivilegeUserPo();
            privilegeUserPo.setPuId(tmpPrivilegeUserDto.getPuId());
            // 删除用户权限
            int flag = privilegeUserV1InnerServiceSMOImpl.deletePrivilegeUser(privilegeUserPo);
            // 检查删除操作是否成功
            if (flag < 1) {
                throw new CmdException("删除员工失败");
            }
        }
    }

    /**
     * 删除报修类型用户关联
     * 
     * 根据参数删除报修类型与用户的关联关系
     * 
     * @param paramInJson 接口调用方传入参数，包含报修类型用户关联信息
     */
    public void deleteRepairTypeUser(JSONObject paramInJson) {

        // 将JSON参数转换为PO对象
        RepairTypeUserPo repairTypeUserPo = BeanConvertUtil.covertBean(paramInJson, RepairTypeUserPo.class);
        // 调用服务删除报修类型用户关联
        int flag = repairTypeUserV1InnerServiceSMOImpl.deleteRepairTypeUser(repairTypeUserPo);
        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除员工失败");
        }
    }

}