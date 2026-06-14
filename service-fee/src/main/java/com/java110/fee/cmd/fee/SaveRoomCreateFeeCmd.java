package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.room.RoomDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.owner.OwnerDto;
import com.java110.dto.payFee.PayFeeBatchDto;
import com.java110.dto.user.UserDto;
import com.java110.fee.bmo.fee.IFeeBMO;
import com.java110.fee.feeMonth.IPayFeeMonth;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.*;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.fee.FeeAttrPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.payFee.PayFeeBatchPo;
import com.java110.utils.constant.CommonConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 创建费用命令类
 * 用于处理房屋费用的创建，包括费用信息、费用属性的保存以及相关业务逻辑处理
 * 支持普通费用和租金递增费用的创建
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "fee.saveRoomCreateFee")
public class SaveRoomCreateFeeCmd extends Cmd {

    /** 默认添加费用数量常量 */
    private static final int DEFAULT_ADD_FEE_COUNT = 200;

    /** 房屋内部服务 */
    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /** 费用配置内部服务 */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /** 业主内部服务 */
    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    /** 付费批次内部服务 */
    @Autowired
    private IPayFeeBatchV1InnerServiceSMO payFeeBatchV1InnerServiceSMOImpl;

    /** 用户内部服务 */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /** 费用业务操作接口 */
    @Autowired
    private IFeeBMO feeBMOImpl;

    /** 费用内部服务 */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /** 费用属性内部服务 */
    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    /** 付费月份处理接口 */
    @Autowired
    private IPayFeeMonth payFeeMonthImpl;

