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
package com.java110.report.cmd.reportCustomComponent;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.report.IReportCustomComponentV1InnerServiceSMO;
import com.java110.po.reportCustom.ReportCustomComponentPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：删除
 * 服务编码：reportCustomComponent.deleteReportCustomComponent
 * 请求路劲：/app/reportCustomComponent.DeleteReportCustomComponent
 * add by 吴学文 at 2021-11-09 13:18:41 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 删除报表自定义组件命令类
 * 
 * 该类负责处理删除报表自定义组件的业务逻辑，继承自Cmd基类，
 * 通过注解@Java110Cmd指定服务编码，用于路由到对应的命令处理器
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2021-11-09
 */
@Java110Cmd(serviceCode = "reportCustomComponent.deleteReportCustomComponent")
public class DeleteReportCustomComponentCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteReportCustomComponentCmd.class);

    /**
     * 报表自定义组件服务接口
     * 用于调用删除报表自定义组件的业务方法
     */
    @Autowired
    private IReportCustomComponentV1InnerServiceSMO reportCustomComponentV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保删除操作的有效性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含componentId字段
        Assert.hasKeyAndValue(reqJson, "componentId", "componentId不能为空");
        // 重复验证componentId字段，确保该字段存在且不为空
        Assert.hasKeyAndValue(reqJson, "componentId", "componentId不能为空");
    }

    /**
     * 执行删除命令方法
     * 
     * 该方法负责执行实际的删除操作，将请求参数转换为PO对象，
     * 调用服务层进行数据删除，并处理删除结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为报表自定义组件PO对象
        ReportCustomComponentPo reportCustomComponentPo = BeanConvertUtil.covertBean(reqJson, ReportCustomComponentPo.class);
        
        // 调用服务层接口执行删除操作，返回影响的行数
        int flag = reportCustomComponentV1InnerServiceSMOImpl.deleteReportCustomComponent(reportCustomComponentPo);

        // 检查删除操作是否成功，如果影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}