package com.java110.store.bmo.contract.impl;

import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.contract.ContractRoomDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.room.RoomDto;
import com.java110.dto.shop.BuildingShopDto;
import com.java110.dto.shop.BuildingShopOwnerDto;
import com.java110.intf.community.IBuildingShopOwnerInnerServiceSMO;
import com.java110.intf.community.IBuildingShopV1InnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.intf.store.IContractRoomInnerServiceSMO;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.po.owner.OwnerRoomRelPo;
import com.java110.po.room.RoomPo;
import com.java110.po.shop.BuildingShopOwnerPo;
import com.java110.po.shop.BuildingShopPo;
import com.java110.store.bmo.contract.IBindOwnerRoomRelBMO;
import com.java110.utils.constant.StatusConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * 合同生效绑定业主与资产关系：房屋写 building_owner_room_rel，商铺写 building_shop_owner 并改商铺状态
 * 业务流水表（business_*）由框架维护，此处不做写入
 */
@Service("bindOwnerRoomRelBMOImpl")
public class BindOwnerRoomRelBMOImpl implements IBindOwnerRoomRelBMO {

    private static Logger logger = LoggerFactory.getLogger(BindOwnerRoomRelBMOImpl.class);

    private static final String REL_REMARK = "签订合同自动绑定";

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IContractRoomInnerServiceSMO contractRoomInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IBuildingShopOwnerInnerServiceSMO buildingShopOwnerInnerServiceSMOImpl;

    @Autowired
    private IBuildingShopV1InnerServiceSMO buildingShopV1InnerServiceSMOImpl;

