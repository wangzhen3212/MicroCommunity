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
package com.java110.community.cmd.inspectionTask;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.inspection.InspectionTaskDto;
import com.java110.intf.community.IInspectionTaskInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.inspectionTask.ApiInspectionTaskDataVo;
import com.java110.vo.api.inspectionTask.ApiInspectionTaskVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 类表述：查询巡检任务列表命令类
 * 服务编码：inspectionTask.listInspectionTask
 * 请求路劲：/app/inspectionTask.ListInspectionTask
 * 功能描述：该类用于处理巡检任务列表查询请求，包括参数验证、数据查询和结果返回
 * add by 吴学文 at 2022-03-16 09:35:12 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "inspectionTask.listInspectionTasks")
public class ListInspectionTasksCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListInspectionTasksCmd.class);

    /**
     * 巡检任务内部服务接口
     */
    @Autowired
    private IInspectionTaskInnerServiceSMO inspectionTaskInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证属性信息
        super.validateProperty(cmdDataFlowContext);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含小区信息");
    }

    /**
     * 执行巡检任务列表查询命令
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为巡检任务DTO对象
        InspectionTaskDto inspectionTaskDto = BeanConvertUtil.covertBean(reqJson, InspectionTaskDto.class);

        // 处理多状态查询条件
        if (reqJson.containsKey("moreState") && reqJson.getString("moreState").contains(",")) {
            // 将逗号分隔的状态字符串转换为数组
            inspectionTaskDto.setStates(reqJson.getString("moreState").split(","));
        }

        // 查询当天巡检任务标识
        if (!StringUtil.isEmpty(reqJson.getString("isToday"))) {
            // 设置是否为当天任务的查询条件
            inspectionTaskDto.setDayTask(reqJson.getString("isToday"));
        }
        
        // 查询巡检任务总数
        int count = inspectionTaskInnerServiceSMOImpl.queryInspectionTasksCount(inspectionTaskDto);

        List<ApiInspectionTaskDataVo> inspectionTasks = null;

        if (count > 0) {
            // 如果有数据，查询巡检任务列表并转换为VO对象
            inspectionTasks = BeanConvertUtil.covertBeanList(
                inspectionTaskInnerServiceSMOImpl.queryInspectionTasks(inspectionTaskDto), 
                ApiInspectionTaskDataVo.class
            );
        } else {
            // 如果没有数据，返回空列表
            inspectionTasks = new ArrayList<>();
        }

        // 构建返回结果VO对象
        ApiInspectionTaskVo apiInspectionTaskVo = new ApiInspectionTaskVo();

        // 设置总记录数
        apiInspectionTaskVo.setTotal(count);
        // 计算总页数
        apiInspectionTaskVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置巡检任务数据列表
        apiInspectionTaskVo.setInspectionTasks(inspectionTasks);

        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiInspectionTaskVo), 
            HttpStatus.OK
        );

        // 设置响应实体到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}