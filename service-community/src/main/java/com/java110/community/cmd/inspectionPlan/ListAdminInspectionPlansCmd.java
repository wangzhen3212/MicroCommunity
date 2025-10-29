package com.java110.community.cmd.inspectionPlan;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.inspection.InspectionPlanDto;
import com.java110.dto.inspection.InspectionPlanStaffDto;
import com.java110.dto.repair.RepairDto;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.intf.community.IInspectionPlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IInspectionPlanV1InnerServiceSMO;
import com.java110.intf.community.IInspectionRouteInnerServiceSMO;
import com.java110.intf.user.IOrgStaffRelInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 巡检计划管理命令类
 * 功能：处理管理员巡检计划列表查询请求，包括数据验证、权限校验、数据查询和结果封装
 * 用途：为系统管理员提供巡检计划的查询功能，支持分页查询和关联数据加载
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "inspectionPlan.listAdminInspectionPlans")
public class ListAdminInspectionPlansCmd extends Cmd {

    @Autowired
    private IInspectionPlanV1InnerServiceSMO inspectionPlanV1InnerServiceSMOImpl;

    @Autowired
    private IOrgStaffRelInnerServiceSMO iOrgStaffRelInnerServiceSMO;

    @Autowired
    private IInspectionRouteInnerServiceSMO inspectionRouteInnerServiceSMOImpl;

    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;

    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 功能：验证分页参数和管理员权限
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 命令执行方法
     * 功能：执行巡检计划查询业务逻辑，包括数据查询、员工信息补充、小区名称刷新和结果封装
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为巡检计划DTO对象
        InspectionPlanDto inspectionPlanDto = BeanConvertUtil.covertBean(reqJson, InspectionPlanDto.class);
        // 从上下文中获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询当前员工管理的小区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果员工管理的小区不为空，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            inspectionPlanDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }
        
        // 查询巡检计划总数
        int count = inspectionPlanV1InnerServiceSMOImpl.queryInspectionPlansCount(inspectionPlanDto);

        List<InspectionPlanDto> inspectionPlans = null;
        // 如果存在巡检计划数据，则查询详细列表
        if (count > 0) {
            inspectionPlans = BeanConvertUtil.covertBeanList(inspectionPlanV1InnerServiceSMOImpl.queryInspectionPlans(inspectionPlanDto), InspectionPlanDto.class);
            // 查询并设置巡检计划对应的员工信息
            queryStaffs(inspectionPlans);
        } else {
            // 如果没有数据，返回空列表
            inspectionPlans = new ArrayList<>();
        }
        // 刷新小区名称信息
        refreshCommunityName(inspectionPlans);

        // 构造分页结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, inspectionPlans);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新小区名称信息
     * 功能：根据小区ID查询小区名称并设置到巡检计划对象中
     * 
     * @param inspectionPlans 巡检计划列表
     */
    private void refreshCommunityName(List<InspectionPlanDto> inspectionPlans) {
        // 如果巡检计划列表为空，直接返回
        if(ListUtil.isNull(inspectionPlans)){
            return;
        }

        // 收集所有小区ID
        List<String> communityIds = new ArrayList<>();
        for (InspectionPlanDto inspectionPlanDto : inspectionPlans) {
            communityIds.add(inspectionPlanDto.getCommunityId());
        }

        // 如果小区ID列表为空，直接返回
        if(ListUtil.isNull(communityIds)){
            return ;
        }
        
        // 构建小区查询条件
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        // 查询小区信息
        List<CommunityDto> communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);
        
        // 如果小区信息为空，直接返回
        if(ListUtil.isNull(communityDtos)){
            return;
        }
        
        // 遍历巡检计划，设置对应的小区名称
        for (InspectionPlanDto inspectionPlanDto : inspectionPlans) {
            for (CommunityDto tCommunityDto : communityDtos) {
                // 匹配小区ID
                if (!inspectionPlanDto.getCommunityId().equals(tCommunityDto.getCommunityId())) {
                    continue;
                }
                // 设置小区名称
                inspectionPlanDto.setCommunityName(tCommunityDto.getName());
            }
        }
    }

    /**
     * 查询巡检计划对应的员工信息
     * 功能：根据巡检计划ID查询关联的员工信息并设置到巡检计划对象中
     * 
     * @param inspectionPlans 巡检计划列表
     */
    private void queryStaffs(List<InspectionPlanDto> inspectionPlans) {
        // 如果巡检计划列表为空，直接返回
        if (ListUtil.isNull(inspectionPlans)) {
            return;
        }

        // 收集所有巡检计划ID
        List<String> planIds = new ArrayList<>();
        for (InspectionPlanDto inspectionPlanDto : inspectionPlans) {
            planIds.add(inspectionPlanDto.getInspectionPlanId());
        }

        // 构建巡检计划员工查询条件
        InspectionPlanStaffDto inspectionPlanStaffDto = new InspectionPlanStaffDto();
        inspectionPlanStaffDto.setInspectionPlanIds(planIds.toArray(new String[planIds.size()]));
        // 设置小区ID（取第一个巡检计划的小区ID）
        inspectionPlanStaffDto.setCommunityId(inspectionPlans.get(0).getCommunityId());
        // 查询巡检计划员工关联信息
        List<InspectionPlanStaffDto> inspectionPlanStaffDtos = inspectionPlanStaffV1InnerServiceSMOImpl.queryInspectionPlanStaffs(inspectionPlanStaffDto);

        // 如果员工关联信息为空，直接返回
        if (ListUtil.isNull(inspectionPlanStaffDtos)) {
            return;
        }

        // 为每个巡检计划设置对应的员工列表
        List<InspectionPlanStaffDto> staffDtos = null;
        for (InspectionPlanDto inspectionPlanDto : inspectionPlans) {
            staffDtos = new ArrayList<>();
            // 遍历员工关联信息，匹配当前巡检计划
            for (InspectionPlanStaffDto tmpInspectionPlanStaffDto : inspectionPlanStaffDtos) {
                if (inspectionPlanDto.getInspectionPlanId().equals(tmpInspectionPlanStaffDto.getInspectionPlanId())) {
                    staffDtos.add(tmpInspectionPlanStaffDto);
                }
            }
            // 设置员工列表到巡检计划对象
            inspectionPlanDto.setStaffs(staffDtos);
        }
    }
}