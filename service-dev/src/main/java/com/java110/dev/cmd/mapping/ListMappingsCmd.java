package com.java110.dev.cmd.mapping;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.mapping.MappingDto;
import com.java110.intf.community.IMappingInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.mapping.ApiMappingDataVo;
import com.java110.vo.api.mapping.ApiMappingVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 映射列表查询命令类
 * 负责处理映射配置的列表查询请求
 * 
 * @Java110Cmd 注解声明这是一个Java110框架的命令类，serviceCode指定服务编码为"mapping.listMappings"
 */
@Java110Cmd(serviceCode = "mapping.listMappings")
public class ListMappingsCmd extends Cmd {

    /**
     * 映射服务接口，用于调用映射相关的业务逻辑
     */
    @Autowired
    private IMappingInnerServiceSMO mappingInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性
     *
     * @param event   命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法，验证分页参数（如page、row等）的有效性
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理映射列表查询的核心业务逻辑
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当业务处理过程中出现错误时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为MappingDto对象，用于查询条件
        MappingDto mappingDto = BeanConvertUtil.covertBean(reqJson, MappingDto.class);

        // 查询映射记录总数
        int count = mappingInnerServiceSMOImpl.queryMappingsCount(mappingDto);

        // 定义映射数据列表
        List<ApiMappingDataVo> mappings = null;

        // 根据查询结果数量判断是否需要查询详细数据
        if (count > 0) {
            // 查询映射列表数据，并将结果转换为ApiMappingDataVo对象列表
            mappings = BeanConvertUtil.covertBeanList(
                mappingInnerServiceSMOImpl.queryMappings(mappingDto), 
                ApiMappingDataVo.class
            );
        } else {
            // 如果没有数据，返回空列表
            mappings = new ArrayList<>();
        }

        // 创建API响应对象
        ApiMappingVo apiMappingVo = new ApiMappingVo();

        // 设置总记录数
        apiMappingVo.setTotal(count);
        // 计算总页数：总记录数/每页记录数，并向上取整
        apiMappingVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置映射数据列表
        apiMappingVo.setMappings(mappings);

        // 创建HTTP响应实体，将响应对象转换为JSON字符串，状态码为200 OK
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiMappingVo), 
            HttpStatus.OK
        );

        // 将响应实体设置到上下文中，供框架后续处理
        context.setResponseEntity(responseEntity);
    }
}