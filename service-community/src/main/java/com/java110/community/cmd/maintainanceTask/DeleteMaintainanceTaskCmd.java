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
package com.java110.community.cmd.maintainanceTask;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.community.IMaintainanceTaskV1InnerServiceSMO;
import com.java110.po.maintainance.MaintainanceTaskPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 维护任务删除命令类
 * 
 * 该类负责处理维护任务的删除操作，实现了删除维护任务的业务逻辑。
 * 通过注解标识服务编码，接收前端请求并执行相应的删除操作。
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2022-11-08 15:51:01
 * @mail 928255095@qq.com
 * @see Cmd
 * @see IMaintainanceTaskV1InnerServiceSMO
 * @see MaintainanceTaskPo
 */
@Java110Cmd(serviceCode = "maintainanceTask.deleteMaintainanceTask")
public class DeleteMaintainanceTaskCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteMaintainanceTaskCmd.class);

    /**
     * 维护任务服务接口
     * 用于调用维护任务的删除操作
     */
    @Autowired
    private IMaintainanceTaskV1InnerServiceSMO maintainanceTaskV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保删除操作的有效性。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含taskId字段
        Assert.hasKeyAndValue(reqJson, "taskId", "taskId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除命令方法
     * 
     * 将请求参数转换为维护任务对象，调用服务层执行删除操作，
     * 并根据删除结果设置响应信息。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为维护任务POJO对象
        MaintainanceTaskPo maintainanceTaskPo = BeanConvertUtil.covertBean(reqJson, MaintainanceTaskPo.class);
        
        // 调用服务层删除维护任务，返回影响的行数
        int flag = maintainanceTaskV1InnerServiceSMOImpl.deleteMaintainanceTask(maintainanceTaskPo);

        // 检查删除操作是否成功，影响行数小于1表示删除失败
        if (flag < 1) {
            // 删除失败时抛出异常
            throw new CmdException("删除数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}