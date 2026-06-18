/**
 * 费用配置更新命令类
 * 
 * 该类负责处理费用配置的更新操作，包括费用项信息的修改、费用状态的变更以及相关业务逻辑处理
 * 通过注解@Java110Cmd声明服务代码为"feeConfig.updateFeeConfig"
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.feeConfig;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.payFee.PayFeeRuleDto;
import com.java110.intf.fee.IFeeConfigInnerServiceSMO;
import com.java110.intf.fee.IPayFeeConfigV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeRuleV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.po.fee.PayFeeConfigPo;
import com.java110.po.fee.PayFeePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "feeConfig.updateFeeConfig")
public class UpdateFeeConfigCmd extends Cmd {

    /** 费用配置V1版本内部服务 */
    @Autowired
    private IPayFeeConfigV1InnerServiceSMO payFeeConfigV1InnerServiceSMOImpl;

    /** 费用配置内部服务 */
    @Autowired
    private IFeeConfigInnerServiceSMO feeConfigInnerServiceSMOImpl;

    /** 缴费规则V1版本内部服务 */
    @Autowired
    private IPayFeeRuleV1InnerServiceSMO payFeeRuleV1InnerServiceSMOImpl;

    /** 缴费V1版本内部服务 */
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效，确保必要的参数都存在且不为空
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "configId", "费用项ID不能为空");
        Assert.hasKeyAndValue(reqJson, "feeTypeCd", "必填，请选择费用类型");
        Assert.hasKeyAndValue(reqJson, "feeName", "必填，请填写收费项目");
        Assert.hasKeyAndValue(reqJson, "feeFlag", "必填，请选择费用标识");
        Assert.hasKeyAndValue(reqJson, "startTime", "必填，请选择计费起始时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "必填，请选择计费终止时间");
        Assert.hasKeyAndValue(reqJson, "computingFormula", "必填，请填写附加费用");
        Assert.hasKeyAndValue(reqJson, "squarePrice", "必填，请填写计费单价");
        Assert.hasKeyAndValue(reqJson, "additionalAmount", "必填，请填写附加费用");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "billType", "必填，请填写出账类型");
    }

    /**
     * 执行费用配置更新命令
     * 
     * 主要业务逻辑包括：
     * 1. 查询并验证费用配置信息
     * 2. 更新费用配置数据
     * 3. 处理费用结束逻辑
     * 4. 更新费用标识
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 构建查询条件，查询费用配置信息
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));
        feeConfigDto.setConfigId(reqJson.getString("configId"));
        List<FeeConfigDto> feeConfigDtos = feeConfigInnerServiceSMOImpl.queryFeeConfigs(feeConfigDto);
        
        // 验证查询结果，确保只找到一条记录
        Assert.listOnlyOne(feeConfigDtos, "未找到该费用项");
        
        // 准备业务数据，复制请求参数并设置默认值
        JSONObject businessFeeConfig = new JSONObject();
        businessFeeConfig.putAll(reqJson);
        businessFeeConfig.put("isDefault", feeConfigDtos.get(0).getIsDefault());
        
        // 转换数据对象
        PayFeeConfigPo payFeeConfigPo = BeanConvertUtil.covertBean(businessFeeConfig, PayFeeConfigPo.class);
        
        // 处理状态字段的特殊逻辑：如果状态为"NA"，则转换为"N"
        if("NA".equals(payFeeConfigPo.getState())){
            payFeeConfigPo.setState("N");
        }
        
        // 执行费用配置更新操作
        int flag = payFeeConfigV1InnerServiceSMOImpl.updatePayFeeConfig(payFeeConfigPo);
        if (flag < 1) {
            throw new CmdException("修改费用项失败");
        }
        
        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());

        // 处理费用结束逻辑
        finishFee(reqJson, feeConfigDtos);

        // 检查是否需要更新费用标识
        if (!reqJson.containsKey("feeFlag")) {
            return;
        }
        String feeFlag = reqJson.getString("feeFlag");
        
        // 如果费用标识没有变化，则直接返回
        if (feeFlag.equals(feeConfigDtos.get(0).getFeeFlag())) {
            return;
        }

        // 检查是否为账单模式：查询缴费规则数量，如果有规则数据则不允许修改
        PayFeeRuleDto payFeeRuleDto = new PayFeeRuleDto();
        payFeeRuleDto.setConfigId(feeConfigDtos.get(0).getConfigId());
        payFeeRuleDto.setCommunityId(reqJson.getString("communityId"));
        int count = payFeeRuleV1InnerServiceSMOImpl.queryPayFeeRulesCount(payFeeRuleDto);
        if (count > 0) {
            return; // 存在缴费规则，不允许修改费用标识
        }

        // 更新费用标识
        PayFeePo payFeePo = new PayFeePo();
        payFeePo.setConfigId(feeConfigDtos.get(0).getConfigId());
        payFeePo.setFeeFlag(reqJson.getString("feeFlag"));

        payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
    }

    /**
     * 结束费用方法
     * 
     * 当费用状态变更为"NA"时，将对应的费用状态更新为结束状态
     * 
     * @param reqJson 请求参数JSON对象，包含状态信息
     * @param feeConfigDtos 费用配置数据列表
     */
    private void finishFee(JSONObject reqJson, List<FeeConfigDto> feeConfigDtos) {
        // 获取状态参数
        String state = reqJson.getString("state");
        
        // 只有当状态为"NA"时才执行结束操作
        if(!"NA".equals(state)){
            return;
        }
        
        // 构建费用更新对象
        PayFeePo payFeePo = new PayFeePo();
        payFeePo.setConfigId(feeConfigDtos.get(0).getConfigId());
        payFeePo.setState(FeeDto.STATE_FINISH); // 设置为结束状态

        // 执行费用状态更新
        payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
    }
}