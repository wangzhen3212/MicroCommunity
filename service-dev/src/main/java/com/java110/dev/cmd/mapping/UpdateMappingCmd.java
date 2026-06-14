package com.java110.dev.cmd.mapping;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.mapping.MappingDto;
import com.java110.intf.community.IMappingInnerServiceSMO;
import com.java110.utils.constant.ResponseConstant;
import com.java110.utils.exception.CmdException;
import com.java110.utils.exception.ListenerExecuteException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 更新映射配置命令类
 * 负责处理映射配置的更新操作
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码为"mapping.updateMapping"
 */
@Java110Cmd(serviceCode = "mapping.updateMapping")
public class UpdateMappingCmd extends Cmd {

    // 注入映射内部服务接口
    @Autowired
    private IMappingInnerServiceSMO mappingInnerServiceSMOImpl;

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
        // 验证必须包含id字段且不能为空
        Assert.hasKeyAndValue(reqJson, "id", "编码ID不能为空");
        // 验证必须包含domain字段且不能为空
        Assert.hasKeyAndValue(reqJson, "domain", "必填，请填写域");
        // 验证必须包含name字段且不能为空
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写名称");
        // 验证必须包含key字段且不能为空
        Assert.hasKeyAndValue(reqJson, "key", "必填，请填写键");
        // 验证必须包含value字段且不能为空
        Assert.hasKeyAndValue(reqJson, "value", "必填，请填写值");
    }

    /**
     * 执行命令方法
     * 处理映射配置的更新业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当业务执行失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 将请求JSON数据转换为MappingDto对象
        MappingDto mappingDto = BeanConvertUtil.covertBean(reqJson, MappingDto.class);

        // 调用服务层更新映射配置，返回影响的行数
        int count = mappingInnerServiceSMOImpl.updateMapping(mappingDto);

        // 检查更新是否成功，如果影响行数小于1，说明更新失败
        if (count < 1) {
            // 抛出监听器执行异常，返回错误码和错误信息
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "编辑数据失败");
        }

        // 创建成功的HTTP响应，返回空内容和200状态码
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);

        // 将响应设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}