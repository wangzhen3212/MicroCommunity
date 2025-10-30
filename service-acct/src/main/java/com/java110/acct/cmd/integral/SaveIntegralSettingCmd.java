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
import com.java110.intf.acct.IIntegralSettingV1InnerServiceSMO;
import com.java110.po.integral.IntegralSettingPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存积分设置信息命令类
 * 服务编码：integralSetting.saveIntegralSetting
 * 请求路劲：/app/integralSetting.SaveIntegralSetting
 * 功能描述：处理积分设置信息的保存操作，包括参数验证和数据持久化
 * add by 吴学文 at 2022-12-13 11:29:13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "integral.saveIntegralSetting")
public class SaveIntegralSettingCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveIntegralSettingCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 积分设置服务接口
     */
    @Autowired
    private IIntegralSettingV1InnerServiceSMO integralSettingV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否完整，确保必要的参数都存在
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证社区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证金额参数是否存在
        Assert.hasKeyAndValue(reqJson, "money", "请求报文中未包含money");
        // 验证备注参数是否存在
        Assert.hasKeyAndValue(reqJson, "remark", "请求报文中未包含remark");
        // 验证第三方标识参数是否存在
        Assert.hasKeyAndValue(reqJson, "thirdFlag", "请求报文中未包含thirdFlag");
    }

    /**
     * 执行保存积分设置命令
     * 功能描述：将请求参数转换为积分设置对象，生成唯一ID并保存到数据库
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为积分设置POJO对象
        IntegralSettingPo integralSettingPo = BeanConvertUtil.covertBean(reqJson, IntegralSettingPo.class);
        
        // 生成积分设置ID，使用前缀+自增序列的方式
        integralSettingPo.setSettingId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存积分设置信息
        int flag = integralSettingV1InnerServiceSMOImpl.saveIntegralSetting(integralSettingPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}