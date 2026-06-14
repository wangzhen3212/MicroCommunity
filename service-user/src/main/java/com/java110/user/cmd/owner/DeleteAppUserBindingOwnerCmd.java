/**
 * 删除APP用户与业主绑定关系命令类
 * 
 * 该命令用于处理删除APP用户与业主绑定关系的业务逻辑，包括验证环境和执行解绑操作。
 * 在演示环境(TEST/DEV)下禁止解绑操作，防止演示账号无法登录。
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.owner;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.owner.OwnerAppUserDto;
import com.java110.intf.user.IOwnerAppUserV1InnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.owner.OwnerAppUserPo;
import com.java110.po.user.UserPo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.constant.MappingConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "owner.deleteAppUserBindingOwner")
public class DeleteAppUserBindingOwnerCmd extends Cmd {

    /**
     * 业主APP用户绑定关系服务接口
     */
    @Autowired
    private IOwnerAppUserV1InnerServiceSMO ownerAppUserV1InnerServiceSMOImpl;

    /**
     * 用户服务接口
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 验证请求参数和环境配置
     * 
     * 该方法用于验证解绑操作的合法性，包括：
     * 1. 验证必要的请求参数是否存在
     * 2. 检查当前环境是否为演示环境，演示环境下禁止解绑操作
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败或演示环境下尝试解绑时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含appUserId
        Assert.hasKeyAndValue(reqJson, "appUserId", "绑定ID不能为空");

        // 获取当前环境配置
        String env = MappingCache.getValue(MappingConstant.ENV_DOMAIN, "HC_ENV");

        // 演示环境(TEST/DEV)下禁止解绑操作，防止演示账号无法登录
        if ("DEV".equals(env) || "TEST".equals(env)) {
            throw new IllegalArgumentException("演示环境不能解绑，解绑后演示账号手机端无法登陆");
        }
    }

    /**
     * 执行删除APP用户与业主绑定关系的核心业务逻辑
     * 
     * 该方法主要执行以下操作：
     * 1. 查询绑定信息
     * 2. 删除绑定关系
     * 3. 根据条件删除关联的用户信息
     * 4. 设置操作结果响应
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 根据appUserId查询绑定信息
        OwnerAppUserDto ownerAppUserDto = new OwnerAppUserDto();
        ownerAppUserDto.setAppUserId(reqJson.getString("appUserId"));
        List<OwnerAppUserDto> ownerAppUserDtos = ownerAppUserV1InnerServiceSMOImpl.queryOwnerAppUsers(ownerAppUserDto);

        // 验证绑定信息存在且唯一
        Assert.listOnlyOne(ownerAppUserDtos, "绑定信息不存在");

        // 将请求参数转换为PO对象并执行删除操作
        OwnerAppUserPo ownerAppUserPo = BeanConvertUtil.covertBean(reqJson, OwnerAppUserPo.class);
        int flag = ownerAppUserV1InnerServiceSMOImpl.deleteOwnerAppUser(ownerAppUserPo);
        
        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除失败");
        }

        // 如果绑定信息中没有用户ID，直接返回成功
        if (StringUtil.isEmpty(ownerAppUserDtos.get(0).getUserId())) {
            return;
        }
        
        // 如果用户ID以"-"开头，说明是特殊用户，不删除用户信息
        if (ownerAppUserDtos.get(0).getUserId().startsWith("-")) {
            return;
        }
        
        // 删除关联的用户信息
        UserPo userPo = new UserPo();
        userPo.setUserId(ownerAppUserDtos.get(0).getUserId());
        userV1InnerServiceSMOImpl.deleteUser(userPo);

        // 设置操作成功的响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}