/**
 * 用户员工禁用命令类
 * 
 * 该命令类负责处理用户员工禁用操作，通过接收前端请求参数，验证必要字段后调用服务层进行用户状态更新
 * 主要用于管理系统中的员工账户禁用功能
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.user.UserPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

/**
 * 用户员工禁用命令实现类
 * 服务码：user.staff.disable
 * 负责处理用户禁用相关的业务逻辑
 */
@Java110Cmd(serviceCode = "user.staff.disable")
public class UserStaffDisableCmd extends Cmd {
    
    /**
     * 用户服务接口，用于操作用户数据
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中是否包含必要的userId参数
     * 如果缺少必要参数，会抛出CmdException异常
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含userId节点
        Assert.jsonObjectHaveKey(reqJson, "userId", "当前请求报文中未包含userId节点");
    }

    /**
     * 命令执行方法
     * 
     * 将请求参数转换为用户对象，调用服务层更新用户状态
     * 主要用于禁用用户账户操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当用户禁用操作失败时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为UserPo对象
        UserPo userPo = BeanConvertUtil.covertBean(reqJson, UserPo.class);

        // 调用用户服务接口更新用户信息
        int flag = userV1InnerServiceSMOImpl.updateUser(userPo);

        // 检查更新操作是否成功，flag小于1表示更新失败
        if (flag < 1) {
            throw new CmdException("禁用用户失败");
        }
    }
}