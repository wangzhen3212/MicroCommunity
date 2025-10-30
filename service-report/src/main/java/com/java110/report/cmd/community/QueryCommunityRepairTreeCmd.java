package com.java110.report.cmd.community;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.dict.DictDto;
import com.java110.dto.repair.RepairSettingDto;
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
 * 查询小区报修树形结构命令类
 * 
 * 该类负责处理小区报修树形结构的查询请求，构建包含小区、报修类型和状态的多级树形结构数据
 * 主要用于前端展示小区报修的分类和状态信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "community.queryCommunityRepairTree")
public class QueryCommunityRepairTreeCmd extends Cmd {

    /**
     * 报表社区内部服务接口
     */
    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    /**
     * 字典服务接口
     */
    @Autowired
    private IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl;

    /**
     * 员工社区关联服务接口
     */
    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证用户权限，确保只有管理员可以执行此操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证当前用户是否为管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行命令方法
     * 
     * 构建小区报修树形结构数据，包含小区、报修类型和状态三个层级
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        List<RepairSettingDto> repairSettingDtos = null;

        // 创建报修设置查询条件对象
        RepairSettingDto repairSettingDto = new RepairSettingDto();
        // 获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询当前员工管理的所有小区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果员工管理的小区不为空，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            repairSettingDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 查询小区报修树形结构数据
        repairSettingDtos = reportCommunityInnerServiceSMOImpl.queryCommunityRepairTree(repairSettingDto);
        // 创建小区列表JSON数组
        JSONArray communitys = new JSONArray();
        
        // 如果查询结果为空，返回空数组
        if (ListUtil.isNull(repairSettingDtos)) {
            context.setResponseEntity(ResultVo.createResponseEntity(communitys));
            return;
        }

        JSONObject communityInfo = null;
        // 遍历报修设置数据，构建小区层级
        for (RepairSettingDto tmpRepairSettingDto : repairSettingDtos) {
            // 检查小区是否已添加到列表中
            if (!hasInCommunity(tmpRepairSettingDto, communitys)) {
                communityInfo = new JSONObject();
                communityInfo.put("id", "c_" + tmpRepairSettingDto.getCommunityId()); // 小区节点ID
                communityInfo.put("communityId", tmpRepairSettingDto.getCommunityId()); // 小区实际ID
                communityInfo.put("text", tmpRepairSettingDto.getCommunityName()); // 小区名称
                communityInfo.put("icon", "/img/org.png"); // 小区图标
                communityInfo.put("children", new JSONArray()); // 初始化子节点数组
                communitys.add(communityInfo);
            }
        }

        // 查询报修状态字典数据
        DictDto dictDto = new DictDto();
        dictDto.setTableName("r_repair_pool"); // 设置表名
        dictDto.setTableColumns("state"); // 设置列名
        List<DictDto> dictDtos = dictV1InnerServiceSMOImpl.queryDicts(dictDto);

