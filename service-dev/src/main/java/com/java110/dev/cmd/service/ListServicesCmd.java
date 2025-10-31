package com.java110.dev.cmd.service;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.service.ServiceDto;
import com.java110.intf.community.IServiceInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.service.ApiServiceDataVo;
import com.java110.vo.api.service.ApiServiceVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务列表查询命令类
 * 处理服务列表查询请求，返回分页的服务信息
 * 
 * @Java110Cmd 注解定义服务代码为 "service.listServices"
 */
@Java110Cmd(serviceCode = "service.listServices")
public class ListServicesCmd extends Cmd {

    /**
     * 服务内部服务接口，用于查询服务数据
     */
    @Autowired
    private IServiceInnerServiceSMO serviceInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数，主要验证分页信息
     *
     * @param event   Cmd事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 查询服务列表并返回分页结果
     *
     * @param event   Cmd事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当命令执行过程中出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为ServiceDto对象
        ServiceDto serviceDto = BeanConvertUtil.covertBean(reqJson, ServiceDto.class);

        // 查询服务总数
        int count = serviceInnerServiceSMOImpl.queryServicesCount(serviceDto);

        // 服务数据列表
        List<ApiServiceDataVo> services = null;

        // 如果有数据，则查询服务列表；否则返回空列表
        if (count > 0) {
            // 查询服务列表并转换为ApiServiceDataVo对象列表
            services = BeanConvertUtil.covertBeanList(
                serviceInnerServiceSMOImpl.queryServices(serviceDto), 
                ApiServiceDataVo.class
            );
        } else {
            // 没有数据时返回空列表
            services = new ArrayList<>();
        }

        // 创建API服务响应对象
        ApiServiceVo apiServiceVo = new ApiServiceVo();

        // 设置总记录数
        apiServiceVo.setTotal(count);
        // 计算总页数：总记录数 / 每页记录数，向上取整
        apiServiceVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置服务数据列表
        apiServiceVo.setServices(services);

        // 创建HTTP响应实体，将结果转换为JSON字符串
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiServiceVo), 
            HttpStatus.OK
        );

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}