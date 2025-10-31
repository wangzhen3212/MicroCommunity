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
 * 删除服务实现命令类
 * 负责处理服务实现的删除操作
 * 
 * @Java110Cmd 注解定义服务代码为"serviceImpl.deleteServiceImpl"
 */
@Java110Cmd(serviceCode = "serviceImpl.deleteServiceImpl")
public class DeleteServiceImplCmd extends Cmd {

    /**
     * 服务业务内部服务接口
     * 用于执行实际的删除操作
     */
    @Autowired
    private IServiceBusinessInnerServiceSMO serviceBusinessInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否包含必要的字段
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求JSON中必须包含serviceBusinessId字段
        Assert.hasKeyAndValue(reqJson, "serviceBusinessId", "服务实现ID不能为空");
    }

    /**
     * 执行命令方法
     * 处理服务实现的删除逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当执行过程中出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 响应实体，用于返回操作结果
        ResponseEntity<String> responseEntity = null;

        // 将请求JSON转换为ServiceBusinessDto对象
        ServiceBusinessDto serviceImplDto = BeanConvertUtil.covertBean(reqJson, ServiceBusinessDto.class);

        // 调用服务删除方法，返回删除结果
        int saveFlag = serviceBusinessInnerServiceSMOImpl.deleteServiceBusiness(serviceImplDto);

        // 根据删除结果设置响应实体
        // 删除成功返回HTTP 200状态码和"成功"消息
        // 删除失败返回HTTP 400状态码和"失败"消息
        responseEntity = new ResponseEntity<String>(
            saveFlag > 0 ? "成功" : "失败", 
            saveFlag > 0 ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}