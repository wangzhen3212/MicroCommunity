package com.java110.report.cmd.community;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.RoomDto;
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
 * 查询小区单元树形结构命令类
 * 该命令用于查询当前用户有权限访问的小区、楼栋、单元的三级树形结构数据
 * 主要用于前端展示小区组织结构树
 */
@Java110Cmd(serviceCode = "community.queryCommunityUnitTree")
public class QueryCommunityUnitTreeCmd extends Cmd {

    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证当前用户是否为管理员权限
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException  命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证当前用户必须为管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行命令方法
     * 查询小区单元树形结构数据并构建前端所需的树形结构
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException  命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        List<UnitDto> unitDtos = null;

        UnitDto unitDto = new UnitDto();
        // 从上下文中获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询当前用户有权限访问的小区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户有权限的小区列表不为空，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            unitDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 查询小区单元树形数据
        unitDtos = reportCommunityInnerServiceSMOImpl.queryCommunityUnitTree(unitDto);
        JSONArray communitys = new JSONArray();
        
        // 如果查询结果为空，直接返回空数组
        if (ListUtil.isNull(unitDtos)) {
            context.setResponseEntity(ResultVo.createResponseEntity(communitys));
            return;
        }

        JSONObject communityInfo = null;
        // 遍历单元数据，构建小区级别的树形结构
        for (UnitDto tmpUnitDto : unitDtos) {
            // 检查小区是否已添加到结果中，避免重复添加
            if (!hasInCommunity(tmpUnitDto, communitys)) {
                communityInfo = new JSONObject();
                communityInfo.put("id", "c_" + tmpUnitDto.getCommunityId());  // 小区节点ID
                communityInfo.put("communityId", tmpUnitDto.getCommunityId());  // 小区实际ID
                communityInfo.put("text", tmpUnitDto.getCommunityName());  // 小区显示名称
                communityInfo.put("icon", "/img/org.png");  // 小区图标
                communityInfo.put("children", new JSONArray());  // 小区子节点（楼栋）
                communitys.add(communityInfo);
            }
        }

