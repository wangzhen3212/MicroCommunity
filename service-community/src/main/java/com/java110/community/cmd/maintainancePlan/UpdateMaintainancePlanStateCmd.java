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
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.maintainance.MaintainancePlanDto;
import com.java110.intf.community.IMaintainancePlanStaffV1InnerServiceSMO;
import com.java110.intf.community.IMaintainancePlanV1InnerServiceSMO;
import com.java110.po.maintainance.MaintainancePlanPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;


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
 * 维护计划状态更新命令类
 * 
 * 该类负责处理维护计划状态更新的业务逻辑，包括参数验证和状态更新操作。
 * 通过调用维护计划服务接口实现状态的变更，并确保数据的一致性和完整性。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-11-07
 */
@Java110Cmd(serviceCode = "maintainancePlan.updateMaintainancePlanState")
public class UpdateMaintainancePlanStateCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(UpdateMaintainancePlanStateCmd.class);

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
     * 
     * 验证请求参数是否完整和有效，确保必要的参数都存在且不为空。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证计划ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "planId", "planId不能为空");
        // 验证小区ID参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
        // 验证状态参数是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "state", "必填，请填写状态");
    }

    /**
     * 执行状态更新命令
     * 
     * 根据请求参数更新维护计划的状态，包括查询现有计划、验证数据唯一性、
     * 更新状态并返回操作结果。
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建维护计划数据传输对象
        MaintainancePlanDto maintainancePlanDto = new MaintainancePlanDto();
        // 设置小区ID
        maintainancePlanDto.setCommunityId(reqJson.getString("communityId"));
        // 设置计划ID
        maintainancePlanDto.setPlanId(reqJson.getString("planId"));
        
        // 根据条件查询维护计划列表
        List<MaintainancePlanDto> maintainancePlanDtos = maintainancePlanV1InnerServiceSMOImpl.queryMaintainancePlans(maintainancePlanDto);

        // 验证查询结果是否只有一条记录，确保数据唯一性
        Assert.listOnlyOne(maintainancePlanDtos, "根据计划ID查询到多条记录，请检查数据");
        
        // 创建业务维护计划JSON对象
        JSONObject businessMaintainancePlan = new JSONObject();
        // 将查询到的维护计划数据转换为Map并放入JSON对象
        businessMaintainancePlan.putAll(BeanConvertUtil.beanCovertMap(maintainancePlanDtos.get(0)));
        // 更新状态字段
        businessMaintainancePlan.put("state", reqJson.getString("state"));
        
        // 将JSON对象转换为维护计划持久化对象
        MaintainancePlanPo maintainancePlanPo = BeanConvertUtil.covertBean(businessMaintainancePlan, MaintainancePlanPo.class);
        // 执行更新操作
        int flag = maintainancePlanV1InnerServiceSMOImpl.updateMaintainancePlan(maintainancePlanPo);
        
        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("修改巡检计划失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}