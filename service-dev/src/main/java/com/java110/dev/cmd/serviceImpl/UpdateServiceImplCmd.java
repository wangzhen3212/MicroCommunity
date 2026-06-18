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
 * 更新服务实现命令类
 * 用于处理服务业务信息的更新操作
 * 
 * @Java110Cmd 注解定义服务编码，对应前端请求的服务标识
 */
@Java110Cmd(serviceCode = "serviceImpl.updateServiceImpl")
public class UpdateServiceImplCmd extends Cmd {

    /**
     * 服务业务内部服务接口
     * 用于调用服务业务相关的数据操作
     */
    @Autowired
    private IServiceBusinessInnerServiceSMO serviceBusinessInnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 对请求参数进行必填项校验，确保数据完整性
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数校验失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 校验服务业务ID不能为空
        Assert.hasKeyAndValue(reqJson, "serviceBusinessId", "必填，请填写应用ID");
        // 校验业务类型编码不能为空
        Assert.hasKeyAndValue(reqJson, "businessTypeCd", "必填，请填写业务类型");
        // 校验业务名称不能为空
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写业务名称");
        // 校验调用类型不能为空
        Assert.hasKeyAndValue(reqJson, "invokeType", "必填，请填写调用类型");
        // 校验调用地址不能为空，该地址对应mapping表中domain为DOMAIN.COMMON的映射key
        Assert.hasKeyAndValue(reqJson, "url", "必填，请填写调用地址，为mapping 表中domain为DOMAIN.COMMON映射key");
        // 校验超时时间不能为空
        Assert.hasKeyAndValue(reqJson, "timeout", "必填，请填写超时时间");
        // 校验重试次数不能为空
        Assert.hasKeyAndValue(reqJson, "retryCount", "必填，请填写重试次数");
    }

    /**
     * 命令执行方法
     * 执行服务业务信息的更新操作，并返回执行结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        ResponseEntity<String> responseEntity = null;

        // 将请求JSON数据转换为ServiceBusinessDto对象
        ServiceBusinessDto serviceImplDto = BeanConvertUtil.covertBean(reqJson, ServiceBusinessDto.class);

        // 调用服务更新接口，返回更新影响的行数
        int saveFlag = serviceBusinessInnerServiceSMOImpl.updateServiceBusiness(serviceImplDto);

        // 根据更新结果设置响应实体
        // 更新成功返回HTTP 200状态码和"成功"消息
        // 更新失败返回HTTP 400状态码和"失败"消息
        responseEntity = new ResponseEntity<String>(
            saveFlag > 0 ? "成功" : "失败", 
            saveFlag > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}