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
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IResourceAuditFlowV1InnerServiceSMO;
import com.java110.po.resource.ResourceAuditFlowPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：删除资源审核流程命令类
 * 服务编码：resourceAuditFlow.deleteResourceAuditFlow
 * 请求路劲：/app/resourceAuditFlow.DeleteResourceAuditFlow
 * add by 吴学文 at 2023-03-17 01:00:14 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * @description 删除资源审核流程的命令处理类，负责处理删除资源审核流程的请求
 * @author 吴学文
 * @version 1.0
 * @since 2023-03-17
 */
@Java110Cmd(serviceCode = "resourceStore.deleteResourceAuditFlow")
public class DeleteResourceAuditFlowCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteResourceAuditFlowCmd.class);

    /**
     * 资源审核流程服务接口
     */
    @Autowired
    private IResourceAuditFlowV1InnerServiceSMO resourceAuditFlowV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     * @description 验证删除资源审核流程请求的必填参数是否完整
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含rafId（资源审核流程ID）
        Assert.hasKeyAndValue(reqJson, "rafId", "rafId不能为空");
        // 验证请求参数中必须包含communityId（小区ID）
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含小区");
    }

    /**
     * 执行删除资源审核流程命令
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     * @description 执行实际的资源审核流程删除操作，包含事务管理
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从上下文中获取商户ID
        String storeId = CmdContextUtils.getStoreId(cmdDataFlowContext);

        // 将请求JSON对象转换为资源审核流程实体对象
        ResourceAuditFlowPo resourceAuditFlowPo = BeanConvertUtil.covertBean(reqJson, ResourceAuditFlowPo.class);
        // 设置商户ID到实体对象中
        resourceAuditFlowPo.setStoreId(storeId);
        
        // 调用服务层删除资源审核流程，返回影响的行数
        int flag = resourceAuditFlowV1InnerServiceSMOImpl.deleteResourceAuditFlow(resourceAuditFlowPo);

        // 检查删除操作是否成功，如果影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}