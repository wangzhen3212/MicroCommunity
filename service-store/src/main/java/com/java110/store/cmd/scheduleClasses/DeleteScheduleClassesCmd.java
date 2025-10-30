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
import com.java110.dto.classes.ScheduleClassesDto;
import com.java110.dto.classes.ScheduleClassesDayDto;
import com.java110.dto.classes.ScheduleClassesStaffDto;
import com.java110.dto.classes.ScheduleClassesTimeDto;
import com.java110.intf.store.IScheduleClassesDayV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesStaffV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesTimeV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesV1InnerServiceSMO;
import com.java110.po.classes.ScheduleClassesPo;
import com.java110.po.classes.ScheduleClassesDayPo;
import com.java110.po.classes.ScheduleClassesTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：删除排班信息命令类
 * 服务编码：scheduleClasses.deleteScheduleClasses
 * 请求路劲：/app/scheduleClasses.DeleteScheduleClasses
 * 功能描述：该命令类用于处理删除排班信息的业务逻辑，包括删除排班主信息、排班天信息和排班时间信息
 * 注意事项：删除排班前需要确保该排班下没有关联的人员信息
 * add by 吴学文 at 2022-10-29 15:29:19 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "scheduleClasses.deleteScheduleClasses")
public class DeleteScheduleClassesCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteScheduleClassesCmd.class);

    /**
     * 排班信息服务接口
     */
    @Autowired
    private IScheduleClassesV1InnerServiceSMO scheduleClassesV1InnerServiceSMOImpl;

    /**
     * 排班天信息服务接口
     */
    @Autowired
    private IScheduleClassesDayV1InnerServiceSMO scheduleClassesDayV1InnerServiceSMOImpl;

    /**
     * 排班时间信息服务接口
     */
    @Autowired
    private IScheduleClassesTimeV1InnerServiceSMO scheduleClassesTimeV1InnerServiceSMOImpl;

    /**
     * 排班人员信息服务接口
     */
    @Autowired
    private IScheduleClassesStaffV1InnerServiceSMO scheduleClassesStaffV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 功能描述：验证删除排班请求的参数有效性，检查排班下是否有关联人员
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数校验失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 校验请求参数中必须包含scheduleId
        Assert.hasKeyAndValue(reqJson, "scheduleId", "scheduleId不能为空");
        
        // 检查排班下是否有关联人员
        ScheduleClassesStaffDto scheduleClassesStaffDto = new ScheduleClassesStaffDto();
        scheduleClassesStaffDto.setScheduleId(reqJson.getString("scheduleId"));
        int count = scheduleClassesStaffV1InnerServiceSMOImpl.queryScheduleClassesStaffsCount(scheduleClassesStaffDto);
        
        // 如果排班下有关联人员，则不允许删除
        if (count > 0) {
            throw new CmdException("请先解除人员再删除");
        }
    }

    /**
     * 执行删除排班命令
     * 功能描述：删除排班信息及其相关的天信息和时间信息，采用事务保证数据一致性
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 查询排班信息
        ScheduleClassesDto scheduleClassesDto = new ScheduleClassesDto();
        scheduleClassesDto.setScheduleId(reqJson.getString("scheduleId"));
        List<ScheduleClassesDto> scheduleClassesDtos = scheduleClassesV1InnerServiceSMOImpl.queryScheduleClassess(scheduleClassesDto);
        
        // 确保只查询到一条排班记录
        Assert.listOnlyOne(scheduleClassesDtos, "查询排班信息错误！");
        
        // 转换请求参数为排班PO对象
        ScheduleClassesPo scheduleClassesPo = BeanConvertUtil.covertBean(reqJson, ScheduleClassesPo.class);
        
        // 删除排班主信息
        int flag = scheduleClassesV1InnerServiceSMOImpl.deleteScheduleClasses(scheduleClassesPo);
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }
        
        // 查询并删除排班天信息
        ScheduleClassesDayDto scheduleClassesDayDto = new ScheduleClassesDayDto();
        scheduleClassesDayDto.setScheduleId(scheduleClassesDtos.get(0).getScheduleId());
        List<ScheduleClassesDayDto> scheduleClassesDayDtos = scheduleClassesDayV1InnerServiceSMOImpl.queryScheduleClassesDays(scheduleClassesDayDto);
        
        // 如果存在排班天信息，则删除
        if (scheduleClassesDayDtos != null && scheduleClassesDayDtos.size() == 1) {
            ScheduleClassesDayPo scheduleClassesDayPo = new ScheduleClassesDayPo();
            scheduleClassesDayPo.setScheduleId(scheduleClassesPo.getScheduleId());
            flag = scheduleClassesDayV1InnerServiceSMOImpl.deleteScheduleClassesDay(scheduleClassesDayPo);
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }
        
        // 查询并删除排班时间信息
        ScheduleClassesTimeDto scheduleClassesTimeDto = new ScheduleClassesTimeDto();
        scheduleClassesTimeDto.setScheduleId(scheduleClassesDtos.get(0).getScheduleId());
        scheduleClassesTimeDto.setDayId(scheduleClassesDayDtos.get(0).getDayId());
        List<ScheduleClassesTimeDto> scheduleClassesTimeDtos = scheduleClassesTimeV1InnerServiceSMOImpl.queryScheduleClassesTimes(scheduleClassesTimeDto);
        
        // 如果存在排班时间信息，则删除
        if (scheduleClassesTimeDtos != null && scheduleClassesTimeDtos.size() == 1) {
            ScheduleClassesTimePo scheduleClassesTimePo = new ScheduleClassesTimePo();
            scheduleClassesTimePo.setScheduleId(scheduleClassesPo.getScheduleId());
            flag = scheduleClassesTimeV1InnerServiceSMOImpl.deleteScheduleClassesTime(scheduleClassesTimePo);
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}