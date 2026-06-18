package com.java110.community.cmd.inspection;

import com.alibaba.fastjson.JSONObject;
import com.java110.community.smo.impl.InspectionPlanV1InnerServiceSMOImpl;
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
import com.java110.vo.api.inspectionPlan.ApiInspectionPlanDataVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询巡检点关联的巡检计划命令类
 * 
 * 该命令类负责根据指定的巡检点查询相关的巡检计划信息，
 * 包括巡检计划的基本信息和关联的巡检人员信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "inspection.queryPointInspectionPlan")
public class QueryPointInspectionPlanCmd extends Cmd {

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
     * 验证请求参数的有效性
     * 
     * 该方法用于验证传入的请求参数是否包含必要的字段，
     * 包括分页信息和必填的业务字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页信息
        super.validatePageInfo(reqJson);

        // 验证必填参数：巡检点ID
        Assert.hasKeyAndValue(reqJson, "inspectionId", "未包含巡检点");
        // 验证必填参数：小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 执行查询巡检计划命令
     * 
     * 该方法根据巡检点查询相关的巡检计划列表，
     * 并关联查询每个巡检计划的执行人员信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 将请求JSON转换为巡检计划数据传输对象
        InspectionPlanDto inspectionPlanDto = BeanConvertUtil.covertBean(reqJson, InspectionPlanDto.class);

        // 查询符合条件的巡检计划总数
        int count = inspectionPlanV1InnerServiceSMOImpl.queryPointInspectionPlansCount(inspectionPlanDto);

        List<InspectionPlanDto> inspectionPlans = null;

        // 如果存在巡检计划，则查询详细信息
        if (count > 0) {
            // 查询巡检计划列表并转换为DTO对象
            inspectionPlans = BeanConvertUtil.covertBeanList(inspectionPlanV1InnerServiceSMOImpl.queryPointInspectionPlans(inspectionPlanDto), InspectionPlanDto.class);
            // 查询每个巡检计划关联的人员信息
            queryStaffs(inspectionPlans);
        } else {
            // 如果没有查询到数据，返回空列表
            inspectionPlans = new ArrayList<>();
        }
        
        // 计算总页数：总记录数除以每页显示数量，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, inspectionPlans);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }

    /**
     * 查询巡检计划关联的巡检人员信息
     * 
     * 该方法根据巡检计划列表批量查询每个计划关联的巡检人员，
     * 并将人员信息设置到对应的巡检计划对象中
     * 
     * @param inspectionPlans 巡检计划列表，查询结果会设置到每个计划的staffs属性中
     */
    private void queryStaffs(List<InspectionPlanDto> inspectionPlans) {

        // 如果巡检计划列表为空，直接返回
        if (ListUtil.isNull(inspectionPlans)) {
            return;
        }

        // 收集所有巡检计划的ID
        List<String> planIds = new ArrayList<>();
        for (InspectionPlanDto inspectionPlanDto : inspectionPlans) {
            planIds.add(inspectionPlanDto.getInspectionPlanId());
        }

        // 构建查询条件对象
        InspectionPlanStaffDto inspectionPlanStaffDto = new InspectionPlanStaffDto();
        // 设置要查询的巡检计划ID数组
        inspectionPlanStaffDto.setInspectionPlanIds(planIds.toArray(new String[planIds.size()]));
        // 设置小区ID（使用第一个巡检计划的小区ID）
        inspectionPlanStaffDto.setCommunityId(inspectionPlans.get(0).getCommunityId());
        
        // 批量查询巡检计划人员关联信息
        List<InspectionPlanStaffDto> inspectionPlanStaffDtos = inspectionPlanStaffV1InnerServiceSMOImpl.queryInspectionPlanStaffs(inspectionPlanStaffDto);

        // 如果没有查询到人员信息，直接返回
        if (ListUtil.isNull(inspectionPlanStaffDtos)) {
            return;
        }

        // 为每个巡检计划设置关联的人员信息
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