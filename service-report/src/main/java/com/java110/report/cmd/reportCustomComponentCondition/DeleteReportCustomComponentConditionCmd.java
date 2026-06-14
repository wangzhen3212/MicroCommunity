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
package com.java110.report.cmd.reportCustomComponentCondition;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.report.IReportCustomComponentConditionV1InnerServiceSMO;
import com.java110.po.reportCustom.ReportCustomComponentConditionPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：删除报表自定义组件条件命令类
 * 服务编码：reportCustomComponentCondition.deleteReportCustomComponentCondition
 * 请求路劲：/app/reportCustomComponentCondition.DeleteReportCustomComponentCondition
 * add by 吴学文 at 2021-11-09 13:33:12 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该命令类用于处理删除报表自定义组件条件的业务逻辑，包括参数验证和执行删除操作
 */
@Java110Cmd(serviceCode = "reportCustomComponentCondition.deleteReportCustomComponentCondition")
public class DeleteReportCustomComponentConditionCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteReportCustomComponentConditionCmd.class);

    /**
     * 报表自定义组件条件服务接口
     */
    @Autowired
    private IReportCustomComponentConditionV1InnerServiceSMO reportCustomComponentConditionV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的字段
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含conditionId字段
        Assert.hasKeyAndValue(reqJson, "conditionId", "conditionId不能为空");
        // 重复验证conditionId字段，确保该字段不为空
        Assert.hasKeyAndValue(reqJson, "conditionId", "conditionId不能为空");
    }

    /**
     * 执行删除命令方法
     * 将请求参数转换为PO对象并调用服务层执行删除操作
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为报表自定义组件条件PO对象
        ReportCustomComponentConditionPo reportCustomComponentConditionPo = BeanConvertUtil.covertBean(reqJson, ReportCustomComponentConditionPo.class);
        
        // 调用服务层接口执行删除操作，返回受影响的行数
        int flag = reportCustomComponentConditionV1InnerServiceSMOImpl.deleteReportCustomComponentCondition(reportCustomComponentConditionPo);

        // 检查删除操作是否成功，如果受影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}