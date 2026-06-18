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
 * 删除映射关系命令类
 * 负责处理映射关系的删除操作
 * 
 * @Java110Cmd 注解声明这是一个命令类，serviceCode指定服务编码为"mapping.deleteMapping"
 */
@Java110Cmd(serviceCode = "mapping.deleteMapping")
public class DeleteMappingCmd extends Cmd {
    
    /**
     * 映射关系内部服务接口，用于执行实际的删除操作
     */
    @Autowired
    private IMappingInnerServiceSMO mappingInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否满足删除操作的要求
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含id字段，用于标识要删除的映射记录
        Assert.hasKeyAndValue(reqJson, "id", "编码ID不能为空");
    }

    /**
     * 执行删除映射关系的核心命令方法
     * 将请求参数转换为映射对象并调用服务层执行删除操作
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当删除操作失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON数据转换为MappingDto对象
        MappingDto mappingDto = BeanConvertUtil.covertBean(reqJson, MappingDto.class);

        // 调用映射服务接口执行删除操作，返回受影响的行数
        int count = mappingInnerServiceSMOImpl.deleteMapping(mappingDto);

        // 检查删除操作是否成功执行（至少影响一行数据）
        if (count < 1) {
            // 如果删除失败，抛出监听器执行异常
            throw new ListenerExecuteException(ResponseConstant.RESULT_CODE_ERROR, "编辑数据失败");
        }

        // 创建成功的HTTP响应，返回空内容和200状态码
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("", HttpStatus.OK);

        // 将响应设置到上下文中，返回给调用方
        context.setResponseEntity(responseEntity);
    }
}