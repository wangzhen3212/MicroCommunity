/**
 * 权限检查命令类
 * 
 * 该类用于检查指定用户是否拥有特定权限标识的访问权限。
 * 通过注解@Java110Cmd声明服务代码为"check.user.hasPrivilege"，
 * 当接收到对应请求时，会执行权限验证逻辑。
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
import com.java110.dto.privilege.HasPrivilegeDto;
import com.java110.intf.community.IMenuInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Java110Cmd(serviceCode = "check.user.hasPrivilege")
public class HasPrivilegeCmd extends Cmd {

    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(HasPrivilegeCmd.class);

    /**
     * 菜单内部服务接口，用于权限相关的数据操作
     */
    @Autowired
    private IMenuInnerServiceSMO menuInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求报文中是否包含必要的参数节点，确保后续业务逻辑能够正常执行。
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含userId节点
        Assert.hasKeyAndValue(reqJson, "userId", "请求报文中未包含userId节点");
        // 验证请求报文中必须包含pId节点（权限标识）
        Assert.hasKeyAndValue(reqJson, "pId", "请求报文中未包含pId节点");
    }

    /**
     * 执行权限检查命令
     * 
     * 根据用户ID和权限标识检查用户是否拥有相应权限。
     * 如果用户没有权限，返回401未授权状态；如果有权限，返回200成功状态。
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity<String> responseEntity = null;

        // 创建权限检查数据传输对象
        HasPrivilegeDto hasPrivilegeDto = new HasPrivilegeDto();
        // 设置用户ID
        hasPrivilegeDto.setUserId(reqJson.getString("userId"));
        // 设置权限标识
        hasPrivilegeDto.setpId(reqJson.getString("pId"));
        
        // 调用菜单服务检查用户权限
        List<HasPrivilegeDto> privilegeDtos = menuInnerServiceSMOImpl.hasPrivilege(hasPrivilegeDto);

        // 检查权限结果：如果返回结果为空或结果列表大小为0，表示用户没有权限
        if (privilegeDtos == null || privilegeDtos.size() < 1) {
            // 设置未授权响应
            context.setResponseEntity(ResultVo.error("没有权限操作", HttpStatus.UNAUTHORIZED));
            return;
        }

        // 用户拥有权限，设置成功响应
        responseEntity = new ResponseEntity<String>("成功", HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }
}