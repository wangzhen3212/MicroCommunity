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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.community.ICommunityV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.community.CommunityDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：查询小区列表命令类
 * 服务编码：community.listCommunity
 * 请求路劲：/app/community.ListCommunity
 * add by 吴学文 at 2021-09-18 12:54:57 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：处理小区列表查询请求的命令类，负责接收前端请求并返回小区列表数据
 * 实现逻辑：通过调用社区服务接口查询小区信息，支持分页查询
 */
@Java110Cmd(serviceCode = "community.listCommunity")
public class ListCommunityCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListCommunityCmd.class);
    
    /**
     * 社区服务接口，用于查询小区相关信息
     */
    @Autowired
    private ICommunityV1InnerServiceSMO communityV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，主要验证分页参数
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 功能描述：执行小区列表查询的核心业务逻辑
     * 实现步骤：
     * 1. 将请求参数转换为CommunityDto对象
     * 2. 查询小区总数
     * 3. 根据查询结果获取小区列表
     * 4. 构建响应结果并返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为CommunityDto对象，便于后续查询
        CommunityDto communityDto = BeanConvertUtil.covertBean(reqJson, CommunityDto.class);

        // 查询符合条件的小区总数，用于分页计算
        int count = communityV1InnerServiceSMOImpl.queryCommunitysCount(communityDto);

        // 定义小区列表变量
        List<CommunityDto> communityDtos = null;

        // 根据查询结果判断是否有数据
        if (count > 0) {
            // 有数据时，查询小区详细信息列表
            communityDtos = communityV1InnerServiceSMOImpl.queryCommunitys(communityDto);
        } else {
            // 无数据时，返回空列表
            communityDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象，包含总页数、总记录数和小区列表数据
        ResultVo resultVo = new ResultVo(totalPage, count, communityDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中，返回给前端
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}