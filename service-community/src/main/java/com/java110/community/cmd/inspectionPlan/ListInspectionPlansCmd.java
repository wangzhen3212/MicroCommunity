package com.java110.community.cmd.inspectionPlan;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionPlanDto;
import com.java110.dto.inspection.InspectionPlanStaffDto;
import com.java110.dto.inspection.InspectionRouteDto;
import com.java110.dto.org.OrgStaffRelDto;
import com.java110.intf.community.IInspectionPlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IInspectionPlanV1InnerServiceSMO;
import com.java110.intf.community.IInspectionRouteInnerServiceSMO;
import com.java110.intf.user.IOrgStaffRelInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.inspectionPlan.ApiInspectionPlanDataVo;
import com.java110.vo.api.inspectionPlan.ApiInspectionPlanVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 巡检计划列表查询命令类
 * 负责处理巡检计划列表查询请求，包括参数验证、数据查询和结果返回
 * 支持分页查询，并关联查询巡检计划对应的员工信息
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "inspectionPlan.listInspectionPlans")
public class ListInspectionPlansCmd extends Cmd {

    /** 巡检计划服务接口 */
    @Autowired
    private IInspectionPlanV1InnerServiceSMO inspectionPlanV1InnerServiceSMOImpl;

    /** 组织员工关系服务接口 */
    @Autowired
    private IOrgStaffRelInnerServiceSMO iOrgStaffRelInnerServiceSMO;

    /** 巡检路线服务接口 */
    @Autowired
    private IInspectionRouteInnerServiceSMO inspectionRouteInnerServiceSMOImpl;

    /** 巡检计划员工关系服务接口 */
    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 验证分页参数和必要字段，确保请求数据的完整性
     *
     * @param event   命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含小区ID");
        // 验证属性信息
        super.validateProperty(context);
    }

    /**
     * 命令执行方法
     * 处理巡检计划列表查询业务逻辑，包括数据查询、员工信息关联和结果返回
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为巡检计划DTO对象
        InspectionPlanDto inspectionPlanDto = BeanConvertUtil.covertBean(reqJson, InspectionPlanDto.class);

        // 查询巡检计划总数
        int count = inspectionPlanV1InnerServiceSMOImpl.queryInspectionPlansCount(inspectionPlanDto);

        List<InspectionPlanDto> inspectionPlans = null;
        if (count > 0) {
            // 查询巡检计划列表数据
            inspectionPlans = BeanConvertUtil.covertBeanList(inspectionPlanV1InnerServiceSMOImpl.queryInspectionPlans(inspectionPlanDto), InspectionPlanDto.class);
            // 关联查询巡检计划对应的员工信息
            queryStaffs(inspectionPlans);
        } else {
            // 如果没有数据，返回空列表
            inspectionPlans = new ArrayList<>();
        }

        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, inspectionPlans);

        // 设置HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        context.setResponseEntity(responseEntity);
    }

    /**
     * 查询巡检计划对应的员工信息
     * 根据巡检计划ID列表批量查询关联的员工信息，并设置到对应的巡检计划对象中
     *
     * @param inspectionPlans 巡检计划列表，查询结果会设置每个计划的员工信息
     */
    private void queryStaffs(List<InspectionPlanDto> inspectionPlans) {

        // 检查巡检计划列表是否为空
        if (ListUtil.isNull(inspectionPlans)) {
            return;
        }

        // 收集所有巡检计划ID
        List<String> planIds = new ArrayList<>();
        for (InspectionPlanDto inspectionPlanDto : inspectionPlans) {
            planIds.add(inspectionPlanDto.getInspectionPlanId());
        }

        // 构建查询条件
        InspectionPlanStaffDto inspectionPlanStaffDto = new InspectionPlanStaffDto();
        inspectionPlanStaffDto.setInspectionPlanIds(planIds.toArray(new String[planIds.size()]));
        inspectionPlanStaffDto.setCommunityId(inspectionPlans.get(0).getCommunityId());
        
        // 批量查询巡检计划员工关系数据
        List<InspectionPlanStaffDto> inspectionPlanStaffDtos = inspectionPlanStaffV1InnerServiceSMOImpl.queryInspectionPlanStaffs(inspectionPlanStaffDto);

        // 检查查询结果是否为空
        if (ListUtil.isNull(inspectionPlanStaffDtos)) {
            return;
        }

        // 将员工信息按巡检计划ID分组设置
        List<InspectionPlanStaffDto> staffDtos = null;
        for (InspectionPlanDto inspectionPlanDto : inspectionPlans) {
            staffDtos = new ArrayList<>();
            // 遍历所有员工关系数据，匹配当前巡检计划
            for (InspectionPlanStaffDto tmpInspectionPlanStaffDto : inspectionPlanStaffDtos) {
                if (inspectionPlanDto.getInspectionPlanId().equals(tmpInspectionPlanStaffDto.getInspectionPlanId())) {
                    staffDtos.add(tmpInspectionPlanStaffDto);
                }
            }
            // 设置当前巡检计划的员工列表
            inspectionPlanDto.setStaffs(staffDtos);
        }
    }
}