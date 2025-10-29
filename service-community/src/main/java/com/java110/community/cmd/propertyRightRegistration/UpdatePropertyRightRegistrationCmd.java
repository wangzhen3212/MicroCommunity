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
package com.java110.community.cmd.propertyRightRegistration;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.room.PropertyRightRegistrationDto;
import com.java110.intf.community.IPropertyRightRegistrationV1InnerServiceSMO;
import com.java110.po.room.PropertyRightRegistrationPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

import java.util.List;

/**
 * 类表述：更新
 * 服务编码：propertyRightRegistration.updatePropertyRightRegistration
 * 请求路劲：/app/propertyRightRegistration.UpdatePropertyRightRegistration
 * add by 吴学文 at 2021-10-09 10:34:14 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 产权登记更新命令类
 * 负责处理产权登记信息的更新操作，包括数据验证、业务逻辑处理和数据库更新
 * 主要功能：更新产权登记信息，支持审核状态变更和重复申请检查
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-10-09
 */
@Java110Cmd(serviceCode = "propertyRightRegistration.updatePropertyRightRegistration")
public class UpdatePropertyRightRegistrationCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdatePropertyRightRegistrationCmd.class);

    /**
     * 产权登记服务接口，用于操作产权登记相关数据
     */
    @Autowired
    private IPropertyRightRegistrationV1InnerServiceSMO propertyRightRegistrationV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中必须包含prrId和communityId字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置数据流信息
     * @param reqJson 请求参数JSON对象，包含前端传递的所有参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含prrId字段
        Assert.hasKeyAndValue(reqJson, "prrId", "prrId不能为空");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行产权登记更新命令
     * 处理产权登记信息的更新逻辑，包括状态验证、重复申请检查和数据更新
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务逻辑验证失败或数据更新失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取状态参数
        String state = reqJson.getString("state");
        // 检查是否为审核拒绝状态（状态值为2）
        if (!StringUtil.isEmpty(state) && state.equals("2")) {
            // 审核拒绝时，必须填写备注信息
            if (reqJson.containsKey("remark")) {
                // 获取备注内容
                String remark = reqJson.getString("remark");
                // 验证备注不能为空
                if (StringUtil.isEmpty(remark)) {
                    throw new CmdException("备注不能为空！");
                }
            }
        }
        
        // 创建产权登记查询对象
        PropertyRightRegistrationDto propertyRightRegistrationDto = new PropertyRightRegistrationDto();
        // 设置房屋ID用于查询
        propertyRightRegistrationDto.setRoomId(reqJson.getString("roomId"));
        // 根据房屋ID查询已存在的产权登记记录
        List<PropertyRightRegistrationDto> propertyRightRegistrationDtos = propertyRightRegistrationV1InnerServiceSMOImpl.queryPropertyRightRegistrations(propertyRightRegistrationDto);
        
        // 检查是否存在其他产权登记记录
        if (propertyRightRegistrationDtos != null && propertyRightRegistrationDtos.size() > 0) {
            for (PropertyRightRegistrationDto propertyRightRegistration : propertyRightRegistrationDtos) {
                // 获取现有记录的产权登记ID
                String prrId = propertyRightRegistration.getPrrId();
                // 如果存在其他房屋的产权登记记录，则抛出异常
                if (!prrId.equals(reqJson.getString("prrId"))) {
                    throw new CmdException("该房屋已经申请过房屋产权！");
                }
            }
        }
        
        // 将请求JSON对象转换为产权登记PO对象
        PropertyRightRegistrationPo propertyRightRegistrationPo = BeanConvertUtil.covertBean(reqJson, PropertyRightRegistrationPo.class);
        int flag = 0;
        
        // 根据flag参数决定更新逻辑
        if (reqJson.containsKey("flag") && reqJson.getString("flag").equals("1")) {
            // flag为1时，直接更新产权登记信息
            flag = propertyRightRegistrationV1InnerServiceSMOImpl.updatePropertyRightRegistration(propertyRightRegistrationPo);
        } else {
            // flag不为1时，将状态重置为未审核状态（状态值为0）
            propertyRightRegistrationPo.setState("0");
            // 更新产权登记信息
            flag = propertyRightRegistrationV1InnerServiceSMOImpl.updatePropertyRightRegistration(propertyRightRegistrationPo);
        }
        
        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}