    @Override
    public boolean bindOwnerRoomRel(String contractId, String storeId, String communityId) {
        List<ContractRoomDto> contractRoomDtos = queryContractRooms(contractId);
        if (ListUtil.isNull(contractRoomDtos)) {
            return false;
        }

        ContractDto contractDto = queryContract(contractId, storeId, communityId);
        String ownerId = contractDto.getObjId();
        if (StringUtil.isEmpty(ownerId) || ownerId.startsWith("-")) {
            logger.warn("合同[{}]未关联业主信息，跳过业主资产关系绑定", contractId);
            return false;
        }
        String ownerName = StringUtil.isEmpty(contractDto.getObjPersonName())
                ? contractDto.getPartyB() : contractDto.getObjPersonName();

        boolean bound = false;
        for (ContractRoomDto contractRoomDto : contractRoomDtos) {
            String payerObjType = StringUtil.isEmpty(contractRoomDto.getPayerObjType())
                    ? FeeDto.PAYER_OBJ_TYPE_ROOM : contractRoomDto.getPayerObjType();
            if (FeeDto.PAYER_OBJ_TYPE_SHOP.equals(payerObjType)) {
                bindShopOwner(contractDto, contractRoomDto, ownerId, ownerName);
                bound = true;
                continue;
            }
            if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(payerObjType)) {
                bindRoomOwner(contractDto, contractRoomDto, ownerId);
                bound = true;
            }
        }
        return bound;
    }

    /**
     * 查询合同（不存在的合同直接报错，避免绑定到脏数据上）
     */
    private ContractDto queryContract(String contractId, String storeId, String communityId) {
        ContractDto contractDto = new ContractDto();
        contractDto.setContractId(contractId);
        contractDto.setStoreId(storeId);
        contractDto.setCommunityId(communityId);
        contractDto.setStatusCd(StatusConstant.STATUS_CD_VALID);
        List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);
        Assert.listOnlyOne(contractDtos, "合同不存在");
        return contractDtos.get(0);
    }

    private List<ContractRoomDto> queryContractRooms(String contractId) {
        ContractRoomDto contractRoomDto = new ContractRoomDto();
        contractRoomDto.setContractId(contractId);
        contractRoomDto.setStatusCd(StatusConstant.STATUS_CD_VALID);
        return contractRoomInnerServiceSMOImpl.queryContractRooms(contractRoomDto);
    }

    /**
     * 房屋：同业主跳过（幂等），业主不同先逻辑删除老关系再写新关系，最后置房屋为已入住
     */
    private void bindRoomOwner(ContractDto contractDto, ContractRoomDto contractRoomDto, String ownerId) {
        String roomId = contractRoomDto.getRoomId();
        OwnerRoomRelDto relQueryDto = new OwnerRoomRelDto();
        relQueryDto.setRoomId(roomId);
        List<OwnerRoomRelDto> oldOwnerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(relQueryDto);

        if (!ListUtil.isNull(oldOwnerRoomRelDtos) && ownerId.equals(oldOwnerRoomRelDtos.get(0).getOwnerId())) {
            updateRoomState(roomId);
            return;
        }

        if (!ListUtil.isNull(oldOwnerRoomRelDtos)) {
            OwnerRoomRelPo deletePo = new OwnerRoomRelPo();
            deletePo.setRelId(oldOwnerRoomRelDtos.get(0).getRelId());
            deletePo.setStatusCd(StatusConstant.STATUS_CD_INVALID);
            ownerRoomRelInnerServiceSMOImpl.updateOwnerRoomRels(deletePo);
        }

        OwnerRoomRelPo ownerRoomRelPo = new OwnerRoomRelPo();
        ownerRoomRelPo.setRelId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_relId));
        ownerRoomRelPo.setOwnerId(ownerId);
        ownerRoomRelPo.setRoomId(roomId);
        ownerRoomRelPo.setStartTime(contractDto.getStartTime());
        ownerRoomRelPo.setEndTime(contractDto.getEndTime());
        ownerRoomRelPo.setState(RoomDto.STATE_SELL);
        ownerRoomRelPo.setUserId("-1");
        ownerRoomRelPo.setRemark(REL_REMARK);
        int flag = ownerRoomRelInnerServiceSMOImpl.saveOwnerRoomRels(ownerRoomRelPo);
        if (flag < 1) {
            throw new CmdException("保存房屋业主关系失败");
        }

        updateRoomState(roomId);
    }

    /**
     * 房屋状态置为已入住
     */
    private void updateRoomState(String roomId) {
        RoomPo roomPo = new RoomPo();
        roomPo.setRoomId(roomId);
        roomPo.setState(RoomDto.STATE_SELL);
        roomPo.setStatusCd(StatusConstant.STATUS_CD_VALID);
        roomInnerServiceSMOImpl.updateRooms(roomPo);
    }

    /**
     * 商铺：同业主跳过关系写入（幂等），业主不同先逻辑删除老关系再写新关系，最后置商铺为已出租
     */
    private void bindShopOwner(ContractDto contractDto, ContractRoomDto contractRoomDto, String ownerId, String ownerName) {
        String shopId = contractRoomDto.getRoomId();
        String shopCommunityId = StringUtil.isEmpty(contractRoomDto.getCommunityId())
                ? contractDto.getCommunityId() : contractRoomDto.getCommunityId();

        BuildingShopOwnerDto shopOwnerQueryDto = new BuildingShopOwnerDto();
        shopOwnerQueryDto.setShopId(shopId);
        shopOwnerQueryDto.setCommunityId(shopCommunityId);
        shopOwnerQueryDto.setStatusCd(StatusConstant.STATUS_CD_VALID);
        List<BuildingShopOwnerDto> shopOwnerDtos = buildingShopOwnerInnerServiceSMOImpl.queryBuildingShopOwners(shopOwnerQueryDto);

        boolean sameOwner = !ListUtil.isNull(shopOwnerDtos) && ownerId.equals(shopOwnerDtos.get(0).getOwnerId());
        if (!sameOwner) {
            if (!ListUtil.isNull(shopOwnerDtos)) {
                BuildingShopOwnerPo deletePo = new BuildingShopOwnerPo();
                deletePo.setShopId(shopId);
                deletePo.setCommunityId(shopCommunityId);
                buildingShopOwnerInnerServiceSMOImpl.deleteBuildingShopOwner(deletePo);
            }

            BuildingShopOwnerPo shopOwnerPo = new BuildingShopOwnerPo();
            shopOwnerPo.setShopOwnerId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_shopId));
            shopOwnerPo.setShopId(shopId);
            shopOwnerPo.setOwnerId(ownerId);
            shopOwnerPo.setOwnerName(ownerName);
            shopOwnerPo.setLink(contractDto.getbLink());
            shopOwnerPo.setState(BuildingShopOwnerDto.STATE_RENT);
            shopOwnerPo.setStartTime(parseDate(contractDto.getStartTime()));
            shopOwnerPo.setEndTime(parseDate(contractDto.getEndTime()));
            shopOwnerPo.setCommunityId(shopCommunityId);
            int flag = buildingShopOwnerInnerServiceSMOImpl.saveBuildingShopOwner(shopOwnerPo);
            if (flag < 1) {
                throw new CmdException("保存商铺业主关系失败");
            }
        }

        BuildingShopPo shopPo = new BuildingShopPo();
        shopPo.setShopId(shopId);
        shopPo.setState(BuildingShopDto.STATE_RENT);
        shopPo.setStatusCd(StatusConstant.STATUS_CD_VALID);
        int updateFlag = buildingShopV1InnerServiceSMOImpl.updateBuildingShop(shopPo);
        if (updateFlag < 1) {
            throw new CmdException("更新商铺状态失败");
        }
    }

    /**
     * 合同租期为字符串，兼容 yyyy-MM-dd HH:mm:ss 与 yyyy-MM-dd 两种格式
     */
    private Date parseDate(String date) {
        if (StringUtil.isEmpty(date)) {
            return null;
        }
        try {
            return DateUtil.getDateFromStringAdaptTwoPattern(date);
        } catch (ParseException e) {
            logger.warn("合同租期[{}]格式不正确，业主关系起止时间置空", date);
            return null;
        }
    }
}
