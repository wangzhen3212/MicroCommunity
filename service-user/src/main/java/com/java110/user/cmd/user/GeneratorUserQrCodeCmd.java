package com.java110.user.cmd.user;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.doc.annotation.*;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;


/**
 * 用户二维码生成命令类
 * 
 * 该类负责处理生成用户二维码的请求，通过调用用户服务生成用户的唯一二维码信息，
 * 并将生成的二维码返回给客户端。主要用于用户身份识别和快速访问。
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110CmdDoc(title = "生成用户二维码",
        description = "生成用户的唯一二维码信息",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/user.generatorUserQrCode",
        resource = "userDoc",
        author = "吴学文",
        serviceCode = "user.generatorUserQrCode",
        seq = 10
)


@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
                @Java110ParamDoc(name = "data", type = "String", length = 250, defaultValue = "二维码", remark = "二维码"),
        }
)

@Java110ExampleDoc(
        reqBody = "{}",
        resBody = "{'code':0,'msg':'成功','data':'sdsfsdf'}"
)

@Java110Cmd(serviceCode = "user.generatorUserQrCode")
public class GeneratorUserQrCodeCmd extends Cmd {

    /**
     * 用户服务接口，用于调用用户相关的业务逻辑
     */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证请求参数的合法性，目前暂未实现具体的验证逻辑，
     * 可根据业务需求添加参数校验规则。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 当前方法为空实现，可根据业务需求添加参数验证逻辑
        // 例如：验证必填参数、参数格式、参数范围等
    }

    /**
     * 执行生成用户二维码命令
     * 
     * 该方法从请求头中获取用户ID，调用用户服务生成对应的二维码，
     * 并将生成的二维码设置到响应中返回给客户端。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于设置响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现异常时抛出
     * @throws ParseException 当参数解析出现异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求头中获取用户ID
        String userId = context.getReqHeaders().get("user-id");
        
        // 调用用户服务生成用户二维码
        String qrCode = userV1InnerServiceSMOImpl.generatorUserIdQrCode(userId);
        
        // 将生成的二维码设置到响应实体中
        context.setResponseEntity(ResultVo.createResponseEntity(qrCode));
    }
}