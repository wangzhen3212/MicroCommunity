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
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.dto.unit.UnitDto;
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
 * 查询小区停车场树形结构命令类
 * 
 * 该类负责处理小区停车场树形结构查询请求，构建包含小区、停车场和租赁类型的树形数据结构
 * 主要用于前端展示小区停车场的层级关系
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "community.queryCommunityParkingTree")
public class QueryCommunityParkingTreeCmd extends Cmd {

    @Autowired
    private IReportCommunityInnerServiceSMO reportCommunityInnerServiceSMOImpl;

    @Autowired
    private IDictV1InnerServiceSMO dictV1InnerServiceSMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 验证请求参数和权限
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证当前用户必须是管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行小区停车场树形结构查询命令
     * 
     * 构建三层树形结构：
     * 第一层：小区信息
     * 第二层：停车场信息
     * 第三层：租赁类型信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        List<ParkingAreaDto> parkingAreaDtos = null;

        ParkingAreaDto parkingAreaDto = new ParkingAreaDto();

        // 获取当前用户ID
        String staffId = CmdContextUtils.getUserId(context);

        // 查询当前用户有权限的小区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户有权限的小区不为空，设置到查询条件中
        if (!ListUtil.isNull(communityIds)) {
            parkingAreaDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }

        // 查询小区停车场数据
        parkingAreaDtos = reportCommunityInnerServiceSMOImpl.queryCommunityParkingTree(parkingAreaDto);
        JSONArray communitys = new JSONArray();
        
        // 如果没有查询到数据，返回空数组
        if (ListUtil.isNull(parkingAreaDtos)) {
            context.setResponseEntity(ResultVo.createResponseEntity(communitys));
            return;
        }

        JSONObject communityInfo = null;
        // 构建第一层：小区信息
        for (ParkingAreaDto tmpParkingAreaDto : parkingAreaDtos) {
            // 检查小区是否已添加到结果中，避免重复添加
            if (!hasInCommunity(tmpParkingAreaDto, communitys)) {
                communityInfo = new JSONObject();
                communityInfo.put("id", "c_" + tmpParkingAreaDto.getCommunityId()); // 小区唯一标识
                communityInfo.put("communityId", tmpParkingAreaDto.getCommunityId()); // 小区ID
                communityInfo.put("text", tmpParkingAreaDto.getCommunityName()); // 小区名称
                communityInfo.put("icon", "/img/org.png"); // 小区图标
                communityInfo.put("children", new JSONArray()); // 初始化子节点数组
                communitys.add(communityInfo);
            }
        }

        // 查询租赁类型字典数据
        DictDto dictDto = new DictDto();
        dictDto.setTableName("owner_car");
        dictDto.setTableColumns("lease_type");
        List<DictDto> dictDtos = dictV1InnerServiceSMOImpl.queryDicts(dictDto);

        JSONObject community = null;
        // 为每个小区构建停车场和租赁类型数据
        for (int cIndex = 0; cIndex < communitys.size(); cIndex++) {
            community = communitys.getJSONObject(cIndex);
            // 查找并构建该小区的停车场数据
            findParkingArea(community, parkingAreaDtos, dictDtos);
        }
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(communitys));
    }

    /**
     * 查找并构建停车场数据
     * 
     * 为指定小区查找对应的停车场信息，并构建第二层树形结构
     * 
     * @param community 小区JSON对象
     * @param parkingAreaDtos 停车场数据列表
     * @param dictDtos 字典数据列表
     */
    private void findParkingArea(JSONObject community, List<ParkingAreaDto> parkingAreaDtos, List<DictDto> dictDtos) {
        JSONArray parkings = community.getJSONArray("children");
        JSONObject parkingInfo = null;
        
        // 遍历所有停车场数据，筛选出属于当前小区的停车场
        for (ParkingAreaDto tmpParkingAreaDto : parkingAreaDtos) {
            // 检查停车场是否属于当前小区
            if(!community.getString("communityId").equals(tmpParkingAreaDto.getCommunityId())){
                continue;
            }
            // 检查停车场是否已添加到结果中，避免重复添加
            if (!hasInParkingArea(tmpParkingAreaDto, parkings)) {
                parkingInfo = new JSONObject();
                parkingInfo.put("id", "p_" + tmpParkingAreaDto.getPaId()); // 停车场唯一标识
                parkingInfo.put("paId", tmpParkingAreaDto.getPaId()); // 停车场ID
                parkingInfo.put("text", tmpParkingAreaDto.getNum() + "停车场"); // 停车场名称
                parkingInfo.put("icon", "/img/floor.png"); // 停车场图标
                parkingInfo.put("children", new JSONArray()); // 初始化子节点数组
                parkings.add(parkingInfo);
            }
        }

        JSONObject parking = null;
        // 为每个停车场构建租赁类型数据
        for (int cIndex = 0; cIndex < parkings.size(); cIndex++) {
            parking = parkings.getJSONObject(cIndex);
            // 查找并构建该停车场的租赁类型数据
            findLeaseType(parking, parkingAreaDtos, dictDtos);
        }
    }

    /**
     * 查找并构建租赁类型数据
     * 
     * 为指定停车场添加所有可能的租赁类型选项，构建第三层树形结构
     * 
     * @param parking 停车场JSON对象
     * @param parkingAreaDtos 停车场数据列表
     * @param dictDtos 字典数据列表
     */
    private void findLeaseType(JSONObject parking, List<ParkingAreaDto> parkingAreaDtos, List<DictDto> dictDtos) {
        JSONArray leaseTypes = parking.getJSONArray("children");
        JSONObject leaseTypeInfo = null;
        
        // 遍历字典数据，添加所有租赁类型选项
        for (DictDto tmpDictDto : dictDtos) {
            leaseTypeInfo = new JSONObject();
            leaseTypeInfo.put("id", "l_" + GenerateCodeFactory.getUUID()); // 租赁类型唯一标识
            leaseTypeInfo.put("leaseType", tmpDictDto.getStatusCd()); // 租赁类型编码
            leaseTypeInfo.put("text", tmpDictDto.getName()); // 租赁类型名称
            leaseTypeInfo.put("icon", "/img/unit.png"); // 租赁类型图标
            leaseTypes.add(leaseTypeInfo);
        }
    }

    /**
     * 检查停车场是否已存在于结果数组中
     * 
     * @param tmpParkingAreaDto 待检查的停车场数据对象
     * @param parkings 停车场JSON数组
     * @return true-已存在，false-不存在
     */
    private boolean hasInParkingArea(ParkingAreaDto tmpParkingAreaDto, JSONArray parkings) {
        JSONObject parkingArea = null;
        for (int cIndex = 0; cIndex < parkings.size(); cIndex++) {
            parkingArea = parkings.getJSONObject(cIndex);
            // 检查JSON对象是否包含paId字段
            if (!parkingArea.containsKey("paId")) {
                continue;
            }
            // 检查paId字段是否为空
            if (StringUtil.isEmpty(parkingArea.getString("paId"))) {
                continue;
            }
            // 比较paId是否匹配
            if (parkingArea.getString("paId").equals(tmpParkingAreaDto.getPaId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查小区是否已存在于结果数组中
     * 
     * @param tmpParkingAreaDto 待检查的停车场数据对象（包含小区信息）
     * @param communitys 小区JSON数组
     * @return true-已存在，false-不存在
     */
    private boolean hasInCommunity(ParkingAreaDto tmpParkingAreaDto, JSONArray communitys) {
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
            // 比较communityId是否匹配
            if (community.getString("communityId").equals(tmpParkingAreaDto.getCommunityId())) {
                return true;
            }
        }
        return false;
    }
}