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
import com.java110.dto.maintainance.MaintainancePlanMachineDto;
import com.java110.intf.common.IEquipmentAccountV1InnerServiceSMO;
import com.java110.intf.community.IMaintainancePlanMachineV1InnerServiceSMO;
import com.java110.po.maintainance.MaintainancePlanMachinePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 保养计划设备关联保存命令类
 * 
 * 该类负责处理保养计划与设备关联的保存操作，包括参数验证和业务逻辑处理
 * 服务编码：maintainancePlanMachine.saveMaintainancePlanMachine
 * 请求路径：/app/maintainancePlanMachine.SaveMaintainancePlanMachine
 * 
 * @author 吴学文 at 2022-11-07 02:22:18 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "maintainancePlan.saveMaintainancePlanMachine")
public class SaveMaintainancePlanMachineCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveMaintainancePlanMachineCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 保养计划设备关联服务接口
     */
    @Autowired
    private IMaintainancePlanMachineV1InnerServiceSMO maintainancePlanMachineV1InnerServiceSMOImpl;

    /**
     * 设备台账服务接口
     */
    @Autowired
    private IEquipmentAccountV1InnerServiceSMO equipmentAccountV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括必填字段检查、设备数据验证和重复关联检查
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "planId", "请求报文中未包含planId");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");

        // 验证是否包含设备数据
        if(!reqJson.containsKey("machines")){
            throw new CmdException("未包含保养设备");
        }

        JSONArray items = reqJson.getJSONArray("machines");

        // 验证设备列表是否为空
        if(items.size() < 1){
            throw new CmdException("未包含保养设备");
        }
        
        String machineId = "";
        MaintainancePlanMachineDto maintainancePlanMachineDto = new MaintainancePlanMachineDto();
        maintainancePlanMachineDto.setPlanId(reqJson.getString("planId"));
        List<MaintainancePlanMachineDto> maintainancePlanMachineDtos = null;
        
        // 遍历所有设备，检查是否已关联到当前保养计划
        for(int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
            machineId = items.getString(itemIndex);
            maintainancePlanMachineDto.setMachineId(machineId);
            // 查询设备是否已关联到当前保养计划
            maintainancePlanMachineDtos = maintainancePlanMachineV1InnerServiceSMOImpl.queryMaintainancePlanMachines(maintainancePlanMachineDto);

            // 如果设备已关联，抛出异常
            if (maintainancePlanMachineDtos != null && maintainancePlanMachineDtos.size() >0) {
                throw new CmdException(maintainancePlanMachineDtos.get(0).getMachineName()+"已经添加");
            }
        }
    }

    /**
     * 执行保存保养计划设备关联命令
     * 
     * 处理保养计划与设备的关联关系保存，包括设备信息验证和关联记录创建
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        MaintainancePlanMachinePo maintainancePlanMachinePo = null;

        int flag = 0;
        JSONArray machineIds = reqJson.getJSONArray("machines");
        EquipmentAccountDto equipmentAccountDto = null;
        List<EquipmentAccountDto> accountDtos = null;
        
        // 遍历所有设备ID，为每个设备创建保养计划关联记录
        for (int machineIndex = 0; machineIndex < machineIds.size(); machineIndex++) {
            // 创建保养计划设备关联对象
            maintainancePlanMachinePo = new MaintainancePlanMachinePo();
            maintainancePlanMachinePo.setCommunityId(reqJson.getString("communityId"));
            maintainancePlanMachinePo.setPlanId(reqJson.getString("planId"));
            // 生成唯一ID
            maintainancePlanMachinePo.setMpmId(GenerateCodeFactory.getGeneratorId("11"));
            maintainancePlanMachinePo.setMachineId(machineIds.getString(machineIndex));

            // 验证设备是否存在
            equipmentAccountDto = new EquipmentAccountDto();
            equipmentAccountDto.setMachineId(machineIds.getString(machineIndex));
            accountDtos = equipmentAccountV1InnerServiceSMOImpl.queryEquipmentAccounts(equipmentAccountDto);
            // 确保设备存在且唯一
            Assert.listOnlyOne(accountDtos, "设备不存在");

            // 设置设备名称
            maintainancePlanMachinePo.setMachineName(accountDtos.get(0).getMachineName());
            // 保存保养计划设备关联记录
            flag = maintainancePlanMachineV1InnerServiceSMOImpl.saveMaintainancePlanMachine(maintainancePlanMachinePo);
            // 检查保存是否成功
            if(flag < 1){
                throw new CmdException("未包含保养设备");
            }
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}