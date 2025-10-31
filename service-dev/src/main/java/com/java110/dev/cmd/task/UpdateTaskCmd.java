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
import com.java110.intf.dev.ITaskAttrV1InnerServiceSMO;
import com.java110.intf.dev.ITaskV1InnerServiceSMO;
import com.java110.po.task.TaskPo;
import com.java110.po.task.TaskAttrPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：更新任务信息
 * 服务编码：task.updateTask
 * 请求路径：/app/task.UpdateTask
 * add by 吴学文 at 2022-08-05 10:23:14 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "task.updateTask")
public class UpdateTaskCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateTaskCmd.class);

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
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验必填参数
        Assert.hasKeyAndValue(reqJson, "taskId", "taskId不能为空");
        Assert.hasKeyAndValue(reqJson, "taskName", "请求报文中未包含taskName");
        Assert.hasKeyAndValue(reqJson, "templateId", "请求报文中未包含templateId");
        Assert.hasKeyAndValue(reqJson, "taskCron", "请求报文中未包含taskCron");
    }

    /**
     * 执行更新任务命令
     * 
     * @param event 命令事件
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 更新任务信息
        updateTask(reqJson);

        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 更新任务信息及任务属性
     * 
     * @param paramInJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    public void updateTask(JSONObject paramInJson) {
        // 将JSON参数转换为任务PO对象
        TaskPo taskPo = BeanConvertUtil.covertBean(paramInJson, TaskPo.class);
        
        // 更新任务基本信息
        int flag = taskV1InnerServiceSMOImpl.updateTask(taskPo);

        // 检查更新结果
        if (flag < 1) {
            throw new CmdException("更新任务数据失败");
        }

        // 如果没有模板规格参数，直接返回
        if (!paramInJson.containsKey("templateSpecs")) {
            return;
        }

        // 处理任务属性信息
        JSONArray templateSpecs = paramInJson.getJSONArray("templateSpecs");
        JSONObject specObj = null;
        TaskAttrPo taskAttrPo = null;
        
        // 遍历所有模板规格
        for (int specIndex = 0; specIndex < templateSpecs.size(); specIndex++) {
            specObj = templateSpecs.getJSONObject(specIndex);
            
            // 如果属性ID存在且不为-1，则更新现有属性
            if (specObj.containsKey("attrId") && !"-1".equals(specObj.getString("attrId"))) {
                taskAttrPo = new TaskAttrPo();
                taskAttrPo.setAttrId(specObj.getString("attrId"));
                taskAttrPo.setTaskId(taskPo.getTaskId());
                taskAttrPo.setValue(specObj.getString("value"));
                taskAttrPo.setSpecCd(specObj.getString("specCd"));
                
                flag = taskAttrV1InnerServiceSMOImpl.updateTaskAttr(taskAttrPo);
                if (flag < 1) {
                    throw new CmdException("更新任务属性数据失败");
                }
                continue;
            }
            
            // 否则创建新的任务属性
            taskAttrPo = new TaskAttrPo();
            taskAttrPo.setAttrId("-" + (specIndex + 1)); // 生成临时属性ID
            taskAttrPo.setTaskId(taskPo.getTaskId());
            taskAttrPo.setValue(specObj.getString("value"));
            taskAttrPo.setSpecCd(specObj.getString("specCd"));
            
            flag = taskAttrV1InnerServiceSMOImpl.saveTaskAttr(taskAttrPo);
            if (flag < 1) {
                throw new CmdException("保存任务属性数据失败");
            }
        }
    }
}