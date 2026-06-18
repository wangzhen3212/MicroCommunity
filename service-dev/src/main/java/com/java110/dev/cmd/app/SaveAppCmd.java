package com.java110.dev.cmd.app;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.app.AppDto;
import com.java110.intf.community.IAppInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 保存应用信息命令类
 * 处理应用信息的新增操作
 * 
 * @Java110Cmd 注解定义服务编码为 "app.saveApp"
 */
@Java110Cmd(serviceCode = "app.saveApp")
public class SaveAppCmd extends Cmd {

    // 注入应用服务接口
    @Autowired
    private IAppInnerServiceSMO appInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含应用名称
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写应用名称");
    }

    /**
     * 执行保存应用信息的核心业务逻辑
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务执行失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为AppDto对象
        AppDto appDto = BeanConvertUtil.covertBean(reqJson, AppDto.class);

        // 生成应用ID
        appDto.setAppId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_id));

        // 调用服务保存应用信息
        int count = appInnerServiceSMOImpl.saveApp(appDto);

        // 检查保存结果
        if (count < 1) {
            // 保存失败，抛出异常
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "保存数据失败");
        }

        // 构建成功响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);

        // 设置响应到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}