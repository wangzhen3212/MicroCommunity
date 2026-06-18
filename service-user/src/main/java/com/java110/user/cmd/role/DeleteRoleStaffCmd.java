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

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.privilege.PrivilegeUserDto;
import com.java110.intf.user.IPrivilegeUserV1InnerServiceSMO;
import com.java110.po.privilege.PrivilegeUserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 类表述：删除角色员工关系
 * 服务编码：roleStaff.deleteRoleStaff
 * 请求路劲：/app/roleStaff.DeleteRoleStaff
 * add by 吴学文 at 2022-07-25 17:12:49 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroStaff
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 删除角色员工关系命令类
 * 该类负责处理删除角色与员工关联关系的业务逻辑，通过服务编码"role.deleteRoleStaff"对外提供服务
 * 主要功能包括参数验证、权限用户关系查询和删除操作
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2022-07-25
 */
@Java110Cmd(serviceCode = "role.deleteRoleStaff")
public class DeleteRoleStaffCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(DeleteRoleStaffCmd.class);

    /**
     * 权限用户服务接口，用于操作权限用户关系数据
     */
    @Autowired
    private IPrivilegeUserV1InnerServiceSMO privilegeUserV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的角色ID和用户ID
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含roleId
        Assert.hasKeyAndValue(reqJson, "roleId", "roleId不能为空");
        // 验证请求参数中必须包含userId
        Assert.hasKeyAndValue(reqJson, "userId", "userId不能为空");
    }

    /**
     * 执行删除角色员工关系命令
     * 根据角色ID和用户ID查询权限用户关系，并删除对应的关系记录
     * 使用事务注解确保操作的原子性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 创建权限用户查询条件对象
        PrivilegeUserDto privilegeUserDto = new PrivilegeUserDto();
        // 设置角色ID作为查询条件
        privilegeUserDto.setpId(reqJson.getString("roleId"));
        // 设置用户ID作为查询条件
        privilegeUserDto.setUserId(reqJson.getString("userId"));
        
        // 根据条件查询权限用户关系列表
        List<PrivilegeUserDto> privilegeUserDtos = privilegeUserV1InnerServiceSMOImpl.queryPrivilegeUsers(privilegeUserDto);

        // 如果查询结果为空，直接返回，无需执行删除操作
        if (privilegeUserDtos == null || privilegeUserDtos.size() < 1) {
            return;
        }

        // 遍历查询到的权限用户关系列表，逐个删除
        for(PrivilegeUserDto tmpPrivilegeUserDto : privilegeUserDtos) {
            // 创建权限用户PO对象用于删除操作
            PrivilegeUserPo privilegeUserPo = new PrivilegeUserPo();
            // 设置要删除的权限用户关系ID
            privilegeUserPo.setPuId(tmpPrivilegeUserDto.getPuId());
            
            // 执行删除操作，返回影响的行数
            int flag = privilegeUserV1InnerServiceSMOImpl.deletePrivilegeUser(privilegeUserPo);

            // 如果删除失败（影响行数小于1），抛出异常
            if (flag < 1) {
                throw new CmdException("删除数据失败");
            }
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}