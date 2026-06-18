package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.intf.community.IParkingAreaInnerServiceSMO;
import com.java110.intf.fee.IPayFeeConfigV1InnerServiceSMO;
import com.java110.intf.fee.ITempCarFeeConfigAttrV1InnerServiceSMO;
import com.java110.intf.fee.ITempCarFeeConfigV1InnerServiceSMO;
import com.java110.po.fee.PayFeeConfigPo;
import com.java110.po.tempCarFee.TempCarFeeConfigPo;
import com.java110.po.tempCarFee.TempCarFeeConfigAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 保存临时车辆收费配置命令类
 * 用于处理临时车辆收费配置的创建和保存操作，包括费用配置和属性配置的保存
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "fee.saveTempCarFeeConfig")
public class SaveTempCarFeeConfigCmd extends Cmd {

    @Autowired
    private IParkingAreaInnerServiceSMO parkingAreaInnerServiceSMOImpl;

    @Autowired
    private IPayFeeConfigV1InnerServiceSMO payFeeConfigV1InnerServiceSMOImpl;

    @Autowired
    private ITempCarFeeConfigV1InnerServiceSMO tempCarFeeConfigV1InnerServiceSMOImpl;

    @Autowired
    private ITempCarFeeConfigAttrV1InnerServiceSMO tempCarFeeConfigAttrV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 检查必要的参数是否存在，并验证停车场的有效性
     *
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "feeName", "请求报文中未包含feeName");
        Assert.hasKeyAndValue(reqJson, "paId", "请求报文中未包含paId");
        Assert.hasKeyAndValue(reqJson, "carType", "请求报文中未包含carType");
        Assert.hasKeyAndValue(reqJson, "ruleId", "请求报文中未包含ruleId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "startTime", "请求报文中未包含startTime");
        Assert.hasKeyAndValue(reqJson, "endTime", "请求报文中未包含endTime");
        
        // 查询停车场信息并验证其存在性
        ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
        parkingAreaDto.setPaId(reqJson.getString("paId"));
        parkingAreaDto.setCommunityId(reqJson.getString("communityId"));
        List<ParkingAreaDto> parkingAreaDtos = parkingAreaInnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);
        
        // 验证停车场存在且唯一
        Assert.listOnlyOne(parkingAreaDtos, "停车场不存在");
        
        // 将停车场编号添加到请求参数中
        reqJson.put("areaNum", parkingAreaDtos.get(0).getNum());
    }

    /**
     * 执行保存临时车辆收费配置的主要业务逻辑
     * 包括生成配置ID、保存临时收费配置、保存费用配置等操作
     *
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 生成费用配置ID和临时收费配置ID
        reqJson.put("feeConfigId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_configId));
        reqJson.put("configId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_configId));
        
        // 转换请求参数为临时收费配置PO对象
        TempCarFeeConfigPo tempCarFeeConfigPo = BeanConvertUtil.covertBean(reqJson, TempCarFeeConfigPo.class);
        
        // 处理临时收费配置的属性
        dealAttr(reqJson, context);
        
        // 保存临时收费配置
        int flag = tempCarFeeConfigV1InnerServiceSMOImpl.saveTempCarFeeConfig(tempCarFeeConfigPo);
        if (flag < 1) {
            throw new CmdException("保存临时收费失败");
        }
        
        // 创建并配置费用配置对象
        PayFeeConfigPo payFeeConfigPo = new PayFeeConfigPo();
        // 设置费用配置的基本属性
        payFeeConfigPo.setAdditionalAmount("0");  // 附加金额设为0
        payFeeConfigPo.setBillType(FeeConfigDto.BILL_TYPE_YEAR);  // 账单类型为年账单
        payFeeConfigPo.setCommunityId(reqJson.getString("communityId"));  // 小区ID
        payFeeConfigPo.setComputingFormula(FeeConfigDto.COMPUTING_FORMULA_DYNAMIC);  // 计算公式为动态计算
        payFeeConfigPo.setComputingFormulaText("");  // 计算公式文本为空
        payFeeConfigPo.setConfigId(reqJson.getString("feeConfigId"));  // 配置ID
        payFeeConfigPo.setEndTime(reqJson.getString("endTime"));  // 结束时间
        payFeeConfigPo.setStartTime(reqJson.getString("startTime"));  // 开始时间
        payFeeConfigPo.setFeeFlag(FeeDto.FEE_FLAG_ONCE);  // 费用标志为一次性费用
        payFeeConfigPo.setFeeName(reqJson.getString("feeName"));  // 费用名称
        payFeeConfigPo.setFeeTypeCd(FeeConfigDto.FEE_TYPE_CD_PARKING);  // 费用类型为停车费
        payFeeConfigPo.setIsDefault(FeeConfigDto.DEFAULT_FEE_CONFIG);  // 是否为默认配置
        payFeeConfigPo.setPaymentCd(FeeConfigDto.PAYMENT_CD_PRE);  // 支付方式为预支付
        payFeeConfigPo.setPaymentCycle("1");  // 支付周期为1
        payFeeConfigPo.setSquarePrice("0");  // 单价为0
        payFeeConfigPo.setDeductFrom(FeeConfigDto.DEDUCT_FROM_N);  // 扣费来源为N
        payFeeConfigPo.setDecimalPlace("2");  // 小数位数为2
        payFeeConfigPo.setScale("1");  // 比例为1
        payFeeConfigPo.setUnits("元");  // 单位为元
        payFeeConfigPo.setPayOnline("Y");  // 支持在线支付
        
        // 保存费用配置
        flag = payFeeConfigV1InnerServiceSMOImpl.savePayFeeConfig(payFeeConfigPo);
        if (flag < 1) {
            throw new CmdException("保存临时收费失败");
        }
    }

    /**
     * 处理临时收费配置的属性信息
     * 遍历属性数组，为每个属性生成ID并保存到数据库
     *
     * @param reqJson 请求的JSON数据对象，包含属性信息
     * @param context 数据流上下文对象
     * @throws CmdException 当属性保存失败时抛出异常
     */
    private void dealAttr(JSONObject reqJson, ICmdDataFlowContext context) {
        // 检查请求中是否包含属性信息
        if (!reqJson.containsKey("attrs")) {
            return;
        }
        
        // 获取属性数组
        JSONArray attrs = reqJson.getJSONArray("attrs");
        if (attrs == null || attrs.size() < 1) {
            return;
        }
        
        JSONObject attr = null;
        int flag = 0;
        // 遍历所有属性
        for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
            attr = attrs.getJSONObject(attrIndex);
            // 设置属性的必要信息
            attr.put("configId", reqJson.getString("configId"));  // 配置ID
            attr.put("communityId", reqJson.getString("communityId"));  // 小区ID
            attr.put("attrId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));  // 生成属性ID
            
            // 转换属性对象并保存
            TempCarFeeConfigAttrPo tempCarFeeConfigAttrPo = BeanConvertUtil.covertBean(attr, TempCarFeeConfigAttrPo.class);
            flag = tempCarFeeConfigAttrV1InnerServiceSMOImpl.saveTempCarFeeConfigAttr(tempCarFeeConfigAttrPo);
            
            // 检查保存是否成功
            if (flag < 1) {
                throw new CmdException("保存临时收费失败");
            }
        }
    }
}