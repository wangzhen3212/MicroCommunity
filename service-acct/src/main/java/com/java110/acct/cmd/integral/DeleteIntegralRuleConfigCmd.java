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
 * 删除积分规则配置命令类
 * 
 * 该类负责处理删除积分规则配置的请求，包括参数验证和业务逻辑执行。
 * 服务编码：integral.deleteIntegralRuleConfig
 * 请求路径：/app/integralRuleConfig.DeleteIntegralRuleConfig
 * 
 * @author 吴学文 at 2022-12-11 22:11:51 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 * @see IIntegralRuleConfigV1InnerServiceSMO
 */
@Java110Cmd(serviceCode = "integral.deleteIntegralRuleConfig")
public class DeleteIntegralRuleConfigCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteIntegralRuleConfigCmd.class);

    /**
     * 积分规则配置服务接口
     */
    @Autowired
    private IIntegralRuleConfigV1InnerServiceSMO integralRuleConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段：ircId和communityId
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含ircId字段
        Assert.hasKeyAndValue(reqJson, "ircId", "ircId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除积分规则配置命令
     * 
     * 将请求参数转换为积分规则配置PO对象，并调用服务层进行删除操作。
     * 如果删除失败，则抛出CmdException异常。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为积分规则配置PO对象
        IntegralRuleConfigPo integralRuleConfigPo = BeanConvertUtil.covertBean(reqJson, IntegralRuleConfigPo.class);
        
        // 调用服务层删除积分规则配置，返回影响的行数
        int flag = integralRuleConfigV1InnerServiceSMOImpl.deleteIntegralRuleConfig(integralRuleConfigPo);

        // 检查删除操作是否成功，如果影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}