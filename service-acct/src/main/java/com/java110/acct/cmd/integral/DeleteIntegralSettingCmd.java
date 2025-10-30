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
 * 类表述：删除积分设置命令类
 * 服务编码：integralSetting.deleteIntegralSetting
 * 请求路劲：/app/integralSetting.DeleteIntegralSetting
 * add by 吴学文 at 2022-12-13 11:29:13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * @description 删除积分设置命令处理类，负责处理删除积分设置的业务逻辑
 * @serviceCode integral.deleteIntegralSetting
 * @author 吴学文
 * @version 1.0
 * @since 2022-12-13
 */
@Java110Cmd(serviceCode = "integral.deleteIntegralSetting")
public class DeleteIntegralSettingCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteIntegralSettingCmd.class);

    /**
     * 积分设置服务接口
     */
    @Autowired
    private IIntegralSettingV1InnerServiceSMO integralSettingV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * @description 验证删除积分设置请求的必要参数是否完整
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象，包含前端传递的所有参数
     * @throws CmdException 当参数校验失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验settingId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "settingId", "settingId不能为空");
        // 校验communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * @description 执行删除积分设置的业务逻辑，包括数据转换、服务调用和结果处理
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为积分设置PO对象
        IntegralSettingPo integralSettingPo = BeanConvertUtil.covertBean(reqJson, IntegralSettingPo.class);
        
        // 调用积分设置服务删除数据
        int flag = integralSettingV1InnerServiceSMOImpl.deleteIntegralSetting(integralSettingPo);

        // 检查删除操作是否成功，flag表示受影响的行数
        if (flag < 1) {
            // 删除失败时抛出异常
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}