    /** 账单生成规则服务 */
    @Autowired
    private IRuleGeneratorPayFeeBillV1InnerServiceSMO ruleGeneratorPayFeeBillV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性和完整性
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "locationObjId", "未包含收费对象");
        Assert.hasKeyAndValue(reqJson, "configId", "未包含收费项目");
        Assert.hasKeyAndValue(reqJson, "storeId", "未包含商户ID");
        Assert.hasKeyAndValue(reqJson, "startTime", "未包含计费起始时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "未包含计费结束时间");

        // 处理结束时间格式，如果没有时分秒则补充
        String endTime = reqJson.getString("endTime");
        if (!endTime.contains(":")) {
            endTime += " 23:59:59";
            reqJson.put("endTime", endTime);
        }

        // 验证费用配置信息
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));
        feeConfigDto.setConfigId(reqJson.getString("configId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        Assert.listOnlyOne(feeConfigDtos, "当前费用项ID不存在或存在多条" + reqJson.getString("configId"));
        
        // 将费用配置信息添加到请求参数中
        reqJson.put("feeTypeCd", feeConfigDtos.get(0).getFeeTypeCd());
        reqJson.put("feeFlag", feeConfigDtos.get(0).getFeeFlag());
        reqJson.put("configEndTime", feeConfigDtos.get(0).getEndTime());

        // 如果是租金递增公式，验证相关参数
        if (FeeConfigDto.COMPUTING_FORMULA_RANT_RATE.equals(feeConfigDtos.get(0).getComputingFormula())) {
            Assert.hasKeyAndValue(reqJson, "rateCycle", "未包含递增周期");
            Assert.hasKeyAndValue(reqJson, "rate", "未包含递增率");
            Assert.hasKeyAndValue(reqJson, "rateStartTime", "未包含递增开始时间");
            reqJson.put("configComputingFormula", feeConfigDtos.get(0).getComputingFormula());
        }
    }

    /**
     * 执行命令方法
     * 处理房屋费用的创建逻辑
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取当前用户ID并添加到请求参数中
        String userId = cmdDataFlowContext.getReqHeaders().get(CommonConstant.USER_ID);
        reqJson.put("userId", userId);
        List<RoomDto> roomDtos = null;

        // 生成费用批次号
        generatorBatch(reqJson);

        // 查询房屋信息
        RoomDto roomDto = new RoomDto();
        roomDto.setCommunityId(reqJson.getString("communityId"));
        roomDto.setRoomId(reqJson.getString("locationObjId"));
        roomDtos = roomInnerServiceSMOImpl.queryRooms(roomDto);

        // 验证房屋是否存在
        if (ListUtil.isNull(roomDtos)) {
            throw new IllegalArgumentException("未查到需要付费的房屋或未绑定业主");
        }
        
        // 处理房屋费用
        dealRoomFee(roomDtos.get(0), cmdDataFlowContext, reqJson, event);
    }

    /**
     * 处理房屋费用方法
     * 处理单个房屋的费用创建，包括业主信息关联、费用属性设置等
     *
     * @param roomDto 房屋数据对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @param event 命令事件对象
     */
    private void dealRoomFee(RoomDto roomDto, ICmdDataFlowContext context, JSONObject reqJson, CmdEvent event) {
        // 查询房屋关联的业主信息
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setCommunityId(roomDto.getCommunityId());
        ownerDto.setRoomId(roomDto.getRoomId());
        List<OwnerDto> ownerDtos = ownerInnerServiceSMOImpl.queryOwnersByRoom(ownerDto);

        // 如果存在业主信息，则关联到房屋信息中
        if (!ListUtil.isNull(ownerDtos)) {
            roomDto.setOwnerId(ownerDtos.get(0).getOwnerId());
            roomDto.setOwnerName(ownerDtos.get(0).getName());
            roomDto.setLink(ownerDtos.get(0).getLink());
        }

        List<PayFeePo> feePos = new ArrayList<>();
        List<FeeAttrPo> feeAttrsPos = new ArrayList<>();
        ResponseEntity<String> responseEntity = null;

        // 添加房屋费用到费用列表
        feePos.add(BeanConvertUtil.covertBean(feeBMOImpl.addRoomFee(roomDto, reqJson, context), PayFeePo.class));

        // 检查是否需要生成账单模式
        String billModal = ruleGeneratorPayFeeBillV1InnerServiceSMOImpl.needGeneratorBillData(feePos);
        if ("Y".equals(billModal)) {
            // 如果是账单模式，直接返回成功结果
            JSONObject paramOut = new JSONObject();
            paramOut.put("totalRoom", 1);
            paramOut.put("successRoom", 1);
            paramOut.put("errorRoom", 0);
            responseEntity = new ResponseEntity<>(paramOut.toJSONString(), HttpStatus.OK);
            context.setResponseEntity(responseEntity);
            return;
        }

        // 添加费用属性：一次性费用截止时间
        feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson,
                context,
                FeeAttrDto.SPEC_CD_ONCE_FEE_DEADLINE_TIME,
                reqJson.getString("endTime")));

        // 如果存在业主ID，添加业主相关属性
        if (!StringUtil.isEmpty(roomDto.getOwnerId())) {
            feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_OWNER_ID, roomDto.getOwnerId()));
            feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_OWNER_LINK, roomDto.getLink()));
            feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_OWNER_NAME, roomDto.getOwnerName()));
        }

        // 如果是租金递增公式，添加租金递增相关属性
        if (reqJson.containsKey("configComputingFormula") && FeeConfigDto.COMPUTING_FORMULA_RANT_RATE.equals(reqJson.getString("configComputingFormula"))) {
            feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_RATE_CYCLE, reqJson.getString("rateCycle")));
            feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_RATE, reqJson.getString("rate")));
            feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_RATE_START_TIME, reqJson.getString("rateStartTime")));
        }
        
        // 添加付费对象名称属性（楼层-单元-房间号）
        feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_PAY_OBJECT_NAME, roomDto.getFloorNum() + "-" + roomDto.getUnitNum() + "-" + roomDto.getRoomNum()));

        // 保存费用和属性信息
        int saveFlag = saveFeeAndAttrs(feePos, feeAttrsPos);

        // 返回处理结果
        JSONObject paramOut = new JSONObject();
        paramOut.put("totalRoom", 1);
        paramOut.put("successRoom", 1);
        paramOut.put("errorRoom", 0);
        responseEntity = new ResponseEntity<>(paramOut.toJSONString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 保存费用和属性方法
     * 保存费用信息和费用属性，并异步生成月数据和欠费数据
     *
     * @param feePos 费用对象列表
     * @param feeAttrsPos 费用属性对象列表
     * @return 保存结果，大于0表示成功，小于等于0表示失败
     */
    private int saveFeeAndAttrs(List<PayFeePo> feePos, List<FeeAttrPo> feeAttrsPos) {
        // 检查费用列表是否为空
        if (feePos == null || feePos.size() < 1) {
            return 1;
        }
        
        // 保存费用信息
        int flag = feeInnerServiceSMOImpl.saveFee(feePos);
        if (flag < 1) {
            return flag;
        }

        // 保存费用属性信息
        flag = feeAttrInnerServiceSMOImpl.saveFeeAttrs(feeAttrsPos);

        // 异步生成月数据和欠费数据
        List<String> feeIds = new ArrayList<>();
        for (PayFeePo feePo : feePos) {
            feeIds.add(feePo.getFeeId());
        }
        
        // 生成费用月数据
        payFeeMonthImpl.doGeneratorFeeMonths(feeIds, feePos.get(0).getCommunityId());
        // 生成欠费数据
        payFeeMonthImpl.doGeneratorOweFees(feeIds, feePos.get(0).getCommunityId());
        
        return flag;
    }

    /**
     * 生成批次号方法
     * 创建费用批次记录，用于跟踪费用创建批次
     *
     * @param reqJson 请求参数JSON对象
     * @throws IllegalArgumentException 当批次生成失败时抛出异常
     */
    private void generatorBatch(JSONObject reqJson) {
        // 创建批次对象并设置属性
        PayFeeBatchPo payFeeBatchPo = new PayFeeBatchPo();
        payFeeBatchPo.setBatchId(GenerateCodeFactory.getGeneratorId("12"));
        payFeeBatchPo.setCommunityId(reqJson.getString("communityId"));
        payFeeBatchPo.setCreateUserId(reqJson.getString("userId"));
        
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户不存在");
        
        // 设置创建用户名称和批次状态
        payFeeBatchPo.setCreateUserName(userDtos.get(0).getUserName());
        payFeeBatchPo.setState(PayFeeBatchDto.STATE_NORMAL);
        payFeeBatchPo.setMsg("正常");
        
        // 保存批次信息
        int flag = payFeeBatchV1InnerServiceSMOImpl.savePayFeeBatch(payFeeBatchPo);

        if (flag < 1) {
            throw new IllegalArgumentException("生成批次失败");
        }

        // 将批次ID添加到请求参数中
        reqJson.put("batchId", payFeeBatchPo.getBatchId());
    }
}