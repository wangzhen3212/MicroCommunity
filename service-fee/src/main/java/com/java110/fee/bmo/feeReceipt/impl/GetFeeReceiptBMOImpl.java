package com.java110.fee.bmo.feeReceipt.impl;

import com.java110.dto.room.RoomDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.FeeReceiptDto;
import com.java110.dto.fee.FeeReceiptDtoNew;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.parking.ParkingSpaceDto;
import com.java110.fee.bmo.feeReceipt.IGetFeeReceiptBMO;
import com.java110.intf.community.IParkingSpaceV1InnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeReceiptInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelV1InnerServiceSMO;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("getFeeReceiptBMOImpl")
public class GetFeeReceiptBMOImpl implements IGetFeeReceiptBMO {

    @Autowired
    private IFeeReceiptInnerServiceSMO feeReceiptInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceV1InnerServiceSMO parkingSpaceV1InnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelV1InnerServiceSMO ownerRoomRelV1InnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;


    /**
     * @param feeReceiptDto
     * @return 订单服务能够接受的报文
     */
    public ResponseEntity<String> get(FeeReceiptDto feeReceiptDto) {


        int count = feeReceiptInnerServiceSMOImpl.queryFeeReceiptsCount(feeReceiptDto);

        List<FeeReceiptDto> feeReceiptDtos = null;
        if (count > 0) {
            feeReceiptDtos = feeReceiptInnerServiceSMOImpl.queryFeeReceipts(feeReceiptDto);
            for (FeeReceiptDto feeReceipt : feeReceiptDtos) {
                feeReceipt.setStoreName(feeReceiptDto.getStoreName());
            }
            //输入房屋信息
            freshRoomInfo(feeReceiptDtos);
        } else {
            feeReceiptDtos = new ArrayList<>();
        }



        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) feeReceiptDto.getRow()), count, feeReceiptDtos);

        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        return responseEntity;
    }


    /**
     * 刷入房屋信息
     *
     * @param feeReceiptDtos
     */
    private void freshRoomInfo(List<FeeReceiptDto> feeReceiptDtos) {

        List<String> psIds = new ArrayList<>();
        for (FeeReceiptDto feeReceiptDto : feeReceiptDtos) {
            if (!FeeDto.PAYER_OBJ_TYPE_PARKING_SPACE.equals(feeReceiptDto.getObjType())) {
                feeReceiptDto.setRoomName(feeReceiptDto.getObjName());
                feeReceiptDto.setCarNum("-");
                feeReceiptDto.setPsNum("-");
                continue;
            }
            feeReceiptDto.setPsNum("-");
            feeReceiptDto.setCarNum("-");
            feeReceiptDto.setRoomName("-");
            if (!StringUtil.isEmpty(feeReceiptDto.getObjId()) && !psIds.contains(feeReceiptDto.getObjId())) {
                psIds.add(feeReceiptDto.getObjId());
            }
        }

        if (ListUtil.isNull(psIds)) {
            return;
        }

        Map<String, ParkingSpaceDto> parkingSpaceDtoMap = queryParkingSpaceDtoMap(psIds);
        Map<String, String> ownerRoomNameMap = queryOwnerRoomNameMap(parkingSpaceDtoMap);

        for (FeeReceiptDto feeReceiptDto : feeReceiptDtos) {
            if (!FeeDto.PAYER_OBJ_TYPE_PARKING_SPACE.equals(feeReceiptDto.getObjType())) {
                continue;
            }
            ParkingSpaceDto parkingSpaceDto = parkingSpaceDtoMap.get(feeReceiptDto.getObjId());
            if (parkingSpaceDto == null) {
                continue;
            }
            feeReceiptDto.setPsId(parkingSpaceDto.getPsId());
            feeReceiptDto.setPsNum(getParkingSpaceNum(parkingSpaceDto));
            if (!StringUtil.isEmpty(parkingSpaceDto.getCarNum())) {
                feeReceiptDto.setCarNum(parkingSpaceDto.getCarNum());
            }
            String roomName = ownerRoomNameMap.get(parkingSpaceDto.getOwnerId());
            if (!StringUtil.isEmpty(roomName)) {
                feeReceiptDto.setRoomName(roomName);
            }
        }
    }

    /**
     * 查询车位信息
     *
     * @param psIds 车位ID集合
     * @return 车位ID 与 车位信息 映射
     */
    private Map<String, ParkingSpaceDto> queryParkingSpaceDtoMap(List<String> psIds) {
        Map<String, ParkingSpaceDto> parkingSpaceDtoMap = new HashMap<>();
        ParkingSpaceDto parkingSpaceDto = new ParkingSpaceDto();
        parkingSpaceDto.setPsIds(psIds.toArray(new String[psIds.size()]));
        List<ParkingSpaceDto> parkingSpaceDtos = parkingSpaceV1InnerServiceSMOImpl.queryParkingSpaces(parkingSpaceDto);
        if (ListUtil.isNull(parkingSpaceDtos)) {
            return parkingSpaceDtoMap;
        }
        for (ParkingSpaceDto tParkingSpaceDto : parkingSpaceDtos) {
            parkingSpaceDtoMap.put(tParkingSpaceDto.getPsId(), tParkingSpaceDto);
        }
        return parkingSpaceDtoMap;
    }

    /**
     * 查询车位所属业主房号
     *
     * @param parkingSpaceDtoMap 车位信息映射
     * @return 业主ID 与 房号 映射
     */
    private Map<String, String> queryOwnerRoomNameMap(Map<String, ParkingSpaceDto> parkingSpaceDtoMap) {
        Map<String, String> ownerRoomNameMap = new HashMap<>();
        Map<String, List<String>> ownerRoomIdsMap = new HashMap<>();
        List<String> ownerIds = new ArrayList<>();
        List<String> roomIds = new ArrayList<>();

        for (ParkingSpaceDto parkingSpaceDto : parkingSpaceDtoMap.values()) {
            if (StringUtil.isEmpty(parkingSpaceDto.getOwnerId())) {
                continue;
            }
            if (!ownerIds.contains(parkingSpaceDto.getOwnerId())) {
                ownerIds.add(parkingSpaceDto.getOwnerId());
            }
        }

        if (ListUtil.isNull(ownerIds)) {
            return ownerRoomNameMap;
        }

        OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
        ownerRoomRelDto.setOwnerIds(ownerIds.toArray(new String[ownerIds.size()]));
        List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelV1InnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
        if (ListUtil.isNull(ownerRoomRelDtos)) {
            return ownerRoomNameMap;
        }

        for (OwnerRoomRelDto tOwnerRoomRelDto : ownerRoomRelDtos) {
            if (StringUtil.isEmpty(tOwnerRoomRelDto.getRoomId())) {
                continue;
            }
            List<String> ownerRoomIds = ownerRoomIdsMap.get(tOwnerRoomRelDto.getOwnerId());
            if (ownerRoomIds == null) {
                ownerRoomIds = new ArrayList<>();
                ownerRoomIdsMap.put(tOwnerRoomRelDto.getOwnerId(), ownerRoomIds);
            }
            if (ownerRoomIds.contains(tOwnerRoomRelDto.getRoomId())) {
                continue;
            }
            ownerRoomIds.add(tOwnerRoomRelDto.getRoomId());
            if (!roomIds.contains(tOwnerRoomRelDto.getRoomId())) {
                roomIds.add(tOwnerRoomRelDto.getRoomId());
            }
        }

        if (ListUtil.isNull(roomIds)) {
            return ownerRoomNameMap;
        }

        Map<String, RoomDto> roomDtoMap = new HashMap<>();
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomIds(roomIds.toArray(new String[roomIds.size()]));
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
        if (ListUtil.isNull(roomDtos)) {
            return ownerRoomNameMap;
        }
        for (RoomDto tRoomDto : roomDtos) {
            roomDtoMap.put(tRoomDto.getRoomId(), tRoomDto);
        }

        for (Map.Entry<String, List<String>> ownerRoomIdsEntry : ownerRoomIdsMap.entrySet()) {
            String roomName = "";
            for (String roomId : ownerRoomIdsEntry.getValue()) {
                RoomDto tRoomDto = roomDtoMap.get(roomId);
                if (tRoomDto == null) {
                    continue;
                }
                roomName += (tRoomDto.getFloorNum() + "栋" + tRoomDto.getUnitNum() + "单元" + tRoomDto.getRoomNum() + "室" + "/");
            }
            roomName = roomName.endsWith("/") ? roomName.substring(0, roomName.length() - 1) : roomName;
            if (StringUtil.isEmpty(roomName)) {
                continue;
            }
            ownerRoomNameMap.put(ownerRoomIdsEntry.getKey(), roomName);
        }
        return ownerRoomNameMap;
    }


    /**
     * 拼接车位名称，与建费时的缴费对象名称规则保持一致
     *
     * @param parkingSpaceDto 车位信息
     * @return 停车场-区段-车位号
     */
    private String getParkingSpaceNum(ParkingSpaceDto parkingSpaceDto) {
        String num = parkingSpaceDto.getNum();
        if (StringUtil.isEmpty(num)) {
            return "-";
        }
        if (StringUtil.isEmpty(parkingSpaceDto.getAreaNum())) {
            return num;
        }
        if (StringUtil.isEmpty(parkingSpaceDto.getSectionName())) {
            return parkingSpaceDto.getAreaNum() + "-" + num;
        }
        return parkingSpaceDto.getAreaNum() + "-" + parkingSpaceDto.getSectionName() + "-" + num;
    }



    @Override
    public ResponseEntity<String> gets(FeeReceiptDtoNew feeReceiptDtonew) {
        FeeReceiptDto feeReceiptDto = new FeeReceiptDto();
        feeReceiptDto.setPage(feeReceiptDtonew.getPage());
        feeReceiptDto.setRow(feeReceiptDtonew.getRow());
        feeReceiptDto.setCommunityId(feeReceiptDtonew.getCommunityId());
        feeReceiptDto.setReceiptId(feeReceiptDtonew.getReceiptId());
        feeReceiptDto.setObjType(feeReceiptDto.getObjType());
        feeReceiptDto.setObjName(feeReceiptDto.getObjName());
        int count = feeReceiptInnerServiceSMOImpl.queryFeeReceiptsCount(feeReceiptDto);

        List<FeeReceiptDtoNew> feeReceiptDtos = null;
        if (count > 0) {
            feeReceiptDtos = feeReceiptInnerServiceSMOImpl.queryFeeReceiptsNew(feeReceiptDtonew);
        } else {
            feeReceiptDtos = new ArrayList<>();
        }

        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) feeReceiptDto.getRow()), count, feeReceiptDtos);

        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        return responseEntity;
    }
}
