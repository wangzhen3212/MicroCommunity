package com.java110.store.bmo.contract.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.room.RoomDto;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.contract.ContractCollectionPlanDto;
import com.java110.dto.fee.FeeDto;

import com.java110.dto.owner.OwnerRoomRelDto;
import com.java110.dto.renting.RentingPoolDto;
import com.java110.dto.store.StoreDto;
import com.java110.intf.common.IContractApplyUserInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.store.*;
import com.java110.intf.user.IOwnerRoomRelInnerServiceSMO;
import com.java110.intf.user.IRentingPoolInnerServiceSMO;
import com.java110.po.contract.ContractPo;
import com.java110.po.contract.ContractAttrPo;
import com.java110.po.contract.ContractCollectionPlanPo;
import com.java110.po.contract.ContractFilePo;
import com.java110.po.contract.ContractRoomPo;
import com.java110.po.contract.ContractTypeTemplatePo;
import com.java110.po.renting.RentingPoolPo;
import com.java110.store.bmo.contract.IContractContentBMO;
import com.java110.store.bmo.contract.ISaveContractBMO;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ContractContentUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("saveContractBMOImpl")
public class SaveContractBMOImpl implements ISaveContractBMO {

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IContractAttrInnerServiceSMO contractAttrInnerServiceSMOImpl;

    @Autowired
    private IRentingPoolInnerServiceSMO rentingPoolInnerServiceSMOImpl; // 房屋租赁


    @Autowired
    private IContractApplyUserInnerServiceSMO contractApplyUserInnerServiceSMOImpl;

    @Autowired
    private IContractFileInnerServiceSMO contractFileInnerServiceSMOImpl;

    @Autowired
    private IContractRoomInnerServiceSMO contractRoomInnerServiceSMOImpl;

    @Autowired
    private IOwnerRoomRelInnerServiceSMO ownerRoomRelInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IContractContentBMO contractContentBMOImpl;

    @Autowired
    private IContractCollectionPlanInnerServiceSMO contractCollectionPlanInnerServiceSMOImpl;

    @Autowired
    private IContractTypeTemplateInnerServiceSMO contractTypeTemplateInnerServiceSMOImpl;

    private static final String PREFIX_PLAN_ID = "95";


    /**
     * 添加小区信息
     *
     * @param contractPo
     * @return 订单服务能够接受的报文
     */
    @Java110Transactional
    public ResponseEntity<String> save(ContractPo contractPo, JSONObject reqJson) {

        // 兼容旧前端/旧调用方：只传了业主人员ID时，回填合同对象ID
        if ((StringUtil.isEmpty(contractPo.getObjId()) || contractPo.getObjId().startsWith("-"))
                && !StringUtil.isEmpty(contractPo.getObjPersonId())) {
            contractPo.setObjId(contractPo.getObjPersonId());
        }

        validateRoom(contractPo, reqJson);

        boolean draft = ContractDto.STATE_DRAFT.equals(reqJson.getString("state"));

        if (draft) {
            contractPo.setState(ContractDto.STATE_DRAFT);
        } else {
            contractPo.setState(ContractDto.STATE_AUDIT);
        }

        //校验合同编号是否重复
        ContractDto contractDto = new ContractDto();
        contractDto.setStoreId(contractPo.getStoreId());
        contractDto.setContractCode(contractPo.getContractCode());
        List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);

        if (contractDtos != null && contractDtos.size() > 0) {
            throw new IllegalArgumentException("合同" + "[" + contractPo.getContractCode() + "]已存在");
        }

