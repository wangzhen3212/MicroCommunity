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

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.dev.ITaskV1InnerServiceSMO;
import com.java110.po.task.TaskPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：删除任务命令类
 * 服务编码：task.deleteTask
 * 请求路径：/app/task.DeleteTask
 * 
 * 功能描述：
 * 该命令类用于处理删除任务的业务逻辑，包括参数验证和数据库删除操作。
 * 通过注解标识服务编码，系统会根据该编码路由到对应的命令处理器。
 * 
 * 主要流程：
 * 1. 验证请求参数中是否包含必要的taskId
 * 2. 将请求参数转换为任务持久化对象
 * 3. 调用服务层删除任务数据
 * 4. 处理删除结果并返回响应
 * 
 * 注意事项：
 * - 使用事务注解确保数据一致性
 * - 删除操作需要验证操作权限
 * - 删除失败时会抛出CmdException异常
 * 
 * @author 吴学文 at 2022-08-05 10:23:14 
 * mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "task.deleteTask")
public class DeleteTaskCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteTaskCmd.class);

    /**
     * 任务服务接口，用于调用任务相关的业务逻辑
     */
    @Autowired
    private ITaskV1InnerServiceSMO taskV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能说明：
     * 验证请求参数是否完整和有效，确保删除操作的必要参数存在
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * 
     * 验证规则：
     * - taskId不能为空，用于标识要删除的任务
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含taskId
        Assert.hasKeyAndValue(reqJson, "taskId", "taskId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 功能说明：
     * 执行具体的任务删除逻辑，包括数据转换、服务调用和结果处理
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     * 
     * 执行流程：
     * 1. 将JSON请求参数转换为TaskPo对象
     * 2. 调用服务层删除任务数据
     * 3. 检查删除结果，失败则抛出异常
     * 4. 设置成功响应
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 记录调试日志，便于问题排查
        logger.debug("开始执行任务删除操作，请求参数：{}", reqJson.toJSONString());

        // 将请求JSON对象转换为任务持久化对象
        TaskPo taskPo = BeanConvertUtil.covertBean(reqJson, TaskPo.class);
        
        // 调用服务层删除任务，返回影响的行数
        int flag = taskV1InnerServiceSMOImpl.deleteTask(taskPo);

        // 检查删除结果，如果影响行数小于1说明删除失败
        if (flag < 1) {
            logger.error("删除任务失败，taskId：{}", taskPo.getTaskId());
            throw new CmdException("删除数据失败");
        }

        // 记录成功日志
        logger.info("任务删除成功，taskId：{}", taskPo.getTaskId());
        
        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}