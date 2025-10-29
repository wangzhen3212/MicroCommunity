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
import com.java110.dto.fee.TempCarFeeConfigDto;
import com.java110.intf.fee.*;
import com.java110.po.fee.PayFeeConfigPo;
import com.java110.po.tempCarFee.TempCarFeeConfigPo;
import com.java110.po.tempCarFee.TempCarFeeConfigAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 更新临时车费用配置命令类
 * 
 * 该类负责处理临时车费用配置的更新操作，包括验证请求参数、更新临时车费用配置信息、
 * 更新相关属性信息以及同步更新费用项配置数据
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "fee.updateTempCarFeeConfig")
public class UpdateTempCarFeeConfigCmd extends Cmd {

    @Autowired
    private ITempCarFeeConfigInnerServiceSMO tempCarFeeConfigInnerServiceSMOImpl;

    @Autowired
    private ITempCarFeeConfigV1InnerServiceSMO tempCarFeeConfigV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeConfigV1InnerServiceSMO payFeeConfigV1InnerServiceSMOImpl;

    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    @Autowired
    private ITempCarFeeConfigAttrV1InnerServiceSMO tempCarFeeConfigAttrV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 检查请求JSON中是否包含必要的参数，确保请求数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流信息
     * @param reqJson 请求JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "configId", "configId不能为空");
        Assert.hasKeyAndValue(reqJson, "feeName", "请求报文中未包含feeName");
        Assert.hasKeyAndValue(reqJson, "paId", "请求报文中未包含paId");
        Assert.hasKeyAndValue(reqJson, "carType", "请求报文中未包含carType");
        Assert.hasKeyAndValue(reqJson, "ruleId", "请求报文中未包含ruleId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "startTime", "请求报文中未包含startTime");
        Assert.hasKeyAndValue(reqJson, "endTime", "请求报文中未包含endTime");
    }

    /**
     * 执行更新临时车费用配置命令
     * 
     * 该方法负责主要的业务逻辑处理，包括查询验证、更新临时车费用配置、
     * 更新属性信息和同步更新费用项配置
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 查询并验证费用配置信息
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigId(reqJson.getString("feeConfigId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        Assert.listOnlyOne(feeConfigDtos, "查询费用项错误！");
        
        // 查询并验证临时车费用配置信息
        TempCarFeeConfigDto tempCarFeeConfigDto = new TempCarFeeConfigDto();
        tempCarFeeConfigDto.setConfigId(reqJson.getString("configId"));
        tempCarFeeConfigDto.setCommunityId(reqJson.getString("communityId"));
        List<TempCarFeeConfigDto> tempCarFeeConfigDtos = tempCarFeeConfigInnerServiceSMOImpl.queryTempCarFeeConfigs(tempCarFeeConfigDto);
        Assert.listOnlyOne(tempCarFeeConfigDtos, "临时车收费标准不存在");
        
        // 转换并更新临时车费用配置
        TempCarFeeConfigPo tempCarFeeConfigPo = BeanConvertUtil.covertBean(reqJson, TempCarFeeConfigPo.class);
        updateAttr(reqJson);
        int flag = tempCarFeeConfigV1InnerServiceSMOImpl.updateTempCarFeeConfig(tempCarFeeConfigPo);
        if (flag < 1) {
            throw new CmdException("修改临时车费用配置失败");
        }
        
        // 补全并更新费用项配置数据
        PayFeeConfigPo payFeeConfigPo = new PayFeeConfigPo();
        payFeeConfigPo.setCommunityId(reqJson.getString("communityId"));
        payFeeConfigPo.setConfigId(tempCarFeeConfigDtos.get(0).getFeeConfigId());
        payFeeConfigPo.setEndTime(reqJson.getString("endTime"));
        payFeeConfigPo.setStartTime(reqJson.getString("startTime"));
        payFeeConfigPo.setFeeName(reqJson.getString("feeName"));
        updateFeeConfig(BeanConvertUtil.beanCovertJson(payFeeConfigPo), context);
    }

    /**
     * 更新临时车费用配置属性
     * 
     * 处理属性列表的更新操作，包括新增属性和修改现有属性
     * 
     * @param reqJson 请求JSON对象，包含属性列表信息
     * @throws CmdException 当属性更新失败时抛出异常
     */
    private void updateAttr(JSONObject reqJson) {
        int flag;
        // 获取属性列表
        JSONArray attrs = reqJson.getJSONArray("attrs");
        if (attrs == null || attrs.size() < 1) {
            return; // 没有属性需要更新，直接返回
        }
        
        JSONObject attr = null;
        // 遍历所有属性进行处理
        for (int attrIndex = 0; attrIndex < attrs.size(); attrIndex++) {
            attr = attrs.getJSONObject(attrIndex);
            // 补充属性信息
            attr.put("ruleId", reqJson.getString("ruleId"));
            attr.put("communityId", reqJson.getString("communityId"));
            
            // 判断是否为新增属性（attrId为空或以"-"开头）
            if (!attr.containsKey("attrId") || attr.getString("attrId").startsWith("-") || StringUtil.isEmpty(attr.getString("attrId"))) {
                // 生成新的属性ID并保存新属性
                attr.put("attrId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                TempCarFeeConfigAttrPo tempCarFeeConfigAttrPo = BeanConvertUtil.covertBean(attr, TempCarFeeConfigAttrPo.class);
                flag = tempCarFeeConfigAttrV1InnerServiceSMOImpl.saveTempCarFeeConfigAttr(tempCarFeeConfigAttrPo);
                if (flag < 1) {
                    throw new CmdException("修改属性失败");
                }
                continue; // 处理下一个属性
            }
            
            // 更新现有属性
            TempCarFeeConfigAttrPo tempCarFeeConfigAttrPo = BeanConvertUtil.covertBean(attr, TempCarFeeConfigAttrPo.class);
            flag = tempCarFeeConfigAttrV1InnerServiceSMOImpl.updateTempCarFeeConfigAttr(tempCarFeeConfigAttrPo);
            if (flag < 1) {
                throw new CmdException("修改属性失败");
            }
        }
    }

    /**
     * 更新费用项配置
     * 
     * 根据参数更新费用项配置信息，确保费用项数据与临时车费用配置保持一致
     * 
     * @param paramInJson 包含费用项配置参数的JSON对象
     * @param dataFlowContext 数据流上下文对象
     * @throws CmdException 当费用项更新失败时抛出异常
     */
    public void updateFeeConfig(JSONObject paramInJson, ICmdDataFlowContext dataFlowContext) {
        // 查询并验证费用项配置
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(paramInJson.getString("communityId"));
        feeConfigDto.setConfigId(paramInJson.getString("configId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        Assert.listOnlyOne(feeConfigDtos, "未找到该费用项");
        
        // 构建费用项配置数据
        JSONObject businessFeeConfig = new JSONObject();
        businessFeeConfig.putAll(paramInJson);
        businessFeeConfig.put("isDefault", feeConfigDtos.get(0).getIsDefault()); // 保持原有的默认设置
        
        // 转换并更新费用项配置
        PayFeeConfigPo payFeeConfigPo = BeanConvertUtil.covertBean(businessFeeConfig, PayFeeConfigPo.class);
        int flag = payFeeConfigV1InnerServiceSMOImpl.updatePayFeeConfig(payFeeConfigPo);
        if (flag < 1) {
            throw new CmdException("修改费用项失败");
        }
    }
}