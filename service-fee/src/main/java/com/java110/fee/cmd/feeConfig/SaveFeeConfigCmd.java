/**
 * 费用配置保存命令类
 * 
 * 该类负责处理费用配置的保存操作，包括参数验证和业务逻辑处理
 * 通过注解@Java110Cmd声明服务代码为"feeConfig.saveFeeConfig"
 * 继承自Cmd基类，实现命令模式的处理流程
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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.intf.fee.IPayFeeConfigV1InnerServiceSMO;
import com.java110.po.fee.PayFeeConfigPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

@Java110Cmd(serviceCode = "feeConfig.saveFeeConfig")
public class SaveFeeConfigCmd extends Cmd {

    /**
     * 费用配置服务接口，用于操作费用配置数据
     */
    @Autowired
    private IPayFeeConfigV1InnerServiceSMO payFeeConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效，包括必填字段检查和业务规则验证
     * 主要验证费用名称在小区内的唯一性
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败或业务规则不满足时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "feeTypeCd", "必填，请选择费用类型");
        Assert.hasKeyAndValue(reqJson, "feeName", "必填，请填写收费项目");
        Assert.hasKeyAndValue(reqJson, "feeFlag", "必填，请选择费用标识");
        Assert.hasKeyAndValue(reqJson, "startTime", "必填，请选择计费起始时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "必填，请选择计费终止时间");
        Assert.hasKeyAndValue(reqJson, "computingFormula", "必填，请填写附加费用");
        Assert.hasKeyAndValue(reqJson, "squarePrice", "必填，请填写计费单价");
        Assert.hasKeyAndValue(reqJson, "additionalAmount", "必填，请填写附加费用");
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "billType", "未包含出账类型");
        Assert.hasKeyAndValue(reqJson, "paymentCd", "付费类型不能为空");
        Assert.hasKeyAndValue(reqJson, "paymentCycle", "缴费周期不能为空");
        Assert.hasKeyAndValue(reqJson, "state", "状态不能为空");

        // 校验费用名称在小区内的唯一性
        // 避免重复的费用名称导致管理混乱
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setFeeName(reqJson.getString("feeName"));        // 设置费用名称
        feeConfigDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        feeConfigDto.setIsDefault("F");                               // 设置非默认费用标识
        
        // 查询相同费用名称在小区内的数量
        int count = payFeeConfigV1InnerServiceSMOImpl.queryPayFeeConfigsCount(feeConfigDto);

        // 如果已存在相同名称的费用配置，抛出异常
        if(count > 0){
            throw new CmdException(reqJson.getString("feeName")+"已存在");
        }

    }

    /**
     * 执行保存费用配置命令
     * 
     * 生成配置ID，转换数据对象并保存费用配置信息
     * 使用事务注解确保数据操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 生成唯一的配置ID
        reqJson.put("configId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_configId));
        // 设置费用配置为非默认配置
        reqJson.put("isDefault", "F");
        
        // 将JSON对象转换为费用配置持久化对象
        PayFeeConfigPo payFeeConfigPo = BeanConvertUtil.covertBean(reqJson, PayFeeConfigPo.class);

        // 调用服务保存费用配置，返回影响的行数
        int flag = payFeeConfigV1InnerServiceSMOImpl.savePayFeeConfig(payFeeConfigPo);

        // 检查保存是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存费用项失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}