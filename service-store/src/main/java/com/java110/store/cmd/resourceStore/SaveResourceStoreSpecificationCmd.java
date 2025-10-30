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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.store.IResourceStoreSpecificationV1InnerServiceSMO;
import com.java110.po.resource.ResourceStoreSpecificationPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存资源存储规格信息命令类
 * 服务编码：resourceStoreSpecification.saveResourceStoreSpecification
 * 请求路径：/app/resourceStoreSpecification.SaveResourceStoreSpecification
 * 功能描述：该类用于处理资源存储规格信息的保存操作，包括参数验证和数据持久化
 * add by 吴学文 at 2022-08-08 14:19:39 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "resourceStore.saveResourceStoreSpecification")
public class SaveResourceStoreSpecificationCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveResourceStoreSpecificationCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 资源存储规格服务接口
     */
    @Autowired
    private IResourceStoreSpecificationV1InnerServiceSMO resourceStoreSpecificationV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否完整，确保必要的参数都存在且不为空
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证规格名称参数是否存在
        Assert.hasKeyAndValue(reqJson, "specName", "请求报文中未包含specName");
        // 验证资源规格类型ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "rstId", "请求报文中未包含rstId");
        // 验证店铺ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含storeId");
    }

    /**
     * 命令执行方法
     * 功能描述：执行资源存储规格信息的保存操作，包括参数处理、数据转换和持久化
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 处理rsId参数：如果存在rsId参数且值为空，则将其值赋给rstId
        if (reqJson.containsKey("rsId") && StringUtil.isEmpty(reqJson.getString("rsId"))) {
            reqJson.put("rstId", reqJson.getString("rsId"));
        }
        
        // 将JSON对象转换为资源存储规格持久化对象
        ResourceStoreSpecificationPo resourceStoreSpecificationPo = BeanConvertUtil.covertBean(reqJson, ResourceStoreSpecificationPo.class);
        
        // 生成资源存储规格ID
        resourceStoreSpecificationPo.setRssId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存资源存储规格信息
        int flag = resourceStoreSpecificationV1InnerServiceSMOImpl.saveResourceStoreSpecification(resourceStoreSpecificationPo);

        // 检查保存结果，如果影响行数小于1则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}