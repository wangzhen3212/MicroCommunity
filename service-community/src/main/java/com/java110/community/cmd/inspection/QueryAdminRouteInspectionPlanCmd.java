package com.java110.community.cmd.inspection;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionPlanDto;
import com.java110.dto.inspection.InspectionPlanStaffDto;
import com.java110.intf.community.IInspectionPlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IInspectionPlanV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 巡检计划查询命令类
 * 根据巡检路线查询关联的巡检计划信息，包括计划详情和对应的巡检人员信息
 * 主要用于管理员查看巡检路线下的所有巡检计划安排
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode inspection.queryAdminRouteInspectionPlan
 */
@Java110Cmd(serviceCode = "inspection.queryAdminRouteInspectionPlan")
public class QueryAdminRouteInspectionPlanCmd extends Cmd {

    /**
     * 巡检计划服务接口，用于查询巡检计划相关信息
     */
    @Autowired
    private IInspectionPlanV1InnerServiceSMO inspectionPlanV1InnerServiceSMOImpl;

    /**
     * 巡检计划人员服务接口，用于查询巡检计划关联的人员信息
     */
    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 验证分页参数、管理员权限和必要的业务参数
     *
     * @param event   命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException    当命令执行出现异常时抛出
     * @throws ParseException  当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
        // 验证必须包含巡检路线ID参数
        Assert.hasKeyAndValue(reqJson, "inspectionRouteId", "未包含巡检路线");
    }

    /**
     * 命令执行主方法
     * 根据巡检路线查询巡检计划列表，并关联查询每个计划的巡检人员信息
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException    当命令执行出现异常时抛出
     * @throws ParseException  当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为巡检计划DTO对象
        InspectionPlanDto inspectionPlanDto = BeanConvertUtil.covertBean(reqJson, InspectionPlanDto.class);

        // 查询巡检计划总数
        int count = inspectionPlanV1InnerServiceSMOImpl.queryRouteInspectionPlansCount(inspectionPlanDto);

        List<InspectionPlanDto> inspectionPlans = null;

        if (count > 0) {
            // 如果存在巡检计划，查询计划列表并转换为DTO对象
            inspectionPlans = BeanConvertUtil.covertBeanList(
                inspectionPlanV1InnerServiceSMOImpl.queryRouteInspectionPlans(inspectionPlanDto), 
                InspectionPlanDto.class
            );
            // 为每个巡检计划查询关联的巡检人员信息
            queryStaffs(inspectionPlans);
        } else {
            // 如果没有巡检计划，返回空列表
            inspectionPlans = new ArrayList<>();
        }

        // 构建分页结果对象
        // 计算总页数：总记录数 / 每页记录数，向上取整
        ResultVo resultVo = new ResultVo(
            (int) Math.ceil((double) count / (double) reqJson.getInteger("row")), 
            count, 
            inspectionPlans
        );

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 查询巡检计划关联的巡检人员信息
     * 批量查询所有巡检计划的人员信息，并按计划ID进行分组关联
     *
     * @param inspectionPlans 巡检计划列表，查询完成后会为每个计划设置对应的巡检人员列表
     */
    private void queryStaffs(List<InspectionPlanDto> inspectionPlans) {
        // 检查巡检计划列表是否为空
        if (ListUtil.isNull(inspectionPlans)) {
            return;
        }

        // 收集所有巡检计划的ID，用于批量查询
        List<String> planIds = new ArrayList<>();
        for (InspectionPlanDto inspectionPlanDto : inspectionPlans) {
            planIds.add(inspectionPlanDto.getInspectionPlanId());
        }

        // 构建巡检计划人员查询条件
        InspectionPlanStaffDto inspectionPlanStaffDto = new InspectionPlanStaffDto();
        // 设置要查询的巡检计划ID数组
        inspectionPlanStaffDto.setInspectionPlanIds(planIds.toArray(new String[planIds.size()]));
        // 设置小区ID（从第一个巡检计划中获取）
        inspectionPlanStaffDto.setCommunityId(inspectionPlans.get(0).getCommunityId());
        
        // 批量查询巡检计划人员信息
        List<InspectionPlanStaffDto> inspectionPlanStaffDtos = 
            inspectionPlanStaffV1InnerServiceSMOImpl.queryInspectionPlanStaffs(inspectionPlanStaffDto);

        // 检查查询结果是否为空
        if (ListUtil.isNull(inspectionPlanStaffDtos)) {
            return;
        }

        // 按巡检计划ID分组关联人员信息
        List<InspectionPlanStaffDto> staffDtos = null;
        for (InspectionPlanDto inspectionPlanDto : inspectionPlans) {
            staffDtos = new ArrayList<>();
            // 遍历所有人员信息，找到属于当前巡检计划的人员
            for (InspectionPlanStaffDto tmpInspectionPlanStaffDto : inspectionPlanStaffDtos) {
                if (inspectionPlanDto.getInspectionPlanId().equals(tmpInspectionPlanStaffDto.getInspectionPlanId())) {
                    staffDtos.add(tmpInspectionPlanStaffDto);
                }
            }
            // 将人员列表设置到巡检计划对象中
            inspectionPlanDto.setStaffs(staffDtos);
        }
    }
}