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
package com.java110.report.cmd.reportCustomComponentRel;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.report.IReportCustomComponentRelV1InnerServiceSMO;
import com.java110.po.reportCustom.ReportCustomComponentRelPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：删除报表自定义组件关系命令类
 * 服务编码：reportCustomComponentRel.deleteReportCustomComponentRel
 * 请求路劲：/app/reportCustomComponentRel.DeleteReportCustomComponentRel
 * add by 吴学文 at 2021-11-09 13:28:13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该命令类用于处理删除报表自定义组件关系的业务逻辑，包括参数验证和删除操作
 * 主要职责：
 * 1. 验证删除操作的必要参数
 * 2. 执行删除报表自定义组件关系的业务逻辑
 * 3. 处理删除结果并返回响应
 */
@Java110Cmd(serviceCode = "reportCustomComponentRel.deleteReportCustomComponentRel")
public class DeleteReportCustomComponentRelCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteReportCustomComponentRelCmd.class);

    /**
     * 报表自定义组件关系服务接口
     */
    @Autowired
    private IReportCustomComponentRelV1InnerServiceSMO reportCustomComponentRelV1InnerServiceSMOImpl;

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
        // 验证请求参数中必须包含relId字段
        Assert.hasKeyAndValue(reqJson, "relId", "relId不能为空");
        // 验证请求参数中必须包含customId字段
        Assert.hasKeyAndValue(reqJson, "customId", "customId不能为空");
    }

    /**
     * 执行删除报表自定义组件关系的命令方法
     * 该方法使用事务注解确保删除操作的原子性
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为报表自定义组件关系PO对象
        ReportCustomComponentRelPo reportCustomComponentRelPo = BeanConvertUtil.covertBean(reqJson, ReportCustomComponentRelPo.class);
        
        // 调用服务层方法执行删除操作，返回受影响的行数
        int flag = reportCustomComponentRelV1InnerServiceSMOImpl.deleteReportCustomComponentRel(reportCustomComponentRelPo);

        // 检查删除操作是否成功，如果受影响行数小于1则认为删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}