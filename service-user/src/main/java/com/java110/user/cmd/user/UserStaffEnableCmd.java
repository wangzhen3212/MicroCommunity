/**
 * 用户员工启用命令类
 * 
 * 该命令类负责处理用户员工启用操作，通过接收包含用户ID的请求报文，
 * 将指定用户的状态更新为启用状态。主要用于管理系统中的用户账号启用功能。
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
 * 用户员工启用命令实现类
 * 服务编码：user.staff.enable
 */
@Java110Cmd(serviceCode = "user.staff.enable")
public class UserStaffEnableCmd extends Cmd {
    
    /**
     * 用户服务接口，用于操作用户数据
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中是否包含必要的userId参数，确保后续操作能够正常进行
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求JSON对象，包含客户端传递的参数
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含userId节点
        Assert.jsonObjectHaveKey(reqJson, "userId", "当前请求报文中未包含userId节点");
    }

    /**
     * 命令执行方法
     * 
     * 将请求参数转换为用户对象，并调用用户服务更新用户状态为启用状态
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求JSON对象，包含客户端传递的参数
     * @throws CmdException 当启用用户操作失败时抛出命令异常
     * @throws ParseException 当参数解析异常时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为UserPo对象，便于后续数据库操作
        UserPo userPo = BeanConvertUtil.covertBean(reqJson, UserPo.class);
        
        // 调用用户服务更新用户信息，返回受影响的行数
        int flag = userV1InnerServiceSMOImpl.updateUser(userPo);

        // 检查更新操作是否成功，如果受影响行数小于1，说明启用操作失败
        if (flag < 1) {
            throw new CmdException("启用用户失败");
        }
    }
}