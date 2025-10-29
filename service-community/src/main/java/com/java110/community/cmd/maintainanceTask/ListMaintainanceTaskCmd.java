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
package com.java110.community.cmd.maintainanceTask;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.community.IMaintainanceTaskV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.maintainance.MaintainanceTaskDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：查询
 * 服务编码：maintainanceTask.listMaintainanceTask
 * 请求路劲：/app/maintainanceTask.ListMaintainanceTask
 * add by 吴学文 at 2022-11-08 15:51:01 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 维护任务查询命令类
 * 
 * 该类用于处理维护任务列表查询请求，实现分页查询维护任务信息的功能
 * 通过调用维护任务服务接口获取数据，并返回标准格式的响应结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-08
 */
@Java110Cmd(serviceCode = "maintainanceTask.listMaintainanceTask")
public class ListMaintainanceTaskCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(ListMaintainanceTaskCmd.class);

    /**
     * 维护任务服务接口
     */
    @Autowired
    private IMaintainanceTaskV1InnerServiceSMO maintainanceTaskV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数和必填字段的校验
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于处理请求和响应
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
    }

    /**
     * 命令执行方法
     * 
     * 执行维护任务查询业务逻辑，包括参数转换、数据查询和响应构建
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为维护任务DTO对象
        MaintainanceTaskDto maintainanceTaskDto = BeanConvertUtil.covertBean(reqJson, MaintainanceTaskDto.class);
        
        // 处理多状态查询条件
        if (reqJson.containsKey("moreState") && reqJson.getString("moreState").contains(",")) {
            // 将逗号分隔的状态字符串转换为数组
            maintainanceTaskDto.setStates(reqJson.getString("moreState").split(","));
        }
        
        // 查询维护任务总数
        int count = maintainanceTaskV1InnerServiceSMOImpl.queryMaintainanceTasksCount(maintainanceTaskDto);
        
        List<MaintainanceTaskDto> maintainanceTaskDtos = null;
        
        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 查询维护任务列表数据
            maintainanceTaskDtos = maintainanceTaskV1InnerServiceSMOImpl.queryMaintainanceTasks(maintainanceTaskDto);
        } else {
            // 无数据时返回空列表
            maintainanceTaskDtos = new ArrayList<>();
        }
        
        // 构建分页结果对象
        // 计算总页数：总记录数 / 每页行数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, maintainanceTaskDtos);
        
        // 构建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        
        // 设置响应结果到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}