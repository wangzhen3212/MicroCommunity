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
package com.java110.community.cmd.communitySpace;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.ICommunitySpaceOpenTimeV1InnerServiceSMO;
import com.java110.intf.community.ICommunitySpaceV1InnerServiceSMO;
import com.java110.po.communitySpace.CommunitySpacePo;
import com.java110.po.communitySpace.CommunitySpaceOpenTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 类表述：保存
 * 服务编码：communitySpace.saveCommunitySpace
 * 请求路劲：/app/communitySpace.SaveCommunitySpace
 * add by 吴学文 at 2022-09-30 10:29:06 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 社区空间保存命令类
 * 
 * 该类负责处理社区空间信息的保存请求，包括社区空间基本信息及其开放时间的保存。
 * 实现了Cmd接口，通过注解@Java110Cmd指定服务编码为"communitySpace.saveCommunitySpace"。
 * 该类会验证请求参数的有效性，并将数据保存到数据库中。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-09-30
 */
@Java110Cmd(serviceCode = "communitySpace.saveCommunitySpace")
public class SaveCommunitySpaceCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveCommunitySpaceCmd.class);

    /**
     * ID生成前缀，用于生成空间ID和时间ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 社区空间服务接口，用于操作社区空间数据
     */
    @Autowired
    private ICommunitySpaceV1InnerServiceSMO communitySpaceV1InnerServiceSMOImpl;

    /**
     * 社区空间开放时间服务接口，用于操作社区空间开放时间数据
     */
    @Autowired
    private ICommunitySpaceOpenTimeV1InnerServiceSMO communitySpaceOpenTimeV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 
     * 该方法会检查请求报文中是否包含必要的参数，如果缺少必要参数会抛出异常。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求报文JSON对象，包含前端传递的参数
     * @throws CmdException 当请求参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中是否包含必要的参数
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        Assert.hasKeyAndValue(reqJson, "startTime", "请求报文中未包含startTime");
        Assert.hasKeyAndValue(reqJson, "endTime", "请求报文中未包含endTime");
        Assert.hasKeyAndValue(reqJson, "feeMoney", "请求报文中未包含feeMoney");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "state", "请求报文中未包含state");
    }

    /**
     * 执行社区空间保存命令
     * 
     * 该方法负责保存社区空间信息及其24小时的开放时间配置。
     * 使用@Java110Transactional注解确保数据库操作的原子性。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求报文JSON对象，包含前端传递的参数
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为社区空间PO对象
        CommunitySpacePo communitySpacePo = BeanConvertUtil.covertBean(reqJson, CommunitySpacePo.class);
        // 生成空间ID
        communitySpacePo.setSpaceId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 保存社区空间信息到数据库
        int flag = communitySpaceV1InnerServiceSMOImpl.saveCommunitySpace(communitySpacePo);

        // 检查保存结果，如果失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 创建社区空间开放时间列表，为24小时分别创建开放时间记录
        List<CommunitySpaceOpenTimePo> communitySpaceOpenTimePos = new ArrayList<>();
        CommunitySpaceOpenTimePo communitySpaceOpenTimePo = null;
        
        // 循环24小时，为每个小时创建开放时间记录
        for (int hours = 0; hours < 24; hours++) {
            communitySpaceOpenTimePo = new CommunitySpaceOpenTimePo();
            // 设置社区ID
            communitySpaceOpenTimePo.setCommunityId(reqJson.getString("communityId"));
            // 设置空间ID，与上面保存的空间ID关联
            communitySpaceOpenTimePo.setSpaceId(communitySpacePo.getSpaceId());
            // 默认设置为不开放状态
            communitySpaceOpenTimePo.setIsOpen("N");
            // 生成时间ID
            communitySpaceOpenTimePo.setTimeId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 设置小时数
            communitySpaceOpenTimePo.setHours(hours + "");
            // 添加到列表
            communitySpaceOpenTimePos.add(communitySpaceOpenTimePo);
        }
        
        // 批量保存社区空间开放时间信息到数据库
        flag = communitySpaceOpenTimeV1InnerServiceSMOImpl.saveCommunitySpaceOpenTimes(communitySpaceOpenTimePos);
        
        // 检查保存结果，如果失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}