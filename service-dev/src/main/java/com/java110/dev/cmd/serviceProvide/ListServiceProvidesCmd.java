package com.java110.dev.cmd.serviceProvide;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.service.ServiceProvideDto;
import com.java110.intf.community.IServiceInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.serviceProvide.ApiServiceProvideDataVo;
import com.java110.vo.api.serviceProvide.ApiServiceProvideVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询服务提供列表命令类
 * 负责处理服务提供列表查询请求
 * 
 * @Java110Cmd 注解声明这是一个Java110框架的命令类，serviceCode指定服务编码
 */
@Java110Cmd(serviceCode = "serviceProvide.listServiceProvides")
public class ListServiceProvidesCmd extends Cmd {

    /**
     * 服务内部服务接口，用于查询服务提供相关数据
     */
    @Autowired
    private IServiceInnerServiceSMO serviceInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法，验证分页参数
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 处理服务提供列表查询业务逻辑
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为ServiceProvideDto对象
        ServiceProvideDto serviceProvideDto = BeanConvertUtil.covertBean(reqJson, ServiceProvideDto.class);

        // 查询服务提供记录总数
        int count = serviceInnerServiceSMOImpl.queryServiceProvidesCount(serviceProvideDto);

        // 声明服务提供数据列表
        List<ApiServiceProvideDataVo> serviceProvides = null;

        // 根据查询结果数量处理数据
        if (count > 0) {
            // 如果有数据，查询服务提供列表并转换为VO对象
            serviceProvides = BeanConvertUtil.covertBeanList(
                serviceInnerServiceSMOImpl.queryServiceProvides(serviceProvideDto), 
                ApiServiceProvideDataVo.class
            );
        } else {
            // 如果没有数据，返回空列表
            serviceProvides = new ArrayList<>();
        }

        // 创建API响应VO对象
        ApiServiceProvideVo apiServiceProvideVo = new ApiServiceProvideVo();

        // 设置总记录数
        apiServiceProvideVo.setTotal(count);
        // 计算总页数：总记录数 / 每页记录数，向上取整
        apiServiceProvideVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置服务提供数据列表
        apiServiceProvideVo.setServiceProvides(serviceProvides);

        // 创建HTTP响应实体，将VO对象转换为JSON字符串
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiServiceProvideVo), 
            HttpStatus.OK
        );

        // 将响应实体设置到上下文中
        context.setResponseEntity(responseEntity);
    }
}