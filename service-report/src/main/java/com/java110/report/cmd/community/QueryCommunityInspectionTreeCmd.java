package com.java110.report.cmd.community;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionPlanDto;
import com.java110.intf.dev.IDictV1InnerServiceSMO;
import com.java110.intf.report.IReportCommunityInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 查询小区巡检树形结构命令类
 * 
 * 该类负责处理小区巡检树形结构的查询请求，构建包含小区、巡检计划和巡检人员的树形数据结构
 * 主要用于前端展示小区巡检的层级关系
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "community.queryCommunityInspectionTree")
public class QueryCommunityInspectionTreeCmd extends Cmd {

    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    @Autowired
    private IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证当前用户是否为管理员权限，确保只有管理员可以执行此操作
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证当前用户必须为管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行命令的主方法
     * 
     * 根据当前用户权限查询其管理的小区，构建小区-巡检计划-巡检人员的树形结构
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        List<InspectionPlanDto> inspectionPlanDtos = null;

        InspectionPlanDto inspectionPlanDto = new InspectionPlanDto();

        // 获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询当前用户管理的小区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户管理的小区不为空，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            inspectionPlanDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 查询小区巡检树形数据
        inspectionPlanDtos = reportCommunityInnerServiceSMOImpl.queryCommunityInspectionTree(inspectionPlanDto);
        JSONArray communitys = new JSONArray();
        
        // 如果查询结果为空，直接返回空数组
        if (ListUtil.isNull(inspectionPlanDtos)) {
            context.setResponseEntity(ResultVo.createResponseEntity(communitys));
            return;
        }

        JSONObject communityInfo = null;
        // 遍历巡检计划数据，构建小区层级
        for (InspectionPlanDto tmpInspectionPlanDto : inspectionPlanDtos) {
            // 检查小区是否已添加到结果中，避免重复添加
            if (!hasInCommunity(tmpInspectionPlanDto, communitys)) {
                communityInfo = new JSONObject();
                communityInfo.put("id", "c_" + tmpInspectionPlanDto.getCommunityId());
                communityInfo.put("communityId", tmpInspectionPlanDto.getCommunityId());
                communityInfo.put("text", tmpInspectionPlanDto.getCommunityName());
                communityInfo.put("icon", "/img/org.png");
                communityInfo.put("children", new JSONArray());
                communitys.add(communityInfo);
            }
        }

