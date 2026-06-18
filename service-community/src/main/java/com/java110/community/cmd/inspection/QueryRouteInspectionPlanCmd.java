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
 * 
 * 该类用于处理根据巡检路线查询巡检计划的业务逻辑，包括参数验证、数据查询和结果返回等功能。
 * 主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 查询指定巡检路线的巡检计划信息
 * 3. 关联查询巡检计划对应的员工信息
 * 4. 返回分页格式的查询结果
 * 
 * @author Java110
 * @version 1.0
 * @see Cmd
 */
@Java110Cmd(serviceCode = "inspection.queryRouteInspectionPlan")
public class QueryRouteInspectionPlanCmd extends Cmd {

    /**
     * 巡检计划服务接口，用于查询巡检计划相关信息
     */
    @Autowired
    private IInspectionPlanV1InnerServiceSMO inspectionPlanV1InnerServiceSMOImpl;

    /**
     * 巡检计划员工服务接口，用于查询巡检计划关联的员工信息
     */
    @Autowired
    private IInspectionPlanStaffV1InnerServiceSMO inspectionPlanStaffV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数、必填字段等
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当参数解析失败时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证属性参数
        super.validateProperty(context);
        // 验证巡检路线ID参数
        Assert.hasKeyAndValue(reqJson, "inspectionRouteId", "未包含巡检路线");
        // 验证小区ID参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 执行命令方法
     * 
     * 处理巡检计划查询的核心业务逻辑，包括：
     * 1. 查询巡检计划数据
     * 2. 关联查询员工信息
     * 3. 构建分页响应结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行失败时抛出命令异常
     * @throws ParseException 当数据解析失败时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为巡检计划DTO对象
        InspectionPlanDto inspectionPlanDto = BeanConvertUtil.covertBean(reqJson, InspectionPlanDto.class);

        // 查询巡检计划总数
        int count = inspectionPlanV1InnerServiceSMOImpl.queryRouteInspectionPlansCount(inspectionPlanDto);

        List<InspectionPlanDto> inspectionPlans = null;

        if (count > 0) {
            // 查询巡检计划列表数据
            inspectionPlans = BeanConvertUtil.covertBeanList(
                inspectionPlanV1InnerServiceSMOImpl.queryRouteInspectionPlans(inspectionPlanDto), 
                InspectionPlanDto.class
            );
            // 查询每个巡检计划对应的员工信息
            queryStaffs(inspectionPlans);
        } else {
            // 如果没有数据，返回空列表
            inspectionPlans = new ArrayList<>();
        }

        // 构建分页响应结果
        // 计算总页数：总记录数 / 每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, inspectionPlans);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 查询巡检计划对应的员工信息
     * 
     * 为每个巡检计划查询关联的员工信息，并将员工列表设置到巡检计划对象中
     * 
     * @param inspectionPlans 巡检计划列表，查询结果将设置到每个巡检计划的staffs属性中
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

        // 构建员工查询条件
        InspectionPlanStaffDto inspectionPlanStaffDto = new InspectionPlanStaffDto();
        // 设置要查询的巡检计划ID数组
        inspectionPlanStaffDto.setInspectionPlanIds(planIds.toArray(new String[planIds.size()]));
        // 设置小区ID（取第一个巡检计划的小区ID）
        inspectionPlanStaffDto.setCommunityId(inspectionPlans.get(0).getCommunityId());
        
        // 批量查询巡检计划员工信息
        List<InspectionPlanStaffDto> inspectionPlanStaffDtos = inspectionPlanStaffV1InnerServiceSMOImpl.queryInspectionPlanStaffs(inspectionPlanStaffDto);

        // 检查查询结果是否为空
        if (ListUtil.isNull(inspectionPlanStaffDtos)) {
            return;
        }

        // 为每个巡检计划分配对应的员工信息
        List<InspectionPlanStaffDto> staffDtos = null;
        for (InspectionPlanDto inspectionPlanDto : inspectionPlans) {
            staffDtos = new ArrayList<>();
            // 遍历所有员工数据，筛选出属于当前巡检计划的员工
            for (InspectionPlanStaffDto tmpInspectionPlanStaffDto : inspectionPlanStaffDtos) {
                if (inspectionPlanDto.getInspectionPlanId().equals(tmpInspectionPlanStaffDto.getInspectionPlanId())) {
                    staffDtos.add(tmpInspectionPlanStaffDto);
                }
            }
            // 将员工列表设置到巡检计划对象中
            inspectionPlanDto.setStaffs(staffDtos);
        }
    }
}