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
package com.java110.user.cmd.examine;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IExamineStaffValueV1InnerServiceSMO;
import com.java110.po.examine.ExamineStaffValuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除员工考核值命令类
 * 
 * 该类负责处理删除员工考核值的业务逻辑，包括参数验证和执行删除操作
 * 服务编码：examineStaffValue.deleteExamineStaffValue
 * 请求路径：/app/examineStaffValue.DeleteExamineStaffValue
 * 
 * @author 吴学文 at 2023-03-07 16:34:48 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 * @see IExamineStaffValueV1InnerServiceSMO
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @official website: http://www.homecommunity.cn
 */
@Java110Cmd(serviceCode = "examine.deleteExamineStaffValue")
public class DeleteExamineStaffValueCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteExamineStaffValueCmd.class);

    /**
     * 员工考核值服务接口
     */
    @Autowired
    private IExamineStaffValueV1InnerServiceSMO examineStaffValueV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含esvId字段
        Assert.hasKeyAndValue(reqJson, "esvId", "esvId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除员工考核值命令
     * 
     * 将请求参数转换为PO对象，调用服务层执行删除操作，并处理删除结果
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
        // 将请求JSON对象转换为员工考核值PO对象
        ExamineStaffValuePo examineStaffValuePo = BeanConvertUtil.covertBean(reqJson, ExamineStaffValuePo.class);
        
        // 调用服务层删除员工考核值记录，返回受影响的行数
        int flag = examineStaffValueV1InnerServiceSMOImpl.deleteExamineStaffValue(examineStaffValuePo);

        // 检查删除操作是否成功，如果受影响行数小于1表示删除失败
        if (flag < 1) {
            // 删除失败时抛出命令异常
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}