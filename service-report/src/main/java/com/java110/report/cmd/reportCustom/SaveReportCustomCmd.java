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
package com.java110.report.cmd.reportCustom;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.report.IReportCustomV1InnerServiceSMO;
import com.java110.po.reportCustom.ReportCustomPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

import java.text.ParseException;

/**
 * 类表述：保存
 * 服务编码：reportCustom.saveReportCustom
 * 请求路劲：/app/reportCustom.SaveReportCustom
 * add by 吴学文 at 2021-11-09 13:25:20 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 报表自定义配置保存命令类
 * 
 * 该类负责处理报表自定义配置的保存请求，包括参数验证和业务逻辑处理
 * 通过注解@Java110Cmd声明服务编码，与前端请求路径对应
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-11-09
 */
@Java110Cmd(serviceCode = "reportCustom.saveReportCustom")
public class SaveReportCustomCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveReportCustomCmd.class);

    /**
     * ID生成前缀，用于生成自定义配置ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 报表自定义配置服务接口
     */
    @Autowired
    private IReportCustomV1InnerServiceSMO reportCustomV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中是否包含必要的参数，确保请求数据的完整性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中是否包含groupId参数
        Assert.hasKeyAndValue(reqJson, "groupId", "请求报文中未包含groupId");
        // 验证请求报文中是否包含title参数
        Assert.hasKeyAndValue(reqJson, "title", "请求报文中未包含title");
        // 验证请求报文中是否包含seq参数
        Assert.hasKeyAndValue(reqJson, "seq", "请求报文中未包含seq");
    }

    /**
     * 执行保存报表自定义配置命令
     * 
     * 将请求参数转换为实体对象，生成唯一ID，调用服务层保存数据
     * 使用事务注解确保数据操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为ReportCustomPo实体对象
        ReportCustomPo reportCustomPo = BeanConvertUtil.covertBean(reqJson, ReportCustomPo.class);
        
        // 生成自定义配置的唯一ID
        reportCustomPo.setCustomId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层保存数据，返回影响的行数
        int flag = reportCustomV1InnerServiceSMOImpl.saveReportCustom(reportCustomPo);

        // 检查保存操作是否成功，flag小于1表示保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}