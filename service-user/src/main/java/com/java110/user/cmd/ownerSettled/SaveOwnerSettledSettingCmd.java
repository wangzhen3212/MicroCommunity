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
package com.java110.user.cmd.ownerSettled;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.oaWorkflow.OaWorkflowDto;
import com.java110.dto.oaWorkflow.WorkflowModelDto;
import com.java110.intf.common.IWorkflowInnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowInnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowXmlInnerServiceSMO;
import com.java110.intf.user.IOwnerSettledSettingV1InnerServiceSMO;
import com.java110.po.oaWorkflow.OaWorkflowPo;
import com.java110.po.oaWorkflow.OaWorkflowXmlPo;
import com.java110.po.owner.OwnerSettledSettingPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.BpmnXml;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 业主入驻设置保存命令类
 * 
 * 该类负责处理业主入驻设置的新增操作，包括创建工作流模型、保存工作流配置和业主入驻设置信息
 * 服务编码：ownerSettled.saveOwnerSettledSetting
 * 请求路径：/app/ownerSettledSetting.SaveOwnerSettledSetting
 * 
 * @author 吴学文 at 2023-01-26 00:45:55 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "ownerSettled.saveOwnerSettledSetting")
public class SaveOwnerSettledSettingCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveOwnerSettledSettingCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 业主入驻设置服务接口
     */
    @Autowired
    private IOwnerSettledSettingV1InnerServiceSMO ownerSettledSettingV1InnerServiceSMOImpl;

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
     * OA工作流XML配置服务接口
     */
    @Autowired
    private IOaWorkflowXmlInnerServiceSMO oaWorkflowXmlInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含settingName字段
        Assert.hasKeyAndValue(reqJson, "settingName", "请求报文中未包含settingName");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行保存业主入驻设置命令
     * 
     * 该方法执行以下操作：
     * 1. 创建工作流模型
     * 2. 保存工作流配置信息
     * 3. 保存默认的工作流XML配置
     * 4. 保存业主入驻设置信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取店铺ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
        
        // 创建工作流PO对象
        OaWorkflowPo oaWorkflowPo = new OaWorkflowPo();
        oaWorkflowPo.setStoreId(storeId);
        // 生成工作流ID
        oaWorkflowPo.setFlowId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_flowId));
        // 设置工作流名称，基于设置名称加上"审批流程"后缀
        oaWorkflowPo.setFlowName(reqJson.getString("settingName")+"审批流程");
        // 设置工作流类型为业主入驻类型
        oaWorkflowPo.setFlowType(OaWorkflowDto.FLOW_TYPE_OWNER_SETTLED);

        // 创建工作流模型DTO对象
        WorkflowModelDto workflowModelDto = new WorkflowModelDto();
        workflowModelDto.setName(oaWorkflowPo.getFlowName());
        workflowModelDto.setKey(oaWorkflowPo.getFlowId());
        // 调用工作流服务创建模型
        workflowModelDto = workflowInnerServiceSMOImpl.createModel(workflowModelDto);

        // 设置工作流PO对象的模型相关信息
        oaWorkflowPo.setModelId(workflowModelDto.getModelId());
        oaWorkflowPo.setFlowKey(workflowModelDto.getKey());
        // 设置工作流状态为等待状态
        oaWorkflowPo.setState(OaWorkflowDto.STATE_WAIT);
        // 保存工作流配置信息
        int flag = oaWorkflowInnerServiceSMOImpl.saveOaWorkflow(oaWorkflowPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 创建工作流XML配置PO对象，用于保存默认的工作流配置
        OaWorkflowXmlPo oaWorkflowXmlPo = new OaWorkflowXmlPo();
        oaWorkflowXmlPo.setStoreId(storeId);
        oaWorkflowXmlPo.setFlowId(oaWorkflowPo.getFlowId());
        // 生成XML配置ID
        oaWorkflowXmlPo.setXmlId(GenerateCodeFactory.getGeneratorId("79"));
        oaWorkflowXmlPo.setSvgXml("");
        // 设置默认的业主入驻BPMN XML配置
        oaWorkflowXmlPo.setBpmnXml(BpmnXml.getDefaultOwnerSettledBpmnXml(oaWorkflowPo.getFlowId()));

        // 保存工作流XML配置信息
        flag = oaWorkflowXmlInnerServiceSMOImpl.saveOaWorkflowXml(oaWorkflowXmlPo);
        if (flag < 1) {
            throw new CmdException("保存模型数据失败");
        }

        // 将请求参数转换为业主入驻设置PO对象
        OwnerSettledSettingPo ownerSettledSettingPo = BeanConvertUtil.covertBean(reqJson, OwnerSettledSettingPo.class);
        // 生成设置ID
        ownerSettledSettingPo.setSettingId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        // 设置关联的工作流ID和名称
        ownerSettledSettingPo.setFlowId(oaWorkflowPo.getFlowId());
        ownerSettledSettingPo.setFlowName(oaWorkflowPo.getFlowName());
        // 保存业主入驻设置信息
        flag = ownerSettledSettingV1InnerServiceSMOImpl.saveOwnerSettledSetting(ownerSettledSettingPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}