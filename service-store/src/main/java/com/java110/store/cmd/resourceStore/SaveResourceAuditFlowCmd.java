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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.oaWorkflow.OaWorkflowDto;
import com.java110.dto.oaWorkflow.WorkflowModelDto;
import com.java110.dto.resource.ResourceAuditFlowDto;
import com.java110.intf.common.IWorkflowInnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowInnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowXmlInnerServiceSMO;
import com.java110.intf.store.IResourceAuditFlowV1InnerServiceSMO;
import com.java110.po.oaWorkflow.OaWorkflowPo;
import com.java110.po.oaWorkflow.OaWorkflowXmlPo;
import com.java110.po.resource.ResourceAuditFlowPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.BpmnXml;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：保存资源审核流程命令类
 * 服务编码：resourceAuditFlow.saveResourceAuditFlow
 * 请求路劲：/app/resourceAuditFlow.SaveResourceAuditFlow
 * 功能描述：该类负责处理资源审核流程的保存操作，包括创建OA工作流、资源审核流程记录等
 * add by 吴学文 at 2023-03-17 01:00:14 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "resourceStore.saveResourceAuditFlow")
public class SaveResourceAuditFlowCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveResourceAuditFlowCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 资源审核流程服务接口
     */
    @Autowired
    private IResourceAuditFlowV1InnerServiceSMO resourceAuditFlowV1InnerServiceSMOImpl;

    /**
     * 工作流服务接口
     */
    @Autowired
    private IWorkflowInnerServiceSMO workflowInnerServiceSMOImpl;

    /**
     * OA工作流服务接口
     */
    @Autowired
    private IOaWorkflowInnerServiceSMO oaWorkflowInnerServiceSMOImpl;

    /**
     * OA工作流XML服务接口
     */
    @Autowired
    private IOaWorkflowXmlInnerServiceSMO oaWorkflowXmlInnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * @param event CMD事件对象
     * @param cmdDataFlowContext CMD数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数校验失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验必填参数
        Assert.hasKeyAndValue(reqJson, "auditType", "请求报文中未包含flowType");
        Assert.hasKeyAndValue(reqJson, "flowName", "请求报文中未包含flowType");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含小区");
    }

    /**
     * 执行保存资源审核流程命令
     * 
     * @param event CMD事件对象
     * @param cmdDataFlowContext CMD数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取商户ID
        String storeId = CmdContextUtils.getStoreId(cmdDataFlowContext);

        // 创建OA工作流对象
        OaWorkflowPo oaWorkflowPo = new OaWorkflowPo();
        oaWorkflowPo.setStoreId(storeId);
        // 生成工作流ID
        oaWorkflowPo.setFlowId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_flowId));
        oaWorkflowPo.setFlowName(reqJson.getString("flowName"));

        // 根据审核类型设置工作流类型
        String auditType = reqJson.getString("auditType");
        if(ResourceAuditFlowDto.AUDIT_TYPE_PURCHASE_APPLY.equals(auditType)) {
            // 采购申请类型
            oaWorkflowPo.setFlowType(OaWorkflowDto.FLOW_TYPE_PURCHASE_APPLY);
        }else if(ResourceAuditFlowDto.AUDIT_TYPE_RESOURCE_OUT.equals(auditType)) {
            // 资源出库类型
            oaWorkflowPo.setFlowType(OaWorkflowDto.FLOW_TYPE_RESOURCE_OUT);
        }else{
            // 调拨类型
            oaWorkflowPo.setFlowType(OaWorkflowDto.FLOW_TYPE_ALLOCATION);
        }

        // 创建工作流模型
        WorkflowModelDto workflowModelDto = new WorkflowModelDto();
        workflowModelDto.setName(oaWorkflowPo.getFlowName());
        workflowModelDto.setKey(oaWorkflowPo.getFlowId());
        workflowModelDto = workflowInnerServiceSMOImpl.createModel(workflowModelDto);

        // 设置工作流模型相关信息
        oaWorkflowPo.setModelId(workflowModelDto.getModelId());
        oaWorkflowPo.setFlowKey(workflowModelDto.getKey());
        oaWorkflowPo.setState(OaWorkflowDto.STATE_WAIT);
        
        // 保存OA工作流
        int flag = oaWorkflowInnerServiceSMOImpl.saveOaWorkflow(oaWorkflowPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 创建默认的工作流XML配置，防止流程图绘制错误
        OaWorkflowXmlPo oaWorkflowXmlPo = new OaWorkflowXmlPo();
        oaWorkflowXmlPo.setStoreId(oaWorkflowPo.getStoreId());
        oaWorkflowXmlPo.setFlowId(oaWorkflowPo.getFlowId());
        oaWorkflowXmlPo.setXmlId(GenerateCodeFactory.getGeneratorId("79"));
        oaWorkflowXmlPo.setSvgXml("");
        // 设置默认的BPMN XML配置
        oaWorkflowXmlPo.setBpmnXml(BpmnXml.getResourceBpmnXml(oaWorkflowPo.getFlowId()));

        // 保存工作流XML配置
        flag = oaWorkflowXmlInnerServiceSMOImpl.saveOaWorkflowXml(oaWorkflowXmlPo);
        if (flag < 1) {
            throw new CmdException("保存模型数据失败");
        }

        // 创建资源审核流程记录
        ResourceAuditFlowPo resourceAuditFlowPo = BeanConvertUtil.covertBean(reqJson, ResourceAuditFlowPo.class);
        resourceAuditFlowPo.setStoreId(storeId);
        // 生成资源审核流程ID
        resourceAuditFlowPo.setRafId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        resourceAuditFlowPo.setFlowId(oaWorkflowPo.getFlowId());
        
        // 保存资源审核流程
        flag = resourceAuditFlowV1InnerServiceSMOImpl.saveResourceAuditFlow(resourceAuditFlowPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}