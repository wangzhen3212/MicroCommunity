/**
 * 用户列表查询命令类
 * 
 * 该类负责处理用户列表查询请求，从数据库中获取用户信息并返回分页结果
 * 实现了Cmd接口，通过Spring框架进行依赖注入和管理
 * 服务码：user.listUsers
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
import com.java110.dto.user.UserDto;
import com.java110.intf.user.IUserInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.user.ApiUserDataVo;
import com.java110.vo.api.user.ApiUserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "user.listUsers")
public class ListUsersCmd extends Cmd {

    /**
     * 用户内部服务接口，用于操作用户相关数据
     */
    @Autowired
    private IUserInnerServiceSMO userInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 主要验证分页参数的有效性，确保请求中包含正确的分页信息
     * 
     * @param event 命令事件对象，包含请求的原始信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法，验证page和row等分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行用户列表查询命令
     * 
     * 根据请求参数查询用户列表，支持分页查询，返回用户数据及分页信息
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象，包含查询条件和分页信息
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为UserDto对象，用于查询条件
        UserDto userDto = BeanConvertUtil.covertBean(reqJson, UserDto.class);

        // 查询符合条件的用户总数
        int count = userInnerServiceSMOImpl.getUserCount(userDto);

        // 初始化用户列表变量
        List<ApiUserDataVo> users = null;

        // 根据查询结果判断是否有数据
        if (count > 0) {
            // 如果有数据，查询用户详细信息列表
            List<UserDto> userDtos = userInnerServiceSMOImpl.getUsers(userDto);
            // 将UserDto列表转换为ApiUserDataVo列表
            users = BeanConvertUtil.covertBeanList(userDtos, ApiUserDataVo.class);
        } else {
            // 如果没有数据，返回空列表
            users = new ArrayList<>();
        }

        // 创建API返回对象
        ApiUserVo apiOrgVo = new ApiUserVo();

        // 设置总记录数
        apiOrgVo.setTotal(count);
        // 计算总页数：总记录数 / 每页显示条数，向上取整
        apiOrgVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置用户数据列表
        apiOrgVo.setUsers(users);

        // 创建HTTP响应实体，将结果转换为JSON字符串
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiOrgVo), HttpStatus.OK);

        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}