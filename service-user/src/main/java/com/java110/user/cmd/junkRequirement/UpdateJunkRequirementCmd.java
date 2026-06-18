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
package com.java110.user.cmd.junkRequirement;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.itemRelease.JunkRequirementDto;
import com.java110.intf.user.IJunkRequirementInnerServiceSMO;
import com.java110.intf.user.IJunkRequirementV1InnerServiceSMO;
import com.java110.po.junkRequirement.JunkRequirementPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


/**
 * 类表述：更新
 * 服务编码：junkRequirement.updateJunkRequirement
 * 请求路劲：/app/junkRequirement.UpdateJunkRequirement
 * add by 吴学文 at 2022-08-08 08:53:50 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 垃圾需求更新命令类
 * 
 * 该类负责处理垃圾需求信息的更新操作，包括参数验证和业务逻辑处理
 * 通过注解@Java110Cmd标识服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-08-08
 */
@Java110Cmd(serviceCode = "junkRequirement.updateJunkRequirement")
public class UpdateJunkRequirementCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateJunkRequirementCmd.class);

    /**
     * 垃圾需求V1版本内部服务接口
     * 用于执行垃圾需求的更新操作
     */
    @Autowired
    private IJunkRequirementV1InnerServiceSMO junkRequirementV1InnerServiceSMOImpl;

    /**
     * 垃圾需求内部服务接口
     * 用于查询垃圾需求信息
     */
    @Autowired
    private IJunkRequirementInnerServiceSMO junkRequirementInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整和有效，确保必要的参数都存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数：垃圾需求ID
        Assert.hasKeyAndValue(reqJson, "junkRequirementId", "junkRequirementId不能为空");
        
        // 注释掉的参数验证，可根据业务需要开启
        // Assert.hasKeyAndValue(reqJson, "typeCd", "请求报文中未包含typeCd");
        // Assert.hasKeyAndValue(reqJson, "classification", "请求报文中未包含classification");
        // Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // Assert.hasKeyAndValue(reqJson, "context", "请求报文中未包含context");
        // Assert.hasKeyAndValue(reqJson, "referencePrice", "请求报文中未包含referencePrice");
        // Assert.hasKeyAndValue(reqJson, "publishUserId", "请求报文中未包含publishUserId");
        // Assert.hasKeyAndValue(reqJson, "publishUserName", "请求报文中未包含publishUserName");
        // Assert.hasKeyAndValue(reqJson, "publishUserLink", "请求报文中未包含publishUserLink");
        
        // 验证必填参数：状态
        Assert.hasKeyAndValue(reqJson, "state", "请求报文中未包含state");
    }

    /**
     * 执行命令方法
     * 
     * 处理垃圾需求更新的核心业务逻辑，包括数据查询、数据转换和更新操作
     * 使用@Java110Transactional注解确保事务性操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建查询条件对象
        JunkRequirementDto junkRequirementDto = new JunkRequirementDto();
        // 设置查询条件：垃圾需求ID
        junkRequirementDto.setJunkRequirementId(reqJson.getString("junkRequirementId"));
        
        // 注释掉的社区ID设置，可根据业务需要开启
        // junkRequirementDto.setCommunityId(paramInJson.getString("communityId"));
        
        // 根据ID查询垃圾需求信息
        List<JunkRequirementDto> junkRequirementDtos = junkRequirementInnerServiceSMOImpl.queryJunkRequirements(junkRequirementDto);

        // 验证查询结果：必须且只能有一条记录
        Assert.listOnlyOne(junkRequirementDtos, "未找到需要修改的活动 或多条数据");

        // 创建业务数据对象，合并原有数据和更新数据
        JSONObject businessJunkRequirement = new JSONObject();
        // 将查询到的原有数据放入业务对象
        businessJunkRequirement.putAll(BeanConvertUtil.beanCovertMap(junkRequirementDtos.get(0)));
        // 将请求中的更新数据合并到业务对象（相同key会覆盖）
        businessJunkRequirement.putAll(reqJson);
        
        // 将业务数据对象转换为持久化对象
        JunkRequirementPo junkRequirementPo = BeanConvertUtil.covertBean(businessJunkRequirement, JunkRequirementPo.class);

        // 执行更新操作
        int flag = junkRequirementV1InnerServiceSMOImpl.updateJunkRequirement(junkRequirementPo);

        // 检查更新结果
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}