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
import com.java110.dto.classes.ScheduleClassesStaffDto;
import com.java110.intf.store.IScheduleClassesDayV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesStaffV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;


/**
 * 类表述：查询 员工 某月 排班情况
 * 服务编码：scheduleClassesStaff.listScheduleClassesStaff
 * 请求路劲：/app/scheduleClassesStaff.ListScheduleClassesStaff
 * add by 吴学文 at 2022-10-29 16:16:44 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 员工管理员月度排班查询命令类
 * 
 * 该类用于处理员工管理员查看指定月份排班情况的业务逻辑，
 * 包括参数验证、数据查询和结果返回等功能
 * 
 * @author 吴学文
 * @version 1.0
 * @date 2022-10-29
 */
@Java110Cmd(serviceCode = "scheduleClasses.staffAdminMonthScheduleClasses")
public class StaffAdminMonthScheduleClassesCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(StaffAdminMonthScheduleClassesCmd.class);
    
    /**
     * 排班员工信息服务接口
     */
    @Autowired
    private IScheduleClassesStaffV1InnerServiceSMO scheduleClassesStaffV1InnerServiceSMOImpl;

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
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和必要参数检查
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证必须包含当前日期参数
        Assert.hasKeyAndValue(reqJson, "curDate", "未包含月 YYYY-MM");
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
    }

    /**
     * 执行命令方法
     * 
     * 处理员工管理员月度排班查询业务逻辑，包括数据查询、计算和结果返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 创建排班员工数据传输对象
        ScheduleClassesStaffDto scheduleClassesStaffDto = new ScheduleClassesStaffDto();
        // 设置员工ID查询条件
        scheduleClassesStaffDto.setStaffId(reqJson.getString("staffId"));
        // 设置员工姓名模糊查询条件
        scheduleClassesStaffDto.setStaffNameLike(reqJson.getString("staffNameLike"));
        // 设置排班ID查询条件
        scheduleClassesStaffDto.setScheduleId(reqJson.getString("scheduleId"));
        // 设置分页参数
        scheduleClassesStaffDto.setPage(reqJson.getIntValue("page"));
        scheduleClassesStaffDto.setRow(reqJson.getIntValue("row"));
        // 设置组织ID查询条件
        scheduleClassesStaffDto.setOrgId(reqJson.getString("orgId"));

        // 查询符合条件的排班员工总数
        int count = scheduleClassesStaffV1InnerServiceSMOImpl.queryScheduleClassesStaffsCount(scheduleClassesStaffDto);

        List<ScheduleClassesStaffDto> scheduleClassesStaffDtos = null;
        // 初始化临时结果列表
        List<ScheduleClassesStaffDto> tmpScheduleClassesStaffDtos =  new ArrayList<>();
        ScheduleClassesStaffDto tmpScheduleClassesStaffDto = null;

        // 如果查询结果数量大于0，则进行详细数据查询和计算
        if (count > 0) {
            // 查询排班员工详细信息列表
            scheduleClassesStaffDtos = scheduleClassesStaffV1InnerServiceSMOImpl.queryScheduleClassesStaffs(scheduleClassesStaffDto);
            // 遍历每个排班员工记录，计算当月工作日信息
            for (ScheduleClassesStaffDto scheduleClassesStaffDto1 : scheduleClassesStaffDtos) {
                // 设置当前日期参数，用于计算当月工作日
                scheduleClassesStaffDto1.setCurDate(reqJson.getString("curDate"));
                // 计算员工当月工作日信息
                tmpScheduleClassesStaffDto = scheduleClassesStaffV1InnerServiceSMOImpl.computeStaffCurMonthWorkday(scheduleClassesStaffDto1);
                // 将计算结果添加到临时结果列表
                tmpScheduleClassesStaffDtos.add(tmpScheduleClassesStaffDto);
            }
        }

        // 构建返回结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, tmpScheduleClassesStaffDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }


}