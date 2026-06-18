/**
 * 业主车辆查询命令类
 * 
 * 该类用于处理管理员查询业主车辆信息的请求，支持分页查询，
 * 并提供车辆关联的房屋信息、车位信息和物联网设备状态的补充查询功能
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.machine.MachineTranslateDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.dto.room.RoomDto;
import com.java110.intf.common.IMachineTranslateV1InnerServiceSMO;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.intf.user.IStaffCommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "owner.queryAdminOwnerCars")
public class QueryAdminOwnerCarsCmd extends Cmd {

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IMachineTranslateV1InnerServiceSMO machineTranslateV1InnerServiceSMOImpl;

    @Autowired
    private IStaffCommunityV1InnerServiceSMO staffCommunityV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证管理员权限和分页参数
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
        // 验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行查询业主车辆命令
     * 
     * 根据查询条件获取业主车辆列表，支持分页查询，并补充关联的房屋信息、车位信息和物联网状态
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求参数转换为OwnerCarDto对象
        OwnerCarDto ownerCarDto = BeanConvertUtil.covertBean(reqJson, OwnerCarDto.class);

        // 获取当前登录用户ID
        String staffId = CmdContextUtils.getUserId(cmdDataFlowContext);

        // 查询用户管理的社区ID列表
        List<String> communityIds = staffCommunityV1InnerServiceSMOImpl.queryStaffCommunityIds(staffId);

        // 如果用户有管理的社区，设置查询条件中的社区ID
        if (!ListUtil.isNull(communityIds)) {
            ownerCarDto.setCommunityIds(communityIds.toArray(new String[communityIds.size()]));
        }
        
        // 获取每页显示行数
        int row = reqJson.getIntValue("row");
        
        // 查询总记录数
        int total = ownerCarInnerServiceSMOImpl.queryOwnerCarsCount(ownerCarDto);
        
        List<OwnerCarDto> ownerCarDtoList = null;
        
        // 如果有数据，查询车辆列表
        if (total > 0) {
            ownerCarDtoList = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);
            
            // 当数据量小于20条时，补充关联信息（避免大数据量时性能问题）
            if (row < 20) {
                // 补充车位信息
                freshPs(ownerCarDtoList);
                // 补充房屋信息
                freshRoomInfo(ownerCarDtoList);
                // 补充物联网设备状态
                freshTransactionIotState(ownerCarDtoList);
            }
        } else {
            // 没有数据时返回空列表
            ownerCarDtoList = new ArrayList<>();
        }

        // 计算总页数并构建响应结果
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity((int) Math.ceil((double) total / (double) row), total, ownerCarDtoList);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新物联网设备状态信息
     * 
     * 根据车辆关联的设备ID查询物联网设备的状态信息，并更新到车辆信息中
     * 
     * @param ownerCarDtoList 业主车辆列表
     */
    private void freshTransactionIotState(List<OwnerCarDto> ownerCarDtoList) {
        // 参数校验
        if (ownerCarDtoList == null || ownerCarDtoList.size() < 1) {
            return;
        }
        
        // 收集所有有车位ID的车辆成员ID
        List<String> memberIds = new ArrayList<>();
        for (OwnerCarDto ownerCarDto : ownerCarDtoList) {
            if (StringUtil.isEmpty(ownerCarDto.getPsId())) {
                continue;
            }
            memberIds.add(ownerCarDto.getMemberId());
        }

        // 构建查询条件，查询物联网设备状态
        MachineTranslateDto machineTranslateDto = new MachineTranslateDto();
        machineTranslateDto.setObjIds(memberIds.toArray(new String[memberIds.size()]));
        List<MachineTranslateDto> machineTranslateDtos = machineTranslateV1InnerServiceSMOImpl.queryObjStateInMachineTranslates(machineTranslateDto);

        // 如果没有物联网设备状态数据，直接返回
        if (ListUtil.isNull(machineTranslateDtos)) {
            return;
        }

        // 将物联网设备状态信息更新到对应的车辆信息中
        for (OwnerCarDto ownerCarDto : ownerCarDtoList) {
            for (MachineTranslateDto tmpMachineTranslateDto : machineTranslateDtos) {
                if (!ownerCarDto.getMemberId().equals(tmpMachineTranslateDto.getObjId())) {
                    continue;
                }
                // 设置物联网状态名称和备注
                ownerCarDto.setIotStateName(tmpMachineTranslateDto.getStateName());
                ownerCarDto.setIotRemark(tmpMachineTranslateDto.getRemark());
            }
        }
    }

    /**
     * 刷新车位信息
     * 
     * 根据车位ID查询车位详细信息，并更新到车辆信息中
     * 
     * @param ownerCarDtoList 业主车辆列表
     */
    private void freshPs(List<OwnerCarDto> ownerCarDtoList) {
        // 参数校验
        if (ownerCarDtoList == null || ownerCarDtoList.size() < 1) {
            return;
        }
        
        // 收集所有有车位ID的车辆
        List<String> psIds = new ArrayList<>();
        for (OwnerCarDto ownerCarDto : ownerCarDtoList) {
            if (StringUtil.isEmpty(ownerCarDto.getPsId())) {
                continue;
            }
            psIds.add(ownerCarDto.getPsId());
        }
        
        // 构建车位查询条件
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setCommunityId(ownerCarDtoList.get(0).getCommunityId());
        parkingSpaceDto.setPsIds(psIds.toArray(new String[psIds.size()]));
        
        // 查询车位详细信息
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceInnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
        
        // 将车位信息更新到对应的车辆信息中
        for (ParkingSpaceDto tmpParkingSpaceDto : parkingSpaceDtos) {
            for (OwnerCarDto ownerCarDto : ownerCarDtoList) {
                if (tmpParkingSpaceDto.getPsId().equals(ownerCarDto.getPsId())) {
                    // 设置区域编号、车位编号和停车类型
                    ownerCarDto.setAreaNum(tmpParkingSpaceDto.getAreaNum());
                    ownerCarDto.setNum(tmpParkingSpaceDto.getNum());
                    ownerCarDto.setParkingType(tmpParkingSpaceDto.getParkingType());
                }
            }
        }
    }

    /**
     * 刷新房屋信息
     * 
     * 为每个车辆信息补充关联的房屋信息
     * 
     * @param ownerCarDtos 业主车辆列表
     */
    private void freshRoomInfo(List<OwnerCarDto> ownerCarDtos) {
        // 遍历每个车辆，刷新房屋信息
        for (OwnerCarDto ownerCarDto : ownerCarDtos) {
            doFreshRoomInfo(ownerCarDto);
        }
    }

    /**
     * 为单个车辆信息刷新房屋信息
     * 
     * 根据业主ID查询关联的房屋信息，并将房屋信息格式化后设置到车辆信息中
     * 
     * @param ownerCarDto 业主车辆信息
     */
    private void doFreshRoomInfo(OwnerCarDto ownerCarDto) {
        // 构建业主房屋关系查询条件
        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setOwnerId(ownerCarDto.getOwnerId());
        ownerRoomRelDto.setPage(1);
        // 限制最多显示3个房屋，避免页面显示混乱
        ownerRoomRelDto.setRow(3);
        
        // 查询业主房屋关系
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
        
        // 如果没有关联房屋，设置默认值
        if (ListUtil.isNull(ownerRoomRelDtos)) {
            ownerCarDto.setRoomName("-");
            return;
        }
        
        // 收集所有房屋ID
        List<String> roomIds = new ArrayList<>();
        for (OwnerRoomRelDto tOwnerRoomRelDto : ownerRoomRelDtos) {
            roomIds.add(tOwnerRoomRelDto.getRoomId());
        }
        
        // 构建房屋查询条件
        RoomDto roomDto = new RoomDto();
        roomDto.setCommunityId(ownerCarDto.getCommunityId());
        roomDto.setRoomIds(roomIds.toArray(new String[roomIds.size()]));
        
        // 查询房屋详细信息
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
        
        // 格式化房屋名称：楼层-单元-房号
        String roomName = "";
        for (RoomDto tRoomDto : roomDtos) {
            roomName += (tRoomDto.getFloorNum() + "-" + tRoomDto.getUnitNum() + "-" + tRoomDto.getRoomNum() + "-" + "/");
        }
        
        // 去除末尾多余的斜杠
        roomName = roomName.endsWith("/") ? roomName.substring(0, roomName.length() - 1) : roomName;
        
        // 设置格式化后的房屋名称
        ownerCarDto.setRoomName(roomName);
    }
}