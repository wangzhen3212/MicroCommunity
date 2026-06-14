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
import com.java110.intf.store.IScheduleClassesDayV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesTimeV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesV1InnerServiceSMO;
import com.java110.po.classes.ScheduleClassesPo;
import com.java110.po.classes.ScheduleClassesDayPo;
import com.java110.po.classes.ScheduleClassesTimePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：排班信息保存命令类
 * 服务编码：scheduleClasses.saveScheduleClasses
 * 请求路劲：/app/scheduleClasses.SaveScheduleClasses
 * 功能描述：该类负责处理排班信息的保存请求，包括排班基本信息、排班天信息和排班时间信息的保存
 * add by 吴学文 at 2022-10-29 15:29:19 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "scheduleClasses.saveScheduleClasses")
public class SaveScheduleClassesCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveScheduleClassesCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

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
     * 参数校验方法
     * 功能描述：验证请求参数的有效性，确保必要的参数存在且格式正确
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        Assert.hasKeyAndValue(reqJson, "scheduleType", "请求报文中未包含scheduleType");
        Assert.hasKeyAndValue(reqJson, "scheduleCycle", "请求报文中未包含scheduleCycle");
        
        // 验证排班天信息是否存在
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
            // 如果是非工作日，跳过时间验证
            if (ScheduleClassesDayDto.WORKDAY_NO.equals(day.getString("workday"))) {
                continue;
            }
            // 验证工作日必须包含时间信息
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
     * 执行保存排班信息命令
     * 功能描述：保存排班信息，包括排班基本信息、排班天信息和排班时间信息
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取店铺ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");
        
        // 保存排班基本信息
        ScheduleClassesPo scheduleClassesPo = BeanConvertUtil.covertBean(reqJson, ScheduleClassesPo.class);
        scheduleClassesPo.setScheduleId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID)); // 生成排班ID
        scheduleClassesPo.setStoreId(storeId); // 设置店铺ID
        scheduleClassesPo.setState(ScheduleClassesDto.STATE_START); // 设置状态为启用
        scheduleClassesPo.setComputeTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_B)); // 设置计算时间
        
        int flag = scheduleClassesV1InnerServiceSMOImpl.saveScheduleClasses(scheduleClassesPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 保存排班天信息
        JSONArray days = reqJson.getJSONArray("days");
        JSONObject day = null;
        ScheduleClassesDayPo scheduleClassesDayPo = null;
        JSONObject time = null;
        ScheduleClassesTimePo scheduleClassesTimePo = null;
        JSONArray times = null;
        
        // 遍历每个排班天
        for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
            day = days.getJSONObject(dayIndex);
            scheduleClassesDayPo = new ScheduleClassesDayPo();
            scheduleClassesDayPo.setDayId(GenerateCodeFactory.getGeneratorId("11")); // 生成天ID
            scheduleClassesDayPo.setDay(day.getString("day")); // 设置天
            scheduleClassesDayPo.setScheduleId(scheduleClassesPo.getScheduleId()); // 设置排班ID
            scheduleClassesDayPo.setWeekFlag(StringUtil.isEmpty(day.getString("weekFlag")) ? "1" : day.getString("weekFlag")); // 设置周标志，默认为1
            scheduleClassesDayPo.setWorkday(day.getString("workday")); // 设置工作日标志
            
            flag = scheduleClassesDayV1InnerServiceSMOImpl.saveScheduleClassesDay(scheduleClassesDayPo);
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
            
            // 保存排班时间信息
            times = day.getJSONArray("times");
            for (int timeIndex = 0; timeIndex < times.size(); timeIndex++) {
                time = times.getJSONObject(timeIndex);
                scheduleClassesTimePo = new ScheduleClassesTimePo();
                scheduleClassesTimePo.setDayId(scheduleClassesDayPo.getDayId()); // 设置天ID
                scheduleClassesTimePo.setEndTime(time.getString("endTime")); // 设置结束时间
                scheduleClassesTimePo.setScheduleId(scheduleClassesPo.getScheduleId()); // 设置排班ID
                scheduleClassesTimePo.setStartTime(time.getString("startTime")); // 设置开始时间
                scheduleClassesTimePo.setTimeId(GenerateCodeFactory.getGeneratorId("11")); // 生成时间ID
                
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