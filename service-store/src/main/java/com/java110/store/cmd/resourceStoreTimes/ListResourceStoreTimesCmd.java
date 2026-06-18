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
package com.java110.store.cmd.resourceStoreTimes;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.intf.store.IResourceStoreTimesV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.resource.ResourceStoreTimesDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 查询物品进货明细 功能
 */

@Java110CmdDoc(title = "查询物品进货明细",
        description = "主要用于 查询物品进货明细",
        httpMethod = "get",
        url = "http://{ip}:{port}/app/resourceStoreTimes.listResourceStoreTimes",
        resource = "storeDoc",
        author = "吴学文",
        serviceCode = "resourceStoreTimes.listResourceStoreTimes"
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "resCode", length = 30, remark = "物品编号"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "Array", length = 0, remark = "数据"),
                @Java110ParamDoc(parentNodeName = "data",name = "resCode", type = "String", length = 30,  remark = "物品编号"),
                @Java110ParamDoc(parentNodeName = "data",name = "price", type = "String", length = 30,  remark = "价格"),
                @Java110ParamDoc(parentNodeName = "data",name = "stock", type = "String", length = 30,  remark = "库存"),
        }
)

@Java110ExampleDoc(
        reqBody="http://{ip}:{port}/app/resourceStoreTimes.listResourceStoreTimes?resCode=123123",
        resBody="{'code':0,'msg':'成功','data':[{'resCode':'123','price':'1.2','stock':'12'}]}"
)

/**
 * 物品进货明细查询命令类
 * 
 * 该类负责处理物品进货明细的查询请求，包括参数验证、数据查询和结果返回等功能。
 * 通过调用资源存储次数服务接口，根据物品编号等条件查询进货明细信息。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-06
 */
@Java110Cmd(serviceCode = "resourceStoreTimes.listResourceStoreTimes")
public class ListResourceStoreTimesCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListResourceStoreTimesCmd.class);
    
    /**
     * 资源存储次数服务接口
     */
    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息的验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理物品进货明细查询的核心业务逻辑，包括：
     * 1. 参数转换和设置
     * 2. 数据查询和计数
     * 3. 结果封装和返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为资源存储次数DTO对象
        ResourceStoreTimesDto resourceStoreTimesDto = BeanConvertUtil.covertBean(reqJson, ResourceStoreTimesDto.class);

        // 从请求头中获取店铺ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
        // 设置店铺ID到查询条件中
        resourceStoreTimesDto.setStoreId(storeId);

        // 查询符合条件的记录总数
        int count = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimessCount(resourceStoreTimesDto);

        List<ResourceStoreTimesDto> resourceStoreTimesDtos = null;

        // 根据查询结果数量处理数据
        if (count > 0) {
            // 如果有数据，查询具体的物品进货明细列表
            resourceStoreTimesDtos = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimess(resourceStoreTimesDto);
        } else {
            // 如果没有数据，返回空列表
            resourceStoreTimesDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示数量，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, resourceStoreTimesDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到数据流上下文中
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}