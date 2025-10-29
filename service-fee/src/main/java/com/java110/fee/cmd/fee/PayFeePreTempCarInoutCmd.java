package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.machine.CarInoutDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.intf.common.ICarInoutInnerServiceSMO;
import com.java110.intf.common.ICarInoutV1InnerServiceSMO;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.store.ISmallWeChatInnerServiceSMO;
import com.java110.po.car.CarInoutPo;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.fee.PayFeePo;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 临时车辆出场费用支付命令类
 * 处理临时车辆出场时的费用计算、支付明细记录、费用信息更新和车辆进出记录状态变更
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "fee.payFeePreTempCarInout")
public class PayFeePreTempCarInoutCmd extends Cmd {

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private ICarInoutInnerServiceSMO carInoutInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;
    
    @Autowired
    private ISmallWeChatInnerServiceSMO smallWeChatInnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailV1InnerServiceSMOImpl;

    @Autowired
    private ICarInoutV1InnerServiceSMO carInoutV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 检查请求报文中是否包含必要的参数节点，并验证参数值的有效性
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必要参数是否存在
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求报文中未包含communityId节点");
        Assert.jsonObjectHaveKey(reqJson, "receivedAmount", "请求报文中未包含receivedAmount节点");
        Assert.jsonObjectHaveKey(reqJson, "feeId", "请求报文中未包含feeId节点");

