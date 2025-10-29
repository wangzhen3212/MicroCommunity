package com.java110.community.cmd.inspectionRoute;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.community.CommunityDto;
import com.java110.dto.inspection.InspectionRoutePointRelDto;
import com.java110.dto.room.RoomDto;
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
 * 巡检路线管理点查询命令类
 * 功能：查询巡检路线下的管理点信息，支持分页查询，并根据位置类型刷新对应的位置信息（小区、单元、房屋）
 * 用途：管理员查看巡检路线关联的巡检点详细信息
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "inspectionRoute.listAdminInspectionRoutePoints")
public class ListAdminInspectionRoutePointsCmd extends Cmd {

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
     * 验证分页参数和管理员权限
     *
     * @param event   命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行命令方法
     * 查询巡检路线点关联信息，并返回分页结果
     *
     * @param event   命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为DTO对象
        InspectionRoutePointRelDto inspectionRoutePointRelDto = BeanConvertUtil.covertBean(reqJson, InspectionRoutePointRelDto.class);

        // 查询巡检路线点关联记录总数
        int count = inspectionRoutePointRelInnerServiceSMOImpl.queryInspectionRoutePointRelsCount(inspectionRoutePointRelDto);

        List<ApiInspectionPointDataVo> inspectionPoints = null;

        if (count > 0) {
            // 查询巡检路线点关联记录列表并转换为VO对象
            inspectionPoints = BeanConvertUtil.covertBeanList(inspectionRoutePointRelInnerServiceSMOImpl.queryInspectionRoutePointRels(inspectionRoutePointRelDto), ApiInspectionPointDataVo.class);
            // 刷新位置信息（小区、单元、房屋）
            refreshMachines(inspectionPoints);
        } else {
            // 无数据时返回空列表
            inspectionPoints = new ArrayList<>();
        }

        // 构建返回VO对象
        ApiInspectionPointVo apiInspectionPointVo = new ApiInspectionPointVo();
        apiInspectionPointVo.setTotal(count);
        // 计算总页数
        apiInspectionPointVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiInspectionPointVo.setInspectionPoints(inspectionPoints);

        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiInspectionPointVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新巡检点位置信息
     * 批量处理小区、单元和房屋的位置信息
     *
     * @param inspectionPoints 巡检点数据列表
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
     * 刷新小区位置信息
     * 处理位置类型为小区的巡检点，设置小区名称
     *
     * @param inspectionPoints 巡检点数据列表
     */
    private void refreshCommunitys(List<ApiInspectionPointDataVo> inspectionPoints) {
        List<String> communityIds = new ArrayList<String>();
        List<ApiInspectionPointDataVo> inspectionPointDataVo = new ArrayList<>();
        
        // 遍历巡检点，筛选出位置类型为小区的记录
        for (ApiInspectionPointDataVo inspectionPoint : inspectionPoints) {
            // 位置类型编码：2000-单元，3000-房屋，其他-小区
            if (!"2000".equals(inspectionPoint.getLocationTypeCd()) && !"3000".equals(inspectionPoint.getLocationTypeCd())) {
                communityIds.add(inspectionPoint.getLocationObjId());
                inspectionPointDataVo.add(inspectionPoint);
            }
        }

        // 如果没有小区类型的巡检点，直接返回
        if (communityIds.size() < 1) {
            return;
        }
        
        String[] tmpCommunityIds = communityIds.toArray(new String[communityIds.size()]);

        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityIds(tmpCommunityIds);
        // 根据小区ID查询小区信息
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);

