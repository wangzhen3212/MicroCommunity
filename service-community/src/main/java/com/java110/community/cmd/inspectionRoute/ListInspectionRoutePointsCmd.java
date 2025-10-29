package com.java110.community.cmd.inspectionRoute;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.RoomDto;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.inspection.InspectionRoutePointRelDto;
import com.java110.dto.unit.FloorAndUnitDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.IInspectionRoutePointRelInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.community.IUnitInnerServiceSMO;
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
 * 巡检路线点位列表查询命令类
 * 功能：查询指定巡检路线下的所有巡检点位信息，并返回分页结果
 * 用途：用于管理系统中的巡检路线点位关联关系查询
 */
@Java110Cmd(serviceCode = "inspectionRoute.listInspectionRoutePoints")
public class ListInspectionRoutePointsCmd extends Cmd {

    @Autowired
    private IInspectionRoutePointRelInnerServiceSMO inspectionRoutePointRelInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    @Autowired
    private IUnitInnerServiceSMO unitInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能：验证请求参数的有效性，包括分页参数和必要字段
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        super.validatePageInfo(reqJson); // 验证分页参数
        super.validateProperty(context); // 验证属性配置
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空"); // 验证小区ID必填
    }

    /**
     * 命令执行方法
     * 功能：查询巡检路线点位列表并返回分页结果
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为DTO对象
        InspectionRoutePointRelDto inspectionRoutePointRelDto = BeanConvertUtil.covertBean(reqJson, InspectionRoutePointRelDto.class);

        // 查询巡检路线点位关联记录总数
        int count = inspectionRoutePointRelInnerServiceSMOImpl.queryInspectionRoutePointRelsCount(inspectionRoutePointRelDto);

        List<ApiInspectionPointDataVo> inspectionPoints = null;

        if (count > 0) {
            // 查询巡检路线点位关联记录列表并转换为VO对象
            inspectionPoints = BeanConvertUtil.covertBeanList(inspectionRoutePointRelInnerServiceSMOImpl.queryInspectionRoutePointRels(inspectionRoutePointRelDto), ApiInspectionPointDataVo.class);
            // 刷新点位的位置信息（小区、单元、房屋）
            refreshMachines(inspectionPoints);
        } else {
            inspectionPoints = new ArrayList<>();
        }

        // 构建返回结果VO对象
        ApiInspectionPointVo apiInspectionPointVo = new ApiInspectionPointVo();
        apiInspectionPointVo.setTotal(count); // 设置总记录数
        apiInspectionPointVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row"))); // 计算总页数
        apiInspectionPointVo.setInspectionPoints(inspectionPoints); // 设置点位数据列表

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiInspectionPointVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新点位位置信息方法
     * 功能：批量处理巡检点位的位置信息，包括小区、单元和房屋信息
     * @param inspectionPoints 巡检点位数据列表
     */
    private void refreshMachines(List<ApiInspectionPointDataVo> inspectionPoints) {
        // 批量处理小区信息
        refreshCommunitys(inspectionPoints);
        // 批量处理单元信息
        refreshUnits(inspectionPoints);
        // 批量处理房屋信息
        refreshRooms(inspectionPoints);
    }

    /**
     * 刷新小区信息方法
     * 功能：为巡检点位设置对应的小区名称信息
     * @param inspectionPoints 巡检点位数据列表
     */
    private void refreshCommunitys(List<ApiInspectionPointDataVo> inspectionPoints) {
        List<String> communityIds = new ArrayList<String>();
        List<ApiInspectionPointDataVo> inspectionPointDataVo = new ArrayList<>();
        
        // 遍历巡检点位，筛选需要处理小区信息的点位
        for (ApiInspectionPointDataVo inspectionPoint : inspectionPoints) {
            // 位置类型编码：非2000(单元)和3000(房屋)的点位需要处理小区信息
            if (!"2000".equals(inspectionPoint.getLocationTypeCd()) && !"3000".equals(inspectionPoint.getLocationTypeCd())) {
                communityIds.add(inspectionPoint.getLocationObjId());
                inspectionPointDataVo.add(inspectionPoint);
            }
        }

        if (communityIds.size() < 1) {
            return;
        }
        
        // 转换小区ID列表为数组
        String[] tmpCommunityIds = communityIds.toArray(new String[communityIds.size()]);

        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(tmpCommunityIds);
        // 根据小区ID列表查询小区信息
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);

        // 为每个巡检点位设置小区名称
        for (ApiInspectionPointDataVo inspectionPoint : inspectionPointDataVo) {
            for (CommunityDto tmpCommunityDto : communityDtos) {
                if (inspectionPoint.getLocationObjId().equals(tmpCommunityDto.getCommunityId())) {
                    inspectionPoint.setLocationObjName(tmpCommunityDto.getName() + " " + inspectionPoint.getLocationTypeName());
                }
            }
        }
    }

    /**
     * 刷新单元信息方法
     * 功能：为巡检点位设置对应的单元名称信息
     * @param inspectionPoints 巡检点位数据列表
     */
    private void refreshUnits(List<ApiInspectionPointDataVo> inspectionPoints) {
        List<String> unitIds = new ArrayList<String>();
        List<ApiInspectionPointDataVo> tmpInspectionPoints = new ArrayList<>();
        
        // 遍历巡检点位，筛选需要处理单元信息的点位
        for (ApiInspectionPointDataVo inspectionPointDataVo : inspectionPoints) {
            // 位置类型编码：2000表示单元类型
            if ("2000".equals(inspectionPointDataVo.getLocationTypeCd())) {
                unitIds.add(inspectionPointDataVo.getLocationObjId());
                tmpInspectionPoints.add(inspectionPointDataVo);
            }
        }

        if (unitIds.size() < 1) {
            return;
        }
        
        // 转换单元ID列表为数组
        String[] tmpUnitIds = unitIds.toArray(new String[unitIds.size()]);

        FloorAndUnitDto floorAndUnitDto = new FloorAndUnitDto();
        floorAndUnitDto.setUnitIds(tmpUnitIds);
        // 根据单元ID列表查询单元信息
        List<FloorAndUnitDto> unitDtos = unitInnerServiceSMOImpl.getFloorAndUnitInfo(floorAndUnitDto);

        // 为每个巡检点位设置单元名称
        for (ApiInspectionPointDataVo inspectionPointDataVo : tmpInspectionPoints) {
            for (FloorAndUnitDto tmpUnitDto : unitDtos) {
                if (inspectionPointDataVo.getLocationObjId().equals(tmpUnitDto.getUnitId())) {
                    // 设置单元名称格式：X栋Y单元
                    inspectionPointDataVo.setLocationObjName(tmpUnitDto.getFloorNum() + "栋" + tmpUnitDto.getUnitNum() + "单元");
                    BeanConvertUtil.covertBean(tmpUnitDto, inspectionPointDataVo);
                }
            }
        }
    }

    /**
     * 刷新房屋信息方法
     * 功能：为巡检点位设置对应的房屋名称信息
     * @param inspectionPoints 巡检点位数据列表
     */
    private void refreshRooms(List<ApiInspectionPointDataVo> inspectionPoints) {
        List<String> roomIds = new ArrayList<String>();
        List<ApiInspectionPointDataVo> tmpInspectionPoint = new ArrayList<>();
        
        // 遍历巡检点位，筛选需要处理房屋信息的点位
        for (ApiInspectionPointDataVo inspectionPointDataVo : inspectionPoints) {
            // 位置类型编码：3000表示房屋类型
            if ("3000".equals(inspectionPointDataVo.getLocationTypeCd())) {
                roomIds.add(inspectionPointDataVo.getLocationObjId());
                tmpInspectionPoint.add(inspectionPointDataVo);
            }
        }
        
        if (roomIds.size() < 1) {
            return;
        }
        
        // 转换房屋ID列表为数组
        String[] tmpRoomIds = roomIds.toArray(new String[roomIds.size()]);

        RoomDto roomDto = new RoomDto();
        roomDto.setRoomIds(tmpRoomIds);
        roomDto.setCommunityId(inspectionPoints.get(0).getCommunityId()); // 使用第一个点位的社区ID
        // 根据房屋ID列表查询房屋信息
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

        // 为每个巡检点位设置房屋名称
        for (ApiInspectionPointDataVo inspectionPointDataVo : tmpInspectionPoint) {
            for (RoomDto tmpRoomDto : roomDtos) {
                if (inspectionPointDataVo.getLocationObjId().equals(tmpRoomDto.getRoomId())) {
                    // 设置房屋名称格式：X栋Y单元Z室
                    inspectionPointDataVo.setLocationObjName(tmpRoomDto.getFloorNum() + "栋" + tmpRoomDto.getUnitNum() + "单元" + tmpRoomDto.getRoomNum() + "室");
                    BeanConvertUtil.covertBean(tmpRoomDto, inspectionPoints);
                }
            }
        }
    }
}