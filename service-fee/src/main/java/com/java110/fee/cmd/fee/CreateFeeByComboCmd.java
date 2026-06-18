package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.room.RoomDto;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerCarDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.payFee.PayFeeBatchDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.community.IParkingSpaceInnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeAttrInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
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
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 创建组合费用命令类
 * 用于处理通过组合方式创建费用的业务逻辑，支持为房间、车位、合同等不同付费对象创建费用
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "fee.createFeeByCombo")
public class CreateFeeByComboCmd extends Cmd {

    @Autowired
    private IPayFeeBatchV1InnerServiceSMO payFeeBatchV1InnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IParkingSpaceInnerServiceSMO parkingSpaceInnerServiceSMOImpl;

    @Autowired
    private IOwnerCarInnerServiceSMO ownerCarInnerServiceSMOImpl;

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event              命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson            请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证必需参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "payerObjId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "payerObjName", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "payerObjType", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "comboId", "请求报文中未包含communityId节点");

        // 验证费用配置信息
        if (!reqJson.containsKey("configs")) {
            throw new IllegalArgumentException("不包含费用信息");
        }

        JSONArray configs = reqJson.getJSONArray("configs");

        if (configs == null || configs.size() < 1) {
            throw new IllegalArgumentException("不包含费用信息");
        }
    }

    /**
     * 执行命令方法
     * 处理创建组合费用的主要业务逻辑
     *
     * @param event              命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson            请求JSON对象
     * @throws CmdException 当创建费用失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 生成批次号用于追踪本次费用创建
        generatorBatch(reqJson);

        // 获取费用配置数组
        JSONArray configs = reqJson.getJSONArray("configs");

        // 遍历每个费用配置项并创建费用
        for (int configIndex = 0; configIndex < configs.size(); configIndex++) {
            doCreateConfigFee(reqJson, configs.getJSONObject(configIndex));
        }
    }

    /**
     * 创建单个配置费用
     * 根据费用配置信息创建具体的费用记录
     *
     * @param reqJson 请求JSON对象，包含社区ID、付费对象等信息
     * @param config  费用配置JSON对象
     */
    private void doCreateConfigFee(JSONObject reqJson, JSONObject config) {
        // 查询费用配置信息
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));
        feeConfigDto.setConfigId(config.getString("configId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        
        // 验证费用配置是否存在且唯一
        Assert.listOnlyOne(feeConfigDtos, "当前费用项ID不存在或存在多条" + reqJson.getString("configId"));

        // 对于非周期性费用，验证结束时间是否在配置的有效范围内
        if (!FeeDto.FEE_FLAG_CYCLE.equals(feeConfigDtos.get(0).getFeeFlag()) && config.containsKey("endTime")) {
            Date endTime = null;
            Date configEndTime = null;
            try {
                // 解析传入的结束时间和配置的结束时间
                endTime = DateUtil.getDateFromString(config.getString("endTime"), DateUtil.DATE_FORMATE_STRING_B);
                configEndTime = DateUtil.getDateFromString(feeConfigDtos.get(0).getEndTime(), DateUtil.DATE_FORMATE_STRING_A);
                
                // 验证结束时间是否超过配置的限制
                if (endTime.getTime() > configEndTime.getTime()) {
                    throw new IllegalArgumentException("结束时间不能超过费用项时间");
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("结束时间错误" + reqJson.getString("endTime"));
            }
        }

        // 初始化费用和费用属性列表
        List<PayFeePo> feePos = new ArrayList<>();
        List<FeeAttrPo> feeAttrsPos = new ArrayList<>();

        // 添加房间费用信息
        feePos.add(BeanConvertUtil.covertBean(addRoomFee(config, reqJson, feeConfigDtos), PayFeePo.class));

        // 对于非周期性费用，添加一次性费用截止时间属性
        if (!FeeDto.FEE_FLAG_CYCLE.equals(feeConfigDtos.get(0).getFeeFlag())) {
            feeAttrsPos.add(addFeeAttr(reqJson, FeeAttrDto.SPEC_CD_ONCE_FEE_DEADLINE_TIME,
                    config.containsKey("endTime") ? config.getString("endTime") : reqJson.getString("configEndTime")));
        }

        // 保存业主相关费用属性
        saveOwnerFeeAttr(reqJson, feeAttrsPos);

        // 添加组合ID属性
        feeAttrsPos.add(addFeeAttr(reqJson, FeeAttrDto.SPEC_CD_COMBO_ID,
                reqJson.getString("comboId")));

        // 保存费用和费用属性
        int saveFlag = saveFeeAndAttrs(feePos, feeAttrsPos);

        // 验证保存结果
        if (saveFlag < 1) {
            throw new CmdException("创建费用失败");
        }
    }

    /**
     * 保存业主相关费用属性
     * 根据付费对象类型获取业主信息并设置相关费用属性
     *
     * @param reqJson      请求JSON对象
     * @param feeAttrsPos  费用属性列表
     */
    private void saveOwnerFeeAttr(JSONObject reqJson, List<FeeAttrPo> feeAttrsPos) {
        String objName = "";  // 付费对象名称
        String ownerId = "";  // 业主ID
        String tel = "";      // 联系电话
        String name = "";     // 业主姓名

        // 根据付费对象类型处理不同的业务逻辑
        if (FeeDto.PAYER_OBJ_TYPE_ROOM.equals(reqJson.getString("payerObjType"))) {
            // 处理房间类型的付费对象
            RoomDto roomDto = new RoomDto();
            roomDto.setRoomId(reqJson.getString("payerObjId"));
            roomDto.setCommunityId(reqJson.getString("communityId"));
            List<RoomDto> roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);
            
            // 验证房间信息
            if (roomDtos == null || roomDtos.size() != 1) {
                return;
            }
            roomDto = roomDtos.get(0);
            // 构建房间名称：楼层-单元-房间号
            objName = roomDto.getFloorNum() + "-" + roomDto.getUnitNum() + "-" + roomDto.getRoomNum();

            // 查询房间对应的业主信息
            OwnerDto ownerDto = new OwnerDto();
            ownerDto.setRoomId(roomDto.getRoomId());
            List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwners(ownerDto);

            if (ownerDtos != null && ownerDtos.size() > 0) {
                ownerId = ownerDtos.get(0).getOwnerId();
                tel = ownerDtos.get(0).getLink();
                name = ownerDtos.get(0).getName();
            }
        } else if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(reqJson.getString("payerObjType"))) {
            // 处理车位类型的付费对象
            OwnerCarDto ownerCarDto = new OwnerCarDto();
            ownerCarDto.setCarId(reqJson.getString("payerObjId"));
            ownerCarDto.setCommunityId(reqJson.getString("communityId"));
            List<OwnerCarDto> ownerCarDtos = ownerCarInnerServiceSMOImpl.queryOwnerCars(ownerCarDto);

            if (ownerCarDtos == null || ownerCarDtos.size() < 1) {
                return;
            }
            objName = ownerCarDtos.get(0).getCarNum();  // 车牌号作为对象名称
            ownerId = ownerCarDtos.get(0).getOwnerId();
            tel = ownerCarDtos.get(0).getLink();
            name = ownerCarDtos.get(0).getOwnerName();
        } else if (FeeDto.PAYER_OBJ_TYPE_CONTRACT.equals(reqJson.getString("payerObjType"))) {
            // 处理合同类型的付费对象
            ContractDto contractDto = new ContractDto();
            contractDto.setContractId(reqJson.getString("payerObjId"));
            List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);

            if (contractDtos == null || contractDtos.size() < 1) {
                return;
            }

            objName = contractDtos.get(0).getContractCode();  // 合同编号作为对象名称
            tel = contractDtos.get(0).getbLink();
            name = contractDtos.get(0).getPartyB();
        }
        
        // 根据获取到的信息添加相应的费用属性
        if (!StringUtil.isEmpty(ownerId)) {
            feeAttrsPos.add(addFeeAttr(reqJson, FeeAttrDto.SPEC_CD_OWNER_ID, ownerId));
        }
        if (!StringUtil.isEmpty(tel)) {
            feeAttrsPos.add(addFeeAttr(reqJson, FeeAttrDto.SPEC_CD_OWNER_LINK, tel));
        }
        if (!StringUtil.isEmpty(name)) {
            feeAttrsPos.add(addFeeAttr(reqJson, FeeAttrDto.SPEC_CD_OWNER_NAME, name));
        }
        // 添加付费对象名称属性
        if (!StringUtil.isEmpty(objName)) {
            feeAttrsPos.add(addFeeAttr(reqJson, FeeAttrDto.SPEC_CD_PAY_OBJECT_NAME, objName));
        }
    }

    /**
     * 保存费用和费用属性
     * 先保存费用信息，再保存费用属性信息
     *
     * @param feePos       费用对象列表
     * @param feeAttrsPos  费用属性列表
     * @return 保存结果，大于0表示成功
     */
    private int saveFeeAndAttrs(List<PayFeePo> feePos, List<FeeAttrPo> feeAttrsPos) {
        // 先保存费用信息
        int flag = feeInnerServiceSMOImpl.saveFee(feePos);
        if (flag < 1) {
            return flag;
        }

        // 再保存费用属性信息
        flag = feeAttrInnerServiceSMOImpl.saveFeeAttrs(feeAttrsPos);

        return flag;
    }

    /**
     * 添加房间费用信息
     * 构建费用对象的基本信息
     *
     * @param config       费用配置JSON对象
     * @param paramInJson  请求参数JSON对象
     * @param feeConfigDtos 费用配置数据列表
     * @return 包含费用信息的JSON对象
     */
    public JSONObject addRoomFee(JSONObject config, JSONObject paramInJson, List<FeeConfigDto> feeConfigDtos) {
        JSONObject businessUnit = new JSONObject();
        
        // 设置费用基本信息
        businessUnit.put("feeId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_feeId));
        businessUnit.put("configId", feeConfigDtos.get(0).getConfigId());
        businessUnit.put("feeTypeCd", feeConfigDtos.get(0).getFeeTypeCd());
        businessUnit.put("incomeObjId", paramInJson.getString("storeId"));
        businessUnit.put("amount", "-1.00");  // 默认金额
        
        // 如果配置中有金额信息，使用配置的金额
        if (config.containsKey("amount") && !StringUtil.isEmpty(config.getString("amount"))) {
            businessUnit.put("amount", config.getString("amount"));
        }
        
        // 设置时间信息
        businessUnit.put("startTime", config.getString("startTime"));
        businessUnit.put("endTime", config.getString("startTime"));  // 结束时间暂时设置为开始时间
        businessUnit.put("communityId", paramInJson.getString("communityId"));
        
        // 设置付费对象信息
        businessUnit.put("payerObjId", paramInJson.getString("payerObjId"));
        businessUnit.put("payerObjType", paramInJson.getString("payerObjType"));
        businessUnit.put("feeFlag", feeConfigDtos.get(0).getFeeFlag());
        businessUnit.put("state", "2008001");  // 费用状态：正常
        businessUnit.put("batchId", paramInJson.getString("batchId"));
        businessUnit.put("userId", paramInJson.getString("userId"));

        // 将费用ID设置到请求参数中，供后续使用
        paramInJson.put("feeId", businessUnit.getString("feeId"));
        return businessUnit;
    }

    /**
     * 添加费用属性
     * 构建费用属性对象
     *
     * @param paramInJson 请求参数JSON对象
     * @param specCd      属性规格代码
     * @param value       属性值
     * @return 费用属性PO对象
     */
    public FeeAttrPo addFeeAttr(JSONObject paramInJson, String specCd, String value) {
        FeeAttrPo feeAttrPo = new FeeAttrPo();
        feeAttrPo.setCommunityId(paramInJson.getString("communityId"));
        feeAttrPo.setSpecCd(specCd);
        feeAttrPo.setValue(value);
        feeAttrPo.setFeeId(paramInJson.getString("feeId"));
        feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
        return feeAttrPo;
    }

    /**
     * 生成批次号
     * 创建费用批次记录，用于追踪本次费用创建操作
     *
     * @param reqJson 请求JSON对象
     */
    private void generatorBatch(JSONObject reqJson) {
        PayFeeBatchPo payFeeBatchPo = new PayFeeBatchPo();
        
        // 设置批次基本信息
        payFeeBatchPo.setBatchId(GenerateCodeFactory.getGeneratorId("12"));
        payFeeBatchPo.setCommunityId(reqJson.getString("communityId"));
        payFeeBatchPo.setCreateUserId(reqJson.getString("userId"));
        
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户不存在");
        
        // 设置创建用户名称
        payFeeBatchPo.setCreateUserName(userDtos.get(0).getUserName());
        payFeeBatchPo.setState(PayFeeBatchDto.STATE_NORMAL);  // 批次状态：正常
        payFeeBatchPo.setMsg("正常");
        
        // 保存批次信息
        int flag = payFeeBatchV1InnerServiceSMOImpl.savePayFeeBatch(payFeeBatchPo);

        if (flag < 1) {
            throw new IllegalArgumentException("生成批次失败");
        }

        // 将批次ID设置到请求参数中，供后续使用
        reqJson.put("batchId", payFeeBatchPo.getBatchId());
    }
}