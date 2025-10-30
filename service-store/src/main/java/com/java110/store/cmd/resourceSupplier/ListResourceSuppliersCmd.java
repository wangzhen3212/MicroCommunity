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
package com.java110.store.cmd.resourceSupplier;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IResourceSupplierInnerServiceSMO;
import com.java110.intf.store.IResourceSupplierV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.resource.ResourceSupplierDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询资源供应商列表命令类
 * 服务编码：resourceSupplier.listResourceSupplier
 * 请求路劲：/app/resourceSupplier.ListResourceSupplier
 * 功能描述：该类用于处理资源供应商列表查询请求，支持分页查询功能
 * add by 吴学文 at 2022-08-07 16:56:05 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "resourceSupplier.listResourceSuppliers")
public class ListResourceSuppliersCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListResourceSuppliersCmd.class);
    
    /**
     * 资源供应商V1版本内部服务接口
     */
    @Autowired
    private IResourceSupplierV1InnerServiceSMO resourceSupplierV1InnerServiceSMOImpl;

    /**
     * 资源供应商内部服务接口
     */
    @Autowired
    private IResourceSupplierInnerServiceSMO resourceSupplierInnerServiceSMOImpl;
    
    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，主要验证分页参数
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 功能描述：执行资源供应商列表查询，支持分页查询，返回查询结果
     * @param event 命令事件对象
     * @param context 数据流上下文对象，用于设置响应结果
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为ResourceSupplierDto对象
        ResourceSupplierDto resourceSupplierDto = BeanConvertUtil.covertBean(reqJson, ResourceSupplierDto.class);

        // 查询资源供应商总记录数
        int count = resourceSupplierInnerServiceSMOImpl.queryResourceSuppliersCount(resourceSupplierDto);

        List<ResourceSupplierDto> resourceSupplierDtos = null;

        // 根据查询结果数量判断是否查询数据
        if (count > 0) {
            // 查询资源供应商列表数据
            resourceSupplierDtos = resourceSupplierInnerServiceSMOImpl.queryResourceSuppliers(resourceSupplierDto);
        } else {
            // 如果没有数据，返回空列表
            resourceSupplierDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象
        ResultVo resultVo = new ResultVo(totalPage, count, resourceSupplierDtos);

        // 创建HTTP响应实体，设置状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应结果设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}