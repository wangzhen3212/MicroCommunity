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
package com.java110.community.cmd.maintainancePlan;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.maintainance.MaintainancePlanMachineDto;
import com.java110.dto.maintainance.MaintainancePlanStaffDto;
import com.java110.intf.community.IMaintainancePlanMachineV1InnerServiceSMO;
import com.java110.intf.community.IMaintainancePlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IMaintainancePlanV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.maintainance.MaintainancePlanDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：查询保养计划列表命令类
 * 服务编码：maintainancePlan.listMaintainancePlan
 * 请求路劲：/app/maintainancePlan.ListMaintainancePlan
 * 功能描述：该类用于处理保养计划列表查询请求，包括查询保养计划基本信息以及关联的设备和人员数量统计
 * add by 吴学文 at 2022-11-07 02:07:55 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "maintainancePlan.listMaintainancePlan")
public class ListMaintainancePlanCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListMaintainancePlanCmd.class);

    @Autowired
    private IMaintainancePlanV1InnerServiceSMO maintainancePlanV1InnerServiceSMOImpl;

    @Autowired
    private IMaintainancePlanStaffV1InnerServiceSMO maintainancePlanStaffV1InnerServiceSMOImpl;

    @Autowired
    private IMaintainancePlanMachineV1InnerServiceSMO maintainancePlanMachineV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数，主要验证分页参数
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 功能描述：执行保养计划列表查询，包括查询保养计划基本信息、关联设备数量和人员数量
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为保养计划DTO对象
        MaintainancePlanDto maintainancePlanDto = BeanConvertUtil.covertBean(reqJson, MaintainancePlanDto.class);
        
        // 查询保养计划总数
        int count = maintainancePlanV1InnerServiceSMOImpl.queryMaintainancePlansCount(maintainancePlanDto);
        List<MaintainancePlanDto> maintainancePlanDtos = null;
        
        if (count > 0) {
            // 查询保养计划列表
            maintainancePlanDtos = maintainancePlanV1InnerServiceSMOImpl.queryMaintainancePlans(maintainancePlanDto);
            // 刷新保养计划关联的设备和人员数量
            freshStaffAndMachineCount(maintainancePlanDtos);
        } else {
            // 如果没有数据，返回空列表
            maintainancePlanDtos = new ArrayList<>();
        }
        
        // 计算总页数：总记录数 / 每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, maintainancePlanDtos);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 刷新保养计划关联的设备和人员数量
     * 功能描述：为保养计划列表中的每个计划查询并设置关联的设备数量和人员数量
     * @param maintainancePlanDtos 保养计划DTO列表
     */
    private void freshStaffAndMachineCount(List<MaintainancePlanDto> maintainancePlanDtos) {
        // 参数校验：如果列表为空或null，直接返回
        if (maintainancePlanDtos == null || maintainancePlanDtos.size() < 1) {
            return;
        }
        
        // 收集所有保养计划ID
        List<String> planIds = new ArrayList<>();
        for (MaintainancePlanDto maintainancePlanDto : maintainancePlanDtos) {
            planIds.add(maintainancePlanDto.getPlanId());
        }
        
        // 查询保养计划关联的设备数量统计
        MaintainancePlanMachineDto maintainancePlanMachineDto = new MaintainancePlanMachineDto();
        maintainancePlanMachineDto.setPlanIds(planIds.toArray(new String[planIds.size()]));
        maintainancePlanMachineDto.setCommunityId(maintainancePlanDtos.get(0).getCommunityId());
        List<MaintainancePlanMachineDto> maintainancePlanMachineDtos = maintainancePlanMachineV1InnerServiceSMOImpl.queryMaintainancePlanMachinesGroupCount(maintainancePlanMachineDto);
        
        // 将设备数量设置到对应的保养计划中
        if (maintainancePlanMachineDtos != null && maintainancePlanMachineDtos.size() > 0) {
            for (MaintainancePlanDto maintainancePlanDto : maintainancePlanDtos) {
                for (MaintainancePlanMachineDto tmpMaintainancePlanMachineDto : maintainancePlanMachineDtos) {
                    // 根据计划ID匹配，设置设备数量
                    if (maintainancePlanDto.getPlanId().equals(tmpMaintainancePlanMachineDto.getPlanId())) {
                        maintainancePlanDto.setMachineCount(tmpMaintainancePlanMachineDto.getMachineCount());
                    }
                }
            }
        }
        
        // 查询保养计划关联的人员数量统计
        MaintainancePlanStaffDto maintainancePlanStaffDto = new MaintainancePlanStaffDto();
        maintainancePlanStaffDto.setPlanIds(planIds.toArray(new String[planIds.size()]));
        maintainancePlanStaffDto.setCommunityId(maintainancePlanDtos.get(0).getCommunityId());
        List<MaintainancePlanStaffDto> maintainancePlanStaffDtos = maintainancePlanStaffV1InnerServiceSMOImpl.queryMaintainancePlanStaffsGroupCount(maintainancePlanStaffDto);
        
        // 将人员数量设置到对应的保养计划中
        if (maintainancePlanStaffDtos != null && maintainancePlanStaffDtos.size() > 0) {
            for (MaintainancePlanDto maintainancePlanDto : maintainancePlanDtos) {
                for (MaintainancePlanStaffDto tmpMaintainancePlanStaffDto : maintainancePlanStaffDtos) {
                    // 根据计划ID匹配，设置人员数量
                    if (maintainancePlanDto.getPlanId().equals(tmpMaintainancePlanStaffDto.getPlanId())) {
                        maintainancePlanDto.setStaffCount(tmpMaintainancePlanStaffDto.getStaffCount());
                    }
                }
            }
        }
    }
}