        // 设置小区名称
        for (ApiInspectionPointDataVo inspectionPoint : inspectionPointDataVo) {
            for (CommunityDto tmpCommunityDto : communityDtos) {
                if (inspectionPoint.getLocationObjId().equals(tmpCommunityDto.getCommunityId())) {
                    inspectionPoint.setLocationObjName(tmpCommunityDto.getName() + " " + inspectionPoint.getLocationTypeName());
                }
            }
        }
    }

    /**
     * 刷新单元位置信息
     * 处理位置类型为单元的巡检点，设置单元名称
     *
     * @param inspectionPoints 巡检点数据列表
     */
    private void refreshUnits(List<ApiInspectionPointDataVo> inspectionPoints) {
        List<String> unitIds = new ArrayList<String>();
        List<ApiInspectionPointDataVo> tmpInspectionPoints = new ArrayList<>();
        
        // 遍历巡检点，筛选出位置类型为单元的记录
        for (ApiInspectionPointDataVo inspectionPointDataVo : inspectionPoints) {
            if ("2000".equals(inspectionPointDataVo.getLocationTypeCd())) {
                unitIds.add(inspectionPointDataVo.getLocationObjId());
                tmpInspectionPoints.add(inspectionPointDataVo);
            }
        }

        // 如果没有单元类型的巡检点，直接返回
        if (unitIds.size() < 1) {
            return;
        }
        
        String[] tmpUnitIds = unitIds.toArray(new String[unitIds.size()]);

        FloorAndUnitDto floorAndUnitDto = new FloorAndUnitDto();
        floorAndUnitDto.setUnitIds(tmpUnitIds);
        // 根据单元ID查询单元信息
        List<FloorAndUnitDto> unitDtos = unitInnerServiceSMOImpl.getFloorAndUnitInfo(floorAndUnitDto);

        // 设置单元名称和详细信息
        for (ApiInspectionPointDataVo inspectionPointDataVo : tmpInspectionPoints) {
            for (FloorAndUnitDto tmpUnitDto : unitDtos) {
                if (inspectionPointDataVo.getLocationObjId().equals(tmpUnitDto.getUnitId())) {
                    // 设置单元显示名称：X栋X单元
                    inspectionPointDataVo.setLocationObjName(tmpUnitDto.getFloorNum() + "栋" + tmpUnitDto.getUnitNum() + "单元");
                    // 复制单元详细信息到巡检点对象
                    BeanConvertUtil.covertBean(tmpUnitDto, inspectionPointDataVo);
                }
            }
        }
    }

    /**
     * 刷新房屋位置信息
     * 处理位置类型为房屋的巡检点，设置房屋名称
     *
     * @param inspectionPoints 巡检点数据列表
     */
    private void refreshRooms(List<ApiInspectionPointDataVo> inspectionPoints) {
        List<String> roomIds = new ArrayList<String>();
        List<ApiInspectionPointDataVo> tmpInspectionPoint = new ArrayList<>();
        
        // 遍历巡检点，筛选出位置类型为房屋的记录
        for (ApiInspectionPointDataVo inspectionPointDataVo : inspectionPoints) {
            if ("3000".equals(inspectionPointDataVo.getLocationTypeCd())) {
                roomIds.add(inspectionPointDataVo.getLocationObjId());
                tmpInspectionPoint.add(inspectionPointDataVo);
            }
        }
        
        // 如果没有房屋类型的巡检点，直接返回
        if (roomIds.size() < 1) {
            return;
        }
        
        String[] tmpRoomIds = roomIds.toArray(new String[roomIds.size()]);

        RoomDto roomDto = new RoomDto();
        roomDto.setRoomIds(tmpRoomIds);
        // 设置小区ID，用于精确查询
        roomDto.setCommunityId(inspectionPoints.get(0).getCommunityId());
        // 根据房屋ID查询房屋信息
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

        // 设置房屋名称和详细信息
        for (ApiInspectionPointDataVo inspectionPointDataVo : tmpInspectionPoint) {
            for (RoomDto tmpRoomDto : roomDtos) {
                if (inspectionPointDataVo.getLocationObjId().equals(tmpRoomDto.getRoomId())) {
                    // 设置房屋显示名称：X栋X单元X室
                    inspectionPointDataVo.setLocationObjName(tmpRoomDto.getFloorNum() + "栋" + tmpRoomDto.getUnitNum() + "单元" + tmpRoomDto.getRoomNum() + "室");
                    // 复制房屋详细信息到巡检点对象
                    BeanConvertUtil.covertBean(tmpRoomDto, inspectionPoints);
                }
            }
        }
    }
}