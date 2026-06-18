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
package com.java110.store.cmd.complaintAppraise;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IComplaintAppraiseV1InnerServiceSMO;
import com.java110.po.complaint.ComplaintAppraisePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 投诉评价删除命令类
 * 
 * 该类负责处理投诉评价的删除操作，包括参数验证和业务逻辑执行
 * 服务编码：complaintAppraise.deleteComplaintAppraise
 * 请求路径：/app/complaintAppraise.DeleteComplaintAppraise
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2024-02-21 13:04:55
 * @mail 928255095@qq.com
 * @see Cmd
 */
@Java110Cmd(serviceCode = "complaintAppraise.deleteComplaintAppraise")
public class DeleteComplaintAppraiseCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteComplaintAppraiseCmd.class);

    /**
     * 投诉评价服务接口
     */
    @Autowired
    private IComplaintAppraiseV1InnerServiceSMO complaintAppraiseV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证评价ID不能为空
        Assert.hasKeyAndValue(reqJson, "appraiseId", "appraiseId不能为空");
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除投诉评价命令
     * 
     * 将请求参数转换为PO对象，调用服务层删除方法，并处理删除结果
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为投诉评价PO对象
        ComplaintAppraisePo complaintAppraisePo = BeanConvertUtil.covertBean(reqJson, ComplaintAppraisePo.class);
        
        // 调用服务层删除方法，返回影响的行数
        int flag = complaintAppraiseV1InnerServiceSMOImpl.deleteComplaintAppraise(complaintAppraisePo);

        // 检查删除操作是否成功，影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}