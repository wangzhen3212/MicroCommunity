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
package com.java110.report.cmd.reportCustomComponentFooter;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.report.IReportCustomComponentFooterV1InnerServiceSMO;
import com.java110.po.reportCustom.ReportCustomComponentFooterPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.Base64Convert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存报表自定义组件页脚信息
 * 服务编码：reportCustomComponentFooter.saveReportCustomComponentFooter
 * 请求路径：/app/reportCustomComponentFooter.SaveReportCustomComponentFooter
 * 功能描述：该命令类用于处理保存报表自定义组件页脚信息的请求，包括参数验证、数据转换和持久化操作
 * add by 吴学文 at 2021-11-14 01:32:10 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "reportCustomComponentFooter.saveReportCustomComponentFooter")
public class SaveReportCustomComponentFooterCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveReportCustomComponentFooterCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 报表自定义组件页脚服务接口
     */
    @Autowired
    private IReportCustomComponentFooterV1InnerServiceSMO reportCustomComponentFooterV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数是否包含必要的字段
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含componentId字段
        Assert.hasKeyAndValue(reqJson, "componentId", "请求报文中未包含componentId");
        // 验证请求报文中必须包含name字段
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
    }

    /**
     * 命令执行方法
     * 功能描述：处理保存报表自定义组件页脚的核心业务逻辑，包括数据转换、编码处理和持久化操作
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为实体对象
        ReportCustomComponentFooterPo reportCustomComponentFooterPo = BeanConvertUtil.covertBean(reqJson, ReportCustomComponentFooterPo.class);
        // 生成唯一的页脚ID
        reportCustomComponentFooterPo.setFooterId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        try {
            // 如果组件SQL不为空，则进行Base64编码处理
            if (!StringUtil.isEmpty(reportCustomComponentFooterPo.getComponentSql())) {
                reportCustomComponentFooterPo.setComponentSql(Base64Convert.byteToBase64(reportCustomComponentFooterPo.getComponentSql().getBytes("UTF-8")));
            }
            // 如果JavaScript代码不为空，则进行Base64编码处理
            if (!StringUtil.isEmpty(reportCustomComponentFooterPo.getJavaScript())) {
                reportCustomComponentFooterPo.setJavaScript(Base64Convert.byteToBase64(reportCustomComponentFooterPo.getJavaScript().getBytes("UTF-8")));
            }
        } catch (Exception e) {
            // 记录编码失败的异常信息
            logger.error("编码失败", e);
        }
        
        // 调用服务层方法保存数据到数据库
        int flag = reportCustomComponentFooterV1InnerServiceSMOImpl.saveReportCustomComponentFooter(reportCustomComponentFooterPo);

        // 检查保存操作是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置成功响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}