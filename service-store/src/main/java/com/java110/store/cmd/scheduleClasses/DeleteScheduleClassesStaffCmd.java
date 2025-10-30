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
import com.java110.intf.store.IScheduleClassesStaffV1InnerServiceSMO;
import com.java110.po.classes.ScheduleClassesStaffPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 删除排班人员命令类
 * 
 * 该类负责处理删除排班人员信息的业务逻辑，包括参数验证和删除操作
 * 服务编码：scheduleClasses.deleteScheduleClassesStaff
 * 请求路径：/app/scheduleClassesStaff.DeleteScheduleClassesStaff
 * 
 * @author 吴学文 at 2022-10-29 16:16:44 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 * @see ScheduleClassesStaffPo
 * @see IScheduleClassesStaffV1InnerServiceSMO
 */
@Java110Cmd(serviceCode = "scheduleClasses.deleteScheduleClassesStaff")
public class DeleteScheduleClassesStaffCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteScheduleClassesStaffCmd.class);

    /**
     * 排班人员服务接口
     */
    @Autowired
    private IScheduleClassesStaffV1InnerServiceSMO scheduleClassesStaffV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段：scsId和scheduleId
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws IllegalArgumentException 当必要参数缺失时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含scsId字段
        Assert.hasKeyAndValue(reqJson, "scsId", "scsId不能为空");
        // 验证请求参数中必须包含scheduleId字段
        Assert.hasKeyAndValue(reqJson, "scheduleId", "scheduleId不能为空");
    }

    /**
     * 执行删除排班人员命令
     * 
     * 将请求参数转换为实体对象，调用服务层进行删除操作，并处理删除结果
     * 该方法使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将JSON请求参数转换为排班人员实体对象
        ScheduleClassesStaffPo scheduleClassesStaffPo = BeanConvertUtil.covertBean(reqJson, ScheduleClassesStaffPo.class);
        
        // 调用服务层接口执行删除操作，返回影响的行数
        int flag = scheduleClassesStaffV1InnerServiceSMOImpl.deleteScheduleClassesStaff(scheduleClassesStaffPo);

        // 检查删除操作是否成功，如果影响行数小于1表示删除失败
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}