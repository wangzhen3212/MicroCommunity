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
package com.java110.user.cmd.role;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.privilege.PrivilegeUserDto;
import com.java110.intf.user.IPrivilegeUserV1InnerServiceSMO;
import com.java110.po.privilege.PrivilegeUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 角色员工关系保存命令类
 * 
 * 该类用于处理角色与员工关联关系的保存操作，主要功能包括：
 * 1. 验证请求参数的有效性
 * 2. 批量保存角色与员工的关联关系
 * 3. 处理事务性操作确保数据一致性
 * 
 * 服务编码：role.saveRoleStaff
 * 请求路径：/app/role.SaveRoleStaff
 * 
 * @author 吴学文 at 2022-07-25 17:12:49 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 * @see IPrivilegeUserV1InnerServiceSMO
 */
@Java110Cmd(serviceCode = "role.saveRoleStaff")
public class SaveRoleStaffCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SaveRoleStaffCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 权限用户服务接口
     */
    @Autowired
    private IPrivilegeUserV1InnerServiceSMO privilegeUserV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中的必填参数，包括：
     * 1. 角色ID（roleId）必须存在
     * 2. 员工信息列表（staffs）必须存在且不为空
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws IllegalArgumentException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中必须包含roleId参数
        Assert.hasKeyAndValue(reqJson, "roleId", "请求报文中未包含roleId");
        
        // 验证请求报文中必须包含staffs参数且至少包含一个员工信息
        if (!reqJson.containsKey("staffs") || reqJson.getJSONArray("staffs").size() < 1) {
            throw new IllegalArgumentException("未包含员工信息");
        }
    }

    /**
     * 执行保存角色员工关系命令
     * 
     * 该方法用于批量保存角色与员工的关联关系，主要逻辑包括：
     * 1. 从请求头中获取店铺ID
     * 2. 遍历员工列表，为每个员工创建权限用户关系
     * 3. 调用服务保存权限用户关系
     * 4. 处理保存结果并返回响应
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存数据失败时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求头中获取店铺ID
        String storeId = cmdDataFlowContext.getReqHeaders().get("store-id");

        PrivilegeUserPo privilegeUserPo = null;
        // 获取员工信息数组
        JSONArray staffs = reqJson.getJSONArray("staffs");
        
        // 遍历所有员工信息，为每个员工创建角色关联
        for (int staffIndex = 0; staffIndex < staffs.size(); staffIndex++) {
            privilegeUserPo = new PrivilegeUserPo();
            // 设置员工ID
            privilegeUserPo.setUserId(staffs.getJSONObject(staffIndex).getString("staffId"));
            // 设置角色ID
            privilegeUserPo.setpId(reqJson.getString("roleId"));
            // 设置店铺ID
            privilegeUserPo.setStoreId(storeId);
            // 生成权限用户关系ID
            privilegeUserPo.setPuId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            // 设置权限标志为组权限
            privilegeUserPo.setPrivilegeFlag(PrivilegeUserDto.PRIVILEGE_FLAG_GROUP);
            
            // 调用服务保存权限用户关系
            int flag = privilegeUserV1InnerServiceSMOImpl.savePrivilegeUser(privilegeUserPo);

            // 检查保存结果，如果保存失败则抛出异常
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }
        }

        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}