        // 验证参数值是否为空
        Assert.hasLength(reqJson.getString("communityId"), "小区ID不能为空");
        Assert.hasLength(reqJson.getString("receivedAmount"), "实收金额不能为空");
        Assert.hasLength(reqJson.getString("feeId"), "费用ID不能为空");
    }

    /**
     * 执行临时车辆出场费用支付命令
     * 处理费用明细添加、费用信息更新、车辆进出记录状态变更等业务逻辑
     *
     * @param event    命令事件对象
     * @param context  数据流上下文对象
     * @param paramObj 请求参数JSON对象
     * @throws CmdException     当命令执行失败时抛出异常
     * @throws ParseException   当日期解析失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject paramObj) throws CmdException, ParseException {
        // 设置周期数为0，表示临时车辆
        paramObj.put("cycles", 0);
        
        // 添加费用明细记录
        addFeeTempDetail(paramObj, context);
        // 修改临时费用信息
        modifyTempFee(paramObj, context);
        // 修改临时车辆进出记录状态
        modifyTempCarInout(paramObj, context);

        // 查询小程序配置信息
        SmallWeChatDto smallWeChatDto = new SmallWeChatDto();
        smallWeChatDto.setObjId((String) paramObj.get("communityId"));
        smallWeChatDto.setAppId((String) paramObj.get("appId"));
        List<SmallWeChatDto> smallWeChatDtos = smallWeChatInnerServiceSMOImpl.querySmallWeChats(smallWeChatDto);
        
        // 验证小程序配置是否存在
        if (smallWeChatDtos.size() <= 0) {
            throw new IllegalArgumentException("支付失败,小区未配置小程序信息");
        }
        
        // 设置支付相关的应用ID和商户ID
        if ("1000".equals(smallWeChatDtos.get(0).getObjType())) {
            paramObj.put("payAppId", smallWeChatDtos.get(0).getAppId());
            paramObj.put("payMchId", smallWeChatDtos.get(0).getMchId());
        }

        // 设置响应结果
        ResponseEntity responseEntity = new ResponseEntity<>(paramObj.toJSONString(), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 添加临时车辆费用明细
     * 计算停车费用并创建费用明细记录
     *
     * @param paramInJson       输入参数JSON对象
     * @param dataFlowContext   数据流上下文对象
     */
    public void addFeeTempDetail(JSONObject paramInJson, ICmdDataFlowContext dataFlowContext) {
        // 创建费用明细业务对象
        JSONObject businessFeeDetail = new JSONObject();
        businessFeeDetail.putAll(paramInJson);
        businessFeeDetail.put("detailId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_detailId));
        businessFeeDetail.put("primeRate", paramInJson.getString("primeRate"));
        
        // 设置默认状态为1400（待支付）
        if (!businessFeeDetail.containsKey("state")) {
            businessFeeDetail.put("state", "1400");
        }
        
        // 查询费用信息
        FeeDto feeDto = new FeeDto();
        feeDto.setFeeId(paramInJson.getString("feeId"));
        feeDto.setCommunityId(paramInJson.getString("communityId"));
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
        
        // 验证费用信息查询结果
        if (feeDtos == null || feeDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "查询费用信息失败，未查到数据或查到多条数据");
        }
        
        feeDto = feeDtos.get(0);
        paramInJson.put("feeInfo", feeDto);
        
        // 查询费用配置信息
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setFeeTypeCd(feeDto.getFeeTypeCd());
        feeConfigDto.setConfigId(feeDto.getConfigId());
        feeConfigDto.setCommunityId(feeDto.getCommunityId());
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        
        // 验证费用配置信息查询结果
        if (feeConfigDtos == null || feeConfigDtos.size() != 1) {
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "未查到费用配置信息，查询多条数据");
        }
        
        feeConfigDto = feeConfigDtos.get(0);
        Date nowTime = new Date();

        // 计算停车时长（毫秒）
        long diff = nowTime.getTime() - feeDto.getStartTime().getTime();
        long nd = 1000 * 24 * 60 * 60; // 一天的毫秒数
        long nh = 1000 * 60 * 60;      // 一小时的毫秒数
        long nm = 1000 * 60;           // 一分钟的毫秒数
        
        double day = 0;
        double hour = 0;
        double min = 0;
        
        // 计算天数、小时数、分钟数
        day = diff / nd;                // 计算差多少天
        hour = diff % nd / nh + day * 24; // 计算差多少小时
        min = diff % nd % nh / nm + day * 24 * 60; // 计算差多少分钟
        
        double money = 0.00;
        double newHour = hour;
        
        // 不足1小时按1小时计算
        if (min > 0) {
            newHour += 1;
        }
        
        // 计算停车费用
        if (newHour <= 2) {
            // 2小时内按附加金额收费
            money = Double.parseDouble(feeConfigDto.getAdditionalAmount());
        } else {
            // 超过2小时，按单价计算超出部分的费用
            BigDecimal lastHour = new BigDecimal(newHour - 2);
            BigDecimal squarePrice = new BigDecimal(Double.parseDouble(feeDto.getSquarePrice()));
            BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(feeDto.getAdditionalAmount()));
            money = squarePrice.multiply(lastHour).add(additionalAmount).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
        }
        
        double receivableAmount = money;
        businessFeeDetail.put("receivableAmount", receivableAmount);
        
        // 创建费用明细PO对象并保存
        PayFeeDetailPo payFeeDetailPo = BeanConvertUtil.covertBean(businessFeeDetail, PayFeeDetailPo.class);
        payFeeDetailPo.setPayOrderId(payFeeDetailPo.getDetailId());
        payFeeDetailPo.setCashierId("-1");
        payFeeDetailPo.setCashierName("系统收银");
        int flag = payFeeDetailV1InnerServiceSMOImpl.savePayFeeDetailNew(payFeeDetailPo);

        // 验证保存结果
        if (flag < 1) {
            throw new CmdException("保存明细失败");
        }
        
        paramInJson.put("receivableAmount", receivableAmount);
    }

    /**
     * 修改临时费用信息
     * 更新费用信息的结束时间和金额，并将状态设置为已支付
     *
     * @param paramInJson       输入参数JSON对象
     * @param dataFlowContext   数据流上下文对象
     */
    public void modifyTempFee(JSONObject paramInJson, ICmdDataFlowContext dataFlowContext) {
        // 创建费用业务对象
        JSONObject businessFee = new JSONObject();
        FeeDto feeInfo = (FeeDto) paramInJson.get("feeInfo");
        Map feeMap = BeanConvertUtil.beanCovertMap(feeInfo);
        
        // 设置费用结束时间为当前时间
        feeMap.put("startTime", DateUtil.getFormatTimeString(feeInfo.getStartTime(), DateUtil.DATE_FORMATE_STRING_A));
        feeMap.put("endTime", DateUtil.getFormatTimeString(new Date(), DateUtil.DATE_FORMATE_STRING_A));
        feeMap.put("amount", paramInJson.getString("receivableAmount"));
        feeMap.put("state", "2009001"); // 设置状态为已支付
        
        businessFee.putAll(feeMap);
        
        // 转换为PO对象并更新费用信息
        PayFeePo payFeePo = BeanConvertUtil.covertBean(businessFee, PayFeePo.class);
        int flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
        
        // 验证更新结果
        if (flag < 1) {
            throw new CmdException("保存失败");
        }
    }

    /**
     * 修改临时车辆进出记录状态
     * 更新车辆进出记录的状态为已出场
     *
     * @param reqJson   请求参数JSON对象
     * @param context   数据流上下文对象
     */
    public void modifyTempCarInout(JSONObject reqJson, ICmdDataFlowContext context) {
        // 获取费用信息
        FeeDto feeDto = (FeeDto) reqJson.get("feeInfo");
        
        // 查询车辆进出记录
        CarInoutDto tempCarInoutDto = new CarInoutDto();
        tempCarInoutDto.setCommunityId(reqJson.getString("communityId"));
        tempCarInoutDto.setInoutId(feeDto.getPayerObjId());
        List<CarInoutDto> carInoutDtos = carInoutInnerServiceSMOImpl.queryCarInouts(tempCarInoutDto);
        
        // 验证查询结果
        Assert.listOnlyOne(carInoutDtos, "根据费用信息反差车辆进场记录未查到 或查到多条");
        
        CarInoutDto carInoutDto = carInoutDtos.get(0);

        // 创建车辆进出业务对象
        JSONObject businessCarInout = new JSONObject();
        businessCarInout.putAll(BeanConvertUtil.beanCovertMap(carInoutDto));
        businessCarInout.put("state", "100400"); // 设置状态为已出场
        
        // 转换为PO对象并更新车辆进出记录
        CarInoutPo carInoutPo = BeanConvertUtil.covertBean(businessCarInout, CarInoutPo.class);
        int flag = carInoutV1InnerServiceSMOImpl.updateCarInout(carInoutPo);
        
        // 验证更新结果
        if (flag < 1) {
            throw new CmdException("保存失败");
        }
    }
}