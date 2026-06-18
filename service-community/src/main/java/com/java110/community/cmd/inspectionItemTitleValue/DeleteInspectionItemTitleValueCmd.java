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
package com.java110.community.cmd.inspectionItemTitleValue;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.community.IInspectionItemTitleValueV1InnerServiceSMO;
import com.java110.po.inspection.InspectionItemTitleValuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除巡检项标题值命令类
 * 
 * 该类负责处理删除巡检项标题值的业务逻辑，是巡检项标题值删除功能的核心处理器。
 * 通过注解标识服务编码，接收前端请求并执行删除操作。
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2022-03-15 11:10:16
 * @mail 928255095@qq.com
 * @see Cmd
 */
@Java110Cmd(serviceCode = "inspectionItemTitleValue.deleteInspectionItemTitleValue")
public class DeleteInspectionItemTitleValueCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteInspectionItemTitleValueCmd.class);

    /**
     * 巡检项标题值服务接口
     */
    @Autowired
    private IInspectionItemTitleValueV1InnerServiceSMO inspectionItemTitleValueV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证删除巡检项标题值请求中的必要参数是否完整。
     * 主要验证valueId和communityId参数是否存在且不为空。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证valueId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "valueId", "valueId不能为空");
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除巡检项标题值命令
     * 
     * 将请求参数转换为PO对象，调用服务层执行删除操作，
     * 并根据删除结果设置响应信息。
     * 该方法使用事务注解确保操作的原子性。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为巡检项标题值PO对象
        InspectionItemTitleValuePo inspectionItemTitleValuePo = BeanConvertUtil.covertBean(reqJson, InspectionItemTitleValuePo.class);
        
        // 调用服务层执行删除操作，返回影响的行数
        int flag = inspectionItemTitleValueV1InnerServiceSMOImpl.deleteInspectionItemTitleValue(inspectionItemTitleValuePo);

        // 检查删除操作是否成功，如果影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置成功的响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}