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
 * 类表述：更新
 * 服务编码：reportCustomComponentFooter.updateReportCustomComponentFooter
 * 请求路劲：/app/reportCustomComponentFooter.UpdateReportCustomComponentFooter
 * add by 吴学文 at 2021-11-14 01:32:10 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 报表自定义组件页脚更新命令类
 * 
 * 该类负责处理报表自定义组件页脚的更新操作，包括参数验证和业务逻辑处理
 * 通过注解标识服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-11-14
 */
@Java110Cmd(serviceCode = "reportCustomComponentFooter.updateReportCustomComponentFooter")
public class UpdateReportCustomComponentFooterCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateReportCustomComponentFooterCmd.class);

    /**
     * 报表自定义组件页脚服务接口
     */
    @Autowired
    private IReportCustomComponentFooterV1InnerServiceSMO reportCustomComponentFooterV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保更新操作的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含footerId字段
        Assert.hasKeyAndValue(reqJson, "footerId", "footerId不能为空");
        Assert.hasKeyAndValue(reqJson, "footerId", "footerId不能为空");
    }

    /**
     * 命令执行方法
     * 
     * 处理报表自定义组件页脚的更新业务逻辑，包括数据转换、编码处理和数据库更新
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON对象转换为实体对象
        ReportCustomComponentFooterPo reportCustomComponentFooterPo = BeanConvertUtil.covertBean(reqJson, ReportCustomComponentFooterPo.class);
        
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
            // 记录编码过程中的异常信息
            logger.error("编码失败", e);
        }
        
        // 调用服务层方法更新数据
        int flag = reportCustomComponentFooterV1InnerServiceSMOImpl.updateReportCustomComponentFooter(reportCustomComponentFooterPo);

        // 检查更新是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}