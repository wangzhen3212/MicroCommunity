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
package com.java110.community.cmd.communityLocation;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.community.ICommunityLocationV1InnerServiceSMO;
import com.java110.po.community.CommunityLocationPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存
 * 服务编码：communityLocation.saveCommunityLocation
 * 请求路劲：/app/communityLocation.SaveCommunityLocation
 * add by 吴学文 at 2022-07-17 00:10:38 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 社区位置信息保存命令类
 * 
 * 该类负责处理社区位置信息的保存请求，包括参数验证和业务逻辑处理
 * 通过注解@Java110Cmd声明服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-07-17
 */
@Java110Cmd(serviceCode = "communityLocation.saveCommunityLocation")
public class SaveCommunityLocationCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveCommunityLocationCmd.class);

    /**
     * 位置ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 社区位置服务接口
     */
    @Autowired
    private ICommunityLocationV1InnerServiceSMO communityLocationV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段：
     * - locationName: 位置名称
     * - locationType: 位置类型  
     * - communityId: 社区ID
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证位置名称参数是否存在
        Assert.hasKeyAndValue(reqJson, "locationName", "请求报文中未包含locationName");
        // 验证位置类型参数是否存在
        Assert.hasKeyAndValue(reqJson, "locationType", "请求报文中未包含locationType");
        // 验证社区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行保存社区位置信息的核心命令方法
     * 
     * 该方法使用事务注解确保数据操作的原子性，主要功能包括：
     * 1. 将请求参数转换为实体对象
     * 2. 生成位置ID
     * 3. 调用服务层保存数据
     * 4. 处理保存结果并设置响应
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为社区位置实体对象
        CommunityLocationPo communityLocationPo = BeanConvertUtil.covertBean(reqJson, CommunityLocationPo.class);
        
        // 生成位置ID，使用前缀确保ID的唯一性
        communityLocationPo.setLocationId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存社区位置信息，返回影响的行数
        int flag = communityLocationV1InnerServiceSMOImpl.saveCommunityLocation(communityLocationPo);

        // 检查保存是否成功，如果影响行数小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}