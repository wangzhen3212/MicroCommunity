/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.acct.cmd.integral;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.integral.IntegralRuleConfigDto;
import com.java110.intf.acct.IIntegralRuleConfigV1InnerServiceSMO;
import com.java110.po.integral.IntegralRuleConfigPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存积分规则配置命令类
 * 服务编码：integralRuleConfig.saveIntegralRuleConfig
 * 请求路径：/app/integralRuleConfig.SaveIntegralRuleConfig
 * 
 * 功能描述：处理积分规则配置的保存操作，包括参数验证和业务逻辑处理
 * 主要职责：
 * 1. 验证请求参数完整性
 * 2. 检查积分规则配置是否已存在
 * 3. 保存积分规则配置信息
 * 
 * add by 吴学文 at 2022-12-11 22:11:51 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "integral.saveIntegralRuleConfig")
public class SaveIntegralRuleConfigCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveIntegralRuleConfigCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 积分规则配置服务接口
     */
    @Autowired
    private IIntegralRuleConfigV1InnerServiceSMO integralRuleConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能描述：验证请求参数的完整性和业务规则的合法性
     * 验证内容：
     * 1. 检查必填参数是否存在
     * 2. 检查积分规则配置是否已存在，避免重复配置
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败或业务规则不满足时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "ruleId", "请求报文中未包含ruleId");
        Assert.hasKeyAndValue(reqJson, "configId", "请求报文中未包含configId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        
        // 查询规则是否已经关联过优惠券，避免重复配置
        IntegralRuleConfigDto integralRuleConfigDto = new IntegralRuleConfigDto();
        integralRuleConfigDto.setRuleId(reqJson.getString("ruleId"));  // 设置规则ID
        integralRuleConfigDto.setConfigId(reqJson.getString("configId"));  // 设置配置ID
        
        // 查询已存在的配置数量
        int count = integralRuleConfigV1InnerServiceSMOImpl.queryIntegralRuleConfigsCount(integralRuleConfigDto);

        // 如果已存在相同配置，抛出异常
        if(count > 0){
            throw new CmdException("该积分标准已经关联过");
        }
    }

    /**
     * 执行命令方法
     * 
     * 功能描述：执行积分规则配置的保存操作
     * 主要步骤：
     * 1. 将请求参数转换为持久化对象
     * 2. 生成唯一ID
     * 3. 调用服务保存数据
     * 4. 处理保存结果并设置响应
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为积分规则配置持久化对象
        IntegralRuleConfigPo integralRuleConfigPo = BeanConvertUtil.covertBean(reqJson, IntegralRuleConfigPo.class);
        
        // 生成唯一ID
        integralRuleConfigPo.setIrcId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存积分规则配置
        int flag = integralRuleConfigV1InnerServiceSMOImpl.saveIntegralRuleConfig(integralRuleConfigPo);

        // 检查保存结果，如果保存失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}