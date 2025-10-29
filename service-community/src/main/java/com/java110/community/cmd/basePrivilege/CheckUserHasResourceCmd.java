/**
 * 检查用户是否拥有特定资源权限的命令类
 * 
 * 该类继承自Cmd基类，用于处理用户权限验证请求。
 * 通过调用菜单服务检查指定用户是否具有访问特定资源的权限，
 * 并返回权限验证结果。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.community.cmd.basePrivilege;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.log.LoggerFactory;
import com.java110.dto.privilege.BasePrivilegeDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@Java110Cmd(serviceCode = "basePrivilege.CheckUserHasResourceListener")
public class CheckUserHasResourceCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(CheckUserHasResourceCmd.class);

    /**
     * 菜单内部服务接口，用于权限验证
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证传入的请求参数是否合法。
     * 当前实现为空，可根据具体业务需求添加参数验证逻辑。
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 参数验证逻辑可在此处实现
        // 当前为空实现，可根据需要添加具体的参数校验规则
    }

    /**
     * 执行权限检查命令的主要业务逻辑
     * 
     * 该方法接收用户ID和资源标识，通过菜单服务检查用户是否拥有该资源的访问权限，
     * 并将检查结果封装成HTTP响应返回。
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象，包含userId和resource字段
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 记录请求信息，便于调试和问题排查
        logger.debug("请求信息：{}", reqJson);
        
        // 初始化响应实体
        ResponseEntity<String> responseEntity = null;
        
        // 创建权限验证数据传输对象
        BasePrivilegeDto basePrivilegeDto = new BasePrivilegeDto();
        // 从请求中获取资源标识并设置到DTO
        basePrivilegeDto.setResource(reqJson.getString("resource"));
        // 从请求中获取用户ID并设置到DTO
        basePrivilegeDto.setUserId(reqJson.getString("userId"));
        
        // 调用菜单服务检查用户是否拥有指定资源的权限
        List<Map> privileges = menuInnerServiceSMOImpl.checkUserHasResource(basePrivilegeDto);

        // 创建响应数据对象
        JSONObject data = new JSONObject();
        // 将权限检查结果放入响应数据
        data.put("privileges", privileges);
        
        // 构建成功的HTTP响应
        responseEntity = new ResponseEntity<String>(data.toJSONString(), HttpStatus.OK);
        // 将响应设置到上下文，供后续处理使用
        context.setResponseEntity(responseEntity);
    }
}