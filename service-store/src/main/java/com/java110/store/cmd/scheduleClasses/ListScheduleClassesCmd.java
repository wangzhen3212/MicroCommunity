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
import com.java110.intf.store.IScheduleClassesStaffV1InnerServiceSMO;
import com.java110.intf.store.IScheduleClassesV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.classes.ScheduleClassesDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：scheduleClasses.listScheduleClasses
 * 请求路劲：/app/scheduleClasses.ListScheduleClasses
 * add by 吴学文 at 2022-10-29 15:29:19 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 排班查询命令类
 * 负责处理排班列表查询请求，包括分页查询排班信息并关联查询每个排班的员工数量
 * @author 吴学文
 * @version 1.0
 * @since 2022-10-29
 */
@Java110Cmd(serviceCode = "scheduleClasses.listScheduleClasses")
public class ListScheduleClassesCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListScheduleClassesCmd.class);
    
    /**
     * 排班服务接口，用于查询排班信息
     */
    @Autowired
    private IScheduleClassesV1InnerServiceSMO scheduleClassesV1InnerServiceSMOImpl;

    /**
     * 排班员工服务接口，用于查询排班关联的员工信息
     */
    @Autowired
    private IScheduleClassesStaffV1InnerServiceSMO scheduleClassesStaffV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数，主要验证分页参数
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法
     * 处理排班列表查询请求，包括分页查询和员工数量统计
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取店铺ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");

        // 将请求参数转换为排班DTO对象
        ScheduleClassesDto scheduleClassesDto = BeanConvertUtil.covertBean(reqJson, ScheduleClassesDto.class);
        scheduleClassesDto.setStoreId(storeId);

        // 查询排班总数
        int count = scheduleClassesV1InnerServiceSMOImpl.queryScheduleClassessCount(scheduleClassesDto);

        List<ScheduleClassesDto> scheduleClassesDtos = null;

        if (count > 0) {
            // 查询排班列表数据
            scheduleClassesDtos = scheduleClassesV1InnerServiceSMOImpl.queryScheduleClassess(scheduleClassesDto);

            // 为每个排班填充员工数量信息
            freshScheduleClassesStaff(scheduleClassesDtos);
        } else {
            scheduleClassesDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数/每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, scheduleClassesDtos);

        // 构建响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新排班员工数量信息
     * 为排班列表中的每个排班设置对应的员工数量
     * @param scheduleClassesDtos 排班数据列表
     */
    private void freshScheduleClassesStaff(List<ScheduleClassesDto> scheduleClassesDtos) {
        // 参数校验：如果排班列表为空或null，直接返回
        if(scheduleClassesDtos == null || scheduleClassesDtos.size()< 1){
            return ;
        }

        // 收集所有排班ID
        List<String> scheduleIds = new ArrayList<>();
        for(ScheduleClassesDto scheduleClassesDto : scheduleClassesDtos){
            scheduleIds.add(scheduleClassesDto.getScheduleId());
        }

        // 构建查询条件，查询排班员工分组统计信息
        ScheduleClassesStaffDto  scheduleClassesStaffDto = new ScheduleClassesStaffDto();
        scheduleClassesStaffDto.setScheduleIds(scheduleIds.toArray(new String[scheduleIds.size()]));
        List<ScheduleClassesStaffDto> scheduleClassesStaffDtos = scheduleClassesStaffV1InnerServiceSMOImpl.queryGroupScheduleClassesStaffs(scheduleClassesStaffDto);

        // 遍历排班列表，为每个排班设置员工数量
        for(ScheduleClassesDto scheduleClassesDto : scheduleClassesDtos){
            for(ScheduleClassesStaffDto scheduleClassesStaffDto1 : scheduleClassesStaffDtos){
                // 根据排班ID匹配对应的员工统计信息
                if(scheduleClassesDto.getScheduleId().equals(scheduleClassesStaffDto1.getScheduleId())){
                    scheduleClassesDto.setStaffCount(scheduleClassesStaffDto1.getStaffCount());
                }
            }
        }
    }
}