package com.java110.report.cmd.community;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.floor.FloorDto;
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
 * 查询小区楼栋树形结构命令类
 * 
 * 该命令类用于处理查询小区楼栋树形结构的请求，将小区和楼栋数据组织成树形结构返回
 * 主要用于前端展示小区-楼栋的层级关系
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "community.queryCommunityFloorTree")
public class QueryCommunityFloorTreeCmd extends Cmd {

    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;
    
    /**
     * 请求参数验证方法
     * 
     * 验证请求的合法性，必须为管理员权限才能执行此操作
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 必须为管理员权限，调用父类的管理员验证方法
        super.validateAdmin(context);
    }

    /**
     * 命令执行方法
     * 
     * 主要业务逻辑：查询用户有权限的小区列表，获取小区楼栋数据，并组织成树形结构返回
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        List<FloorDto> floorDtos = null;

        FloorDto floorDto = new FloorDto();
        // 从上下文中获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询当前用户有权限的小区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户有权限的小区列表不为空，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            floorDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 调用服务查询小区楼栋树形数据
        floorDtos = reportCommunityInnerServiceSMOImpl.queryCommunityFloorTree(floorDto);
        JSONArray communitys = new JSONArray();
        
        // 如果查询结果为空，直接返回空数组
        if (ListUtil.isNull(floorDtos)) {
            context.setResponseEntity(ResultVo.createResponseEntity(communitys));
            return;
        }

        JSONObject communityInfo = null;
        // 遍历楼栋数据，构建小区节点（去重）
        for (FloorDto tmpFloorDto : floorDtos) {
            if (!hasInCommunity(tmpFloorDto, communitys)) {
                communityInfo = new JSONObject();
                communityInfo.put("id", "c_" + tmpFloorDto.getCommunityId()); // 小区节点ID前缀
                communityInfo.put("communityId", tmpFloorDto.getCommunityId()); // 小区实际ID
                communityInfo.put("text", tmpFloorDto.getCommunityName()); // 小区名称显示文本
                communityInfo.put("icon", "/img/org.png"); // 小区图标
                communityInfo.put("children", new JSONArray()); // 初始化子节点数组
                communitys.add(communityInfo);
            }
        }

        JSONObject community = null;
        // 为每个小区节点查找并添加对应的楼栋数据
        for (int cIndex = 0; cIndex < communitys.size(); cIndex++) {
            community = communitys.getJSONObject(cIndex);
            // 在楼栋数据中查找属于当前小区的楼栋
            findFloor(community, floorDtos);
        }
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(communitys));


    }

    /**
     * 查找并添加小区对应的楼栋数据
     * 
     * 遍历所有楼栋数据，将属于指定小区的楼栋添加到小区的children数组中
     * 
     * @param community 当前小区节点对象
     * @param unitDtos 所有楼栋数据列表
     */
    private void findFloor(JSONObject community, List<FloorDto> unitDtos) {
        JSONArray floors = community.getJSONArray("children");
        JSONObject floorInfo = null;
        // 遍历所有楼栋数据
        for (FloorDto tmpFloorDto : unitDtos) {
            // 只处理属于当前小区的楼栋
            if(!community.getString("communityId").equals(tmpFloorDto.getCommunityId())){
                continue;
            }
            // 如果楼栋尚未添加到小区中，则创建楼栋节点
            if (!hasInFloor(tmpFloorDto, floors)) {
                floorInfo = new JSONObject();
                floorInfo.put("id", "f_" + tmpFloorDto.getFloorId()); // 楼栋节点ID前缀
                floorInfo.put("floorId", tmpFloorDto.getFloorId()); // 楼栋实际ID
                floorInfo.put("communityId", community.getString("communityId")); // 所属小区ID
                floorInfo.put("text", tmpFloorDto.getFloorNum()+"栋"); // 楼栋显示文本
                floorInfo.put("icon", "/img/floor.png"); // 楼栋图标
                floorInfo.put("children", new JSONArray()); // 初始化子节点数组（可为单元或房间）
                floors.add(floorInfo);
            }
        }
    }

    /**
     * 检查楼栋是否已存在于楼栋数组中
     * 
     * 通过楼栋ID判断指定的楼栋是否已经存在于给定的楼栋JSON数组中
     * 
     * @param tmpFloorDto 要检查的楼栋数据传输对象
     * @param floors 楼栋JSON数组
     * @return boolean 如果楼栋已存在返回true，否则返回false
     */
    private boolean hasInFloor(FloorDto tmpFloorDto, JSONArray floors) {
        JSONObject floor = null;
        // 遍历楼栋数组检查是否存在
        for (int cIndex = 0; cIndex < floors.size(); cIndex++) {
            floor = floors.getJSONObject(cIndex);
            // 检查JSON对象是否包含floorId字段
            if (!floor.containsKey("floorId")) {
                continue;
            }
            // 检查floorId字段是否为空
            if (StringUtil.isEmpty(floor.getString("floorId"))) {
                continue;
            }
            // 比较楼栋ID是否匹配
            if (floor.getString("floorId").equals(tmpFloorDto.getFloorId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查小区是否已存在于小区数组中
     * 
     * 通过小区ID判断指定的小区是否已经存在于给定的小区JSON数组中
     * 
     * @param tmpFloorDto 包含小区信息的楼栋数据传输对象
     * @param communitys 小区JSON数组
     * @return boolean 如果小区已存在返回true，否则返回false
     */
    private boolean hasInCommunity(FloorDto tmpFloorDto, JSONArray communitys) {
        JSONObject community = null;
        // 遍历小区数组检查是否存在
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
            // 比较小区ID是否匹配
            if (community.getString("communityId").equals(tmpFloorDto.getCommunityId())) {
                return true;
            }
        }
        return false;
    }

}