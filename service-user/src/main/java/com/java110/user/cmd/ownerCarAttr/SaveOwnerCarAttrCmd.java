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
package com.java110.user.cmd.ownerCarAttr;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.user.IOwnerCarAttrV1InnerServiceSMO;
import com.java110.po.owner.OwnerCarAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存业主车辆属性信息命令类
 * 服务编码：ownerCarAttr.saveOwnerCarAttr
 * 请求路劲：/app/ownerCarAttr.SaveOwnerCarAttr
 * add by 吴学文 at 2022-03-14 18:31:37 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：处理业主车辆属性信息的保存操作，包括参数验证和业务逻辑处理
 * 主要职责：
 * 1. 验证请求参数完整性
 * 2. 转换请求数据为实体对象
 * 3. 生成主键ID
 * 4. 调用服务层保存数据
 * 5. 处理保存结果并返回响应
 */
@Java110Cmd(serviceCode = "ownerCarAttr.saveOwnerCarAttr")
public class SaveOwnerCarAttrCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveOwnerCarAttrCmd.class);

    /**
     * 主键ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 业主车辆属性服务接口
     */
    @Autowired
    private IOwnerCarAttrV1InnerServiceSMO ownerCarAttrV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求报文中必须包含的字段
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含memberId字段
        Assert.hasKeyAndValue(reqJson, "memberId", "请求报文中未包含memberId");
        // 验证请求报文中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证请求报文中必须包含specCd字段
        Assert.hasKeyAndValue(reqJson, "specCd", "请求报文中未包含specCd");
        // 验证请求报文中必须包含value字段
        Assert.hasKeyAndValue(reqJson, "value", "请求报文中未包含value");
    }

    /**
     * 执行保存业主车辆属性命令
     * 将请求参数转换为实体对象并保存到数据库
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为业主车辆属性实体对象
        OwnerCarAttrPo ownerCarAttrPo = BeanConvertUtil.covertBean(reqJson, OwnerCarAttrPo.class);
        
        // 生成主键ID并设置到实体对象中
        ownerCarAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存业主车辆属性信息
        int flag = ownerCarAttrV1InnerServiceSMOImpl.saveOwnerCarAttr(ownerCarAttrPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}