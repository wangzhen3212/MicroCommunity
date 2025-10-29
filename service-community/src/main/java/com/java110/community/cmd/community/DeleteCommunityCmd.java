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
package com.java110.community.cmd.community;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.dto.community.CommunityDto;
import com.java110.intf.community.ICommunityInnerServiceSMO;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.po.community.CommunityPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110CmdDoc(title = "删除小区",
        description = "主要提供给外系统删除小区",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/community.deleteCommunity",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "community.deleteCommunity",
        seq = 3
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区编码"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody="{\"communityId\":\"2022092293190329\"}",
        resBody="{'code':0,'msg':'成功'}"
)
/**
 * 删除小区命令类
 * 
 * 该类负责处理删除小区的业务逻辑，主要功能包括：
 * 1. 验证删除请求参数的合法性
 * 2. 检查小区是否存在
 * 3. 执行小区删除操作
 * 4. 返回操作结果
 * 
 * 服务编码：community.deleteCommunity
 * 请求路径：/app/community.DeleteCommunity
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2021-09-18 12:54:57
 * @mail 928255095@qq.com
 * 
 * 类表述：删除
 * 服务编码：community.deleteCommunity
 * 请求路劲：/app/community.DeleteCommunity
 * add by 吴学文 at 2021-09-18 12:54:57 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "community.deleteCommunity")
public class DeleteCommunityCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteCommunityCmd.class);

    /**
     * 小区V1版本内部服务接口
     */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /**
     * 小区内部服务接口
     */
    @Autowired
    private ICommunityInnerServiceSMO communityInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 该方法用于验证删除小区请求的参数合法性，包括：
     * 1. 检查当前运行环境是否为开发环境
     * 2. 验证必填参数communityId是否存在且不为空
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 检查当前运行环境，仅在开发环境下执行某些特殊逻辑
        Environment.isDevEnv();

        // 验证请求参数中必须包含communityId且其值不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "小区ID不能为空");
    }

    /**
     * 执行删除小区命令
     * 
     * 该方法负责执行小区删除的核心业务逻辑，包括：
     * 1. 根据小区ID查询小区信息
     * 2. 验证小区是否存在
     * 3. 执行删除操作
     * 4. 处理删除结果并返回响应
     * 
     * 该方法使用事务注解确保操作的原子性
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出命令异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建小区数据传输对象并设置小区ID
        CommunityDto communityDto = new CommunityDto();
        communityDto.setCommunityId((String) reqJson.get("communityId"));
        
        // 根据小区ID查询小区信息
        List<CommunityDto> communityDtos = communityInnerServiceSMOImpl.queryCommunitys(communityDto);
        
        // 验证查询结果，确保小区存在
        if (communityDtos.size() == 0 || communityDtos == null) {
            throw new IllegalArgumentException("没有查询到communityId为：" + communityDto.getCommunityId() + "小区信息");
        }
        
        // 注释掉的代码：原逻辑中检查小区状态是否为已审核通过，如果已审核通过则不允许删除
        // if ("1100".equals(communityDtos.get(0).getState())) {
        //     throw new IllegalArgumentException("删除失败,该小区已审核通过");
        // }
        
        // 将请求JSON对象转换为小区持久化对象
        CommunityPo communityPo = BeanConvertUtil.covertBean(reqJson, CommunityPo.class);
        
        // 调用服务层执行删除操作，返回影响的行数
        int flag = communityV1InnerServiceSMOImpl.deleteCommunity(communityPo);

        // 检查删除操作是否成功，如果影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置成功的响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}