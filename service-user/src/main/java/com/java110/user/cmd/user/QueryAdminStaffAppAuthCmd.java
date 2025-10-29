/**
 * 管理员员工应用认证查询命令类
 * 
 * 该类用于处理管理员查询员工应用认证信息的业务逻辑，包括验证管理员权限和查询员工认证状态
 * 主要功能包括验证管理员身份、查询员工信息、查询应用认证状态并返回统一的响应结果
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
import com.java110.dto.user.StaffAppAuthDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IStaffAppAuthInnerServiceSMO;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "user.queryAdminStaffAppAuth")
public class QueryAdminStaffAppAuthCmd extends Cmd {

    /**
     * 员工应用认证内部服务接口
     */
    @Autowired
    private IStaffAppAuthInnerServiceSMO staffAppAuthInnerServiceSMOImpl;

    /**
     * 用户内部服务接口
     */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 验证方法 - 验证请求参数和管理员权限
     * 
     * 该方法主要用于验证当前用户是否具有管理员权限，确保只有管理员可以执行后续操作
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现业务异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证当前用户是否具有管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行命令方法 - 处理管理员查询员工应用认证的核心业务逻辑
     * 
     * 该方法执行以下主要步骤：
     * 1. 转换请求参数为员工应用认证DTO对象
     * 2. 查询员工基本信息
     * 3. 查询员工应用认证状态
     * 4. 根据认证状态设置相应的返回数据
     * 5. 构建并返回响应结果
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现业务异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON转换为StaffAppAuthDto对象
        StaffAppAuthDto staffAppAuthDto = BeanConvertUtil.covertBean(reqJson, StaffAppAuthDto.class);
        // 设置门店ID
        staffAppAuthDto.setStoreId(reqJson.getString("propertyStoreId"));

        // 创建用户查询对象并设置员工ID
        UserDto userDto = new UserDto();
        userDto.setUserId(staffAppAuthDto.getStaffId());
        // 查询员工信息
        List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);

        // 验证员工信息是否存在且唯一
        Assert.listOnlyOne(userDtos, "用户不存在");
        // 设置员工姓名
        staffAppAuthDto.setStaffName(userDtos.get(0).getName());

        // 查询员工应用认证信息
        List<StaffAppAuthDto> staffAppAuthDtos = staffAppAuthInnerServiceSMOImpl.queryStaffAppAuths(staffAppAuthDto);

        // 根据认证状态设置返回数据
        if (ListUtil.isNull(staffAppAuthDtos)) {
            // 未认证状态的处理
            staffAppAuthDto.setStateName("未认证");
            staffAppAuthDto.setState("1001"); // 1001表示未认证状态
            staffAppAuthDto.setOpenId("-"); // 设置默认值
            staffAppAuthDto.setAppType("-"); // 设置默认值
            staffAppAuthDto.setOpenName("-"); // 设置默认值
        } else {
            // 已认证状态的处理
            staffAppAuthDto.setStateName("已认证");
            staffAppAuthDto.setState("2002"); // 2002表示已认证状态
            // 设置认证相关的详细信息
            staffAppAuthDto.setOpenId(staffAppAuthDtos.get(0).getOpenId());
            staffAppAuthDto.setAppType(staffAppAuthDtos.get(0).getAppType());
            staffAppAuthDto.setOpenName(staffAppAuthDtos.get(0).getOpenName());
            staffAppAuthDto.setCreateTime(staffAppAuthDtos.get(0).getCreateTime());
        }
        
        // 创建响应实体并设置到上下文中
        ResponseEntity responseEntity = ResultVo.createResponseEntity(staffAppAuthDto);
        context.setResponseEntity(responseEntity);
    }
}