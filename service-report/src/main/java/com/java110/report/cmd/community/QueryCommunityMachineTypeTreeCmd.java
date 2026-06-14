package com.java110.report.cmd.community;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.floor.FloorDto;
import com.java110.dto.machine.MachineTypeDto;
import com.java110.dto.org.OrgDto;
import com.java110.dto.unit.UnitDto;
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
 * 查询小区设备类型树命令类
 * 
 * 该命令类用于查询用户有权限访问的小区下的设备类型树形结构数据，
 * 返回包含小区和设备类型的层级结构数据，用于前端树形组件展示
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "community.queryCommunityMachineTypeTree")
public class QueryCommunityMachineTypeTreeCmd extends Cmd {

    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要进行管理员权限验证
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        super.validateAdmin(context);
    }

    /**
     * 命令执行主方法
     * 
     * 根据当前用户权限查询小区设备类型树形结构数据，
     * 构建包含小区和设备类型的层级结构返回给前端
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        List<MachineTypeDto> machineTypeDtos = null;

        MachineTypeDto machineTypeDto = new MachineTypeDto();

        // 获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询用户有权限的小区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户有权限的小区列表不为空，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            machineTypeDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 查询小区设备类型数据
        machineTypeDtos = reportCommunityInnerServiceSMOImpl.queryCommunityMachineTypeTree(machineTypeDto);
        JSONArray communitys = new JSONArray();
        
        // 如果查询结果为空，直接返回空数组
        if (ListUtil.isNull(machineTypeDtos)) {
            context.setResponseEntity(ResultVo.createResponseEntity(communitys));
            return;
        }

        // 构建小区层级结构
        JSONObject communityInfo = null;
        for (MachineTypeDto tmpMachineTypeDto : machineTypeDtos) {
            // 检查小区是否已添加到结果中，避免重复添加
            if (!hasInCommunity(tmpMachineTypeDto, communitys)) {
                communityInfo = new JSONObject();
                communityInfo.put("id", "c_" + tmpMachineTypeDto.getCommunityId()); // 小区节点ID前缀
                communityInfo.put("communityId", tmpMachineTypeDto.getCommunityId());
                communityInfo.put("text", tmpMachineTypeDto.getCommunityName()); // 小区显示名称
                communityInfo.put("icon", "/img/org.png"); // 小区节点图标
                communityInfo.put("children", new JSONArray()); // 初始化子节点数组
                communitys.add(communityInfo);
            }
        }

        // 如果没有小区数据，直接返回
        if(ListUtil.isNull(communitys)){
            context.setResponseEntity(ResultVo.createResponseEntity(communitys));
            return;
        }
        
        // 为每个小区构建设备类型树
        for(int cIndex = 0 ;cIndex < communitys.size();cIndex++){
            communityInfo = communitys.getJSONObject(cIndex);
            findOneMachineType(communityInfo, machineTypeDtos);
        }
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(communitys));
    }

    /**
     * 查找一级设备类型
     * 
     * 为指定小区查找一级设备类型（没有父类型的设备类型），
     * 并递归查找下级设备类型
     * 
     * @param communityInfo 小区信息JSON对象
     * @param machineTypeDtos 设备类型数据列表
     */
    private void findOneMachineType(JSONObject communityInfo, List<MachineTypeDto> machineTypeDtos) {
        JSONArray childs = communityInfo.getJSONArray("children");
        JSONObject child = null;
        
        // 查找一级设备类型（没有父类型的设备类型）
        for (MachineTypeDto tmpMachineTypeDto : machineTypeDtos) {
            // 只处理当前小区的设备类型
            if(!communityInfo.getString("communityId").equals(tmpMachineTypeDto.getCommunityId())){
                continue;
            }
            // 一级设备类型：ParentTypeId为空
            if (StringUtil.isEmpty(tmpMachineTypeDto.getParentTypeId())) {
                child = new JSONObject();
                child.put("id", "m_" + tmpMachineTypeDto.getTypeId()); // 设备类型节点ID前缀
                child.put("typeId", tmpMachineTypeDto.getTypeId());
                child.put("communityId", communityInfo.getString("communityId"));
                child.put("text", tmpMachineTypeDto.getMachineTypeName()); // 设备类型显示名称
                child.put("icon", "/img/org.png"); // 设备类型节点图标
                childs.add(child);
            }
        }
        
        // 递归查找每个一级设备类型的子类型
        for(int cIndex = 0 ;cIndex < childs.size();cIndex++){
            child = childs.getJSONObject(cIndex);
            findChilds(child, machineTypeDtos);
        }
    }

    /**
     * 递归查找子设备类型
     * 
     * 为指定的父设备类型查找所有子设备类型，
     * 构建设备类型的层级树形结构
     * 
     * @param parentChild 父设备类型JSON对象
     * @param machineTypeDtos 设备类型数据列表
     */
    private void findChilds(JSONObject parentChild, List<MachineTypeDto> machineTypeDtos) {

        JSONArray childs = new JSONArray();
        JSONObject child = null;
        
        // 查找当前父设备类型的所有子设备类型
        for (MachineTypeDto machineTypeDto : machineTypeDtos) {
            // 跳过自身
            if (parentChild.getString("typeId").equals(machineTypeDto.getTypeId())) {
                continue;
            }
            // 跳过一级设备类型
            if(StringUtil.isEmpty(machineTypeDto.getParentTypeId())){
                continue;
            }
            // 找到父类型ID匹配的设备类型（二级及以下设备类型）
            if (machineTypeDto.getParentTypeId().equals(parentChild.getString("typeId"))) {
                child = new JSONObject();
                child.put("id", "m_" + machineTypeDto.getTypeId()); // 子设备类型节点ID前缀
                child.put("typeId", machineTypeDto.getTypeId());
                child.put("communityId", parentChild.getString("communityId"));
                child.put("text", machineTypeDto.getMachineTypeName()); // 子设备类型显示名称
                child.put("icon", "/img/org.png"); // 子设备类型节点图标
                childs.add(child);
            }
        }

        // 如果没有子设备类型，直接返回
        if (ListUtil.isNull(childs)) {
            return;
        }

        // 将子设备类型数组设置到父设备类型中
        parentChild.put("children",childs);

        // 递归查找每个子设备类型的下级设备类型
        for(int orgIndex = 0 ;orgIndex < childs.size();orgIndex++){
            child = childs.getJSONObject(orgIndex);
            findChilds(child, machineTypeDtos);
        }
    }

    /**
     * 检查小区是否已存在
     * 
     * 检查指定小区是否已经在结果数组中存在，避免重复添加
     * 
     * @param tmpMachineTypeDto 设备类型数据传输对象
     * @param communitys 小区JSON数组
     * @return boolean true-小区已存在，false-小区不存在
     */
    private boolean hasInCommunity(MachineTypeDto tmpMachineTypeDto, JSONArray communitys) {
        JSONObject community = null;
        for (int cIndex = 0; cIndex < communitys.size(); cIndex++) {
            community = communitys.getJSONObject(cIndex);
            // 检查JSON对象是否包含communityId字段
            if (!community.containsKey("communityId")) {
                continue;
            }
            // 检查communityId字段是否为空
            if (StringUtil.isEmpty(community.getString("communityId"))) {
                continue;
            }
            // 比较小区ID是否相同
            if (community.getString("communityId").equals(tmpMachineTypeDto.getCommunityId())) {
                return true;
            }
        }
        return false;
    }
}