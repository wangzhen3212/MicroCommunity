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
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.reportCustom.ReportCustomGroupDto;
import com.java110.intf.report.IReportCustomGroupV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询
 * 服务编码：reportCustomGroup.listReportCustomGroup
 * 请求路劲：/app/reportCustomGroup.ListReportCustomGroup
 * add by 吴学文 at 2021-11-09 13:30:41 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 报表自定义分组查询命令类
 * 
 * 该类负责处理报表自定义分组的列表查询请求，包括数据验证和业务处理
 * 通过服务编码 reportCustomGroup.listReportCustomGroup 对外提供服务
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-11-09
 */
@Java110Cmd(serviceCode = "reportCustomGroup.listReportCustomGroup")
public class ListReportCustomGroupCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListReportCustomGroupCmd.class);
    
    /**
     * 报表自定义分组服务接口
     */
    @Autowired
    private IReportCustomGroupV1InnerServiceSMO reportCustomGroupV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，主要验证分页参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数的有效性
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 
     * 处理报表自定义分组的查询请求，包括数据查询、结果封装和响应设置
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为报表自定义分组数据传输对象
        ReportCustomGroupDto reportCustomGroupDto = BeanConvertUtil.covertBean(reqJson, ReportCustomGroupDto.class);

        // 查询符合条件的报表自定义分组记录总数
        int count = reportCustomGroupV1InnerServiceSMOImpl.queryReportCustomGroupsCount(reportCustomGroupDto);

        List<ReportCustomGroupDto> reportCustomGroupDtos = null;

        // 根据查询结果数量决定如何处理数据
        if (count > 0) {
            // 如果存在记录，则查询具体的报表自定义分组数据列表
            reportCustomGroupDtos = reportCustomGroupV1InnerServiceSMOImpl.queryReportCustomGroups(reportCustomGroupDto);
        } else {
            // 如果没有记录，返回空列表
            reportCustomGroupDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 封装返回结果，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, reportCustomGroupDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中，返回给调用方
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}