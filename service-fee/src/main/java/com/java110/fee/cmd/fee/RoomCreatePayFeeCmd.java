/**
 * 房屋批量创建费用命令类
 * 
 * 该命令类用于处理房屋批量创建费用的业务逻辑，包括参数验证、费用批次生成和数据总线异步处理等功能。
 * 主要功能：
 * 1. 验证请求参数的有效性，包括小区ID、收费项目、时间范围等
 * 2. 根据费用配置信息验证费用类型和计算方式
 * 3. 生成费用批次记录
 * 4. 通过数据总线异步处理房屋费用创建任务
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.data.DatabusDataDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.payFee.PayFeeBatchDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IPayFeeBatchV1InnerServiceSMO;
import com.java110.intf.job.IDataBusInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.po.payFee.PayFeeBatchPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * 房屋批量创建费用命令类
 * 实现房屋批量创建费用的业务逻辑，包括参数验证、批次生成和异步处理
 */
@Java110Cmd(serviceCode = "fee.roomCreatePayFee")
public class RoomCreatePayFeeCmd extends Cmd {

    /**
     * 费用配置服务接口
     */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /**
     * 费用批次服务接口
     */
    @Autowired
    private IPayFeeBatchV1InnerServiceSMO payFeeBatchV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 数据总线服务接口
     */
    @Autowired
    private IDataBusInnerServiceSMO dataBusInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括必填字段检查、时间格式处理、费用配置验证等
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取用户和店铺信息
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     * @throws ParseException 时间解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "configId", "未包含收费项目");
        Assert.hasKeyAndValue(reqJson, "startTime", "开始时间不存在");
        Assert.hasKeyAndValue(reqJson, "endTime", "结束时间不存在");

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
        
        // 确保费用配置存在且唯一
        Assert.listOnlyOne(feeConfigDtos, "当前费用项ID不存在或存在多条" + reqJson.getString("configId"));
        
        // 将费用配置信息添加到请求参数中
        reqJson.put("feeTypeCd", feeConfigDtos.get(0).getFeeTypeCd());
        reqJson.put("feeFlag", feeConfigDtos.get(0).getFeeFlag());
        reqJson.put("configEndTime", feeConfigDtos.get(0).getEndTime());

        // 验证房屋ID列表
        if (!reqJson.containsKey("roomIds")) {
            throw new IllegalArgumentException("未包含房屋");
        }

        JSONArray roomIds = reqJson.getJSONArray("roomIds");
        if (ListUtil.isNull(roomIds)) {
            throw new IllegalArgumentException("未包含房屋");
        }

        // 动态费用计算方式验证：需要金额参数
        if (FeeConfigDto.COMPUTING_FORMULA_DYNAMIC.equals(feeConfigDtos.get(0).getComputingFormula())) {
            Assert.hasKeyAndValue(reqJson, "amount", "未包含金额");
        }

        // 递增费用计算方式验证：需要递增周期、递增率和开始时间参数
        if (FeeConfigDto.COMPUTING_FORMULA_RANT_RATE.equals(feeConfigDtos.get(0).getComputingFormula())) {
            Assert.hasKeyAndValue(reqJson, "rateCycle", "未包含递增周期");
            Assert.hasKeyAndValue(reqJson, "rate", "未包含递增率");
            Assert.hasKeyAndValue(reqJson, "rateStartTime", "未包含递增开始时间");
            reqJson.put("configComputingFormula", feeConfigDtos.get(0).getComputingFormula());
        }

        // 获取并设置用户信息
        String userId = CmdContextUtils.getUserId(context);
        reqJson.put("userId", userId);

        // 获取并设置店铺信息
        String storeId = CmdContextUtils.getStoreId(context);
        reqJson.put("storeId", storeId);
    }

    /**
     * 命令执行方法
     * 
     * 执行房屋批量创建费用的主要业务逻辑，包括：
     * 1. 生成费用批次记录
     * 2. 验证用户信息
     * 3. 通过数据总线异步处理费用创建任务
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     * @throws ParseException 时间解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 生成费用批次记录
        PayFeeBatchPo payFeeBatchPo = new PayFeeBatchPo();
        payFeeBatchPo.setBatchId(GenerateCodeFactory.getGeneratorId("12")); // 生成12位批次ID
        payFeeBatchPo.setCommunityId(reqJson.getString("communityId"));
        payFeeBatchPo.setCreateUserId(reqJson.getString("userId"));
        
        // 验证用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户不存在");
        payFeeBatchPo.setCreateUserName(userDtos.get(0).getUserName());
        
        // 设置批次状态和消息
        payFeeBatchPo.setState(PayFeeBatchDto.STATE_NORMAL);
        payFeeBatchPo.setMsg("正常");
        
        // 保存批次记录
        int flag = payFeeBatchV1InnerServiceSMOImpl.savePayFeeBatch(payFeeBatchPo);

        if (flag < 1) {
            throw new IllegalArgumentException("生成批次失败");
        }

        // 将批次ID添加到请求参数中
        reqJson.put("batchId", payFeeBatchPo.getBatchId());

        // 通过数据总线异步处理房屋费用创建任务，避免同步处理超时
        dataBusInnerServiceSMOImpl.databusData(new DatabusDataDto(DatabusDataDto.BUSINESS_TYPE_ROOM_CREATE_PAY_FEE, reqJson));
    }
}