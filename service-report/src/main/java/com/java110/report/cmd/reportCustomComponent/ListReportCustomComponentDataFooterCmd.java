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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.PageDto;
import com.java110.dto.reportCustom.ReportCustomComponentDto;
import com.java110.dto.reportCustom.ReportCustomComponentFooterDto;
import com.java110.intf.report.IReportCustomComponentFooterV1InnerServiceSMO;
import com.java110.intf.report.IReportCustomComponentV1InnerServiceSMO;
import com.java110.service.smo.IQueryServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;


/**
 * 类表述：组件底部
 * 服务编码：reportCustomComponent.listReportCustomComponentFooter
 * 请求路劲：/app/reportCustomComponent.listReportCustomComponentFooter
 * add by 吴学文 at 2021-11-09 13:18:41 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 自定义报表组件底部数据查询命令类
 * 负责处理报表组件底部数据的查询请求，支持SQL和Java两种查询模式
 * 根据组件配置的查询模式执行相应的数据查询逻辑
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-11-09
 */
@Java110Cmd(serviceCode = "reportCustomComponent.listReportCustomComponentDataFooter")
public class ListReportCustomComponentDataFooterCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListReportCustomComponentDataFooterCmd.class);
    
    /**
     * 自定义报表组件服务接口
     */
    @Autowired
    private IReportCustomComponentV1InnerServiceSMO reportCustomComponentV1InnerServiceSMOImpl;
    
    /**
     * 自定义报表组件底部服务接口
     */
    @Autowired
    private IReportCustomComponentFooterV1InnerServiceSMO reportCustomComponentFooterV1InnerServiceSMOImpl;

    /**
     * 查询服务接口
     */
    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页信息和必要参数检查
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
        // 验证组件ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "componentId", "未包含组件ID");
    }

    /**
     * 执行命令方法
     * 处理自定义报表组件底部数据的查询请求
     * 根据组件配置的查询模式执行相应的查询逻辑
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当查询模式不支持时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 查询组件是否存在
        ReportCustomComponentDto reportCustomComponentDto = new ReportCustomComponentDto();
        reportCustomComponentDto.setComponentId(reqJson.getString("componentId"));
        List<ReportCustomComponentDto> reportCustomComponentDtos = reportCustomComponentV1InnerServiceSMOImpl.queryReportCustomComponents(reportCustomComponentDto);
        // 验证组件存在且唯一
        Assert.listOnlyOne(reportCustomComponentDtos, "组件不存在，请联系开发人员");

        // 查询组件底部配置信息
        ReportCustomComponentFooterDto reportCustomComponentFooterDto = new ReportCustomComponentFooterDto();
        reportCustomComponentFooterDto.setComponentId(reqJson.getString("componentId"));
        List<ReportCustomComponentFooterDto> reportCustomComponentFooterDtos
                = reportCustomComponentFooterV1InnerServiceSMOImpl.queryReportCustomComponentFooters(reportCustomComponentFooterDto);

        // 没有配置底部信息时返回空结果
        if (reportCustomComponentFooterDtos == null || reportCustomComponentFooterDtos.size() < 1) {
            cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(new JSONObject()));
            return;
        }

        // 获取第一个底部配置信息
        reportCustomComponentFooterDto = reportCustomComponentFooterDtos.get(0);

        // 根据查询模式执行相应的处理逻辑
        if (ReportCustomComponentDto.QUERY_MODEL_SQL.equals(reportCustomComponentFooterDto.getQueryModel())) {
            // SQL模式查询
            doDealSql(reqJson, reportCustomComponentFooterDto, cmdDataFlowContext);
        } else if (ReportCustomComponentDto.QUERY_MODEL_JAVA.equals(reportCustomComponentFooterDto.getQueryModel())) {
            // Java模式查询
            doDealJava(reqJson, reportCustomComponentFooterDto, cmdDataFlowContext);
        } else {
            // 不支持的查询模式
            throw new CmdException("组件实现方式不支持，请联系开发人员");
        }
    }

    /**
     * 处理Java模式查询
     * 执行Java代码方式的查询逻辑
     * 
     * @param reqJson 请求参数JSON对象
     * @param reportCustomComponentFooterDto 组件底部配置信息
     * @param cmdDataFlowContext 命令数据流上下文
     */
    private void doDealJava(JSONObject reqJson, ReportCustomComponentFooterDto reportCustomComponentFooterDto, ICmdDataFlowContext cmdDataFlowContext) {
        // 获取Java查询代码
        String sql = reportCustomComponentFooterDto.getComponentSql();

        // 处理分页参数
        int page = reqJson.getInteger("page");
        if (page != PageDto.DEFAULT_PAGE) {
            // 计算起始位置
            reqJson.put("page", (page - 1) * reqJson.getIntValue("row"));
        }
        
        // 执行Java查询
        JSONObject data = queryServiceSMOImpl.execJava(reqJson, sql);
        // 获取查询结果数据
        JSONArray dataTd = data.getJSONArray("td");
        JSONObject paramOut = null;
        
        // 处理查询结果
        if (dataTd != null && dataTd.size() > 0) {
            // 取第一条数据作为结果
            paramOut = dataTd.getJSONObject(0);
        } else {
            // 无数据时返回空对象
            paramOut = new JSONObject();
        }
        
        // 设置响应结果
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(paramOut);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 处理SQL模式查询
     * 执行SQL语句方式的查询逻辑
     * 
     * @param reqJson 请求参数JSON对象
     * @param reportCustomComponentFooterDto 组件底部配置信息
     * @param cmdDataFlowContext 命令数据流上下文
     */
    private void doDealSql(JSONObject reqJson, ReportCustomComponentFooterDto reportCustomComponentFooterDto, ICmdDataFlowContext cmdDataFlowContext) {
        // 获取SQL查询语句
        String sql = reportCustomComponentFooterDto.getComponentSql();
        
        // 处理分页参数
        long total = reqJson.getIntValue("row");
        int page = reqJson.getInteger("page");
        if (page != PageDto.DEFAULT_PAGE) {
            // 计算起始位置
            reqJson.put("page", (page - 1) * reqJson.getIntValue("row"));
        }
        
        // 执行SQL查询
        JSONObject data = queryServiceSMOImpl.execQuerySql(reqJson, sql);
        // 获取查询结果数据
        JSONArray dataTd = data.getJSONArray("td");
        JSONObject paramOut = null;
        
        // 处理查询结果
        if (dataTd != null && dataTd.size() > 0) {
            // 取第一条数据作为结果
            paramOut = dataTd.getJSONObject(0);
        } else {
            // 无数据时返回空对象
            paramOut = new JSONObject();
        }
        
        // 设置响应结果
        ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(paramOut);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}