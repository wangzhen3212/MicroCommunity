package com.java110.dev.cmd.serviceImpl;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.service.ServiceBusinessDto;
import com.java110.intf.community.IServiceBusinessInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 保存服务实现命令类
 * 用于处理服务实现信息的保存操作
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码
 */
@Java110Cmd(serviceCode = "serviceImpl.saveServiceImpl")
public class SaveServiceImplCmd extends Cmd {

    /**
     * 服务业务内部服务接口
     * 用于操作服务业务相关的数据
     */
    @Autowired
    private IServiceBusinessInnerServiceSMO serviceBusinessInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "businessTypeCd", "必填，请填写业务类型");
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写业务名称");
        Assert.hasKeyAndValue(reqJson, "invokeType", "必填，请填写调用类型");
        Assert.hasKeyAndValue(reqJson, "url", "必填，请填写调用地址，为mapping 表中domain为DOMAIN.COMMON映射key");
        Assert.hasKeyAndValue(reqJson, "timeout", "必填，请填写超时时间");
        Assert.hasKeyAndValue(reqJson, "retryCount", "必填，请填写重试次数");
    }

    /**
     * 执行命令方法
     * 处理保存服务实现的业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务处理失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity<String> responseEntity = null;

        // 将请求JSON转换为服务业务数据传输对象
        ServiceBusinessDto serviceImplDto = BeanConvertUtil.covertBean(reqJson, ServiceBusinessDto.class);

        // 调用服务保存服务业务信息
        int saveFlag = serviceBusinessInnerServiceSMOImpl.saveServiceBusiness(serviceImplDto);

        // 根据保存结果设置响应实体
        // 保存成功返回HTTP 200状态码和"成功"消息
        // 保存失败返回HTTP 400状态码和"失败"消息
        responseEntity = new ResponseEntity<String>(
            saveFlag > 0 ? "成功" : "失败", 
            saveFlag > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}