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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.app.AppDto;
import com.java110.dto.wechat.SmallWeChatDto;
import com.java110.dto.wechat.SmallWechatAttrDto;
import com.java110.intf.store.ISmallWechatAttrV1InnerServiceSMO;
import com.java110.intf.store.ISmallWechatV1InnerServiceSMO;
import com.java110.po.wechat.SmallWechatAttrPo;
import com.java110.po.wechat.SmallWechatPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存
 * 服务编码：smallWechat.saveSmallWechat
 * 请求路劲：/app/smallWechat.SaveSmallWechat
 * add by 吴学文 at 2022-05-25 10:46:07 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 保存管理端小程序配置命令类
 * 该类用于处理保存管理端小程序配置信息的业务逻辑，包括参数验证和数据保存操作
 * 通过注解@Java110Cmd指定服务编码为smallWeChat.saveAdminSmallWeChat
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-05-25
 */
@Java110Cmd(serviceCode = "smallWeChat.saveAdminSmallWeChat")
public class SaveAdminSmallWechatCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveAdminSmallWechatCmd.class);

    /**
     * 代码前缀常量，用于生成唯一标识
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 小程序服务接口
     */
    @Autowired
    private ISmallWechatV1InnerServiceSMO smallWechatV1InnerServiceSMOImpl;

    /**
     * 小程序属性服务接口
     */
    @Autowired
    private ISmallWechatAttrV1InnerServiceSMO smallWechatAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效，确保必要的参数都存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的管理员权限验证
        super.validateAdmin(cmdDataFlowContext);
        
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        Assert.hasKeyAndValue(reqJson, "appId", "请求报文中未包含appId");
        Assert.hasKeyAndValue(reqJson, "appSecret", "请求报文中未包含appSecret");
        Assert.hasKeyAndValue(reqJson, "objId", "请求报文中未包含objId(小区id)");
        Assert.hasKeyAndValue(reqJson, "weChatType", "请求报文中未包含类型");
    }

    /**
     * 执行保存小程序配置命令
     * 将请求参数转换为实体对象并保存到数据库，包含事务管理
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 生成小程序唯一ID
        String wechatId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_weChatId);
        
        // 将JSON对象转换为小程序实体对象
        SmallWechatPo smallWechatPo = BeanConvertUtil.covertBean(reqJson, SmallWechatPo.class);
        
        // 设置小程序实体对象的属性值
        smallWechatPo.setWechatId(wechatId);                    // 设置小程序ID
        smallWechatPo.setObjType(SmallWeChatDto.OBJ_TYPE_COMMUNITY); // 设置对象类型为小区类型
        smallWechatPo.setMchId("1");                           // 设置商户ID（默认值）
        smallWechatPo.setMchName("1");                         // 设置商户名称（默认值）
        smallWechatPo.setPayPassword("1");                     // 设置支付密码（默认值）
        smallWechatPo.setWechatType(reqJson.getString("weChatType")); // 设置小程序类型
        
        // 调用服务保存小程序配置信息
        int flag = smallWechatV1InnerServiceSMOImpl.saveSmallWechat(smallWechatPo);

        // 检查保存操作是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}