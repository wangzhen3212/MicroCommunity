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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.machine.EquipmentAccountDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.IEquipmentAccountV1InnerServiceSMO;
import com.java110.intf.community.IMaintainancePlanMachineV1InnerServiceSMO;
import com.java110.intf.community.IMaintainancePlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IMaintainancePlanV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.maintainance.MaintainancePlanPo;
import com.java110.po.maintainance.MaintainancePlanMachinePo;
import com.java110.po.maintainance.MaintainancePlanStaffPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：保存维护计划命令类
 * 服务编码：maintainancePlan.saveMaintainancePlan
 * 请求路劲：/app/maintainancePlan.SaveMaintainancePlan
 * add by 吴学文 at 2022-11-07 02:07:55 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 * 
 * 功能描述：该类用于处理维护计划的新增操作，包括验证请求参数、保存维护计划基本信息、
 *          关联维护人员和设备信息等完整业务流程
 */
@Java110Cmd(serviceCode = "maintainancePlan.saveMaintainancePlan")
public class SaveMaintainancePlanCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveMaintainancePlanCmd.class);

    /** 计划ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 维护计划服务接口 */
    @Autowired
    private IMaintainancePlanV1InnerServiceSMO maintainancePlanV1InnerServiceSMOImpl;

    /** 维护计划设备关联服务接口 */
    @Autowired
    private IMaintainancePlanMachineV1InnerServiceSMO maintainancePlanMachineV1InnerServiceSMOImpl;

    /** 维护计划人员关联服务接口 */
    @Autowired
    private IMaintainancePlanStaffV1InnerServiceSMO maintainancePlanStaffV1InnerServiceSMOImpl;

    /** 设备台账服务接口 */
    @Autowired
    private IEquipmentAccountV1InnerServiceSMO equipmentAccountV1InnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填的基本参数
        Assert.hasKeyAndValue(reqJson, "planName", "请求报文中未包含planName");
        Assert.hasKeyAndValue(reqJson, "standardId", "请求报文中未包含standardId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "startDate", "请求报文中未包含startDate");
        Assert.hasKeyAndValue(reqJson, "endDate", "请求报文中未包含endDate");
        Assert.hasKeyAndValue(reqJson, "planPeriod", "请求报文中未包含planPeriod");

        // 验证维护人员信息
        if (!reqJson.containsKey("staffs")) {
            throw new CmdException("未包含员工");
        }

        JSONArray staffs = reqJson.getJSONArray("staffs");

        if (staffs.size() < 1) {
            throw new CmdException("未包含员工");
        }

        // 验证设备信息
        if (!reqJson.containsKey("machineIds")) {
            throw new CmdException("未包含设备");
        }

        JSONArray machineIds = reqJson.getJSONArray("machineIds");

        if (machineIds.size() < 1) {
            throw new CmdException("未包含设备");
        }
    }

    /**
     * 执行维护计划保存操作
     * 包括保存维护计划基本信息、关联维护人员和设备信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 获取当前操作用户信息
        String userId = cmdDataFlowContext.getReqHeaders().get("user-id");
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setRow(1);
        userDto.setPage(1);
        List<UserDto> userDtos =  userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证用户存在且唯一
        Assert.listOnlyOne(userDtos,"员工不存在");

        // 创建并保存维护计划基本信息
        MaintainancePlanPo maintainancePlanPo = BeanConvertUtil.covertBean(reqJson, MaintainancePlanPo.class);
        maintainancePlanPo.setCreateUserId(userId);
        maintainancePlanPo.setCreateUserName(userDtos.get(0).getName());
        maintainancePlanPo.setPlanId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        int flag = maintainancePlanV1InnerServiceSMOImpl.saveMaintainancePlan(maintainancePlanPo);

        // 检查维护计划保存是否成功
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 保存维护计划关联的人员信息
        JSONArray staffs = reqJson.getJSONArray("staffs");
        MaintainancePlanStaffPo maintainancePlanStaffPo = null;
        for (int staffIndex = 0; staffIndex < staffs.size(); staffIndex++) {
            maintainancePlanStaffPo = new MaintainancePlanStaffPo();
            maintainancePlanStaffPo.setCommunityId(reqJson.getString("communityId"));
            maintainancePlanStaffPo.setPlanId(maintainancePlanPo.getPlanId());
            maintainancePlanStaffPo.setMpsId(GenerateCodeFactory.getGeneratorId("11"));
            maintainancePlanStaffPo.setStaffId(staffs.getJSONObject(staffIndex).getString("userId"));
            maintainancePlanStaffPo.setStaffName(staffs.getJSONObject(staffIndex).getString("name"));
            maintainancePlanStaffV1InnerServiceSMOImpl.saveMaintainancePlanStaff(maintainancePlanStaffPo);
        }

        // 保存维护计划关联的设备信息
        JSONArray machineIds = reqJson.getJSONArray("machineIds");
        MaintainancePlanMachinePo maintainancePlanMachinePo = null;
        EquipmentAccountDto equipmentAccountDto = null;
        List<EquipmentAccountDto> accountDtos = null;
        for (int machineIndex = 0; machineIndex < machineIds.size(); machineIndex++) {
            maintainancePlanMachinePo = new MaintainancePlanMachinePo();
            maintainancePlanMachinePo.setCommunityId(reqJson.getString("communityId"));
            maintainancePlanMachinePo.setPlanId(maintainancePlanPo.getPlanId());
            maintainancePlanMachinePo.setMpmId(GenerateCodeFactory.getGeneratorId("11"));
            maintainancePlanMachinePo.setMachineId(machineIds.getString(machineIndex));

            // 验证设备是否存在
            equipmentAccountDto = new EquipmentAccountDto();
            equipmentAccountDto.setMachineId(machineIds.getString(machineIndex));
            accountDtos = equipmentAccountV1InnerServiceSMOImpl.queryEquipmentAccounts(equipmentAccountDto);
            Assert.listOnlyOne(accountDtos, "设备不存在");

            // 设置设备名称并保存关联关系
            maintainancePlanMachinePo.setMachineName(accountDtos.get(0).getMachineName());
            maintainancePlanMachineV1InnerServiceSMOImpl.saveMaintainancePlanMachine(maintainancePlanMachinePo);
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}