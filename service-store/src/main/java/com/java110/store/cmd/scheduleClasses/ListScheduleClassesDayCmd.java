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
package com.java110.store.cmd.scheduleClasses;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IScheduleClassesDayV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.classes.ScheduleClassesDayDto;
import java.util.List;
import java.util.ArrayList;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：scheduleClassesDay.listScheduleClassesDay
 * 请求路劲：/app/scheduleClassesDay.ListScheduleClassesDay
 * add by 吴学文 at 2022-10-29 15:42:28 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 排班日程查询命令类
 * 该类用于处理排班日程的列表查询请求，包括分页查询排班日程信息
 * 通过调用排班日程服务接口获取数据并返回给前端
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-29
 */
@Java110Cmd(serviceCode = "scheduleClasses.listScheduleClassesDay")
public class ListScheduleClassesDayCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListScheduleClassesDayCmd.class);
    
    /**
     * 排班日程服务接口
     * 用于调用排班日程相关的业务逻辑
     */
    @Autowired
    private IScheduleClassesDayV1InnerServiceSMO scheduleClassesDayV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，主要验证分页参数
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 调用父类的分页信息验证方法，验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理排班日程查询请求，包括查询总数和分页数据
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为排班日程数据传输对象
        ScheduleClassesDayDto scheduleClassesDayDto = BeanConvertUtil.covertBean(reqJson, ScheduleClassesDayDto.class);
        
        // 查询符合条件的排班日程总数
        int count = scheduleClassesDayV1InnerServiceSMOImpl.queryScheduleClassesDaysCount(scheduleClassesDayDto);

        List<ScheduleClassesDayDto> scheduleClassesDayDtos = null;

        // 根据查询结果数量判断是否查询具体数据
        if (count > 0) {
            // 查询总数大于0时，查询具体的排班日程列表数据
            scheduleClassesDayDtos = scheduleClassesDayV1InnerServiceSMOImpl.queryScheduleClassesDays(scheduleClassesDayDto);
        } else {
            // 查询总数为0时，返回空列表
            scheduleClassesDayDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数除以每页显示行数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象，包含总页数、总记录数和数据列表
        ResultVo resultVo = new ResultVo(totalPage, count, scheduleClassesDayDtos);

        // 创建HTTP响应实体，设置响应状态为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应实体设置到数据流上下文中，返回给前端
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}