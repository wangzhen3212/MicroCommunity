package com.java110.acct.cmd.integral;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.MallDataDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.job.IMallInnerServiceSMO;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;


/**
 * 查询用户积分命令类
 * <p>
 * 积分在商城系统管理，所以这里需要调用商城系统接口查询
 * 该类负责处理移动端用户积分查询请求，通过调用商城系统接口获取用户积分信息
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "integral.queryAppUserIntegral")
public class QueryAppUserIntegralCmd extends Cmd {

    /**
     * 用户服务接口，用于查询用户信息
     */
    @Autowired
    IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 商城服务接口，用于调用商城系统功能
     */
    @Autowired
    private IMallInnerServiceSMO mallInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * <p>
     * 验证请求参数的合法性，由于当前业务逻辑简单，该方法为空实现
     * 如有复杂验证需求，可在此方法中添加具体验证逻辑
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 当前业务场景下无需特殊参数验证，保持空实现
        // 如有需要可在此添加参数校验逻辑，如必填字段检查、格式验证等
    }

    /**
     * 命令执行方法
     * <p>
     * 主要业务流程：
     * 1. 从上下文中获取当前用户ID
     * 2. 根据用户ID查询用户详细信息
     * 3. 验证用户存在且唯一
     * 4. 调用商城系统接口查询用户积分
     * 5. 设置响应结果
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

        // 构建用户查询条件对象
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        // 调用用户服务查询用户信息
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证查询结果：必须存在且仅有一个用户记录
        Assert.listOnlyOne(userDtos, "用户不存在");

        // 将用户手机号作为查询条件添加到请求参数中
        // 商城系统需要通过手机号来查询对应的积分信息
        reqJson.put("link", userDtos.get(0).getTel());
        
        // 构建商城数据请求对象，指定具体的业务处理实现类
        MallDataDto mallDataDto = new MallDataDto("queryAppUserIntegralBmoImpl", reqJson);
        // 调用商城服务接口查询用户积分信息
        ResultVo resultVo = mallInnerServiceSMOImpl.postMallData(mallDataDto);
        
        // 将商城系统返回的积分查询结果设置到响应中
        context.setResponseEntity(ResultVo.createResponseEntity(resultVo));
    }
}