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
package com.java110.community.cmd.visit;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.oaWorkflow.OaWorkflowDto;
import com.java110.dto.parking.ParkingAreaDto;
import com.java110.dto.oaWorkflow.WorkflowModelDto;
import com.java110.intf.common.IWorkflowInnerServiceSMO;
import com.java110.intf.community.IParkingAreaInnerServiceSMO;
import com.java110.intf.community.IVisitSettingV1InnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowInnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowXmlInnerServiceSMO;
import com.java110.po.oaWorkflow.OaWorkflowPo;
import com.java110.po.oaWorkflow.OaWorkflowXmlPo;
import com.java110.po.visit.VisitSettingPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.BpmnXml;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：保存访客设置命令类
 * 服务编码：visitSetting.saveVisitSetting
 * 请求路劲：/app/visitSetting.SaveVisitSetting
 * 功能描述：该类负责处理访客设置的新增操作，包括创建审批流程、保存访客设置信息等业务逻辑
 * add by 吴学文 at 2023-01-18 14:43:29 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "visit.saveVisitSetting")
public class SaveVisitSettingCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveVisitSettingCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 访客设置服务接口
     */
    @Autowired
    private IVisitSettingV1InnerServiceSMO visitSettingV1InnerServiceSMOImpl;

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
     * 停车场区域服务接口
     */
    @Autowired
    private IParkingAreaInnerServiceSMO parkingAreaInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     * 功能描述：验证请求参数是否完整，确保必要参数都存在
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "typeName", "请求报文中未包含typeName");
        Assert.hasKeyAndValue(reqJson, "faceWay", "请求报文中未包含faceWay");
        Assert.hasKeyAndValue(reqJson, "carNumWay", "请求报文中未包含carNumWay");
//        Assert.hasKeyAndValue(reqJson, "paId", "请求报文中未包含paId");

        Assert.hasKeyAndValue(reqJson, "auditWay", "请求报文中未包含auditWay");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
    }

    /**
     * 执行保存访客设置命令
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     * 功能描述：处理访客设置的新增逻辑，包括创建审批流程、保存访客设置信息等
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取商户ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
        
        // 创建OA工作流对象
        OaWorkflowPo oaWorkflowPo = new OaWorkflowPo();
        oaWorkflowPo.setStoreId(storeId);
        // 生成工作流ID
        oaWorkflowPo.setFlowId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_flowId));
        // 设置工作流名称为访客类型名称+审批流程
        oaWorkflowPo.setFlowName(reqJson.getString("typeName") + "审批流程");
        oaWorkflowPo.setFlowType(OaWorkflowDto.FLOW_TYPE_VISIT);
        
        // 创建工作流模型
        WorkflowModelDto workflowModelDto = new WorkflowModelDto();
        workflowModelDto.setName(oaWorkflowPo.getFlowName());
        workflowModelDto.setKey(oaWorkflowPo.getFlowId());
        workflowModelDto = workflowInnerServiceSMOImpl.createModel(workflowModelDto);
        
        // 设置模型相关属性
        oaWorkflowPo.setModelId(workflowModelDto.getModelId());
        oaWorkflowPo.setFlowKey(workflowModelDto.getKey());
        oaWorkflowPo.setState(OaWorkflowDto.STATE_WAIT);
        
        // 保存工作流信息
        int flag = oaWorkflowInnerServiceSMOImpl.saveOaWorkflow(oaWorkflowPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 创建工作流XML配置，提供默认流程图
        OaWorkflowXmlPo oaWorkflowXmlPo = new OaWorkflowXmlPo();
        oaWorkflowXmlPo.setStoreId(storeId);
        oaWorkflowXmlPo.setFlowId(oaWorkflowPo.getFlowId());
        oaWorkflowXmlPo.setXmlId(GenerateCodeFactory.getGeneratorId("79"));
        oaWorkflowXmlPo.setSvgXml("");
        // 设置默认的BPMN流程图XML
        oaWorkflowXmlPo.setBpmnXml(BpmnXml.getDefaultVisitBpmnXml(oaWorkflowPo.getFlowId()));

        // 保存工作流XML配置
        flag = oaWorkflowXmlInnerServiceSMOImpl.saveOaWorkflowXml(oaWorkflowXmlPo);
        if (flag < 1) {
            throw new CmdException("保存模型数据失败");
        }

        // 创建访客设置对象并设置属性
        VisitSettingPo visitSettingPo = BeanConvertUtil.covertBean(reqJson, VisitSettingPo.class);
        visitSettingPo.setSettingId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        visitSettingPo.setFlowId(oaWorkflowPo.getFlowId());
        visitSettingPo.setFlowName(oaWorkflowPo.getFlowName());
        
        // 处理访客数量，如果为空则设置为0
        if (StringUtil.isEmpty(reqJson.getString("visitNumber"))) {
            visitSettingPo.setVisitNumber("0");
        }
        
        // 如果设置了停车场ID，验证停车场是否存在并获取停车场编号
        if (!StringUtil.isEmpty(visitSettingPo.getPaId())) {
            ParkingAreaDto parkingAreaDto = new ParkingAreaDto();
            parkingAreaDto.setCommunityId(reqJson.getString("communityId"));
            parkingAreaDto.setPaId(reqJson.getString("paId"));
            List<ParkingAreaDto> parkingAreaDtos = parkingAreaInnerServiceSMOImpl.queryParkingAreas(parkingAreaDto);
            // 验证停车场存在且唯一
            Assert.listOnlyOne(parkingAreaDtos, "停车场不存在");
            visitSettingPo.setPaNum(parkingAreaDtos.get(0).getNum());
        }
        
        // 保存访客设置信息
        flag = visitSettingV1InnerServiceSMOImpl.saveVisitSetting(visitSettingPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}