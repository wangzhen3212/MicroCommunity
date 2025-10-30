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
package com.java110.store.cmd.wechatMenu;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IWechatMenuV1InnerServiceSMO;
import com.java110.po.wechat.WechatMenuPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：删除微信菜单命令类
 * 服务编码：wechatMenu.deleteWechatMenu
 * 请求路劲：/app/wechatMenu.DeleteWechatMenu
 * add by 吴学文 at 2022-07-29 11:48:22 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 类功能：处理删除微信菜单的CMD命令，负责验证请求参数并执行删除操作
 */
@Java110Cmd(serviceCode = "smallWeChat.deleteWechatMenu")
public class DeleteWechatMenuCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteWechatMenuCmd.class);

    /**
     * 微信菜单服务接口，用于执行实际的删除操作
     */
    @Autowired
    private IWechatMenuV1InnerServiceSMO wechatMenuV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 
     * @param event CMD事件对象，包含请求相关信息
     * @param cmdDataFlowContext CMD数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求中必须包含wechatMenuId参数
        Assert.hasKeyAndValue(reqJson, "wechatMenuId", "wechatMenuId不能为空");
        // 验证请求中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除微信菜单的核心业务逻辑
     * 
     * @param event CMD事件对象
     * @param cmdDataFlowContext CMD数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为微信菜单POJO对象
        WechatMenuPo wechatMenuPo = BeanConvertUtil.covertBean(reqJson, WechatMenuPo.class);
        
        // 调用服务层执行删除操作，返回受影响的行数
        int flag = wechatMenuV1InnerServiceSMOImpl.deleteWechatMenu(wechatMenuPo);

        // 检查删除操作是否成功，如果受影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}