        JSONObject community = null;
        // 为每个小区构建报修类型和状态层级
        for (int cIndex = 0; cIndex < communitys.size(); cIndex++) {
            community = communitys.getJSONObject(cIndex);
            // 在当前小区中查找报修设置数据
            findRepairSetting(community, repairSettingDtos, dictDtos);
        }
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(communitys));
    }

    /**
     * 查找报修设置数据并构建报修类型层级
     * 
     * 为指定小区查找对应的报修类型数据，构建报修类型节点
     * 
     * @param community 当前小区JSON对象
     * @param repairSettingDtos 所有报修设置数据列表
     * @param dictDtos 字典数据列表
     */
    private void findRepairSetting(JSONObject community, List<RepairSettingDto> repairSettingDtos, List<DictDto> dictDtos) {
        // 获取小区的子节点数组（报修类型列表）
        JSONArray parkings = community.getJSONArray("children");
        JSONObject parkingInfo = null;
        
        // 遍历报修设置数据，构建报修类型层级
        for (RepairSettingDto tmpRepairSettingDto : repairSettingDtos) {
            // 只处理当前小区的报修设置数据
            if(!community.getString("communityId").equals(tmpRepairSettingDto.getCommunityId())){
                continue;
            }
            // 检查报修类型是否已添加到列表中
            if (!hasInRepairSetting(tmpRepairSettingDto, parkings)) {
                parkingInfo = new JSONObject();
                parkingInfo.put("id", "r_" + tmpRepairSettingDto.getRepairType()); // 报修类型节点ID
                parkingInfo.put("repairType", tmpRepairSettingDto.getRepairType()); // 报修类型代码
                parkingInfo.put("communityId", community.getString("communityId")); // 所属小区ID
                parkingInfo.put("text", tmpRepairSettingDto.getRepairTypeName()); // 报修类型名称
                parkingInfo.put("icon", "/img/floor.png"); // 报修类型图标
                parkingInfo.put("children", new JSONArray()); // 初始化子节点数组（状态列表）
                parkings.add(parkingInfo);
            }
        }

        JSONObject parking = null;
        // 为每个报修类型构建状态层级
        for (int cIndex = 0; cIndex < parkings.size(); cIndex++) {
            parking = parkings.getJSONObject(cIndex);
            // 在当前报修类型中查找状态数据
            findState(parking, repairSettingDtos, dictDtos);
        }
    }

    /**
     * 查找状态数据并构建状态层级
     * 
     * 为指定报修类型查找对应的状态数据，构建状态节点
     * 
     * @param parking 当前报修类型JSON对象
     * @param repairSettingDtos 所有报修设置数据列表
     * @param dictDtos 字典数据列表
     */
    private void findState(JSONObject parking, List<RepairSettingDto> repairSettingDtos, List<DictDto> dictDtos) {
        // 获取报修类型的子节点数组（状态列表）
        JSONArray states = parking.getJSONArray("children");
        JSONObject stateInfo = null;
        
        // 遍历字典数据，构建状态层级
        for (DictDto tmpDictDto : dictDtos) {
            stateInfo = new JSONObject();
            stateInfo.put("id", "s_" + GenerateCodeFactory.getUUID()); // 状态节点ID（使用UUID）
            stateInfo.put("state", tmpDictDto.getStatusCd()); // 状态代码
            stateInfo.put("repairType", parking.getString("repairType")); // 所属报修类型
            stateInfo.put("communityId", parking.getString("communityId")); // 所属小区ID
            stateInfo.put("text", tmpDictDto.getName()); // 状态名称
            stateInfo.put("icon", "/img/unit.png"); // 状态图标
            states.add(stateInfo);
        }
    }

    /**
     * 检查报修类型是否已存在于报修类型列表中
     * 
     * @param tmpRepairSettingDto 当前报修设置数据对象
     * @param parkings 报修类型JSON数组
     * @return true-已存在，false-不存在
     */
    private boolean hasInRepairSetting(RepairSettingDto tmpRepairSettingDto, JSONArray parkings) {
        JSONObject parkingArea = null;
        // 遍历报修类型数组
        for (int cIndex = 0; cIndex < parkings.size(); cIndex++) {
            parkingArea = parkings.getJSONObject(cIndex);
            // 检查节点是否包含repairType字段
            if (!parkingArea.containsKey("repairType")) {
                continue;
            }
            // 检查repairType字段是否为空
            if (StringUtil.isEmpty(parkingArea.getString("repairType"))) {
                continue;
            }
            // 比较报修类型是否匹配
            if (parkingArea.getString("repairType").equals(tmpRepairSettingDto.getRepairType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查小区是否已存在于小区列表中
     * 
     * @param tmpRepairSettingDto 当前报修设置数据对象
     * @param communitys 小区JSON数组
     * @return true-已存在，false-不存在
     */
    private boolean hasInCommunity(RepairSettingDto tmpRepairSettingDto, JSONArray communitys) {
        JSONObject community = null;
        // 遍历小区数组
        for (int cIndex = 0; cIndex < communitys.size(); cIndex++) {
            community = communitys.getJSONObject(cIndex);
            // 检查节点是否包含communityId字段
            if (!community.containsKey("communityId")) {
                continue;
            }
            // 检查communityId字段是否为空
            if (StringUtil.isEmpty(community.getString("communityId"))) {
                continue;
            }
            // 比较小区ID是否匹配
            if (community.getString("communityId").equals(tmpRepairSettingDto.getCommunityId())) {
                return true;
            }
        }
        return false;
    }
}