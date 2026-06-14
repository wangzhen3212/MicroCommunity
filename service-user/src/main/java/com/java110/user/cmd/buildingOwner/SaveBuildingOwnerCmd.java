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
package com.java110.user.cmd.buildingOwner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.user.IBuildingOwnerV1InnerServiceSMO;
import com.java110.po.owner.OwnerPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 类表述：保存业主信息命令类
 * 服务编码：buildingOwner.saveBuildingOwner
 * 请求路劲：http://ip:port/app/buildingOwner.saveBuildingOwner
 * add by 吴学文 at 2021-10-08 17:43:55 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该命令类用于处理业主信息的保存操作，包括参数验证和数据持久化
 * 主要职责：
 * 1. 验证请求参数完整性
 * 2. 转换请求数据为持久化对象
 * 3. 生成业主成员ID
 * 4. 调用服务层保存业主信息
 * 5. 处理保存结果并返回响应
 */
@Java110Cmd(serviceCode = "buildingOwner.saveBuildingOwner")
public class SaveBuildingOwnerCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveBuildingOwnerCmd.class);

    /**
     * 业主成员ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 业主服务接口，用于数据持久化操作
     */
    @Autowired
    private IBuildingOwnerV1InnerServiceSMO buildingOwnerV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求报文中必需的参数是否存在
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "memberId", "请求报文中未包含memberId");
        Assert.hasKeyAndValue(reqJson, "ownerId", "请求报文中未包含ownerId");
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        Assert.hasKeyAndValue(reqJson, "sex", "请求报文中未包含sex");
        Assert.hasKeyAndValue(reqJson, "age", "请求报文中未包含age");
        Assert.hasKeyAndValue(reqJson, "link", "请求报文中未包含link");
        Assert.hasKeyAndValue(reqJson, "userId", "请求报文中未包含userId");
    }

    /**
     * 执行保存业主信息命令
     * 将请求数据转换为持久化对象并保存到数据库
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为OwnerPo对象
        OwnerPo ownerPo = BeanConvertUtil.covertBean(reqJson, OwnerPo.class);
        
        // 生成业主成员ID
        ownerPo.setMemberId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存业主信息
        int flag = buildingOwnerV1InnerServiceSMOImpl.saveBuildingOwner(ownerPo);

        // 检查保存结果
        if (flag < 1) {
            // 保存失败时抛出异常
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}