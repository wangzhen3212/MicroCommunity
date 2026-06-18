package com.java110.user.cmd.carInout;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.machine.CarInoutDto;
import com.java110.intf.common.ICarInoutInnerServiceSMO;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.utils.constant.FeeTypeConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.vo.api.carInout.ApiCarInoutDataVo;
import com.java110.vo.api.carInout.ApiCarInoutVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 车辆进出记录查询命令类
 * 负责处理车辆进出记录的列表查询请求，包括数据验证、查询处理和费用计算等功能
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "carInout.listCarInouts")
public class ListCarInoutsCmd extends Cmd{

    private static Logger logger = LoggerFactory.getLogger(ListCarInoutsCmd.class);

    @Autowired
    private ICarInoutInnerServiceSMO carInoutInnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 验证请求参数是否包含必要的小区信息，并验证分页参数
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        // 调用父类方法验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理车辆进出记录查询请求，包括数据查询、费用计算和响应构建
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为车辆进出记录DTO对象
        CarInoutDto carInoutDto = BeanConvertUtil.covertBean(reqJson, CarInoutDto.class);

        // 处理多状态查询条件
        if (reqJson.containsKey("state") && reqJson.getString("state").contains(",")) {
            carInoutDto.setState("");
            String[] states = reqJson.getString("state").split(",");
            carInoutDto.setStates(states);
        }

        // 查询车辆进出记录总数
        int count = carInoutInnerServiceSMOImpl.queryCarInoutsCount(carInoutDto);

        List<ApiCarInoutDataVo> carInouts = null;

        if (count > 0) {
            // 查询车辆进出记录列表并转换为VO对象
            carInouts = BeanConvertUtil.covertBeanList(carInoutInnerServiceSMOImpl.queryCarInouts(carInoutDto), ApiCarInoutDataVo.class);
            // 刷新车辆进出记录数据，计算停车时间和费用
            refreshCarInout(carInouts, reqJson.getString("communityId"));
        } else {
            carInouts = new ArrayList<>();
        }

        // 构建返回VO对象
        ApiCarInoutVo apiCarInoutVo = new ApiCarInoutVo();
        apiCarInoutVo.setTotal(count);
        // 计算总页数
        apiCarInoutVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiCarInoutVo.setCarInouts(carInouts);

        // 构建响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiCarInoutVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新车辆进出记录数据
     * 为车辆进出记录计算停车时间、停车费用等信息
     *
     * @param carInouts 车辆进出记录数据列表
     * @param communityId 小区ID
     */
    private void refreshCarInout(List<ApiCarInoutDataVo> carInouts, String communityId) {
        Date nowTime = new Date();
        Date inTime = null;
        List<String> inoutIds = new ArrayList<>();
        
        // 收集状态为"100600"（临时停车中）的记录ID
        for (ApiCarInoutDataVo apiCarInoutDataVo : carInouts) {
            if ("100600".equals(apiCarInoutDataVo.getState())) {
                inoutIds.add(apiCarInoutDataVo.getInoutId());
            }
        }

        // 查询临时停车费用信息
        if (inoutIds.size() > 0) {
            FeeDto feeDto = new FeeDto();
            feeDto.setCommunityId(communityId);
            feeDto.setFeeTypeCd(FeeTypeConstant.FEE_TYPE_TEMP_DOWN_PARKING_SPACE);
            feeDto.setPayerObjIds(inoutIds.toArray(new String[inoutIds.size()]));
            feeDto.setState("2008001"); // 费用状态：有效
            feeDto.setFeeFlag("2006012"); // 费用标志：临时停车
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
            
            // 将费用信息中的入场时间设置到车辆进出记录中
            for (FeeDto tmpFeeDto : feeDtos) {
                for (ApiCarInoutDataVo apiCarInoutDataVo : carInouts) {
                    if (tmpFeeDto.getPayerObjId().equals(apiCarInoutDataVo.getInoutId())) {
                        apiCarInoutDataVo.setInTime(DateUtil.getFormatTimeString(tmpFeeDto.getStartTime(), DateUtil.DATE_FORMATE_STRING_A));
                    }
                }
            }
        }

        // 查询默认的临时停车费用配置
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(communityId);
        feeConfigDto.setIsDefault("T");
        feeConfigDto.setFeeTypeCd(FeeTypeConstant.FEE_TYPE_TEMP_DOWN_PARKING_SPACE);
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        if (feeConfigDtos == null || feeConfigDtos.size() < 1) {
            return;
        }
        FeeConfigDto tmpFeeConfigDto = feeConfigDtos.get(0);
        
        // 为每条车辆进出记录计算停车时间和费用
        for (ApiCarInoutDataVo apiCarInoutDataVo : carInouts) {
            // 跳过状态为"100500"（已出场）的记录
            if ("100500".equals(apiCarInoutDataVo.getState())) {
                continue;
            }
            
            try {
                // 解析入场时间字符串为Date对象
                inTime = DateUtil.getDateFromString(apiCarInoutDataVo.getInTime(), DateUtil.DATE_FORMATE_STRING_A);
            } catch (Exception e) {
                logger.error("格式化入场时间出错", e);
                continue;
            }
            
            // 计算停车时间差（毫秒）
            long diff = nowTime.getTime() - inTime.getTime();
            long nd = 1000 * 24 * 60 * 60; // 一天的毫秒数
            long nh = 1000 * 60 * 60;       // 一小时的毫秒数
            long nm = 1000 * 60;            // 一分钟的毫秒数
            
            double day = 0;
            double hour = 0;
            double min = 0;
            day = diff / nd;                // 计算差多少天
            hour = diff % nd / nh + day * 24; // 计算差多少小时
            min = diff % nd % nh / nm;      // 计算差多少分钟
            
            double money = 0.00;
            double newHour = hour;
            
            // 如果停车时间超过整小时，按整小时计费
            if (min > 0) {
                newHour += 1;
            }
            
            // 计算停车费用
            if (newHour <= 2) {
                // 2小时内按附加费用计算（通常为免费或基础费用）
                money = Double.parseDouble(tmpFeeConfigDto.getAdditionalAmount());
            } else {
                // 超过2小时，按单价计算费用
                BigDecimal lastHour = new BigDecimal(newHour - 2); // 超过2小时的部分
                BigDecimal squarePrice = new BigDecimal(Double.parseDouble(tmpFeeConfigDto.getSquarePrice())); // 每小时单价
                BigDecimal additionalAmount = new BigDecimal(Double.parseDouble(tmpFeeConfigDto.getAdditionalAmount())); // 基础费用
                
                // 费用计算：基础费用 + (超过小时数 × 单价)
                money = squarePrice.multiply(lastHour).add(additionalAmount).setScale(2, BigDecimal.ROUND_HALF_EVEN).doubleValue();
            }
            
            // 设置计算结果到VO对象
            apiCarInoutDataVo.setMoney(money);
            apiCarInoutDataVo.setInHours(new Double(hour).intValue());
            apiCarInoutDataVo.setInMin(new Double(min).intValue());
        }
    }
}