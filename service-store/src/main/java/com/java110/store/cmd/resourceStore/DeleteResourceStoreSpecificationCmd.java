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
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.resource.ResourceStoreDto;
import com.java110.intf.store.IResourceStoreInnerServiceSMO;
import com.java110.intf.store.IResourceStoreSpecificationV1InnerServiceSMO;
import com.java110.po.resource.ResourceStoreSpecificationPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：删除资源库规格命令类
 * 服务编码：resourceStoreSpecification.deleteResourceStoreSpecification
 * 请求路径：/app/resourceStoreSpecification.DeleteResourceStoreSpecification
 * 功能描述：该命令类用于处理删除资源库规格的请求，包含参数验证和删除操作
 * add by 吴学文 at 2022-08-08 14:19:39 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "resourceStore.deleteResourceStoreSpecification")
public class DeleteResourceStoreSpecificationCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteResourceStoreSpecificationCmd.class);

    /**
     * 资源库规格服务接口
     */
    @Autowired
    private IResourceStoreSpecificationV1InnerServiceSMO resourceStoreSpecificationV1InnerServiceSMOImpl;

    /**
     * 资源库服务接口
     */
    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证删除资源库规格请求的必要参数，并检查规格是否正在被使用
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含rssId和storeId
        Assert.hasKeyAndValue(reqJson, "rssId", "rssId不能为空");
        Assert.hasKeyAndValue(reqJson, "storeId", "storeId不能为空");
        
        // 创建资源库DTO对象用于查询
        ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
        resourceStoreDto.setRssId(reqJson.getString("rssId"));
        
        // 查询物品信息表，检查该规格是否正在被使用
        List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
        
        // 如果查询结果不为空，说明该规格正在被使用，不能删除
        Assert.listIsNull(resourceStoreDtos, "物品信息中该规格正在使用，不能删除！");
    }

    /**
     * 执行删除命令方法
     * 功能描述：执行资源库规格的删除操作，包含事务管理
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为资源库规格PO对象
        ResourceStoreSpecificationPo resourceStoreSpecificationPo = BeanConvertUtil.covertBean(reqJson, ResourceStoreSpecificationPo.class);
        
        // 调用服务层删除资源库规格
        int flag = resourceStoreSpecificationV1InnerServiceSMOImpl.deleteResourceStoreSpecification(resourceStoreSpecificationPo);

        // 检查删除操作是否成功，如果影响行数小于1则删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}