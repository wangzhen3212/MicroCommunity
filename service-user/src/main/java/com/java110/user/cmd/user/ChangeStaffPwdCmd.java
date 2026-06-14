/**
 * 修改员工密码命令类
 * 
 * 该类负责处理员工密码修改的业务逻辑，包括验证请求参数、校验原始密码、更新新密码等操作
 * 通过注解@Java110Cmd(serviceCode = "user.changeStaffPwd")标识为服务命令
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.AuthenticationFactory;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.user.UserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 修改员工密码命令类
 * 
 * 该类继承自Cmd基类，通过Spring注解实现依赖注入，主要功能包括：
 * 1. 验证请求参数的完整性
 * 2. 对密码进行MD5加密处理
 * 3. 校验原始密码的正确性
 * 4. 更新用户的新密码
 * 5. 处理密码修改过程中的异常情况
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "user.changeStaffPwd")
public class ChangeStaffPwdCmd extends Cmd {
    
    /**
     * 用户内部服务接口，用于查询用户信息
     */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;
    
    /**
     * 用户V1版本内部服务接口，用于更新用户信息
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;
    
    /**
     * 参数验证方法
     * 
     * 在开发环境下进行环境检查，确保在正确的环境中运行
     * 该方法主要用于开发阶段的调试和验证
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 检查当前是否为开发环境，用于开发阶段的调试
        Environment.isDevEnv();
    }

    /**
     * 执行修改密码命令的主要业务逻辑
     * 
     * 验证请求参数完整性，对密码进行MD5加密处理，然后调用修改员工密码方法
     * 该方法包含密码修改的核心流程控制
     * 
     * @param event 命令事件对象，包含事件触发信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 包含用户ID、旧密码和新密码的请求JSON对象
     * @throws CmdException 当命令执行出现异常时抛出，如参数缺失或业务逻辑错误
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含userId、oldPwd和newPwd字段
        // 如果缺少任一字段，Assert会抛出异常中断执行
        Assert.jsonObjectHaveKey(reqJson, "userId", "请求参数中未包含userId 节点，请确认");
        Assert.jsonObjectHaveKey(reqJson, "oldPwd", "请求参数中未包含oldPwd 节点，请确认");
        Assert.jsonObjectHaveKey(reqJson, "newPwd", "请求参数中未包含newPwd 节点，请确认");

        // 对原始密码和新密码进行MD5加密处理，确保密码在传输和存储中的安全性
        reqJson.put("oldPwd", AuthenticationFactory.passwdMd5(reqJson.getString("oldPwd")));
        reqJson.put("newPwd", AuthenticationFactory.passwdMd5(reqJson.getString("newPwd")));

        // 调用修改员工密码方法，执行具体的密码更新逻辑
        modifyStaff(reqJson);
    }
    
    /**
     * 修改员工密码的核心方法
     * 
     * 构建员工信息对象，调用服务层更新用户密码
     * 该方法负责将业务逻辑转换为数据层操作
     * 
     * @param paramObj 包含用户ID、加密后的旧密码和新密码的参数对象
     * @throws CmdException 当密码修改失败时抛出异常，通常是由于数据库操作失败
     */
    public void modifyStaff(JSONObject paramObj) {
        // 构建员工信息JSON对象，包含更新密码所需的所有信息
        JSONObject staffInfo = builderStaffInfo(paramObj);
        // 将JSON对象转换为UserPo对象，便于服务层处理
        UserPo userPo = BeanConvertUtil.covertBean(staffInfo, UserPo.class);
        // 调用用户服务更新用户信息，返回影响的行数
        int flag = userV1InnerServiceSMOImpl.updateUser(userPo);
        // 检查更新结果，如果影响行数小于1则抛出异常，表示更新失败
        if(flag <1){
            throw new CmdException("修改密码失败");
        }
    }

    /**
     * 构建员工信息JSON对象
     * 
     * 根据用户ID查询用户信息，验证原始密码是否正确，然后构建包含新密码的用户信息对象
     * 该方法包含密码验证和用户信息构建的核心逻辑
     * 
     * @param paramObj 包含用户ID、加密后的旧密码和新密码的参数对象
     * @return JSONObject 构建好的包含更新后密码的用户信息对象
     * @throws IllegalArgumentException 当原始密码不匹配时抛出异常
     * @throws CmdException 当查询到多条用户记录时抛出异常
     */
    private JSONObject builderStaffInfo(JSONObject paramObj) {
        // 创建用户查询条件对象，设置查询条件
        UserDto userDto = new UserDto();
        userDto.setStatusCd("0"); // 设置状态为有效状态，只查询有效用户
        userDto.setUserId(paramObj.getString("userId")); // 设置要查询的用户ID
        
        // 查询用户信息（包含密码），获取用户的完整信息用于密码验证
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUserHasPwd(userDto);

        // 验证查询结果，确保只查询到一条用户记录，避免数据不一致
        Assert.listOnlyOne(userDtos, "数据错误查询到多条用户信息或单条");

        // 将查询到的用户DTO对象转换为JSON对象，便于后续处理
        JSONObject userInfo = JSONObject.parseObject(JSONObject.toJSONString(userDtos.get(0)));

        // 验证原始密码是否正确，比较加密后的输入密码与数据库中的密码
        if (!paramObj.getString("oldPwd").equals(userDtos.get(0).getPassword())) {
            throw new IllegalArgumentException("原始密码错误");
        }
        
        // 将参数对象中的内容合并到用户信息中，准备更新操作
        userInfo.putAll(paramObj);
        // 更新密码字段为新密码，完成密码的更新准备
        userInfo.put("password", paramObj.getString("newPwd"));

        return userInfo;
    }
}