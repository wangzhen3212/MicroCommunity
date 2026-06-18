/**
 * 合同费用创建命令类
 * 
 * 该命令类用于处理合同相关的费用创建业务，主要功能包括：
 * 1. 验证费用创建请求参数的合法性
 * 2. 根据合同信息批量生成费用记录
 * 3. 生成费用批次信息用于跟踪
 * 4. 处理费用属性的关联存储
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.contract.ContractDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.payFee.PayFeeBatchDto;
import com.java110.dto.user.UserDto;
import com.java110.fee.bmo.fee.IFeeBMO;
import com.java110.intf.fee.IFeeAttrInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeBatchV1InnerServiceSMO;
import com.java110.intf.store.IContractInnerServiceSMO;
import com.java110.intf.user.IOwnerInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.fee.FeeAttrPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.payFee.PayFeeBatchPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Java110Cmd(serviceCode = "fee.saveContractCreateFee")
public class SaveContractCreateFeeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveContractCreateFeeCmd.class);

    @Autowired
    private IFeeBMO feeBMOImpl;

    /** 默认批量添加费用记录的数量 */
    private static final int DEFAULT_ADD_FEE_COUNT = 200;

    @Autowired
    private IContractInnerServiceSMO contractInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IOwnerInnerServiceSMO ownerInnerServiceSMOImpl;

    @Autowired
    private IPayFeeBatchV1InnerServiceSMO payFeeBatchV1InnerServiceSMOImpl;

    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IFeeAttrInnerServiceSMO feeAttrInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 验证费用创建请求的必填参数，包括小区ID、收费项目、开始时间和结束时间等
     * 对结束时间进行格式化处理，确保包含完整的时间信息
     * 
     * @param event CMD事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "configId", "未包含收费项目");
        Assert.hasKeyAndValue(reqJson, "startTime", "未包含收费其实时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "未包含计费结束时间");

        // 格式化结束时间，确保包含时分秒
        String endTime = reqJson.getString("endTime");
        if (!endTime.contains(":")) {
            endTime += " 23:59:59";
            reqJson.put("endTime", endTime);
        }
    }

    /**
     * 执行费用创建命令
     * 
     * 主要处理逻辑：
     * 1. 验证费用配置信息
     * 2. 检查时间范围合法性
     * 3. 生成批次号
     * 4. 查询合同信息
     * 5. 处理合同费用创建
     * 
     * @param event CMD事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        logger.debug("ServiceDataFlowEvent : {}", event);
        
        // 获取商户ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
        
        // 查询费用配置信息
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));
        feeConfigDto.setConfigId(reqJson.getString("configId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        
        // 验证费用配置唯一性
        Assert.listOnlyOne(feeConfigDtos, "当前费用项ID不存在或存在多条" + reqJson.getString("configId"));
        
        // 设置费用类型和标志
        reqJson.put("feeTypeCd", feeConfigDtos.get(0).getFeeTypeCd());
        reqJson.put("feeFlag", feeConfigDtos.get(0).getFeeFlag());
        reqJson.put("configEndTime", feeConfigDtos.get(0).getEndTime());

        // 验证非周期性费用的结束时间不能超过费用项配置的结束时间
        if (!FeeDto.FEE_FLAG_CYCLE.equals(feeConfigDtos.get(0).getFeeFlag()) && reqJson.containsKey("endTime")) {
            Date endTime = null;
            Date configEndTime = null;
            try {
                endTime = DateUtil.getDateFromString(reqJson.getString("endTime"), DateUtil.DATE_FORMATE_STRING_B);
                configEndTime = DateUtil.getDateFromString(feeConfigDtos.get(0).getEndTime(), DateUtil.DATE_FORMATE_STRING_A);
                if (endTime.getTime() > configEndTime.getTime()) {
                    throw new IllegalArgumentException("结束时间不能超过费用项时间");
                }
            } catch (ParseException e) {
                throw new IllegalArgumentException("结束时间错误" + reqJson.getString("endTime"));
            }
        }
        
        // 生成批次号
        generatorBatch(reqJson);
        
        // 构建合同查询条件
        ContractDto contractDto = new ContractDto();
        // 处理合同状态条件
        if (reqJson.containsKey("contractState") && reqJson.getString("contractState").contains(",")) {
            String states = reqJson.getString("contractState");
            contractDto.setStates(states.split(","));
        }

        contractDto.setContractId(reqJson.getString("contractId"));
        contractDto.setStoreId(storeId);
        
        // 查询合同信息
        List<ContractDto> contractDtos = contractInnerServiceSMOImpl.queryContracts(contractDto);

        if (contractDtos == null || contractDtos.size() < 1) {
            throw new IllegalArgumentException("未查到需要付费的房屋");
        }
        
        // 处理合同费用创建
        dealContractFee(contractDtos, cmdDataFlowContext, reqJson, event);
    }

    /**
     * 生成费用批次号
     * 
     * 创建费用批次记录，用于跟踪批量费用创建操作
     * 
     * @param reqJson 请求参数JSON对象，包含小区ID、用户ID等信息
     */
    private void generatorBatch(JSONObject reqJson) {
        PayFeeBatchPo payFeeBatchPo = new PayFeeBatchPo();
        // 生成批次ID
        payFeeBatchPo.setBatchId(GenerateCodeFactory.getGeneratorId("12"));
        payFeeBatchPo.setCommunityId(reqJson.getString("communityId"));
        payFeeBatchPo.setCreateUserId(reqJson.getString("userId"));
        
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        Assert.listOnlyOne(userDtos, "用户不存在");
        
        // 设置创建用户信息
        payFeeBatchPo.setCreateUserName(userDtos.get(0).getUserName());
        payFeeBatchPo.setState(PayFeeBatchDto.STATE_NORMAL);
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
     * 处理合同费用创建
     * 
     * 批量处理合同费用创建，包括：
     * 1. 创建费用记录
     * 2. 设置费用属性
     * 3. 分批保存数据
     * 4. 统计处理结果
     * 
     * @param contractDtos 合同数据列表
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @param event CMD事件对象
     */
    private void dealContractFee(List<ContractDto> contractDtos, ICmdDataFlowContext context, JSONObject reqJson, CmdEvent event) {
        // 初始化费用和属性列表
        List<PayFeePo> feePos = new ArrayList<>();
        List<FeeAttrPo> feeAttrsPos = new ArrayList<>();
        
        int failRooms = 0; // 失败数量统计
        int curFailRoomCount = 0; // 当前批次失败数量
        int saveFlag = 0; // 保存标志

        // 遍历所有合同，创建费用记录
        for (int roomIndex = 0; roomIndex < contractDtos.size(); roomIndex++) {
            curFailRoomCount++;
            
            // 创建费用记录
            feePos.add(BeanConvertUtil.covertBean(feeBMOImpl.addContractFee(contractDtos.get(roomIndex), reqJson, context), PayFeePo.class));
            
            // 设置费用属性：一次性费用截止时间
            feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_ONCE_FEE_DEADLINE_TIME,
                    reqJson.getString("endTime")));
            
            // 设置业主相关属性
            if (!StringUtil.isEmpty(contractDtos.get(roomIndex).getObjId())) {
                feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_OWNER_ID, contractDtos.get(roomIndex).getObjId()));
                feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_OWNER_LINK, contractDtos.get(roomIndex).getbLink()));
                feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_OWNER_NAME, contractDtos.get(roomIndex).getPartyB()));
            }
            
            // 设置付费对象名称
            feeAttrsPos.add(feeBMOImpl.addFeeAttr(reqJson, context, FeeAttrDto.SPEC_CD_PAY_OBJECT_NAME,
                    contractDtos.get(roomIndex).getContractName()));

            // 分批保存：每处理DEFAULT_ADD_FEE_COUNT条记录保存一次
            if (roomIndex % DEFAULT_ADD_FEE_COUNT == 0 && roomIndex != 0) {
                saveFlag = saveFeeAndAttrs(feePos, feeAttrsPos);
                // 重置列表
                feePos = new ArrayList<>();
                feeAttrsPos = new ArrayList<>();
                if (saveFlag < 1) {
                    failRooms += curFailRoomCount; // 统计失败数量
                } else {
                    curFailRoomCount = 0; // 重置当前批次失败计数
                }
            }
        }
        
        // 处理剩余未保存的记录
        if (!ListUtil.isNull(feePos)) {
            saveFlag = saveFeeAndAttrs(feePos, feeAttrsPos);
            if (saveFlag < 1) {
                failRooms += curFailRoomCount;
            }
        }
        
        // 构建响应结果
        JSONObject paramOut = new JSONObject();
        paramOut.put("totalRoom", contractDtos.size());
        paramOut.put("successRoom", contractDtos.size() - failRooms);
        paramOut.put("errorRoom", failRooms);

        context.setResponseEntity(ResultVo.createResponseEntity(paramOut));
    }

    /**
     * 保存费用记录和属性
     * 
     * 批量保存费用记录和对应的费用属性
     * 
     * @param feePos 费用记录列表
     * @param feeAttrsPos 费用属性列表
     * @return 保存结果，大于0表示成功，小于等于0表示失败
     */
    private int saveFeeAndAttrs(List<PayFeePo> feePos, List<FeeAttrPo> feeAttrsPos) {
        // 先保存费用记录
        int flag = feeInnerServiceSMOImpl.saveFee(feePos);
        if (flag < 1) {
            return flag;
        }

        // 再保存费用属性
        flag = feeAttrInnerServiceSMOImpl.saveFeeAttrs(feeAttrsPos);

        return flag;
    }
}