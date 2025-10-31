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
 * 保存映射配置命令类
 * 处理映射关系的新增操作
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码为"mapping.saveMapping"
 */
@Java110Cmd(serviceCode = "mapping.saveMapping")
public class SaveMappingCmd extends Cmd {
    
    /**
     * 映射关系内部服务接口
     * 用于执行实际的映射数据保存操作
     */
    @Autowired
    private IMappingInnerServiceSMO mappingInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整和有效
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填参数：域
        Assert.hasKeyAndValue(reqJson, "domain", "必填，请填写域");
        // 验证必填参数：名称
        Assert.hasKeyAndValue(reqJson, "name", "必填，请填写名称");
        // 验证必填参数：键
        Assert.hasKeyAndValue(reqJson, "key", "必填，请填写键");
        // 验证必填参数：值
        Assert.hasKeyAndValue(reqJson, "value", "必填，请填写值");
    }

    /**
     * 执行命令方法
     * 处理映射配置的保存逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当保存操作失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为MappingDto对象
        MappingDto mappingDto = BeanConvertUtil.covertBean(reqJson, MappingDto.class);

        // 调用服务保存映射配置，返回影响的行数
        int count = mappingInnerServiceSMOImpl.saveMapping(mappingDto);

        // 检查保存结果，如果影响行数小于1，说明保存失败
        if (count < 1) {
            // 抛出监听器执行异常，返回错误结果码和错误信息
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "保存数据失败");
        }

        // 创建成功的HTTP响应实体，返回空内容和200状态码
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);

        // 将响应实体设置到上下文中，供后续处理使用
        context.setResponseEntity(responseEntity);
    }
}