package com.java110.fee.cmd.meterWater;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.room.RoomDto;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.payFee.PayFeeBatchDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeAttrInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeBatchV1InnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.intf.user.IOwnerCarInnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.fee.FeeAttrPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.payFee.PayFeeBatchPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存代理费用命令类
 * 用于处理水表代理费用的创建和保存，支持房屋、车位、合同等多种收费对象类型
 * 主要功能包括费用信息验证、批次号生成、费用数据保存和相关属性设置
 */
@Java110Cmd(serviceCode = "meterWater.saveProxyFee")
public class SaveProxyFeeCmd extends Cmd {

    @Autowired
    private IPayFeeBatchV1InnerServiceSMO payFeeBatchV1InnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查必要的参数是否存在，并对结束时间进行格式化处理
     *
     * @param event              命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson            请求JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "feeTypeCd", "请求报文中未包含费用类型");
        Assert.hasKeyAndValue(reqJson, "configId", "请求报文中未包含费用项");
        Assert.hasKeyAndValue(reqJson, "objType", "请求报文中未包含objType");
        Assert.hasKeyAndValue(reqJson, "objId", "请求报文中未包含objId");
        Assert.hasKeyAndValue(reqJson, "amount", "请求报文中未包含amount");
        Assert.hasKeyAndValue(reqJson, "consumption", "请求报文中未包含consumption");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "startTime", "请求报文中未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "请求报文中未包含结束时间");

        // 格式化结束时间，如果没有时分秒则添加默认值
        String endTime = reqJson.getString("endTime");
        if (!endTime.contains(":")) {
            endTime += " 23:59:59";
            reqJson.put("endTime", endTime);
        }
    }

    /**
     * 执行保存代理费用的主要业务逻辑
     * 包括生成批次号、创建费用记录、设置费用属性、保存数据等操作
     *
     * @param event              命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson            请求JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 生成费用批次号
        generatorBatch(reqJson);

        // 初始化费用和费用属性列表
        List<PayFeePo> payFeePos = new ArrayList<>();
        List<FeeAttrPo> feeAttrPos = new ArrayList<>();

        // 创建费用主记录
        PayFeePo payFeePo = BeanConvertUtil.covertBean(reqJson, PayFeePo.class);
        payFeePo.setFeeId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
        payFeePo.setIncomeObjId(reqJson.getString("storeId"));
        // payFeePo.setAmount("-1"); // 注释掉的金额设置
        payFeePo.setStartTime(reqJson.getString("startTime"));
        payFeePo.setEndTime(reqJson.getString("startTime")); // 结束时间暂时设置为开始时间
        payFeePo.setPayerObjId(reqJson.getString("objId"));
        payFeePo.setPayerObjType(reqJson.getString("objType"));
        payFeePo.setFeeFlag(FeeDto.FEE_FLAG_ONCE); // 设置为一次性费用
        payFeePo.setState(FeeDto.STATE_DOING); // 费用状态为进行中
        payFeePo.setUserId(reqJson.getString("userId"));
        payFeePo.setBatchId(reqJson.getString("batchId"));

        payFeePos.add(payFeePo);

        // 添加消费量属性
        FeeAttrPo feeAttrPo = new FeeAttrPo();
        feeAttrPo.setCommunityId(reqJson.getString("communityId"));
        feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_PROXY_CONSUMPTION);
        feeAttrPo.setValue(reqJson.getString("consumption"));
        feeAttrPo.setFeeId(payFeePo.getFeeId());
        feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
        feeAttrPos.add(feeAttrPo);
        
        // 添加一次性费用截止时间属性
        feeAttrPo = new FeeAttrPo();
        feeAttrPo.setCommunityId(reqJson.getString("communityId"));
        feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_ONCE_FEE_DEADLINE_TIME);
        feeAttrPo.setValue(reqJson.getString("endTime"));
        feeAttrPo.setFeeId(payFeePo.getFeeId());
        feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
        feeAttrPos.add(feeAttrPo);
        
        // 根据收费对象类型获取相关信息
        String objName = "";
        String ownerId = "";
        String tel = "";
        String name = "";
        
        // 处理房屋类型的收费对象
        if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(reqJson.getString("objType"))) {
            RoomDto roomDto = new RoomDto();
            roomDto.setRoomId(reqJson.getString("objId"));
            roomDto.setCommunityId(reqJson.getString("communityId"));
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
            if (roomDtos == null || roomDtos.size() != 1) {
                return; // 房屋信息不存在或不唯一，直接返回
            }
            roomDto = roomDtos.get(0);
            objName = roomDto.getFloorNum() + "-" + roomDto.getUnitNum() + "-" + roomDto.getRoomNum();

            // 查询房屋业主信息
            OwnerDto ownerDto = new OwnerDto();
            ownerDto.setRoomId(roomDto.getRoomId());
            List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);

            if (ownerDtos != null && ownerDtos.size() > 0) {
                ownerId = ownerDtos.get(0).getOwnerId();
                tel = ownerDtos.get(0).getLink();
                name = ownerDtos.get(0).getName();
            }
        } 
        // 处理车位类型的收费对象
        else if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(reqJson.getString("objType"))) {
            OwnerCarDto ownerCarDto = new OwnerCarDto();
            ownerCarDto.setCarId(reqJson.getString("objId"));
            ownerCarDto.setCommunityId(reqJson.getString("communityId"));
            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

            if (ownerCarDtos == null || ownerCarDtos.size() < 1) {
                return; // 车位信息不存在，直接返回
            }
            objName = ownerCarDtos.get(0).getCarNum();
            ownerId = ownerCarDtos.get(0).getOwnerId();
            tel = ownerCarDtos.get(0).getLink();
            name = ownerCarDtos.get(0).getOwnerName();
        } 
        // 处理合同类型的收费对象
        else if (FeeDto.PAYER_OBJ_TYPE_CONTRACT.equals(reqJson.getString("objType"))) {
            ContractDto contractDto = new ContractDto();
            contractDto.setContractId(reqJson.getString("objId"));
            List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);

            if (contractDtos == null || contractDtos.size() < 1) {
                return; // 合同信息不存在，直接返回
            }

            objName = contractDtos.get(0).getContractCode();
            tel = contractDtos.get(0).getbLink();
            name = contractDtos.get(0).getPartyB();
        }
        
        // 添加业主相关属性信息
        addFeeAttr(FeeAttrDto.SPEC_CD_OWNER_ID, ownerId, feeAttrPos, payFeePo);
        addFeeAttr(FeeAttrDto.SPEC_CD_OWNER_NAME, name, feeAttrPos, payFeePo);
        addFeeAttr(FeeAttrDto.SPEC_CD_OWNER_LINK, tel, feeAttrPos, payFeePo);
        addFeeAttr(FeeAttrDto.SPEC_CD_PAY_OBJECT_NAME, objName, feeAttrPos, payFeePo);

        // 保存费用和属性信息
        int saveFlag = saveFeeAndAttrs(payFeePos, feeAttrPos);

        if (saveFlag < 1) {
            throw new IllegalArgumentException("创建费用失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 添加费用属性到列表中
     * 如果属性值为空则不添加
     *
     * @param specCd     属性规格代码
     * @param value      属性值
     * @param feeAttrPos 费用属性列表
     * @param payFeePo   费用对象
     */
    private void addFeeAttr(String specCd, String value, List<FeeAttrPo> feeAttrPos, PayFeePo payFeePo) {
        // 如果属性值为空，则不添加
        if (StringUtil.isEmpty(value)) {
            return;
        }

        // 创建新的费用属性对象
        FeeAttrPo curFeeAttrPo = new FeeAttrPo();
        curFeeAttrPo.setFeeId(payFeePo.getFeeId());
        curFeeAttrPo.setCommunityId(payFeePo.getCommunityId());
        curFeeAttrPo.setValue(value);
        curFeeAttrPo.setSpecCd(specCd);
        curFeeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
        feeAttrPos.add(curFeeAttrPo);
    }

    /**
     * 生成费用批次号
     * 创建费用批次记录并保存到数据库
     *
     * @param reqJson 请求JSON对象
     */
    private void generatorBatch(JSONObject reqJson) {
        PayFeeBatchPo payFeeBatchPo = new PayFeeBatchPo();
        payFeeBatchPo.setBatchId(GenerateCodeFactory.getGeneratorId("12"));
        payFeeBatchPo.setCommunityId(reqJson.getString("communityId"));
        payFeeBatchPo.setCreateUserId(reqJson.getString("userId"));
        
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户不存在");
        payFeeBatchPo.setCreateUserName(userDtos.get(0).getUserName());
        payFeeBatchPo.setState(PayFeeBatchDto.STATE_NORMAL); // 批次状态为正常
        payFeeBatchPo.setMsg("正常");
        
        // 保存批次信息
        int flag = payFeeBatchV1InnerServiceSMOImpl.savePayFeeBatch(payFeeBatchPo);

        if (flag < 1) {
            throw new IllegalArgumentException("生成批次失败");
        }

        // 将批次ID设置到请求参数中
        reqJson.put("batchId", payFeeBatchPo.getBatchId());
    }

    /**
     * 保存费用和费用属性信息
     * 先保存费用主记录，再保存费用属性记录
     *
     * @param feePos     费用对象列表
     * @param feeAttrsPos 费用属性对象列表
     * @return 保存结果，大于0表示成功
     */
    private int saveFeeAndAttrs(List<PayFeePo> feePos, List<FeeAttrPo> feeAttrsPos) {
        // 先保存费用主记录
        int flag = feeInnerServiceSMOImpl.saveFee(feePos);
        if (flag < 1) {
            return flag; // 费用保存失败，直接返回
        }

        // 保存费用属性记录
        flag = feeAttrInnerServiceSMOImpl.saveFeeAttrs(feeAttrsPos);

        return flag;
    }
}