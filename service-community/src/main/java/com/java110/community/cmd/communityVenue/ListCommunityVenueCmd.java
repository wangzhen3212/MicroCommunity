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
package com.java110.community.cmd.communityVenue;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.intf.community.ICommunityVenueV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.community.CommunityVenueDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Java110CmdDoc(title = "查询场馆",
        description = "查询系统中的查询场馆",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/communityVenue.ListCommunityVenue",
        resource = "communityDoc",
        author = "吴学文",
        serviceCode = "communityVenue.ListCommunityVenue"
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
                @Java110ParamDoc(parentNodeName = "data", name = "name", type = "String", remark = "场地名称"),
                @Java110ParamDoc(parentNodeName = "data", name = "venueId", type = "String", remark = "场馆编号"),
        }
)

@Java110ExampleDoc(
        reqBody = "http://{ip}:{port}/app/communityVenue.ListCommunityVenue?communityId=2022081539020475&page=1&row=10",
        resBody = "{\"code\":0,\"data\":[{\"name\":\"无需文\",\"communityId\":\"2022081539020475\",\"venueId\":\"102022093043260007\"}],\"msg\":\"成功\",\"page\":0,\"records\":1,\"rows\":0,\"total\":2}"
)

/**
 * 查询场馆信息命令类
 * 
 * 该类负责处理查询场馆列表的请求，支持分页查询和按小区ID筛选。
 * 实现了场馆信息的查询、数据验证和结果返回功能。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-03
 * @see Cmd
 */
@Java110Cmd(serviceCode = "communityVenue.listCommunityVenue")
public class ListCommunityVenueCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListCommunityVenueCmd.class);
    
    /**
     * 场馆服务接口，用于调用场馆相关的业务逻辑
     */
    @Autowired
    private ICommunityVenueV1InnerServiceSMO communityVenueV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和必要参数检查
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类方法验证分页信息（page和row参数）
        super.validatePageInfo(reqJson);

        // 验证请求中必须包含小区ID参数
        Assert.hasKeyAndValue(reqJson,"communityId","未包含小区ID");
    }

    /**
     * 执行命令方法
     * 
     * 处理查询场馆列表的核心业务逻辑，包括数据查询、分页处理和结果返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为场馆数据传输对象
        CommunityVenueDto communityVenueDto = BeanConvertUtil.covertBean(reqJson, CommunityVenueDto.class);

        // 查询符合条件的场馆总数
        int count = communityVenueV1InnerServiceSMOImpl.queryCommunityVenuesCount(communityVenueDto);

        // 场馆数据列表
        List<CommunityVenueDto> communityVenueDtos = null;

        if (count > 0) {
            // 如果存在数据，则查询场馆详细信息列表
            communityVenueDtos = communityVenueV1InnerServiceSMOImpl.queryCommunityVenues(communityVenueDto);
        } else {
            // 如果没有数据，返回空列表
            communityVenueDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数/每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, communityVenueDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}