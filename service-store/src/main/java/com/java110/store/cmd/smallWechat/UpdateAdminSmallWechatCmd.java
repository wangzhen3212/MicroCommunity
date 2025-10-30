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
package com.java110.store.cmd.smallWechat;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.po.wechat.SmallWechatPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 类表述：更新
 * 服务编码：smallWechat.updateSmallWechat
 * 请求路劲：/app/smallWechat.UpdateSmallWechat
 * add by 吴学文 at 2022-05-25 10:46:07 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 管理员更新小程序配置命令类
 * 该类负责处理管理员更新小程序配置信息的业务逻辑，包括参数验证和数据更新操作
 * 通过注解@Java110Cmd标识服务编码，与前端请求路由对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-05-25
 */
@Java110Cmd(serviceCode = "smallWeChat.updateAdminSmallWeChat")
public class UpdateAdminSmallWechatCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateAdminSmallWechatCmd.class);

    /**
     * 小程序配置服务接口
     * 用于调用小程序配置的更新操作
     */
    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效，确保必要的参数都存在且不为空
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
        
        // 检查当前环境是否为开发环境
        Environment.isDevEnv();
        
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "wechatId", "wechatId不能为空");
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        Assert.hasKeyAndValue(reqJson, "appId", "请求报文中未包含appId");
        Assert.hasKeyAndValue(reqJson, "appSecret", "请求报文中未包含appSecret");
        // 支付密码参数暂时注释，可根据业务需要开启
        // Assert.hasKeyAndValue(reqJson, "payPassword", "请求报文中未包含payPassword");
    }

    /**
     * 执行命令方法
     * 处理小程序配置的更新操作，将请求参数转换为实体对象并调用服务层进行数据更新
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当数据更新失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为小程序配置实体对象
        SmallWechatPo smallWechatPo = BeanConvertUtil.covertBean(reqJson, SmallWechatPo.class);
        
        // 设置小程序类型参数
        smallWechatPo.setWechatType(reqJson.getString("weChatType"));
        
        // 调用服务层更新小程序配置信息
        int flag = smallWechatV1InnerServiceSMOImpl.updateSmallWechat(smallWechatPo);

        // 检查更新操作是否成功
        if (flag < 1) {
            // 更新失败时抛出异常
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}