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
package com.java110.user.cmd.ownerSettled;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerSettledSettingDto;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.user.IOwnerSettledApplyV1InnerServiceSMO;
import com.java110.intf.user.IOwnerSettledRoomsV1InnerServiceSMO;
import com.java110.intf.user.IOwnerSettledSettingV1InnerServiceSMO;
import com.java110.po.owner.OwnerSettledApplyPo;
import com.java110.po.owner.OwnerSettledRoomsPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：删除业主入驻申请信息
 * 服务编码：ownerSettledApply.deleteOwnerSettledApply
 * 请求路劲：/app/ownerSettledApply.DeleteOwnerSettledApply
 * add by 吴学文 at 2023-01-26 00:52:26 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
/**
 * 删除业主入驻申请命令类
 * 负责处理删除业主入驻申请相关的业务逻辑，包括删除申请信息、关联的房间信息以及处理审核流程
 * @author 吴学文
 * @version 1.0
 * @since 2023-01-26
 */
@Java110Cmd(serviceCode = "ownerSettled.deleteOwnerSettledApply")
public class DeleteOwnerSettledApplyCmd extends Cmd {
    private static Logger logger = LoggerFactory.getLogger(DeleteOwnerSettledApplyCmd.class);

    /**
     * 业主入驻申请服务接口
     */
    @Autowired
    private IOwnerSettledApplyV1InnerServiceSMO ownerSettledApplyV1InnerServiceSMOImpl;

    /**
     * 业主入驻房间服务接口
     */
    @Autowired
    private IOwnerSettledRoomsV1InnerServiceSMO ownerSettledRoomsV1InnerServiceSMOImpl;

    /**
     * 业主入驻设置服务接口
     */
    @Autowired
    private IOwnerSettledSettingV1InnerServiceSMO ownerSettledSettingV1InnerServiceSMOImpl;

    /**
     * OA工作流服务接口
     */
    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowActivitiInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的字段
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证申请ID不能为空
        Assert.hasKeyAndValue(reqJson, "applyId", "applyId不能为空");
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行删除业主入驻申请命令
     * 主要逻辑包括：
     * 1. 删除业主入驻申请信息
     * 2. 删除关联的房间信息
     * 3. 检查是否需要处理审核流程
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为业主入驻申请PO对象
        OwnerSettledApplyPo ownerSettledApplyPo = BeanConvertUtil.covertBean(reqJson, OwnerSettledApplyPo.class);
        // 删除业主入驻申请信息
        int flag = ownerSettledApplyV1InnerServiceSMOImpl.deleteOwnerSettledApply(ownerSettledApplyPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 创建业主入驻房间PO对象并设置申请ID
        OwnerSettledRoomsPo ownerSettledRoomsPo = new OwnerSettledRoomsPo();
        ownerSettledRoomsPo.setApplyId(reqJson.getString("applyId"));
        ownerSettledApplyPo.setCommunityId(reqJson.getString("communityId"));
        // 删除关联的房间信息
        flag = ownerSettledRoomsV1InnerServiceSMOImpl.deleteOwnerSettledRooms(ownerSettledRoomsPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 检查是否开启了审核流程
        OwnerSettledSettingDto ownerSettledSettingDto = new OwnerSettledSettingDto();
        ownerSettledSettingDto.setCommunityId(reqJson.getString("communityId"));
        ownerSettledSettingDto.setAuditWay("Y"); // 设置审核方式为需要审核
        List<OwnerSettledSettingDto> ownerSettledSettingDtos
                = ownerSettledSettingV1InnerServiceSMOImpl.queryOwnerSettledSettings(ownerSettledSettingDto);
        
        // 如果不需要走审核流程，直接返回成功
        if (ownerSettledSettingDtos == null || ownerSettledSettingDtos.size() < 1) {
            return;
        }

        // 如果需要走审核流程，这里可以添加相关的OA工作流处理逻辑
        // oaWorkflowActivitiInnerServiceSMOImpl.

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}