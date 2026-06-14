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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.report.IReportCustomComponentV1InnerServiceSMO;
import com.java110.po.reportCustom.ReportCustomComponentPo;
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
 * 保存报表自定义组件命令类
 * 
 * 该类负责处理报表自定义组件的保存操作，包括参数验证、数据转换和持久化存储。
 * 服务编码：reportCustomComponent.saveReportCustomComponent
 * 请求路径：/app/reportCustomComponent.SaveReportCustomComponent
 * 
 * @author 吴学文 at 2021-11-09 13:18:41 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "reportCustomComponent.saveReportCustomComponent")
public class SaveReportCustomComponentCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveReportCustomComponentCmd.class);

    /** 组件ID前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 报表自定义组件服务接口 */
    @Autowired
    private IReportCustomComponentV1InnerServiceSMO reportCustomComponentV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否包含必要的字段，确保请求数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含name字段
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        // 验证请求报文中必须包含componentType字段
        Assert.hasKeyAndValue(reqJson, "componentType", "请求报文中未包含componentType");
        // 验证请求报文中必须包含queryModel字段
        Assert.hasKeyAndValue(reqJson, "queryModel", "请求报文中未包含queryModel");
    }

    /**
     * 执行保存报表自定义组件命令
     * 
     * 将请求数据转换为持久化对象，进行编码处理后保存到数据库
     * 使用事务确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为报表自定义组件POJO对象
        ReportCustomComponentPo reportCustomComponentPo = BeanConvertUtil.covertBean(reqJson, ReportCustomComponentPo.class);
        
        // 生成组件ID，使用前缀+自增序列的方式
        reportCustomComponentPo.setComponentId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 设置组件分组为"C"
        reportCustomComponentPo.setComponentGroup("C");

        try {
            // 如果组件SQL不为空，则进行Base64编码处理
            if (!StringUtil.isEmpty(reportCustomComponentPo.getComponentSql())) {
                reportCustomComponentPo.setComponentSql(Base64Convert.byteToBase64(
                    reportCustomComponentPo.getComponentSql().getBytes("UTF-8")));
            }
            
            // 如果JavaScript代码不为空，则进行Base64编码处理
            if (!StringUtil.isEmpty(reportCustomComponentPo.getJavaScript())) {
                reportCustomComponentPo.setJavaScript(Base64Convert.byteToBase64(
                    reportCustomComponentPo.getJavaScript().getBytes("UTF-8")));
            }
        } catch (Exception e) {
            // 记录编码失败的异常信息
            logger.error("编码失败", e);
        }

        // 调用服务层保存报表自定义组件数据
        int flag = reportCustomComponentV1InnerServiceSMOImpl.saveReportCustomComponent(reportCustomComponentPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}