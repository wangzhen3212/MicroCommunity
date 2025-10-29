/**
 * 重置系统用户密码命令类
 * 
 * 该类用于处理重置系统用户密码的请求，主要功能包括：
 * 1. 验证管理员权限和请求参数
 * 2. 根据用户ID查询用户信息
 * 3. 生成随机密码并更新到数据库
 * 4. 返回重置后的密码信息
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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.user.UserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "user.resetSystemUserPwd")
public class ResetSystemUserPwdCmd extends Cmd {
    
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
     * 验证请求参数和方法执行权限
     * 
     * 该方法执行以下验证：
     * 1. 检查当前环境是否为开发环境
     * 2. 验证当前用户是否具有管理员权限
     * 3. 验证请求参数中是否包含必要的staffId字段
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 检查当前环境是否为开发环境
        Environment.isDevEnv();
        // 验证当前用户是否具有管理员权限
        super.validateAdmin(context);
        // 验证请求参数中必须包含staffId字段
        Assert.hasKeyAndValue(reqJson,"staffId","未包含人员ID");
    }

    /**
     * 执行重置系统用户密码的核心业务逻辑
     * 
     * 该方法的主要流程：
     * 1. 根据用户ID查询用户信息
     * 2. 生成随机密码并进行MD5加密
     * 3. 更新用户密码到数据库
     * 4. 返回重置后的密码信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当重置密码失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 创建用户查询条件对象
        UserDto userDto = new UserDto();
        // 设置状态码为0（正常状态）
        userDto.setStatusCd("0");
        // 设置要查询的用户ID
        userDto.setUserId(reqJson.getString("staffId"));
        // 查询具有密码的用户信息
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUserHasPwd(userDto);

        // 验证查询结果：必须且只能查询到一条用户记录
        Assert.listOnlyOne(userDtos, "数据错误查询到多条用户信息或单条");

        // 将查询到的用户信息转换为JSON对象，便于后续操作
        JSONObject userInfo = JSONObject.parseObject(JSONObject.toJSONString(userDtos.get(0)));
        // 生成6位随机密码
        String pwd = GenerateCodeFactory.getRandomCode(6);
        // 对密码进行MD5加密并更新到用户信息中
        userInfo.put("password", AuthenticationFactory.passwdMd5(pwd));

        // 将JSON对象转换为UserPo对象，用于数据库更新操作
        UserPo userPo = BeanConvertUtil.covertBean(userInfo, UserPo.class);

        // 执行用户信息更新操作
        int flag = userV1InnerServiceSMOImpl.updateUser(userPo);
        // 检查更新是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("重置失败");
        }

        // 构建响应结果对象
        JSONObject paramOut = new JSONObject();
        // 返回重置后的明文密码（前端需要展示给用户）
        paramOut.put("pwd", pwd);
        // 设置操作成功状态码
        paramOut.put("code",0);
        // 设置操作成功消息
        paramOut.put("msg","成功");
        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<>(paramOut.toJSONString(), HttpStatus.OK);
        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}