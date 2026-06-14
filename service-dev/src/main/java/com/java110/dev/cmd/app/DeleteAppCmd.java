package com.java110.dev.cmd.app;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.app.AppDto;
import com.java110.intf.community.IAppInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.constant.StatusConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 删除应用命令类
 * 
 * <p>该类负责处理删除应用的业务逻辑，通过注解 {@code @Java110Cmd} 指定服务编码为 "app.deleteApp"，
 * 当接收到对应的服务请求时，会执行该命令类中的验证和执行方法。</p>
 * 
 * <p>主要功能包括：</p>
 * <ul>
 *   <li>验证删除应用请求参数的合法性</li>
 *   <li>执行应用的逻辑删除操作（将状态设置为无效）</li>
 *   <li>处理删除结果并返回相应的响应</li>
 * </ul>
 * 
 * @author Java110
 * @version 1.0
 * @see Cmd
 * @see IAppInnerServiceSMO
 * @since 1.0
 */
@Java110Cmd(serviceCode = "app.deleteApp")
public class DeleteAppCmd extends Cmd {

    /**
     * 应用内部服务接口
     * 
     * <p>用于执行应用相关的数据库操作，包括查询、新增、修改和删除等。</p>
     * <p>通过Spring的依赖注入机制自动装配该服务实例。</p>
     */
    @Autowired
    private IAppInnerServiceSMO appInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * <p>在执行业务逻辑前验证请求参数的合法性，确保必要的参数存在且有效。</p>
     * <p>该方法会检查请求JSON中是否包含必需的 {@code appId} 参数。</p>
     *
     * @param event 命令事件对象，包含事件相关的信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求、响应数据
     * @param reqJson 请求参数JSON对象，包含客户端传递的参数
     * @throws CmdException 当参数验证失败时抛出命令异常，包含错误信息
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) 
            throws CmdException {
        // 验证请求参数中必须包含appId，且不能为空
        // 如果appId不存在或为空，会抛出CmdException异常
        Assert.hasKeyAndValue(reqJson, "appId", "应用Id不能为空");
    }

    /**
     * 执行删除应用命令
     * 
     * <p>该方法执行具体的删除应用业务逻辑：</p>
     * <ol>
     *   <li>将请求参数转换为AppDto对象</li>
     *   <li>设置应用状态为无效状态（逻辑删除）</li>
     *   <li>调用服务层执行删除操作</li>
     *   <li>检查删除操作结果并处理异常情况</li>
     *   <li>设置成功的HTTP响应</li>
     * </ol>
     * 
     * <p>注意：此处采用的是逻辑删除而非物理删除，通过修改状态字段来实现。</p>
     *
     * @param event 命令事件对象，包含事件相关的信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求、响应数据
     * @param reqJson 请求参数JSON对象，包含客户端传递的参数
     * @throws CmdException 当删除操作失败时抛出命令异常，包含错误信息
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) 
            throws CmdException {
        // 将请求参数转换为AppDto对象，便于后续操作
        AppDto appDto = BeanConvertUtil.covertBean(reqJson, AppDto.class);
        
        // 设置应用状态为无效状态，实现逻辑删除
        // 使用状态常量 STATUS_CD_INVALID 表示无效状态
        appDto.setStatusCd(StatusConstant.STATUS_CD_INVALID);
        
        // 调用服务层执行删除操作，返回受影响的行数
        int count = appInnerServiceSMOImpl.deleteApp(appDto);
        
        // 检查删除操作是否成功
        // 如果受影响行数小于1，说明删除操作失败
        if (count < 1) {
            // 如果删除失败，抛出监听器执行异常，包含错误码和错误信息
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "删除数据失败");
        }
        
        // 设置成功的HTTP响应
        // 返回空的响应体和HTTP 200状态码
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}