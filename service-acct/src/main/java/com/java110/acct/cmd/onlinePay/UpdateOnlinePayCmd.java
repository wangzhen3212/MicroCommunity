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
package com.java110.acct.cmd.onlinePay;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.acct.IOnlinePayV1InnerServiceSMO;
import com.java110.po.wechat.OnlinePayPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：更新
 * 服务编码：onlinePay.updateOnlinePay
 * 请求路劲：/app/onlinePay.UpdateOnlinePay
 * add by 吴学文 at 2021-12-21 13:05:25 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 在线支付更新命令类
 * 
 * 该类负责处理在线支付信息的更新操作，通过服务编码onlinePay.updateOnlinePay接收请求，
 * 验证请求参数的有效性，并调用相应的服务层方法更新在线支付数据。
 * 更新操作在事务中执行，确保数据的一致性。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-12-21
 */
@Java110Cmd(serviceCode = "onlinePay.updateOnlinePay")
public class UpdateOnlinePayCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateOnlinePayCmd.class);

    /**
     * 在线支付服务接口，用于调用服务层方法进行数据操作
     */
    @Autowired
    private IOnlinePayV1InnerServiceSMO onlinePayV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保请求数据的完整性。
     * 必须包含payId和orderId字段，否则会抛出参数验证异常。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含payId字段
        Assert.hasKeyAndValue(reqJson, "payId", "payId不能为空");
        // 验证请求参数中必须包含orderId字段
        Assert.hasKeyAndValue(reqJson, "orderId", "orderId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 将请求参数转换为在线支付对象，调用服务层方法更新数据。
     * 更新操作在事务中执行，确保数据一致性。
     * 如果更新失败，抛出命令异常；更新成功则设置成功响应。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当数据更新失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON对象转换为OnlinePayPo对象
        OnlinePayPo onlinePayPo = BeanConvertUtil.covertBean(reqJson, OnlinePayPo.class);
        
        // 调用服务层方法更新在线支付数据，返回受影响的行数
        int flag = onlinePayV1InnerServiceSMOImpl.updateOnlinePay(onlinePayPo);

        // 检查更新操作是否成功，flag小于1表示没有数据被更新
        if (flag < 1) {
            // 抛出更新失败异常
            throw new CmdException("更新数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}