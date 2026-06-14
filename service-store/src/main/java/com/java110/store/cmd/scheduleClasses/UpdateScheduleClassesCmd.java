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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.classes.ScheduleClassesDto;
import com.java110.dto.classes.ScheduleClassesDayDto;
import com.java110.dto.classes.ScheduleClassesTimeDto;
import com.java110.intf.store.IScheduleClassesDayV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesTimeV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesV1InnerServiceSMO;
import com.java110.po.classes.ScheduleClassesPo;
import com.java110.po.classes.ScheduleClassesDayPo;
import com.java110.po.classes.ScheduleClassesTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 排班信息更新命令类
 * 
 * 该类负责处理排班信息的更新操作，包括排班基本信息、排班天信息和排班时间信息的更新
 * 服务编码：scheduleClasses.updateScheduleClasses
 * 请求路径：/app/scheduleClasses.UpdateScheduleClasses
 * 
 * @author 吴学文 at 2022-10-29 15:29:19 mail: 928255095@qq.com
 * @version 1.0
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 */
@Java110Cmd(serviceCode = "scheduleClasses.updateScheduleClasses")
public class UpdateScheduleClassesCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateScheduleClassesCmd.class);

    @Autowired
    private IScheduleClassesV1InnerServiceSMO scheduleClassesV1InnerServiceSMOImpl;

    @Autowired
    private IScheduleClassesDayV1InnerServiceSMO scheduleClassesDayV1InnerServiceSMOImpl;

    @Autowired
    private IScheduleClassesTimeV1InnerServiceSMO scheduleClassesTimeV1InnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 
     * 验证请求参数的有效性，包括必填字段检查、排班天信息和时间信息的完整性验证
     * 
     * @param event CMD事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "scheduleId", "scheduleId不能为空");
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        Assert.hasKeyAndValue(reqJson, "scheduleType", "请求报文中未包含scheduleType");
        Assert.hasKeyAndValue(reqJson, "scheduleCycle", "请求报文中未包含scheduleCycle");
        
        // 验证排班天信息
        if (!reqJson.containsKey("days")) {
            throw new CmdException("未包含天");
        }
        JSONArray days = reqJson.getJSONArray("days");
        if (days.size() < 1) {
            throw new CmdException("未包含天");
        }
        
        // 遍历每个排班天，验证时间信息
        JSONObject day = null;
        JSONArray times = null;
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            day = days.getJSONObject(dayIndex);
            // 如果是休息日，跳过时间验证
            if (ScheduleClassesDayDto.WORKDAY_NO.equals(day.getString("workday"))) {
                continue;
            }
            // 验证工作日的时间信息
            if (!day.containsKey("times")) {
                throw new CmdException("未包时间");
            }
            times = day.getJSONArray("times");
            if (times.size() < 1) {
                throw new CmdException("未包时间");
            }
        }
    }

    /**
     * 执行排班信息更新操作
     * 
     * 该方法负责更新排班信息，包括：
     * 1. 更新排班基本信息
     * 2. 删除原有的排班天和时间信息
     * 3. 保存新的排班天和时间信息
     * 
     * @param event CMD事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 查询现有的排班信息
        ScheduleClassesDto scheduleClassesDto = new ScheduleClassesDto();
        scheduleClassesDto.setScheduleId(reqJson.getString("scheduleId"));
        List<ScheduleClassesDto> scheduleClassesDtos = scheduleClassesV1InnerServiceSMOImpl.queryScheduleClassess(scheduleClassesDto);
        Assert.listOnlyOne(scheduleClassesDtos, "查询排班信息错误！");
        
        // 更新排班基本信息
        ScheduleClassesPo scheduleClassesPo = BeanConvertUtil.covertBean(reqJson, ScheduleClassesPo.class);
        int flag = scheduleClassesV1InnerServiceSMOImpl.updateScheduleClasses(scheduleClassesPo);
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 删除原有的排班天信息
        ScheduleClassesDayDto scheduleClassesDayDto = new ScheduleClassesDayDto();
        scheduleClassesDayDto.setScheduleId(scheduleClassesDtos.get(0).getScheduleId());
        List<ScheduleClassesDayDto> scheduleClassesDayDtos = scheduleClassesDayV1InnerServiceSMOImpl.queryScheduleClassesDays(scheduleClassesDayDto);
        if (scheduleClassesDayDtos != null && scheduleClassesDayDtos.size() > 0) {
            ScheduleClassesDayPo scheduleClassesDayPo = new ScheduleClassesDayPo();
            scheduleClassesDayPo.setScheduleId(scheduleClassesPo.getScheduleId());
            flag = scheduleClassesDayV1InnerServiceSMOImpl.deleteScheduleClassesDay(scheduleClassesDayPo);
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }
        
        // 删除原有的排班时间信息
        ScheduleClassesTimeDto scheduleClassesTimeDto = new ScheduleClassesTimeDto();
        scheduleClassesTimeDto.setScheduleId(scheduleClassesDtos.get(0).getScheduleId());
        List<ScheduleClassesTimeDto> scheduleClassesTimeDtos = scheduleClassesTimeV1InnerServiceSMOImpl.queryScheduleClassesTimes(scheduleClassesTimeDto);
        if (scheduleClassesTimeDtos != null && scheduleClassesTimeDtos.size() >0) {
            ScheduleClassesTimePo scheduleClassesTimePo = new ScheduleClassesTimePo();
            scheduleClassesTimePo.setScheduleId(scheduleClassesPo.getScheduleId());
            flag = scheduleClassesTimeV1InnerServiceSMOImpl.deleteScheduleClassesTime(scheduleClassesTimePo);
            if (flag < 1) {
                throw new CmdException("更新数据失败");
            }
        }
        
        // 保存新的排班天和时间信息
        JSONArray days = reqJson.getJSONArray("days");
        JSONObject day = null;
        JSONObject time = null;
        JSONArray times = null;
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            day = days.getJSONObject(dayIndex);
            // 创建并保存排班天信息
            ScheduleClassesDayPo scheduleClassesDayPo = new ScheduleClassesDayPo();
            scheduleClassesDayPo.setScheduleId(scheduleClassesPo.getScheduleId());
            scheduleClassesDayPo.setDayId(GenerateCodeFactory.getGeneratorId("11")); // 生成唯一ID
            scheduleClassesDayPo.setDay(day.getString("day"));
            scheduleClassesDayPo.setScheduleId(scheduleClassesPo.getScheduleId());
            scheduleClassesDayPo.setWeekFlag(StringUtil.isEmpty(day.getString("weekFlag")) ? "1" : day.getString("weekFlag")); // 默认值为1
            scheduleClassesDayPo.setWorkday(day.getString("workday"));
            flag = scheduleClassesDayV1InnerServiceSMOImpl.saveScheduleClassesDay(scheduleClassesDayPo);
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
            
            // 保存该天的排班时间信息
            times = day.getJSONArray("times");
            for (int timeIndex = 0; timeIndex < times.size(); timeIndex++) {
                time = times.getJSONObject(timeIndex);
                ScheduleClassesTimePo scheduleClassesTimePo = new ScheduleClassesTimePo();
                scheduleClassesTimePo.setScheduleId(scheduleClassesPo.getScheduleId());
                scheduleClassesTimePo.setDayId(scheduleClassesDayPo.getDayId()); // 关联排班天ID
                scheduleClassesTimePo.setEndTime(time.getString("endTime"));
                scheduleClassesTimePo.setScheduleId(scheduleClassesPo.getScheduleId());
                scheduleClassesTimePo.setStartTime(time.getString("startTime"));
                scheduleClassesTimePo.setTimeId(GenerateCodeFactory.getGeneratorId("11")); // 生成唯一ID
                flag = scheduleClassesTimeV1InnerServiceSMOImpl.saveScheduleClassesTime(scheduleClassesTimePo);
                if (flag < 1) {
                    throw new CmdException("保存数据失败");
                }
            }
        }
        
        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}