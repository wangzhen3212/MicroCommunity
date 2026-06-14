/**
 * 生成健康代码命令类
 * 
 * 该类负责处理生成健康代码的请求，通过调用用户服务和商城服务，
 * 为用户生成唯一的健康代码并返回给前端
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
package com.java110.user.cmd.login;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.user.UserDto;
import com.java110.intf.job.IMallInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "login.generatorHcCode")
public class GeneratorHcCodeCmd extends Cmd {

    /**
     * 用户内部服务接口，用于查询用户信息
     */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 商城内部服务接口，用于生成健康代码
     */
    @Autowired
    private IMallInnerServiceSMO mallInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，当前实现为空方法，
     * 可根据业务需求添加具体的参数验证逻辑
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 当前方法为空实现，可根据业务需求添加参数验证逻辑
        // 例如：验证必填参数、参数格式、权限校验等
    }

    /**
     * 执行生成健康代码命令
     * 
     * 该方法主要流程：
     * 1. 从上下文中获取当前用户ID
     * 2. 查询用户详细信息
     * 3. 验证用户是否存在
     * 4. 调用商城服务生成健康代码
     * 5. 构建响应结果并返回
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从命令上下文中获取当前登录用户的ID
        String userId = CmdContextUtils.getUserId(context);
        
        // 创建用户查询对象并设置用户ID
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        
        // 调用用户服务查询用户信息
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        // 验证用户是否存在，如果不存在则抛出异常
        if (ListUtil.isNull(userDtos)) {
            throw new IllegalArgumentException("用户不存在");
        }

        // 调用商城服务生成健康代码，传入第一个用户信息
        // 注意：这里假设用户ID是唯一的，所以取列表中的第一个用户
        String hcCode = mallInnerServiceSMOImpl.generatorMallCode(userDtos.get(0));

        // 以下为注释掉的旧实现方式，使用UUID生成代码并缓存用户信息
        // String hcCode = PREFIX_CODE + GenerateCodeFactory.getUUID();
        // CommonCache.setValue(hcCode, JSONObject.toJSONString(userDtos.get(0)), CommonCache.defaultExpireTime);
        
        // 创建响应参数对象
        JSONObject paramOut = new JSONObject();
        // 将生成的健康代码放入响应参数中
        paramOut.put("hcCode", hcCode);
        
        // 设置响应实体，返回成功结果和健康代码
        context.setResponseEntity(ResultVo.createResponseEntity(paramOut));
    }
}