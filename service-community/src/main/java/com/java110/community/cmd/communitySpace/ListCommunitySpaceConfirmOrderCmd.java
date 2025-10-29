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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.intf.community.ICommunitySpaceConfirmOrderV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.community.CommunitySpaceConfirmOrderDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Java110CmdDoc(title = "查询预约核销订单",
        description = "查询预约核销订单",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/communitySpace.listCommunitySpaceConfirmOrder",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communitySpace.listCommunitySpaceConfirmOrder"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "page", type = "int", length = 11, remark = "分页页数"),
        @Java110ParamDoc(name = "row", type = "int", length = 11, remark = "分页行数"),
        @Java110ParamDoc(name = "communityId", length = 30, remark = "小区ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Array", remark = "有效数据"),
                @Java110ParamDoc(parentNodeName = "data", name = "communityId", type = "String", remark = "小区ID"),
                @Java110ParamDoc(parentNodeName = "data", name = "appointmentTime", type = "String", remark = "预约时间"),
                @Java110ParamDoc(parentNodeName = "data", name = "createTime", type = "String", remark = "核销时间"),
                @Java110ParamDoc(parentNodeName = "data", name = "hours", type = "String", remark = "核销小时"),
                @Java110ParamDoc(parentNodeName = "data", name = "orderId", type = "String", remark = "核销订单ID"),
                @Java110ParamDoc(parentNodeName = "data", name = "personName", type = "String", remark = "预约人"),
                @Java110ParamDoc(parentNodeName = "data", name = "personTel", type = "String", remark = "预约电话"),
                @Java110ParamDoc(parentNodeName = "data", name = "tel", type = "String", remark = "联系电话"),
                @Java110ParamDoc(parentNodeName = "data", name = "spaceId", type = "String", remark = "场地ID"),
                @Java110ParamDoc(parentNodeName = "data", name = "spaceName", type = "String", remark = "场地名称"),
                @Java110ParamDoc(parentNodeName = "data", name = "timeId", type = "String", remark = "核销ID"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/communitySpace.listCommunitySpaceConfirmOrder?communityId=2022081539020475&page=1&row=10",
        resBody = "{\"code\":0,\"data\":[{\"appointmentTime\":\"2022-10-04\",\"communityId\":\"2022081539020475\",\"createTime\":\"2022-10-14 17:34:47\",\"cspId\":\"102022100453340014\",\"hours\":\"10\",\"orderId\":\"102022101485880007\",\"page\":-1,\"personName\":\"王菲菲\",\"personTel\":\"18909761234\",\"records\":0,\"row\":0,\"spaceId\":\"102022100400450043\",\"spaceName\":\"2场地\",\"statusCd\":\"0\",\"timeId\":\"102022100475840016\",\"total\":0}],\"msg\":\"成功\",\"page\":0,\"records\":1,\"rows\":0,\"total\":1}"
)

/**
 * 查询社区空间预约核销订单的命令类
 * 
 * 该类负责处理社区空间预约核销订单的查询请求，包括分页查询和结果返回。
 * 通过调用社区空间确认订单服务接口，获取预约核销订单数据并返回给前端。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-14
 * @see Cmd
 * @see ICommunitySpaceConfirmOrderV1InnerServiceSMO
 */
@Java110Cmd(serviceCode = "communitySpace.listCommunitySpaceConfirmOrder")
public class ListCommunitySpaceConfirmOrderCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListCommunitySpaceConfirmOrderCmd.class);
    
    /**
     * 社区空间确认订单服务接口
     */
    @Autowired
    private ICommunitySpaceConfirmOrderV1InnerServiceSMO communitySpaceConfirmOrderV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要验证分页参数是否合法
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证page和row参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理查询社区空间预约核销订单的核心业务逻辑，包括：
     * 1. 参数转换
     * 2. 查询数据总数
     * 3. 查询数据列表
     * 4. 构建返回结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON对象转换为CommunitySpaceConfirmOrderDto对象
        CommunitySpaceConfirmOrderDto communitySpaceConfirmOrderDto = BeanConvertUtil.covertBean(reqJson, CommunitySpaceConfirmOrderDto.class);

        // 查询符合条件的预约核销订单总数
        int count = communitySpaceConfirmOrderV1InnerServiceSMOImpl.queryCommunitySpaceConfirmOrdersCount(communitySpaceConfirmOrderDto);

        // 声明预约核销订单数据列表
        List<CommunitySpaceConfirmOrderDto> communitySpaceConfirmOrderDtos = null;

        // 根据查询结果数量决定如何处理数据查询
        if (count > 0) {
            // 如果存在数据，则查询具体的预约核销订单列表
            communitySpaceConfirmOrderDtos = communitySpaceConfirmOrderV1InnerServiceSMOImpl.queryCommunitySpaceConfirmOrders(communitySpaceConfirmOrderDto);
        } else {
            // 如果没有数据，返回空列表
            communitySpaceConfirmOrderDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, communitySpaceConfirmOrderDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中，返回给前端
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}