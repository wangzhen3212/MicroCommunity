package com.java110.store.smo.impl;


import com.java110.core.base.smo.BaseServiceSMO;
import com.java110.dto.PageDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.contract.ContractRoomDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.shop.BuildingShopDto;
import com.java110.intf.community.IBuildingShopInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.store.IContractRoomInnerServiceSMO;
import com.java110.po.contract.ContractRoomPo;
import com.java110.store.dao.IContractRoomServiceDao;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName FloorInnerServiceSMOImpl
 * @Description 合同房屋内部服务实现类
 * @Author wuxw
 * @Date 2019/4/24 9:20
 * @Version 1.0
 * add by wuxw 2019/4/24
 **/
@RestController
public class ContractRoomInnerServiceSMOImpl extends BaseServiceSMO implements IContractRoomInnerServiceSMO {

    @Autowired
    private IContractRoomServiceDao contractRoomServiceDaoImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IBuildingShopInnerServiceSMO buildingShopInnerServiceSMOImpl;



    @Override
    public int saveContractRoom(@RequestBody ContractRoomPo contractRoomPo) {
        int saveFlag = 1;
        contractRoomServiceDaoImpl.saveContractRoomInfo(BeanConvertUtil.beanCovertMap(contractRoomPo));
        return saveFlag;
    }

    @Override
    public int updateContractRoom(@RequestBody ContractRoomPo contractRoomPo) {
        int saveFlag = 1;
        contractRoomServiceDaoImpl.updateContractRoomInfo(BeanConvertUtil.beanCovertMap(contractRoomPo));
        return saveFlag;
    }

    @Override
    public int deleteContractRoom(@RequestBody ContractRoomPo contractRoomPo) {
        int saveFlag = 1;
        contractRoomPo.setStatusCd("1");
        contractRoomServiceDaoImpl.updateContractRoomInfo(BeanConvertUtil.beanCovertMap(contractRoomPo));
        return saveFlag;
    }

    @Override
    public List<ContractRoomDto> queryContractRooms(@RequestBody ContractRoomDto contractRoomDto) {

        //校验是否传了 分页信息

        int page = contractRoomDto.getPage();

        if (page != PageDto.DEFAULT_PAGE) {
            contractRoomDto.setPage((page - 1) * contractRoomDto.getRow());
        }

        List<ContractRoomDto> contractRooms = BeanConvertUtil.covertBeanList(contractRoomServiceDaoImpl.getContractRoomInfo(BeanConvertUtil.beanCovertMap(contractRoomDto)), ContractRoomDto.class);

        if(contractRooms != null && contractRooms.size() > 0){
            refreshContractRooms(contractRooms);
        }
        return contractRooms;
    }

    private void refreshContractRooms(List<ContractRoomDto> contractRooms) {
        List<String> roomIds = new ArrayList<>();
        List<String> shopIds = new ArrayList<>();

        for (ContractRoomDto contractRoomDto : contractRooms) {
            String payerObjType = StringUtil.isEmpty(contractRoomDto.getPayerObjType())
                    ? FeeDto.PAYER_OBJ_TYPE_ROOM : contractRoomDto.getPayerObjType();
            contractRoomDto.setPayerObjType(payerObjType);
            if (FeeDto.PAYER_OBJ_TYPE_SHOP.equals(payerObjType)) {
                if (!shopIds.contains(contractRoomDto.getRoomId())) {
                    shopIds.add(contractRoomDto.getRoomId());
                }
                continue;
            }
            if (!roomIds.contains(contractRoomDto.getRoomId())) {
                roomIds.add(contractRoomDto.getRoomId());
            }
        }

        refreshRoomInfo(contractRooms, roomIds);
        refreshShopInfo(contractRooms, shopIds);
    }

    private void refreshRoomInfo(List<ContractRoomDto> contractRooms, List<String> roomIds) {
        if (ListUtil.isNull(roomIds)) {
            return;
        }
        RoomDto roomDto = new RoomDto();
        roomDto.setRoomIds(roomIds.toArray(new String[roomIds.size()]));
        List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
        if (ListUtil.isNull(roomDtos)) {
            return;
        }
        for (ContractRoomDto contractRoomDto : contractRooms) {
            if (FeeDto.PAYER_OBJ_TYPE_SHOP.equals(contractRoomDto.getPayerObjType())) {
                continue;
            }
            for (RoomDto tmpRoomDto : roomDtos) {
                if (contractRoomDto.getRoomId().equals(tmpRoomDto.getRoomId())) {
                    BeanConvertUtil.covertBean(tmpRoomDto, contractRoomDto);
                }
            }
        }
    }

    private void refreshShopInfo(List<ContractRoomDto> contractRooms, List<String> shopIds) {
        if (ListUtil.isNull(shopIds)) {
            return;
        }
        for (String shopId : shopIds) {
            BuildingShopDto shopDto = new BuildingShopDto();
            shopDto.setShopId(shopId);
            List<BuildingShopDto> shopDtos = buildingShopInnerServiceSMOImpl.queryBuildingShops(shopDto);
            if (ListUtil.isNull(shopDtos)) {
                continue;
            }
            BuildingShopDto tmpShopDto = shopDtos.get(0);
            for (ContractRoomDto contractRoomDto : contractRooms) {
                if (!FeeDto.PAYER_OBJ_TYPE_SHOP.equals(contractRoomDto.getPayerObjType())
                        || !shopId.equals(contractRoomDto.getRoomId())) {
                    continue;
                }
                contractRoomDto.setCommunityId(tmpShopDto.getCommunityId());
                contractRoomDto.setRoomName(StringUtil.isEmpty(tmpShopDto.getShopNum())
                        ? tmpShopDto.getLocationName() : tmpShopDto.getShopNum());
                contractRoomDto.setBuiltUpArea(tmpShopDto.getBuiltUpArea());
                contractRoomDto.setRoomArea(tmpShopDto.getShopArea());
                contractRoomDto.setRoomRent(tmpShopDto.getShopRent());
                contractRoomDto.setState(tmpShopDto.getState());
            }
        }
    }


    @Override
    public int queryContractRoomsCount(@RequestBody ContractRoomDto contractRoomDto) {
        return contractRoomServiceDaoImpl.queryContractRoomsCount(BeanConvertUtil.beanCovertMap(contractRoomDto));
    }

    @Override
    public List<Map> queryContractByRoomIds(@RequestBody Map info) {
        return contractRoomServiceDaoImpl.queryContractByRoomIds(info);
    }

    public IContractRoomServiceDao getContractRoomServiceDaoImpl() {
        return contractRoomServiceDaoImpl;
    }

    public void setContractRoomServiceDaoImpl(IContractRoomServiceDao contractRoomServiceDaoImpl) {
        this.contractRoomServiceDaoImpl = contractRoomServiceDaoImpl;
    }
}
