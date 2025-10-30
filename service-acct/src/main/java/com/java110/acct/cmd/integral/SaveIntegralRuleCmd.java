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
import com.java110.intf.acct.IIntegralRuleV1InnerServiceSMO;
import com.java110.po.integral.IntegralRulePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存积分规则命令类
 * 服务编码：integralRule.saveIntegralRule
 * 请求路劲：/app/integralRule.SaveIntegralRule
 * add by 吴学文 at 2022-12-11 22:09:01 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * @description 积分规则保存命令处理类，负责处理积分规则的创建和保存操作
 * @author 吴学文
 * @date 2022-12-11
 * @version 1.0
 */
@Java110Cmd(serviceCode = "integral.saveIntegralRule")
public class SaveIntegralRuleCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveIntegralRuleCmd.class);

    /**
     * 积分规则ID前缀，用于生成唯一规则ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 积分规则服务接口，用于操作积分规则数据
     */
    @Autowired
    private IIntegralRuleV1InnerServiceSMO integralRuleV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * @description 验证请求参数是否完整，确保必要的参数存在且有效
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含规则名称
        Assert.hasKeyAndValue(reqJson, "ruleName", "请求报文中未包含ruleName");
        // 验证请求报文中必须包含小区ID
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行保存积分规则命令
     * 
     * @description 处理积分规则的保存逻辑，包括数据转换、ID生成、数据持久化等操作
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为积分规则POJO对象
        IntegralRulePo integralRulePo = BeanConvertUtil.covertBean(reqJson, IntegralRulePo.class);
        
        // 生成唯一的积分规则ID
        integralRulePo.setRuleId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存积分规则数据
        int flag = integralRuleV1InnerServiceSMOImpl.saveIntegralRule(integralRulePo);

        // 检查保存操作是否成功，flag小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}