        JSONObject community = null;
        // 为每个小区查找对应的楼栋数据
        for (int cIndex = 0; cIndex < communitys.size(); cIndex++) {
            community = communitys.getJSONObject(cIndex);
            // 在当前小区中查找楼栋数据
            findFloor(community, unitDtos);
        }
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(communitys));
    }

    /**
     * 查找小区下的楼栋数据
     * 根据小区信息从单元数据中筛选出对应的楼栋数据并构建楼栋级别的树形结构
     *
     * @param community 当前小区JSON对象
     * @param unitDtos  所有单元数据列表
     */
    private void findFloor(JSONObject community, List<UnitDto> unitDtos) {
        JSONArray floors = community.getJSONArray("children");
        JSONObject floorInfo = null;
        
        // 遍历所有单元数据，筛选出属于当前小区的楼栋
        for (UnitDto tmpUnitDto : unitDtos) {
            // 跳过不属于当前小区的数据
            if (!community.getString("communityId").equals(tmpUnitDto.getCommunityId())) {
                continue;
            }
            // 检查楼栋是否已添加到小区中，避免重复添加
            if (!hasInFloor(tmpUnitDto, floors)) {
                floorInfo = new JSONObject();
                floorInfo.put("id", "f_" + tmpUnitDto.getFloorId());  // 楼栋节点ID
                floorInfo.put("floorId", tmpUnitDto.getFloorId());  // 楼栋实际ID
                floorInfo.put("communityId", community.getString("communityId"));  // 所属小区ID
                floorInfo.put("text", tmpUnitDto.getFloorNum() + "栋");  // 楼栋显示名称
                floorInfo.put("icon", "/img/floor.png");  // 楼栋图标
                floorInfo.put("children", new JSONArray());  // 楼栋子节点（单元）
                floors.add(floorInfo);
            }
        }

        JSONObject floor = null;
        // 为每个楼栋查找对应的单元数据
        for (int cIndex = 0; cIndex < floors.size(); cIndex++) {
            floor = floors.getJSONObject(cIndex);
            // 在当前楼栋中查找单元数据
            findUnit(floor, unitDtos);
        }
    }

    /**
     * 查找楼栋下的单元数据
     * 根据楼栋信息从单元数据中筛选出对应的单元数据并构建单元级别的树形结构
     *
     * @param floor    当前楼栋JSON对象
     * @param unitDtos 所有单元数据列表
     */
    private void findUnit(JSONObject floor, List<UnitDto> unitDtos) {
        JSONArray units = floor.getJSONArray("children");
        JSONObject unitInfo = null;
        
        // 遍历所有单元数据，筛选出属于当前楼栋的单元
        for (UnitDto tmpUnitDto : unitDtos) {
            // 跳过不属于当前小区的数据
            if (!floor.getString("communityId").equals(tmpUnitDto.getCommunityId())) {
                continue;
            }
            // 跳过不属于当前楼栋的数据
            if (!floor.getString("floorId").equals(tmpUnitDto.getFloorId())) {
                continue;
            }
            // 检查单元是否已添加到楼栋中，避免重复添加
            if (!hasInUnit(tmpUnitDto, units)) {
                unitInfo = new JSONObject();
                unitInfo.put("id", "u_" + tmpUnitDto.getUnitId());  // 单元节点ID
                unitInfo.put("unitId", tmpUnitDto.getUnitId());  // 单元实际ID
                unitInfo.put("text", tmpUnitDto.getUnitNum() + "单元");  // 单元显示名称
                unitInfo.put("icon", "/img/unit.png");  // 单元图标
                units.add(unitInfo);
            }
        }
    }

    /**
     * 检查单元是否已存在于单元列表中
     *
     * @param unitDto 要检查的单元DTO对象
     * @param units   单元JSON数组
     * @return true-已存在，false-不存在
     */
    private boolean hasInUnit(UnitDto unitDto, JSONArray units) {
        JSONObject unit = null;
        // 遍历单元数组，检查是否已存在相同单元ID
        for (int cIndex = 0; cIndex < units.size(); cIndex++) {
            unit = units.getJSONObject(cIndex);
            // 跳过不包含unitId字段的对象
            if (!unit.containsKey("unitId")) {
                continue;
            }
            // 跳过unitId为空的对象
            if (StringUtil.isEmpty(unit.getString("unitId"))) {
                continue;
            }
            // 如果找到相同单元ID，返回true
            if (unit.getString("unitId").equals(unitDto.getUnitId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查楼栋是否已存在于楼栋列表中
     *
     * @param tmpUnitDto 要检查的单元DTO对象（包含楼栋信息）
     * @param floors     楼栋JSON数组
     * @return true-已存在，false-不存在
     */
    private boolean hasInFloor(UnitDto tmpUnitDto, JSONArray floors) {
        JSONObject floor = null;
        // 遍历楼栋数组，检查是否已存在相同楼栋ID
        for (int cIndex = 0; cIndex < floors.size(); cIndex++) {
            floor = floors.getJSONObject(cIndex);
            // 跳过不包含floorId字段的对象
            if (!floor.containsKey("floorId")) {
                continue;
            }
            // 跳过floorId为空的对象
            if (StringUtil.isEmpty(floor.getString("floorId"))) {
                continue;
            }
            // 如果找到相同楼栋ID，返回true
            if (floor.getString("floorId").equals(tmpUnitDto.getFloorId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查小区是否已存在于小区列表中
     *
     * @param tmpUnitDto 要检查的单元DTO对象（包含小区信息）
     * @param communitys 小区JSON数组
     * @return true-已存在，false-不存在
     */
    private boolean hasInCommunity(UnitDto tmpUnitDto, JSONArray communitys) {
        JSONObject community = null;
        // 遍历小区数组，检查是否已存在相同小区ID
        for (int cIndex = 0; cIndex < communitys.size(); cIndex++) {
            community = communitys.getJSONObject(cIndex);
            // 跳过不包含communityId字段的对象
            if (!community.containsKey("communityId")) {
                continue;
            }
            // 跳过communityId为空的对象
            if (StringUtil.isEmpty(community.getString("communityId"))) {
                continue;
            }
            // 如果找到相同小区ID，返回true
            if (community.getString("communityId").equals(tmpUnitDto.getCommunityId())) {
                return true;
            }
        }
        return false;
    }
}