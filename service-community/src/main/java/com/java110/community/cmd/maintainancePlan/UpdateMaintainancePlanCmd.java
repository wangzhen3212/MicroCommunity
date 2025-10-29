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
import com.java110.intf.community.IMaintainancePlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IMaintainancePlanV1InnerServiceSMO;
import com.java110.po.maintainance.MaintainancePlanPo;
import com.java110.po.maintainance.MaintainancePlanStaffPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：更新
 * 服务编码：maintainancePlan.updateMaintainancePlan
 * 请求路劲：/app/maintainancePlan.UpdateMaintainancePlan
 * add by 吴学文 at 2022-11-07 02:07:55 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 维护计划更新命令类
 * 该类负责处理维护计划的更新请求，包括更新维护计划基本信息以及关联的维护人员信息
 * 通过注解@Java110Cmd标识为命令类，服务编码为maintainancePlan.updateMaintainancePlan
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-07
 */
@Java110Cmd(serviceCode = "maintainancePlan.updateMaintainancePlan")
public class UpdateMaintainancePlanCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateMaintainancePlanCmd.class);

    /**
     * 维护计划服务接口
     */
    @Autowired
    private IMaintainancePlanV1InnerServiceSMO maintainancePlanV1InnerServiceSMOImpl;

    /**
     * 维护计划人员服务接口
     */
    @Autowired
    private IMaintainancePlanStaffV1InnerServiceSMO maintainancePlanStaffV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数planId和communityId是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "planId", "planId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");

        // 验证是否包含员工信息
        if (!reqJson.containsKey("staffs")) {
            throw new CmdException("未包含员工");
        }

        JSONArray staffs = reqJson.getJSONArray("staffs");

        // 验证员工列表是否至少包含一个员工
        if (staffs.size() < 1) {
            throw new CmdException("未包含员工");
        }
    }

    /**
     * 执行维护计划更新命令
     * 该方法负责更新维护计划的基本信息，并重新关联维护人员
     * 使用@Java110Transactional注解确保数据库操作的原子性
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为维护计划PO对象
        MaintainancePlanPo maintainancePlanPo = BeanConvertUtil.covertBean(reqJson, MaintainancePlanPo.class);
        
        // 更新维护计划基本信息
        int flag = maintainancePlanV1InnerServiceSMOImpl.updateMaintainancePlan(maintainancePlanPo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }

        // 准备删除原有维护人员关联关系
        MaintainancePlanStaffPo maintainancePlanStaffPo = null;
        maintainancePlanStaffPo = new MaintainancePlanStaffPo();
        // 设置计划ID用于删除关联人员
        maintainancePlanStaffPo.setPlanId(maintainancePlanPo.getPlanId());
        
        // 删除该维护计划下的所有关联人员
        flag = maintainancePlanStaffV1InnerServiceSMOImpl.deleteMaintainancePlanStaff(maintainancePlanStaffPo);

        // 检查删除是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 获取请求中的员工列表
        JSONArray staffs = reqJson.getJSONArray("staffs");
        
        // 遍历员工列表，重新建立维护计划与人员的关联关系
        for (int staffIndex = 0; staffIndex < staffs.size(); staffIndex++) {
            maintainancePlanStaffPo = new MaintainancePlanStaffPo();
            // 设置社区ID
            maintainancePlanStaffPo.setCommunityId(reqJson.getString("communityId"));
            // 设置维护计划ID
            maintainancePlanStaffPo.setPlanId(maintainancePlanPo.getPlanId());
            // 生成主键ID
            maintainancePlanStaffPo.setMpsId(GenerateCodeFactory.getGeneratorId("11"));
            // 设置员工ID
            maintainancePlanStaffPo.setStaffId(staffs.getJSONObject(staffIndex).getString("userId"));
            // 设置员工姓名
            maintainancePlanStaffPo.setStaffName(staffs.getJSONObject(staffIndex).getString("name"));
            
            // 保存维护计划与人员的关联关系
            maintainancePlanStaffV1InnerServiceSMOImpl.saveMaintainancePlanStaff(maintainancePlanStaffPo);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}