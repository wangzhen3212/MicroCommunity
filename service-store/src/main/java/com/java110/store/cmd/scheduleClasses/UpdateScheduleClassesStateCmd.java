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
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IScheduleClassesDayV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesTimeV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesV1InnerServiceSMO;
import com.java110.po.classes.ScheduleClassesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 类表述：更新
 * 服务编码：scheduleClasses.updateScheduleClassesState
 * 请求路劲：/app/scheduleClasses.UpdateScheduleClasses
 * add by 吴学文 at 2022-10-29 15:29:19 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 排班状态更新命令类
 * 
 * 该类负责处理排班状态更新的业务逻辑，包括参数验证和状态更新操作。
 * 通过注解@Java110Cmd指定服务编码，用于路由到对应的命令处理器。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-29
 */
@Java110Cmd(serviceCode = "scheduleClasses.updateScheduleClassesState")
public class UpdateScheduleClassesStateCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateScheduleClassesStateCmd.class);

    /**
     * 排班信息服务接口
     */
    @Autowired
    private IScheduleClassesV1InnerServiceSMO scheduleClassesV1InnerServiceSMOImpl;

    /**
     * 排班日期信息服务接口
     */
    @Autowired
    private IScheduleClassesDayV1InnerServiceSMO scheduleClassesDayV1InnerServiceSMOImpl;

    /**
     * 排班时间信息服务接口
     */
    @Autowired
    private IScheduleClassesTimeV1InnerServiceSMO scheduleClassesTimeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保排班ID和状态字段不为空。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含scheduleId字段
        Assert.hasKeyAndValue(reqJson, "scheduleId", "scheduleId不能为空");
        
        // 验证请求参数中必须包含state字段
        Assert.hasKeyAndValue(reqJson, "state", "请求报文中未包含状态");
    }

    /**
     * 执行命令方法
     * 
     * 处理排班状态更新的核心业务逻辑，将请求参数转换为实体对象并调用服务进行更新操作。
     * 使用@Java110Transactional注解确保操作的事务性。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为排班实体对象
        ScheduleClassesPo scheduleClassesPo = BeanConvertUtil.covertBean(reqJson, ScheduleClassesPo.class);
        
        // 调用排班服务更新排班状态，返回受影响的行数
        int flag = scheduleClassesV1InnerServiceSMOImpl.updateScheduleClasses(scheduleClassesPo);

        // 检查更新操作是否成功，如果受影响行数小于1则抛出异常
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}