        JSONObject community = null;
        // 为每个小区查找对应的巡检计划
        for (int cIndex = 0; cIndex < communitys.size(); cIndex++) {
            community = communitys.getJSONObject(cIndex);
            // 在当前小区中查找对应的巡检计划数据
            findInspectionPlan(community, inspectionPlanDtos);
        }
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(communitys));
    }

    /**
     * 为小区查找对应的巡检计划数据
     * 
     * 根据小区ID从巡检计划列表中筛选出属于该小区的巡检计划，并构建巡检计划层级
     * 
     * @param community 当前小区对象
     * @param inspectionPlanDtos 所有巡检计划数据列表
     */
    private void findInspectionPlan(JSONObject community, List<InspectionPlanDto> inspectionPlanDtos) {
        JSONArray plans = community.getJSONArray("children");
        JSONObject parkingInfo = null;
        
        // 遍历所有巡检计划，筛选出属于当前小区的计划
        for (InspectionPlanDto tmpInspectionPlanDto : inspectionPlanDtos) {
            // 检查巡检计划是否属于当前小区
            if(!community.getString("communityId").equals(tmpInspectionPlanDto.getCommunityId())){
                continue;
            }
            // 检查巡检计划是否已添加到当前小区中
            if (!hasInInspectionPlan(tmpInspectionPlanDto, plans)) {
                parkingInfo = new JSONObject();
                parkingInfo.put("id", "p_" + tmpInspectionPlanDto.getInspectionPlanId());
                parkingInfo.put("inspectionPlanId", tmpInspectionPlanDto.getInspectionPlanId());
                parkingInfo.put("communityId", community.getString("communityId"));
                parkingInfo.put("text", tmpInspectionPlanDto.getInspectionPlanName());
                parkingInfo.put("icon", "/img/floor.png");
                parkingInfo.put("children", new JSONArray());
                plans.add(parkingInfo);
            }
        }

        JSONObject plan = null;
        // 为每个巡检计划查找对应的巡检人员
        for (int cIndex = 0; cIndex < plans.size(); cIndex++) {
            plan = plans.getJSONObject(cIndex);
            // 在当前巡检计划中查找对应的巡检人员数据
            findInspectionStaff(plan, inspectionPlanDtos);
        }
    }

    /**
     * 为巡检计划查找对应的巡检人员数据
     * 
     * 根据小区ID和巡检计划ID从巡检计划列表中筛选出对应的巡检人员，并构建巡检人员层级
     * 
     * @param plan 当前巡检计划对象
     * @param inspectionPlanDtos 所有巡检计划数据列表
     */
    private void findInspectionStaff(JSONObject plan, List<InspectionPlanDto> inspectionPlanDtos) {
        JSONArray staffs = plan.getJSONArray("children");
        JSONObject unitInfo = null;
        
        // 遍历所有巡检计划，筛选出属于当前计划的巡检人员
        for (InspectionPlanDto tmpInspectionPlanDto : inspectionPlanDtos) {
            // 检查巡检计划是否属于当前小区
            if(!plan.getString("communityId").equals(tmpInspectionPlanDto.getCommunityId())){
                continue;
            }
            // 检查巡检计划ID是否匹配
            if(!plan.getString("inspectionPlanId").equals(tmpInspectionPlanDto.getInspectionPlanId())){
                continue;
            }
            // 检查巡检人员是否已添加到当前计划中
            if (!hasInPlanStaff(tmpInspectionPlanDto, staffs)) {
                unitInfo = new JSONObject();
                unitInfo.put("id", "s_" + tmpInspectionPlanDto.getStaffId());
                unitInfo.put("staffId", tmpInspectionPlanDto.getStaffId());
                unitInfo.put("inspectionPlanId",plan.getString("inspectionPlanId"));
                unitInfo.put("communityId", plan.getString("communityId"));
                unitInfo.put("text", tmpInspectionPlanDto.getStaffName());
                unitInfo.put("icon", "/img/unit.png");
                staffs.add(unitInfo);
            }
        }
    }

    /**
     * 检查巡检人员是否已存在于巡检人员列表中
     * 
     * @param inspectionPlanDto 巡检计划数据传输对象
     * @param staffs 巡检人员JSON数组
     * @return boolean 如果巡检人员已存在返回true，否则返回false
     */
    private boolean hasInPlanStaff(InspectionPlanDto inspectionPlanDto, JSONArray staffs) {
        JSONObject staff = null;
        // 遍历巡检人员数组，检查是否已存在相同staffId的巡检人员
        for (int cIndex = 0; cIndex < staffs.size(); cIndex++) {
            staff = staffs.getJSONObject(cIndex);
            // 检查当前对象是否包含staffId字段
            if (!staff.containsKey("staffId")) {
                continue;
            }
            // 检查staffId字段是否为空
            if (StringUtil.isEmpty(staff.getString("staffId"))) {
                continue;
            }
            // 检查staffId是否匹配
            if (staff.getString("staffId").equals(inspectionPlanDto.getStaffId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查巡检计划是否已存在于巡检计划列表中
     * 
     * @param tmpInspectionPlanDto 巡检计划数据传输对象
     * @param parkings 巡检计划JSON数组
     * @return boolean 如果巡检计划已存在返回true，否则返回false
     */
    private boolean hasInInspectionPlan(InspectionPlanDto tmpInspectionPlanDto, JSONArray parkings) {
        JSONObject plan = null;
        // 遍历巡检计划数组，检查是否已存在相同inspectionPlanId的巡检计划
        for (int cIndex = 0; cIndex < parkings.size(); cIndex++) {
            plan = parkings.getJSONObject(cIndex);
            // 检查当前对象是否包含inspectionPlanId字段
            if (!plan.containsKey("inspectionPlanId")) {
                continue;
            }
            // 检查inspectionPlanId字段是否为空
            if (StringUtil.isEmpty(plan.getString("inspectionPlanId"))) {
                continue;
            }
            // 检查inspectionPlanId是否匹配
            if (plan.getString("inspectionPlanId").equals(tmpInspectionPlanDto.getInspectionPlanId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查小区是否已存在于小区列表中
     * 
     * @param tmpInspectionPlanDto 巡检计划数据传输对象
     * @param communitys 小区JSON数组
     * @return boolean 如果小区已存在返回true，否则返回false
     */
    private boolean hasInCommunity(InspectionPlanDto tmpInspectionPlanDto, JSONArray communitys) {
        JSONObject community = null;
        // 遍历小区数组，检查是否已存在相同communityId的小区
        for (int cIndex = 0; cIndex < communitys.size(); cIndex++) {
            community = communitys.getJSONObject(cIndex);
            // 检查当前对象是否包含communityId字段
            if (!community.containsKey("communityId")) {
                continue;
            }
            // 检查communityId字段是否为空
            if (StringUtil.isEmpty(community.getString("communityId"))) {
                continue;
            }
            // 检查communityId是否匹配
            if (community.getString("communityId").equals(tmpInspectionPlanDto.getCommunityId())) {
                return true;
            }
        }
        return false;
    }
}