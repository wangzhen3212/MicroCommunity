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
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.maintainance.MaintainanceTaskDto;
import com.java110.intf.community.IMaintainanceTaskV1InnerServiceSMO;
import com.java110.po.maintainance.MaintainanceTaskPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import java.util.List;


/**
 * 类表述：更新保养任务命令类
 * 服务编码：maintainanceTask.updateMaintainanceTask
 * 请求路劲：/app/maintainanceTask.UpdateMaintainanceTask
 * 功能描述：该类负责处理保养任务的更新操作，包括验证请求参数和执行更新逻辑
 * add by 吴学文 at 2022-11-08 15:51:01 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "maintainanceTask.updateMaintainanceTask")
public class UpdateMaintainanceTaskCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateMaintainanceTaskCmd.class);

    /**
     * 保养任务服务接口
     */
    @Autowired
    private IMaintainanceTaskV1InnerServiceSMO maintainanceTaskV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证更新保养任务请求的必需参数是否完整
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证taskId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "taskId", "taskId不能为空");
        // 验证communityId参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行命令方法
     * 功能描述：执行保养任务的更新操作，包括数据验证、业务逻辑处理和结果返回
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 创建查询条件对象，用于验证保养任务是否存在
        MaintainanceTaskDto maintainanceTaskDto1 = new MaintainanceTaskDto();
        maintainanceTaskDto1.setCommunityId(reqJson.getString("communityId"));
        maintainanceTaskDto1.setTaskId(reqJson.getString("taskId"));
        // 查询保养任务列表
        List<MaintainanceTaskDto> maintainanceTaskDtoList = maintainanceTaskV1InnerServiceSMOImpl.queryMaintainanceTasks(maintainanceTaskDto1);
        
        // 验证查询结果是否只有一条记录
        if (maintainanceTaskDtoList.size() != 1) {
            // 如果未找到或找到多条记录，返回业务验证失败响应
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "未找到保养任务信息或找到多条！");
            cmdDataFlowContext.setResponseEntity(responseEntity);
            return;
        }

        // 再次创建查询条件对象，用于获取需要更新的保养任务详情
        MaintainanceTaskDto maintainanceTaskDto = new MaintainanceTaskDto();
        maintainanceTaskDto.setTaskId(reqJson.getString("taskId"));
        maintainanceTaskDto.setCommunityId(reqJson.getString("communityId"));
        List<MaintainanceTaskDto> maintainanceTaskDtos = maintainanceTaskV1InnerServiceSMOImpl.queryMaintainanceTasks(maintainanceTaskDto);

        // 断言确保只找到一条需要修改的保养任务记录
        Assert.listOnlyOne(maintainanceTaskDtos, "未找到需要修改的保养任务 或多条数据");

        // 将保养任务数据转换为JSON对象，便于后续处理
        JSONObject businessMaintainanceTask = new JSONObject();
        businessMaintainanceTask.putAll(BeanConvertUtil.beanCovertMap(maintainanceTaskDtos.get(0)));

        // 将JSON对象转换为保养任务PO对象
        MaintainanceTaskPo maintainanceTaskPo = BeanConvertUtil.covertBean(businessMaintainanceTask, MaintainanceTaskPo.class);
        // 设置实际执行时间
        maintainanceTaskPo.setActInsTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        // 设置实际执行用户ID
        maintainanceTaskPo.setActUserId(reqJson.getString("userId"));
        // 设置实际执行用户名称
        maintainanceTaskPo.setActUserName(reqJson.getString("userName"));
        // 设置任务状态
        maintainanceTaskPo.setState(reqJson.getString("state"));
        
        // 处理任务类型为"2000"的特殊情况（可能是转派任务）
        if(!StringUtil.isEmpty(reqJson.getString("taskType")) && reqJson.getString("taskType").equals("2000") ){
            // 注释掉的代码：保存原计划执行人信息
//            maintainanceTaskPo.setOriginalPlanUserId(maintainanceTaskPo.getPlanUserId());
//            maintainanceTaskPo.setOriginalPlanUserName(maintainanceTaskPo.getPlanUserName());
            
            // 更新计划执行人信息（转派给新的执行人）
            maintainanceTaskPo.setPlanUserId(reqJson.getString("staffId"));
            maintainanceTaskPo.setPlanUserName(reqJson.getString("staffName"));
            // 设置任务类型
            maintainanceTaskPo.setTaskType(reqJson.getString("taskType"));
            // 设置转派描述
            maintainanceTaskPo.setTransferDesc(reqJson.getString("transferDesc"));
        }

        // 执行更新操作
        int flag = maintainanceTaskV1InnerServiceSMOImpl.updateMaintainanceTask(maintainanceTaskPo);
        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 返回成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}