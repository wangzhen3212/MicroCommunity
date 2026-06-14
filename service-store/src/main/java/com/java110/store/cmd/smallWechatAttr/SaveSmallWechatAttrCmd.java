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
import com.java110.core.factory.GenerateCodeFactory;
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
 * 类表述：保存小程序微信属性命令类
 * 服务编码：smallWechatAttr.saveSmallWechatAttr
 * 请求路径：/app/smallWechatAttr.SaveSmallWechatAttr
 * 功能描述：该命令类用于处理保存小程序微信属性的业务逻辑，包括参数验证和数据保存操作
 * add by 吴学文 at 2022-05-25 10:58:13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "smallWechatAttr.saveSmallWechatAttr")
public class SaveSmallWechatAttrCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveSmallWechatAttrCmd.class);

    /**
     * ID生成前缀常量
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 小程序微信属性服务接口
     */
    @Autowired
    private ISmallWechatAttrV1InnerServiceSMO smallWechatAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否完整，确保必要的参数都存在
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含attrId参数
        Assert.hasKeyAndValue(reqJson, "attrId", "请求报文中未包含attrId");
        // 验证请求报文中必须包含wechatId参数
        Assert.hasKeyAndValue(reqJson, "wechatId", "请求报文中未包含wechatId");
        // 验证请求报文中必须包含specCd参数
        Assert.hasKeyAndValue(reqJson, "specCd", "请求报文中未包含specCd");
        // 验证请求报文中必须包含value参数
        Assert.hasKeyAndValue(reqJson, "value", "请求报文中未包含value");
        // 验证请求报文中必须包含statusCd参数
        Assert.hasKeyAndValue(reqJson, "statusCd", "请求报文中未包含statusCd");
        // 验证请求报文中必须包含communityId参数
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 命令执行方法
     * 功能描述：执行保存小程序微信属性的核心业务逻辑，包括数据转换、ID生成和数据持久化
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出命令异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为SmallWechatAttrPo对象
        SmallWechatAttrPo smallWechatAttrPo = BeanConvertUtil.covertBean(reqJson, SmallWechatAttrPo.class);
        // 生成属性ID并设置到PO对象中
        smallWechatAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 调用服务层保存小程序微信属性数据
        int flag = smallWechatAttrV1InnerServiceSMOImpl.saveSmallWechatAttr(smallWechatAttrPo);

        // 检查保存操作是否成功，flag小于1表示保存失败
        if (flag < 1) {
            // 抛出命令异常，表示数据保存失败
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}