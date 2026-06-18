/**
 * 系统用户列表查询命令类
 * 
 * 该类用于处理系统用户列表查询请求，支持分页查询管理员用户信息
 * 通过调用用户服务接口获取系统用户数据并返回给前端
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
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.user.ApiUserDataVo;
import com.java110.vo.api.user.ApiUserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "user.listSystemUsers")
public class ListSystemUsersCmd extends Cmd {

    /**
     * 用户服务接口，用于获取系统用户数据
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMO;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数的有效性，包括分页参数验证和管理员权限验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象，包含前端传递的查询条件
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数，确保page和row参数存在且有效
        super.validatePageInfo(reqJson);
        // 验证管理员权限，确保当前用户有权限查询系统用户
        super.validateAdmin(context);
    }

    /**
     * 执行系统用户列表查询命令
     * 
     * 根据查询条件获取系统用户列表，支持分页查询，返回格式化后的用户数据
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象，包含查询条件和分页参数
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为UserDto对象，用于查询条件
        UserDto userDto = BeanConvertUtil.covertBean(reqJson, UserDto.class);
        // 设置用户ID查询条件
        userDto.setUserId(reqJson.getString("systemUserId"));

        // 获取符合条件的系统用户总数
        int count = userV1InnerServiceSMO.getSystemUserCount(userDto);

        List<UserDto> userDtos = null;

        // 根据查询结果数量决定返回数据
        if (count > 0) {
            // 有数据时，获取具体的用户列表
            userDtos = userV1InnerServiceSMO.getSystemUsers(userDto);
        } else {
            // 无数据时返回空列表
            userDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        int totalPage = (int) Math.ceil((double) count / (double) reqJson.getInteger("row"));
        // 构建返回结果对象，包含分页信息和用户数据
        ResultVo resultVo = new ResultVo(totalPage, count, userDtos);

        // 创建HTTP响应实体，设置状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 将响应设置到上下文中，返回给前端
        context.setResponseEntity(responseEntity);
    }
}