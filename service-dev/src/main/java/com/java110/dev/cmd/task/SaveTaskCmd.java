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
package com.java110.dev.cmd.task;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.dev.ITaskAttrV1InnerServiceSMO;
import com.java110.intf.dev.ITaskV1InnerServiceSMO;
import com.java110.po.task.TaskPo;
import com.java110.po.task.TaskAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存任务信息
 * 服务编码：task.saveTask
 * 请求路径：/app/task.SaveTask
 * add by 吴学文 at 2022-08-05 10:23:14 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "task.saveTask")
public class SaveTaskCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveTaskCmd.class);

    /**
     * 任务ID前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 任务服务接口
     */
    @Autowired
    private ITaskV1InnerServiceSMO taskV1InnerServiceSMOImpl;

    /**
     * 任务属性服务接口
     */
    @Autowired
    private ITaskAttrV1InnerServiceSMO taskAttrV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验必填参数
        Assert.hasKeyAndValue(reqJson, "taskName", "请求报文中未包含taskName");
        Assert.hasKeyAndValue(reqJson, "templateId", "请求报文中未包含templateId");
        Assert.hasKeyAndValue(reqJson, "taskCron", "请求报文中未包含taskCron");
    }

    /**
     * 执行保存任务命令
     * 处理任务信息的保存逻辑，包含事务管理
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 添加任务信息
        addTask(reqJson);

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 添加任务信息
     * 保存任务基本信息及任务属性信息
     * 
     * @param paramInJson 接口调用方传入的参数
     * @throws CmdException 当保存数据失败时抛出异常
     */
    public void addTask(JSONObject paramInJson) {
        // 将JSON参数转换为任务PO对象
        TaskPo taskPo = BeanConvertUtil.covertBean(paramInJson, TaskPo.class);
        
        // 生成任务ID并设置任务状态
        taskPo.setTaskId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_taskId));
        taskPo.setState("001"); // 001表示启用状态
        
        // 保存任务基本信息
        int flag = taskV1InnerServiceSMOImpl.saveTask(taskPo);

        // 检查保存结果
        if (flag < 1) {
            throw new CmdException("保存任务数据失败");
        }

        // 如果请求参数中包含模板规格信息，则保存任务属性
        if (!paramInJson.containsKey("templateSpecs")) {
            return;
        }

        // 获取模板规格数组
        JSONArray templateSpecs = paramInJson.getJSONArray("templateSpecs");
        TaskAttrPo taskAttrPo = null;
        JSONObject tmpSpecObj = null;
        
        // 遍历模板规格，保存每个任务属性
        for (int specIndex = 0; specIndex < templateSpecs.size(); specIndex++) {
            taskAttrPo = new TaskAttrPo();
            tmpSpecObj = templateSpecs.getJSONObject(specIndex);
            
            // 设置任务属性信息
            taskAttrPo.setAttrId("-" + (specIndex + 1)); // 临时属性ID
            taskAttrPo.setSpecCd(tmpSpecObj.getString("specCd")); // 规格编码
            taskAttrPo.setTaskId(taskPo.getTaskId()); // 关联任务ID
            taskAttrPo.setValue(tmpSpecObj.getString("value")); // 属性值
            
            // 保存任务属性
            flag = taskAttrV1InnerServiceSMOImpl.saveTaskAttr(taskAttrPo);
            
            // 检查保存结果
            if (flag < 1) {
                throw new CmdException("保存任务属性数据失败");
            }
        }
    }
}