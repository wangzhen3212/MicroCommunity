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
 * 员工月度排班查询命令类
 * 
 * 该类用于处理员工某个月份的排班情况查询请求，包括参数验证、数据查询和结果返回等功能
 * 主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 查询员工排班信息
 * 3. 计算员工当月工作日信息
 * 4. 返回分页查询结果
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-29
 */
@Java110Cmd(serviceCode = "scheduleClasses.staffMonthScheduleClasses")
public class StaffMonthScheduleClassesCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(StaffMonthScheduleClassesCmd.class);
    
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
     * 验证请求参数的有效性，包括：
     * 1. 分页参数验证
     * 2. 必填参数验证（月份）
     * 3. 属性验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证必填参数：月份，格式为YYYY-MM
        Assert.hasKeyAndValue(reqJson, "curDate", "未包含月 YYYY-MM");
        // 验证其他属性
        super.validateProperty(cmdDataFlowContext);
    }

    /**
     * 执行命令方法
     * 
     * 处理员工月度排班查询请求的主要业务逻辑：
     * 1. 获取店铺ID
     * 2. 构建查询条件
     * 3. 查询员工排班总数
     * 4. 查询员工排班详细信息
     * 5. 计算每个员工当月工作日信息
     * 6. 构建分页响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务处理过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 从请求头中获取店铺ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");

        // 构建排班员工查询条件对象
        ScheduleClassesStaffDto scheduleClassesStaffDto = new ScheduleClassesStaffDto();
        scheduleClassesStaffDto.setStaffId(reqJson.getString("staffId"));           // 员工ID
        scheduleClassesStaffDto.setStaffNameLike(reqJson.getString("staffNameLike")); // 员工姓名模糊查询
        scheduleClassesStaffDto.setScheduleId(reqJson.getString("scheduleId"));     // 排班ID
        scheduleClassesStaffDto.setPage(reqJson.getIntValue("page"));               // 当前页码
        scheduleClassesStaffDto.setRow(reqJson.getIntValue("row"));                 // 每页记录数
        scheduleClassesStaffDto.setStoreId(storeId);                               // 店铺ID
        scheduleClassesStaffDto.setOrgId(reqJson.getString("orgId"));              // 组织ID

        // 查询符合条件的员工排班总数
        int count = scheduleClassesStaffV1InnerServiceSMOImpl.queryScheduleClassesStaffsCount(scheduleClassesStaffDto);

        List<ScheduleClassesStaffDto> scheduleClassesStaffDtos = null;
        // 创建临时列表用于存储处理后的员工排班信息
        List<ScheduleClassesStaffDto> tmpScheduleClassesStaffDtos =  new ArrayList<>();
        ScheduleClassesStaffDto tmpScheduleClassesStaffDto = null;

        // 如果查询到记录，则进行详细查询和处理
        if (count > 0) {
            // 查询员工排班详细信息
            scheduleClassesStaffDtos = scheduleClassesStaffV1InnerServiceSMOImpl.queryScheduleClassesStaffs(scheduleClassesStaffDto);
            
            // 遍历每个员工的排班信息，计算当月工作日
            for (ScheduleClassesStaffDto scheduleClassesStaffDto1 : scheduleClassesStaffDtos) {
                // 设置当前查询的月份
                scheduleClassesStaffDto1.setCurDate(reqJson.getString("curDate"));
                // 计算员工当月工作日信息
                tmpScheduleClassesStaffDto = scheduleClassesStaffV1InnerServiceSMOImpl.computeStaffCurMonthWorkday(scheduleClassesStaffDto1);
                // 将处理后的结果添加到临时列表
                tmpScheduleClassesStaffDtos.add(tmpScheduleClassesStaffDto);
            }
        }

        // 构建分页响应结果
        // 计算总页数：总记录数/每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, tmpScheduleClassesStaffDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应结果到数据流上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }


}