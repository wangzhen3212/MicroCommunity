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
package com.java110.report.cmd.reportCustomGroup;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.report.IReportCustomGroupV1InnerServiceSMO;
import com.java110.po.reportCustom.ReportCustomGroupPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 类表述：保存报表自定义分组命令类
 * 服务编码：reportCustomGroup.saveReportCustomGroup
 * 请求路径：/app/reportCustomGroup.SaveReportCustomGroup
 * 
 * 功能描述：处理保存报表自定义分组的业务逻辑，包括参数验证和数据持久化操作
 * 
 * @author 吴学文 at 2021-11-09 13:30:41 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * 
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "reportCustomGroup.saveReportCustomGroup")
public class SaveReportCustomGroupCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveReportCustomGroupCmd.class);

    /**
     * ID生成前缀，用于生成分组ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 报表自定义分组服务接口
     */
    @Autowired
    private IReportCustomGroupV1InnerServiceSMO reportCustomGroupV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 功能描述：验证请求参数是否完整，确保必要的参数存在
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含name参数
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        // 验证请求报文中必须包含url参数
        Assert.hasKeyAndValue(reqJson, "url", "请求报文中未包含url");
    }

    /**
     * 执行保存报表自定义分组命令
     * 
     * 功能描述：将请求数据转换为实体对象，生成唯一ID，调用服务层保存数据
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为ReportCustomGroupPo实体对象
        ReportCustomGroupPo reportCustomGroupPo = BeanConvertUtil.covertBean(reqJson, ReportCustomGroupPo.class);
        
        // 生成唯一的分组ID
        reportCustomGroupPo.setGroupId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存报表自定义分组数据
        int flag = reportCustomGroupV1InnerServiceSMOImpl.saveReportCustomGroup(reportCustomGroupPo);

        // 检查保存操作是否成功，flag小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}