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
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IResourceStoreTimesV1InnerServiceSMO;
import com.java110.po.resource.ResourceStoreTimesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除资源库存次数命令类
 * 
 * 该类负责处理删除资源库存次数的业务逻辑，通过服务编码"resourceStoreTimes.deleteResourceStoreTimes"
 * 接收删除请求，验证必要参数后执行删除操作，并返回操作结果
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2022-10-06 21:26:56
 * @mail 928255095@qq.com
 * @see Cmd
 * @see IResourceStoreTimesV1InnerServiceSMO
 */
@Java110Cmd(serviceCode = "resourceStoreTimes.deleteResourceStoreTimes")
public class DeleteResourceStoreTimesCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteResourceStoreTimesCmd.class);

    /**
     * 资源库存次数服务接口
     */
    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保删除操作有足够的参数信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当必要参数缺失时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证timesId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "timesId", "timesId不能为空");
        // 验证storeId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "storeId", "storeId不能为空");
    }

    /**
     * 执行删除命令方法
     * 
     * 将请求参数转换为资源库存次数实体对象，调用服务层执行删除操作，
     * 并根据删除结果设置响应信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为ResourceStoreTimesPo实体对象
        ResourceStoreTimesPo resourceStoreTimesPo = BeanConvertUtil.covertBean(reqJson, ResourceStoreTimesPo.class);
        
        // 调用服务层接口执行删除操作，返回影响的行数
        int flag = resourceStoreTimesV1InnerServiceSMOImpl.deleteResourceStoreTimes(resourceStoreTimesPo);

        // 检查删除操作是否成功，如果影响行数小于1表示删除失败
        if (flag < 1) {
            // 抛出命令异常，表示删除数据失败
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功状态
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}