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
import com.java110.core.log.LoggerFactory;
import com.java110.dto.PageDto;
import com.java110.dto.reportCustom.ReportCustomComponentDto;
import com.java110.intf.report.IReportCustomComponentV1InnerServiceSMO;
import com.java110.service.smo.IQueryServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;


/**
 * 类表述：查询
 * 服务编码：reportCustomComponent.listReportCustomComponentData
 * 请求路劲：/app/reportCustomComponent.listReportCustomComponentData
 * add by 吴学文 at 2021-11-09 13:18:41 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 报表自定义组件数据查询命令类
 * 
 * 该类负责处理报表自定义组件的数据查询请求，支持SQL查询和Java脚本查询两种模式
 * 根据组件的配置信息执行相应的查询逻辑，并返回分页格式的数据结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-11-09
 */
@Java110Cmd(serviceCode = "reportCustomComponent.listReportCustomComponentData")
public class ListReportCustomComponentDataCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListReportCustomComponentDataCmd.class);

    /**
     * 报表自定义组件服务接口
     */
    @Autowired
    private IReportCustomComponentV1InnerServiceSMO reportCustomComponentV1InnerServiceSMOImpl;

    /**
     * 查询服务接口
     */
    @Autowired
    private IQueryServiceSMO queryServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和必要参数检查
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证组件ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "componentId", "未包含组件ID");
    }

    /**
     * 执行命令方法
     * 
     * 根据组件配置的查询模式执行相应的查询逻辑
     * 支持SQL查询和Java脚本查询两种模式
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当组件实现方式不支持时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取店铺ID并添加到请求参数中
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
        reqJson.put("storeId", storeId);

        // 查询组件是否存在
        ReportCustomComponentDto reportCustomComponentDto = new ReportCustomComponentDto();
        reportCustomComponentDto.setComponentId(reqJson.getString("componentId"));
        List<ReportCustomComponentDto> reportCustomComponentDtos = reportCustomComponentV1InnerServiceSMOImpl.queryReportCustomComponents(reportCustomComponentDto);
        // 验证组件存在且唯一
        Assert.listOnlyOne(reportCustomComponentDtos, "组件不存在，请联系开发人员");

        reportCustomComponentDto = reportCustomComponentDtos.get(0);

        // 根据查询模式执行不同的处理逻辑
        if (ReportCustomComponentDto.QUERY_MODEL_SQL.equals(reportCustomComponentDto.getQueryModel())) {
            // SQL查询模式
            doDealSql(reqJson, reportCustomComponentDto, cmdDataFlowContext);
        } else if (ReportCustomComponentDto.QUERY_MODEL_JAVA.equals(reportCustomComponentDto.getQueryModel())) {
            // Java脚本查询模式
            doDealJava(reqJson, reportCustomComponentDto, cmdDataFlowContext);
        } else {
            // 不支持的查询模式
            throw new CmdException("组件实现方式不支持，请联系开发人员");
        }
    }

    /**
     * 处理Java脚本查询模式
     * 
     * 执行Java脚本查询逻辑，处理分页并返回结果
     * 
     * @param reqJson 请求参数JSON对象
     * @param reportCustomComponentDto 报表自定义组件数据传输对象
     * @param cmdDataFlowContext 命令数据流上下文
     */
    private void doDealJava(JSONObject reqJson, ReportCustomComponentDto reportCustomComponentDto, ICmdDataFlowContext cmdDataFlowContext) {
        // 获取Java脚本内容
        String javaScript = reportCustomComponentDto.getJavaScript();
        int page = reqJson.getInteger("page");
        
        // 处理分页参数：如果不是第一页，计算起始位置
        if (page != PageDto.DEFAULT_PAGE) {
            reqJson.put("page", (page - 1) * reqJson.getIntValue("row"));
        }
        
        // 执行Java脚本查询
        JSONObject data = queryServiceSMOImpl.execJava(reqJson, javaScript);
        long total = 0;
        // 获取总记录数
        if (data.getLong("total") != null) {
            total = data.getLong("total");
        }
        
        // 构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) total / (double) reqJson.getInteger("row")), total, data);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 处理SQL查询模式
     * 
     * 执行SQL查询逻辑，处理分页、总数统计并返回结果
     * 
     * @param reqJson 请求参数JSON对象
     * @param reportCustomComponentDto 报表自定义组件数据传输对象
     * @param cmdDataFlowContext 命令数据流上下文
     */
    private void doDealSql(JSONObject reqJson, ReportCustomComponentDto reportCustomComponentDto, ICmdDataFlowContext cmdDataFlowContext) {
        // 获取SQL语句
        String sql = reportCustomComponentDto.getComponentSql();
        long total = reqJson.getIntValue("row");
        
        // 检查SQL是否包含总数统计逻辑
        if (sql.trim().contains("test=\"count")) { 
            // 构建总数统计请求参数
            JSONObject reqJsonCount = new JSONObject();
            for (String key : reqJson.keySet()) {
                // 排除分页参数
                if ("row".equals(key) || "page".equals(key)) {
                    continue;
                }
                reqJsonCount.put(key, reqJson.get(key));
            }
            // 设置统计标志
            reqJsonCount.put("count", "1");
            // 执行总数统计查询
            JSONObject data = queryServiceSMOImpl.execQuerySql(reqJsonCount, sql);
            total = data.getJSONArray("td").getJSONObject(0).getIntValue("total");
        }
        
        // 设置非统计标志
        reqJson.put("count", "0");
        int page = reqJson.getInteger("page");
        
        // 处理分页参数：如果不是第一页，计算起始位置
        if (page != PageDto.DEFAULT_PAGE) {
            reqJson.put("page", (page - 1) * reqJson.getIntValue("row"));
        }
        
        // 执行数据查询
        JSONObject data = queryServiceSMOImpl.execQuerySql(reqJson, sql);
        
        // 如果SQL不包含总数统计逻辑，从查询结果中获取记录数
        if (!sql.trim().contains("test=\"count")) {
            total = JSONArray.parseArray(data.getString("td")).size();
        }
        
        // 检查SQL是否包含分页限制，如果没有则添加
        if (!StringUtil.isEmpty(sql) && !sql.contains("limit #page#,#row#")) {
            sql = sql + " limit #page#,#row#";
            data = queryServiceSMOImpl.execQuerySql(reqJson, sql);
        }
        
        // 构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) total / (double) reqJson.getInteger("row")), total, data);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}