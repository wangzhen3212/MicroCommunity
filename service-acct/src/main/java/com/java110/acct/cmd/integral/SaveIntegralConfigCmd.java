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
import com.java110.intf.acct.IIntegralConfigV1InnerServiceSMO;
import com.java110.po.integral.IntegralConfigPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存积分配置信息
 * 服务编码：integralConfig.saveIntegralConfig
 * 请求路劲：/app/integralConfig.SaveIntegralConfig
 * add by 吴学文 at 2022-12-11 22:02:59 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 积分配置保存命令类
 * 负责处理积分配置信息的保存操作，包括参数验证和业务逻辑处理
 * 继承自Cmd基类，实现命令模式处理HTTP请求
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-12-11
 */
@Java110Cmd(serviceCode = "integral.saveIntegralConfig")
public class SaveIntegralConfigCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveIntegralConfigCmd.class);

    /**
     * 配置ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 积分配置服务接口
     */
    @Autowired
    private IIntegralConfigV1InnerServiceSMO integralConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证单价参数是否存在
        Assert.hasKeyAndValue(reqJson, "squarePrice", "请求报文中未包含squarePrice");
        // 验证附加金额参数是否存在
        Assert.hasKeyAndValue(reqJson, "additionalAmount", "请求报文中未包含additionalAmount");
        // 验证配置名称参数是否存在
        Assert.hasKeyAndValue(reqJson, "configName", "请求报文中未包含configName");
        // 验证计算公式参数是否存在
        Assert.hasKeyAndValue(reqJson, "computingFormula", "请求报文中未包含computingFormula");
        // 验证比例参数是否存在
        Assert.hasKeyAndValue(reqJson, "scale", "请求报文中未包含scale");
    }

    /**
     * 执行保存积分配置命令
     * 将请求参数转换为积分配置对象并保存到数据库
     * 使用事务保证数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为积分配置POJO对象
        IntegralConfigPo integralConfigPo = BeanConvertUtil.covertBean(reqJson, IntegralConfigPo.class);
        
        // 生成唯一的配置ID
        integralConfigPo.setConfigId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存积分配置信息
        int flag = integralConfigV1InnerServiceSMOImpl.saveIntegralConfig(integralConfigPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}