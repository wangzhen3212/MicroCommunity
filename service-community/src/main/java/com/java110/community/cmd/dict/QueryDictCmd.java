package com.java110.community.cmd.dict;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.dict.DictDto;
import com.java110.dto.dict.DictQueryDto;
import com.java110.intf.community.DictInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 字典查询命令处理类
 * 
 * 该类负责处理字典查询相关的业务逻辑，通过服务码"dict.queryDict"标识，
 * 接收前端请求参数，查询字典数据并返回查询结果
 * 
 * @author Java110
 * @version 1.0
 */
@Java110Cmd(serviceCode = "dict.queryDict")
public class QueryDictCmd extends Cmd {
    
    /**
     * 日志记录器
     */
    private final static Logger logger = LoggerFactory.getLogger(QueryDictCmd.class);

    /**
     * 字典内部服务接口
     */
    @Autowired
    private DictInnerServiceSMO dictInnerServiceSMO;

    /**
     * 请求参数验证方法
     * 
     * 该方法用于验证前端传入的请求参数是否合法，
     * 当前实现为空，可根据业务需求添加具体的验证逻辑
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 参数验证逻辑预留，可根据具体业务需求添加验证规则
        // 例如：检查必要参数是否存在、参数格式是否正确等
    }

    /**
     * 命令执行方法
     * 
     * 该方法负责执行字典查询的核心业务逻辑：
     * 1. 将请求参数转换为查询条件对象
     * 2. 调用字典服务查询字典数据
     * 3. 构建响应实体并设置到上下文中
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为字典查询条件对象
        DictQueryDto dictQueryDto = BeanConvertUtil.covertBean(reqJson, DictQueryDto.class);
        
        // 调用字典内部服务查询字典数据列表
        List<DictDto> dictDtos = this.dictInnerServiceSMO.queryDict(dictQueryDto);
        
        // 将查询结果转换为JSON字符串并构建响应实体
        String responseData = JSONObject.toJSONString(dictDtos);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(responseData, HttpStatus.OK);
        
        // 将响应实体设置到数据流上下文中，供后续处理使用
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }
}