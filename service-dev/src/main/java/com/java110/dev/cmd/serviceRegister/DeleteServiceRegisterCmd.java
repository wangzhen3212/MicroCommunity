package com.java110.dev.cmd.serviceRegister;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.service.RouteDto;
import com.java110.intf.community.IRouteInnerServiceSMO;
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
 * 删除服务注册命令类
 * 负责处理服务注册信息的删除操作
 * 
 * @Java110Cmd 注解定义服务代码为"serviceRegister.deleteServiceRegister"
 */
@Java110Cmd(serviceCode = "serviceRegister.deleteServiceRegister")
public class DeleteServiceRegisterCmd extends Cmd {

    // 注入路由内部服务接口
    @Autowired
    private IRouteInnerServiceSMO routeInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否合法
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含id字段
        Assert.hasKeyAndValue(reqJson, "id", "绑定ID不能为空");
    }

    /**
     * 执行删除服务注册命令
     * 将指定ID的服务注册状态设置为无效，实现逻辑删除
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当删除操作失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为RouteDto对象
        RouteDto routeDto = BeanConvertUtil.covertBean(reqJson, RouteDto.class);

        // 设置状态为无效状态，实现逻辑删除
        routeDto.setStatusCd(StatusConstant.STATUS_CD_INVALID);

        // 调用服务删除路由信息
        int count = routeInnerServiceSMOImpl.deleteRoute(routeDto);

        // 检查删除操作是否成功
        if (count < 1) {
            // 删除失败时抛出异常
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "删除数据失败");
        }

        // 创建成功响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);

        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }
}