        contractPo.setContractId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_contractId));
        //附件保存
        List<ContractFilePo> filePos = contractPo.getContractFilePo();
        int flag = contractInnerServiceSMOImpl.saveContract(contractPo);
        if (filePos != null) {
            for (ContractFilePo file : filePos) {
                if (!StringUtil.isEmpty(file.getFileRealName()) && !StringUtil.isEmpty(file.getFileSaveName())) {
                    file.setContractId(contractPo.getContractId());
                    file.setCommunityId(contractPo.getCommunityId());
                    contractFileInnerServiceSMOImpl.saveContractFile(file);
                }
            }
        }

        if (flag < 0) {
            return ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "保存失败");
        }

        saveContractRooms(reqJson, contractPo);

        // 正文与收款计划（同事务写入，正文独立表）
        saveContractContent(reqJson, contractPo);

        saveContractCollectionPlans(reqJson, contractPo);

        if (!draft && StoreDto.STORE_ADMIN.equals(contractPo.getStoreId())) {
            noticeRentUpdateState(contractPo);
        }



        if (!reqJson.containsKey("contractTypeSpecs")) {
            return ResultVo.createResponseEntity(ResultVo.CODE_OK, "保存成功");
        }

        JSONArray contractTypeSpecs = reqJson.getJSONArray("contractTypeSpecs");

        if (contractTypeSpecs.size() < 1) {
            return ResultVo.createResponseEntity(ResultVo.CODE_OK, "保存成功");
        }

        for (int typeSpecIndex = 0; typeSpecIndex < contractTypeSpecs.size(); typeSpecIndex++) {
            saveContractAttr(contractTypeSpecs.getJSONObject(typeSpecIndex), contractPo);
        }


        return ResultVo.createResponseEntity(ResultVo.CODE_OK, "保存成功");

    }

    /**
     * 保存合同正文（渲染后正文与含变量原文双列写入，同时累加模板使用次数）
     */
    private void saveContractContent(JSONObject reqJson, ContractPo contractPo) {
        String content = reqJson.getString("content");
        String templateContent = reqJson.getString("templateContent");
        if (StringUtil.isEmpty(content) && StringUtil.isEmpty(templateContent)) {
            return;
        }
        if (StringUtil.isEmpty(templateContent)) {
            templateContent = content;
        }
        String renderedContent = ContractContentUtil.render(templateContent, reqJson.getJSONObject("variables"));
        contractContentBMOImpl.saveOrUpdateContent(contractPo.getContractId(), contractPo.getCommunityId(),
                contractPo.getTemplateId(), renderedContent, templateContent);
        if (StringUtil.isEmpty(contractPo.getTemplateId())) {
            return;
        }
        ContractTypeTemplatePo templatePo = new ContractTypeTemplatePo();
        templatePo.setTemplateId(contractPo.getTemplateId());
        templatePo.setStoreId(contractPo.getStoreId());
        templatePo.setCommunityId(contractPo.getCommunityId());
        templatePo.setStatusCd("0");
        contractTypeTemplateInnerServiceSMOImpl.addContractTypeTemplateUseCount(templatePo);
    }

    /**
     * 保存收款计划期次（履约节点）
     */
    private void saveContractCollectionPlans(JSONObject reqJson, ContractPo contractPo) {
        if (!reqJson.containsKey("plans")) {
            return;
        }
        JSONArray plans = reqJson.getJSONArray("plans");
        if (plans == null || plans.isEmpty()) {
            return;
        }
        for (int planIndex = 0; planIndex < plans.size(); planIndex++) {
            JSONObject plan = plans.getJSONObject(planIndex);
            ContractCollectionPlanPo planPo = BeanConvertUtil.covertBean(plan, ContractCollectionPlanPo.class);
            planPo.setPlanId(GenerateCodeFactory.getGeneratorId(PREFIX_PLAN_ID));
            planPo.setContractId(contractPo.getContractId());
            planPo.setStoreId(contractPo.getStoreId());
            planPo.setCommunityId(contractPo.getCommunityId());
            if (StringUtil.isEmpty(planPo.getPlanSeq())) {
                planPo.setPlanSeq(String.valueOf(planIndex + 1));
            }
            if (StringUtil.isEmpty(planPo.getPayState())) {
                planPo.setPayState(ContractCollectionPlanDto.PAY_STATE_DOING);
            }
            contractCollectionPlanInnerServiceSMOImpl.saveContractCollectionPlan(planPo);
        }
    }

    /**
     * 房屋是否欠费校验
     *
     * @param contractPo
     * @param reqJson
     */
    private void validateRoom(ContractPo contractPo, JSONObject reqJson) {
        //校验 房屋上是否有费用存在
        if (!reqJson.containsKey("rooms")) {
            return;
        }
        JSONArray rooms = reqJson.getJSONArray("rooms");
        for (int conFileIndex = 0; conFileIndex < rooms.size(); conFileIndex++) {
            JSONObject roomObj = rooms.getJSONObject(conFileIndex);

            //判断房屋是否存在
            RoomDto roomDto = new RoomDto();
            roomDto.setRoomId(roomObj.getString("roomId"));
            roomDto.setCommunityId(reqJson.getString("communityId"));
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

            Assert.listOnlyOne(roomDtos, "房屋不存在");

            OwnerRoomRelDto ownerRoomRelDto = new OwnerRoomRelDto();
            ownerRoomRelDto.setRoomId(roomObj.getString("roomId"));
            List<OwnerRoomRelDto> ownerRoomRelDtos = ownerRoomRelInnerServiceSMOImpl.queryOwnerRoomRels(ownerRoomRelDto);
            //不存在关系
            if (ownerRoomRelDtos == null || ownerRoomRelDtos.size() == 0) { // 说明业主没有发生变化，后续工作不做处理
                continue;
            }
            //存在关系 并且是他自己
            if (!StringUtil.isEmpty(contractPo.getObjId())
                    && contractPo.getObjId().equals(ownerRoomRelDtos.get(0).getOwnerId())) {
                continue;
            }

            //查询房屋时候有欠费
            FeeDto feeDto = new FeeDto();
            feeDto.setPayerObjType(FeeDto.PAYER_OBJ_TYPE_ROOM);
            feeDto.setPayerObjId(roomObj.getString("roomId"));
            feeDto.setState(FeeDto.STATE_DOING);
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
            if (feeDtos != null && feeDtos.size() > 0) {
                throw new IllegalArgumentException(roomDtos.get(0).getRoomNum() + "房屋存在未结束的费用 请先处理");
            }
        }
    }

    /**
     * 保存合同关联房屋/商铺（只落 contract_room，业主冗余取自合同主体，不触碰资产产权关系）
     */
    private void saveContractRooms(JSONObject reqJson, ContractPo contractPo) {

        if (!reqJson.containsKey("rooms")) {
            return;
        }
        String ownerName = StringUtil.isEmpty(contractPo.getObjPersonName())
                ? contractPo.getPartyB() : contractPo.getObjPersonName();
        JSONArray rooms = reqJson.getJSONArray("rooms");
        for (int conFileIndex = 0; conFileIndex < rooms.size(); conFileIndex++) {
            JSONObject resourceStore = rooms.getJSONObject(conFileIndex);
            ContractRoomPo contractRoomPo = BeanConvertUtil.covertBean(resourceStore, ContractRoomPo.class);
            contractRoomPo.setCrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_crId));
            contractRoomPo.setContractId(contractPo.getContractId());
            contractRoomPo.setStoreId(contractPo.getStoreId());
            contractRoomPo.setCommunityId(contractPo.getCommunityId());
            contractRoomPo.setOwnerId(contractPo.getObjId());
            contractRoomPo.setOwnerName(ownerName);
            String payerObjType = StringUtil.isEmpty(contractRoomPo.getPayerObjType())
                    ? FeeDto.PAYER_OBJ_TYPE_ROOM : contractRoomPo.getPayerObjType();
            contractRoomPo.setPayerObjType(payerObjType);
            if (FeeDto.PAYER_OBJ_TYPE_SHOP.equals(payerObjType)) {
                String shopName = resourceStore.getString("shopNum");
                if (StringUtil.isEmpty(shopName)) {
                    shopName = resourceStore.getString("locationName");
                }
                contractRoomPo.setRoomName(StringUtil.isEmpty(shopName) ? resourceStore.getString("roomName") : shopName);
                contractRoomInnerServiceSMOImpl.saveContractRoom(contractRoomPo);
                continue;
            }
            contractRoomPo.setRoomName(
                    resourceStore.getString("floorNum") + "-"
                            + resourceStore.getString("unitNum") + "-" + resourceStore.getString("roomNum"));
            contractRoomInnerServiceSMOImpl.saveContractRoom(contractRoomPo);
        }

    }


    /**
     * 修改 房屋租赁状态
     *
     * @param contractPo
     */
    private void noticeRentUpdateState(ContractPo contractPo) {


        if (StringUtil.isEmpty(contractPo.getObjType())
                || !contractPo.getObjType().equals(FeeDto.PAYER_OBJ_TYPE_ROOM)
                || StringUtil.isEmpty(contractPo.getObjId())
                || contractPo.getObjId().startsWith("-")) {
            return;
        }
        RentingPoolDto rentingPoolDto = new RentingPoolDto();
        rentingPoolDto.setRoomId(contractPo.getObjId());
        rentingPoolDto.setState(RentingPoolDto.STATE_APPLY_AGREE);
        List<RentingPoolDto> rentingPoolDtos = rentingPoolInnerServiceSMOImpl.queryRentingPools(rentingPoolDto);

        if (rentingPoolDtos == null || rentingPoolDtos.size() < 1) {
            return;
        }


        RentingPoolPo rentingPoolPo = new RentingPoolPo();
        rentingPoolPo.setCommunityId(rentingPoolDtos.get(0).getCommunityId());
        rentingPoolPo.setRentingId(rentingPoolDtos.get(0).getRentingId());

        rentingPoolPo.setState(RentingPoolDto.STATE_ADMIN_AUDIT);
        rentingPoolInnerServiceSMOImpl.updateRentingPool(rentingPoolPo);

    }

    /**
     * 保存合同属性
     *
     * @param jsonObject
     */
    private void saveContractAttr(JSONObject jsonObject, ContractPo contractPo) {

        ContractAttrPo contractAttrPo = new ContractAttrPo();
        contractAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
        contractAttrPo.setContractId(contractPo.getContractId());
        contractAttrPo.setSpecCd(jsonObject.getString("specCd"));
        contractAttrPo.setValue(jsonObject.getString("value"));
        contractAttrPo.setStoreId(contractPo.getStoreId());
        contractAttrPo.setCommunityId(contractPo.getCommunityId());
        int count = contractAttrInnerServiceSMOImpl.saveContractAttr(contractAttrPo);

        if (count < 1) {
            throw new IllegalArgumentException("保存属性失败");
        }
    }

}
