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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.report.IReportCustomComponentConditionV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.reportCustom.ReportCustomComponentConditionDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：reportCustomComponentCondition.listReportCustomComponentCondition
 * 请求路劲：/app/reportCustomComponentCondition.ListReportCustomComponentCondition
 * add by 吴学文 at 2021-11-09 13:33:12 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 报表自定义组件条件查询命令类
 * 
 * 该类负责处理报表自定义组件条件的列表查询请求，包括参数验证、数据查询和结果返回等功能。
 * 通过注解@Java110Cmd指定服务编码，与前端请求路径对应。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-11-09
 */
@Java110Cmd(serviceCode = "reportCustomComponentCondition.listReportCustomComponentCondition")
public class ListReportCustomComponentConditionCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListReportCustomComponentConditionCmd.class);
    
    /**
     * 报表自定义组件条件服务接口
     * 用于调用底层的业务逻辑处理
     */
    @Autowired
    private IReportCustomComponentConditionV1InnerServiceSMO reportCustomComponentConditionV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 继承自父类Cmd，主要用于验证请求参数的有效性，特别是分页参数
     * 
     * @param event Cmd事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，确保分页参数正确
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 主要的业务逻辑处理方法，负责查询报表自定义组件条件数据并返回结果
     * 
     * @param event Cmd事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求JSON对象转换为数据传输对象
        ReportCustomComponentConditionDto reportCustomComponentConditionDto = BeanConvertUtil.covertBean(reqJson, ReportCustomComponentConditionDto.class);

        // 查询符合条件的记录总数
        int count = reportCustomComponentConditionV1InnerServiceSMOImpl.queryReportCustomComponentConditionsCount(reportCustomComponentConditionDto);

        List<ReportCustomComponentConditionDto> reportCustomComponentConditionDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果存在数据，则查询具体的数据列表
            reportCustomComponentConditionDtos = reportCustomComponentConditionV1InnerServiceSMOImpl.queryReportCustomComponentConditions(reportCustomComponentConditionDto);
        } else {
            // 如果没有数据，返回空列表
            reportCustomComponentConditionDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 创建结果对象，包含分页信息和数据
        ResultVo resultVo = new ResultVo(totalPage, count, reportCustomComponentConditionDtos);

        // 创建HTTP响应实体，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中，返回给前端
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}