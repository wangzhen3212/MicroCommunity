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
package com.java110.store.cmd.smallWechatAttr;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.ISmallWechatAttrV1InnerServiceSMO;
import com.java110.po.wechat.SmallWechatAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除小程序属性命令类
 * 
 * 该类负责处理删除小程序属性相关信息的业务逻辑，包括参数验证和执行删除操作。
 * 通过注解标识服务编码，与前端请求路径对应，实现具体的删除功能。
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2022-05-25 10:58:13
 * @mail 928255095@qq.com
 * @see Cmd
 * @see ISmallWechatAttrV1InnerServiceSMO
 */
@Java110Cmd(serviceCode = "smallWechatAttr.deleteSmallWechatAttr")
public class DeleteSmallWechatAttrCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteSmallWechatAttrCmd.class);

    /**
     * 小程序属性服务接口
     */
    @Autowired
    private ISmallWechatAttrV1InnerServiceSMO smallWechatAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保删除操作的基本数据完整性。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证attrId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "attrId", "attrId不能为空");
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除命令方法
     * 
     * 将请求参数转换为实体对象，调用服务层执行删除操作，并处理删除结果。
     * 该方法使用事务注解确保操作的原子性。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为SmallWechatAttrPo实体对象
        SmallWechatAttrPo smallWechatAttrPo = BeanConvertUtil.covertBean(reqJson, SmallWechatAttrPo.class);
        
        // 调用服务层接口执行删除操作，返回受影响的行数
        int flag = smallWechatAttrV1InnerServiceSMOImpl.deleteSmallWechatAttr(smallWechatAttrPo);

        // 检查删除操作是否成功，如果受影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}