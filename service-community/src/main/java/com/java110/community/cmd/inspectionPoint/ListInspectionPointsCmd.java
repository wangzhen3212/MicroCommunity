package com.java110.community.cmd.inspectionPoint;

import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.utils.StringUtils;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.RoomDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.inspection.InspectionDto;
import com.java110.dto.unit.FloorAndUnitDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.IInspectionInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.inspectionPoint.ApiInspectionPointDataVo;
import com.java110.vo.api.inspectionPoint.ApiInspectionPointVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 巡检点列表查询命令类
 * 负责处理巡检点相关的查询请求，包括普通查询和关系查询两种模式
 * 支持分页查询，并能够根据巡检点位置类型刷新对应的位置信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "inspectionPoint.listInspectionPoints")
public class ListInspectionPointsCmd extends Cmd {

    @Autowired
    private IInspectionInnerServiceSMO inspectionPointInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 验证分页参数和必要字段，确保请求数据的完整性
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson); // 验证分页参数
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空"); // 验证小区ID必填
    }

    /**
     * 命令执行主方法
     * 根据请求参数决定执行关系查询或普通查询
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        if (reqJson.containsKey("relationship")) {
            queryRelationship(event, context, reqJson); // 执行关系查询
        } else {
            queryCommon(event, context, reqJson); // 执行普通查询
        }
    }

    /**
     * 关系查询方法
     * 查询巡检点与其他实体的关联关系信息
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    private void queryRelationship(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) {
        // 将请求参数转换为巡检点DTO对象
        InspectionDto inspectionPointDto = BeanConvertUtil.covertBean(reqJson, InspectionDto.class);

        // 查询关系数据总数
        int count = inspectionPointInnerServiceSMOImpl.queryInspectionsRelationShipCount(inspectionPointDto);

        List<ApiInspectionPointDataVo> inspectionPoints = null;

        if (count > 0) {
            // 查询关系数据并转换为VO对象
            inspectionPoints = BeanConvertUtil.covertBeanList(inspectionPointInnerServiceSMOImpl.getInspectionRelationShip(inspectionPointDto), ApiInspectionPointDataVo.class);
            // 刷新巡检点URL信息
            refreshInspections(inspectionPoints);
        } else {
            inspectionPoints = new ArrayList<>(); // 无数据时返回空列表
        }

        // 构建返回VO对象
        ApiInspectionPointVo apiInspectionPointVo = new ApiInspectionPointVo();
        apiInspectionPointVo.setTotal(count); // 设置总记录数
        apiInspectionPointVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row"))); // 计算总页数
        apiInspectionPointVo.setInspectionPoints(inspectionPoints); // 设置巡检点数据列表

        // 构建响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiInspectionPointVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity); // 设置响应结果
    }

    /**
     * 刷新巡检点URL信息
     * 为每个巡检点生成微信端的访问URL
     *
     * @param inspectionPoints 巡检点数据列表
     */
    private void refreshInspections(List<ApiInspectionPointDataVo> inspectionPoints) {
        // 从缓存中获取微信员工端URL
        String staffWechatUrl = MappingCache.getValue(MappingConstant.URL_DOMAIN,"STAFF_WECHAT_URL");
        String baseUrl;
        if (staffWechatUrl.endsWith("#/")) {
            baseUrl = staffWechatUrl + "pages/excuteOneQrCodeInspection/excuteOneQrCodeInspection?inspectionId=";
        } else if (staffWechatUrl.endsWith("/")) {
            baseUrl = staffWechatUrl + "#/pages/excuteOneQrCodeInspection/excuteOneQrCodeInspection?inspectionId=";
        } else {
            baseUrl = staffWechatUrl + "/#/pages/excuteOneQrCodeInspection/excuteOneQrCodeInspection?inspectionId=";
        }
        for (ApiInspectionPointDataVo apiInspectionPointDataVo : inspectionPoints) {
            // 构建巡检点二维码访问URL
            apiInspectionPointDataVo.setUrl(baseUrl
                    + apiInspectionPointDataVo.getInspectionId()
                    + "&inspectionName=" + apiInspectionPointDataVo.getInspectionName()
                    + "&itemId=" + apiInspectionPointDataVo.getItemId());
        }
    }

    /**
     * 普通查询方法
     * 查询巡检点基本信息，包括位置信息的刷新
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    private void queryCommon(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) {
        // 将请求参数转换为巡检点DTO对象
        InspectionDto inspectionPointDto = BeanConvertUtil.covertBean(reqJson, InspectionDto.class);

        // 查询巡检点总数
        int count = inspectionPointInnerServiceSMOImpl.queryInspectionsCount(inspectionPointDto);

        List<ApiInspectionPointDataVo> inspectionPoints = null;

        if (count > 0) {
            // 查询巡检点数据并转换为VO对象
            inspectionPoints = BeanConvertUtil.covertBeanList(inspectionPointInnerServiceSMOImpl.queryInspections(inspectionPointDto), ApiInspectionPointDataVo.class);
            // 刷新位置信息（小区、单元、房屋）
            refreshMachines(inspectionPoints);
            // 刷新巡检点URL信息
            refreshInspections(inspectionPoints);
        } else {
            inspectionPoints = new ArrayList<>(); // 无数据时返回空列表
        }

        // 构建返回VO对象
        ApiInspectionPointVo apiInspectionPointVo = new ApiInspectionPointVo();
        apiInspectionPointVo.setTotal(count); // 设置总记录数
        apiInspectionPointVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row"))); // 计算总页数
        apiInspectionPointVo.setInspectionPoints(inspectionPoints); // 设置巡检点数据列表

        // 构建响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiInspectionPointVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity); // 设置响应结果
    }

    /**
     * 刷新位置信息方法
     * 批量刷新巡检点的小区、单元、房屋位置信息
     *
     * @param inspectionPoints 巡检点数据列表
     */
    private void refreshMachines(List<ApiInspectionPointDataVo> inspectionPoints) {
        refreshCommunitys(inspectionPoints); // 批量处理小区信息
        refreshUnits(inspectionPoints); // 批量处理单元信息
        refreshRooms(inspectionPoints); // 批量处理房屋信息
    }

    /**
     * 刷新小区信息方法
     * 根据巡检点的位置对象ID刷新对应的小区名称信息
     *
     * @param inspectionPoints 巡检点数据列表
     */
    private void refreshCommunitys(List<ApiInspectionPointDataVo> inspectionPoints) {
        List<String> communityIds = new ArrayList<String>();
        List<ApiInspectionPointDataVo> inspectionPointDataVo = new ArrayList<>();
        
        // 筛选需要刷新小区信息的巡检点
        for (ApiInspectionPointDataVo inspectionPoint : inspectionPoints) {
            // 位置类型为小区（非2000和3000）且位置对象ID不为空
            if (!"2000".equals(inspectionPoint.getLocationTypeCd()) && !"3000".equals(inspectionPoint.getLocationTypeCd())) {
                if (StringUtils.isEmpty(inspectionPoint.getLocationObjId())) {
                    continue; // 跳过位置对象ID为空的记录
                }
                communityIds.add(inspectionPoint.getLocationObjId()); // 收集小区ID
                inspectionPointDataVo.add(inspectionPoint); // 收集需要刷新的巡检点
            }
        }

        if (communityIds.size() < 1) {
            return; // 无需要刷新的小区信息
        }
        
        String[] tmpCommunityIds = communityIds.toArray(new String[communityIds.size()]);

        // 构建小区查询条件
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(tmpCommunityIds);
        // 批量查询小区信息
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);

        // 刷新巡检点的小区名称信息
        for (ApiInspectionPointDataVo inspectionPoint : inspectionPointDataVo) {
            for (CommunityDto tmpCommunityDto : communityDtos) {
                if (inspectionPoint.getLocationObjId().equals(tmpCommunityDto.getCommunityId())) {
                    // 设置小区名称和位置类型名称的组合
                    inspectionPoint.setLocationObjName(tmpCommunityDto.getName() + " " + inspectionPoint.getLocationTypeName());
                }
            }
        }
    }

    /**
     * 刷新单元信息方法
     * 根据巡检点的位置对象ID刷新对应的单元信息
     *
     * @param inspectionPoints 巡检点数据列表
     */
    private void refreshUnits(List<ApiInspectionPointDataVo> inspectionPoints) {
        List<String> unitIds = new ArrayList<String>();
        List<ApiInspectionPointDataVo> tmpInspectionPoints = new ArrayList<>();
        
        // 筛选需要刷新单元信息的巡检点（位置类型为2000）
        for (ApiInspectionPointDataVo inspectionPointDataVo : inspectionPoints) {
            if ("2000".equals(inspectionPointDataVo.getLocationTypeCd())) {
                unitIds.add(inspectionPointDataVo.getLocationObjId()); // 收集单元ID
                tmpInspectionPoints.add(inspectionPointDataVo); // 收集需要刷新的巡检点
            }
        }

        if (unitIds.size() < 1) {
            return; // 无需要刷新的单元信息
        }
        
        String[] tmpUnitIds = unitIds.toArray(new String[unitIds.size()]);

        // 构建单元查询条件
        FloorAndUnitDto floorAndUnitDto = new FloorAndUnitDto();
        floorAndUnitDto.setUnitIds(tmpUnitIds);
        // 批量查询单元信息
        List<FloorAndUnitDto> unitDtos = unitInnerServiceSMOImpl.getFloorAndUnitInfo(floorAndUnitDto);

        // 刷新巡检点的单元信息
        for (ApiInspectionPointDataVo inspectionPointDataVo : tmpInspectionPoints) {
            for (FloorAndUnitDto tmpUnitDto : unitDtos) {
                if (inspectionPointDataVo.getLocationObjId().equals(tmpUnitDto.getUnitId())) {
                    // 设置单元位置名称（格式：X栋Y单元）
                    inspectionPointDataVo.setLocationObjName(tmpUnitDto.getFloorNum() + "栋" + tmpUnitDto.getUnitNum() + "单元");
                    // 复制单元属性到巡检点对象
                    BeanConvertUtil.covertBean(tmpUnitDto, inspectionPointDataVo);
                }
            }
        }
    }

    /**
     * 刷新房屋信息方法
     * 根据巡检点的位置对象ID刷新对应的房屋信息
     *
     * @param inspectionPoints 巡检点数据列表
     */
    private void refreshRooms(List<ApiInspectionPointDataVo> inspectionPoints) {
        List<String> roomIds = new ArrayList<String>();
        List<ApiInspectionPointDataVo> tmpInspectionPoint = new ArrayList<>();
        
        // 筛选需要刷新房屋信息的巡检点（位置类型为3000）
        for (ApiInspectionPointDataVo inspectionPointDataVo : inspectionPoints) {
            if ("3000".equals(inspectionPointDataVo.getLocationTypeCd())) {
                roomIds.add(inspectionPointDataVo.getLocationObjId()); // 收集房屋ID
                tmpInspectionPoint.add(inspectionPointDataVo); // 收集需要刷新的巡检点
            }
        }
        
        if (roomIds.size() < 1) {
            return; // 无需要刷新的房屋信息
        }
        
        String[] tmpRoomIds = roomIds.toArray(new String[roomIds.size()]);

        // 构建房屋查询条件
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomIds(tmpRoomIds);
        roomDto.setCommunityId(inspectionPoints.get(0).getCommunityId()); // 使用第一个巡检点的小区ID
        // 批量查询房屋信息
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

        // 刷新巡检点的房屋信息
        for (ApiInspectionPointDataVo inspectionPointDataVo : tmpInspectionPoint) {
            for (RoomDto tmpRoomDto : roomDtos) {
                if (inspectionPointDataVo.getLocationObjId().equals(tmpRoomDto.getRoomId())) {
                    // 设置房屋位置名称（格式：X栋Y单元Z室）
                    inspectionPointDataVo.setLocationObjName(tmpRoomDto.getFloorNum() + "栋" + tmpRoomDto.getUnitNum() + "单元" + tmpRoomDto.getRoomNum() + "室");
                    // 复制房屋属性到巡检点对象
                    BeanConvertUtil.covertBean(tmpRoomDto, inspectionPoints);
                }
            }
        }
    }
}