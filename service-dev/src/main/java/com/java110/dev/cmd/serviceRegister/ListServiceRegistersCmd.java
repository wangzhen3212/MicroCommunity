package com.java110.dev.cmd.serviceRegister;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.service.RouteDto;
import com.java110.intf.community.IRouteInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.serviceRegister.ApiServiceRegisterDataVo;
import com.java110.vo.api.serviceRegister.ApiServiceRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务注册列表查询命令类
 * 用于查询服务注册信息的列表数据
 * 
 * @Java110Cmd 注解定义服务代码为 "serviceRegister.listServiceRegisters"
 */
@Java110Cmd(serviceCode = "serviceRegister.listServiceRegisters")
public class ListServiceRegistersCmd extends Cmd {

    // 注入路由内部服务接口
    @Autowired
    private IRouteInnerServiceSMO routeInnerServiceSMOIMpl;

    /**
     * 参数验证方法
     * 主要验证分页参数
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 命令执行方法
     * 查询服务注册列表数据并返回分页结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为RouteDto对象
        RouteDto routeDto = BeanConvertUtil.covertBean(reqJson, RouteDto.class);

        // 查询路由总数
        int count = routeInnerServiceSMOIMpl.queryRoutesCount(routeDto);

        // 服务注册数据列表
        List<ApiServiceRegisterDataVo> serviceRegisters = null;

        // 如果有数据，则查询详细列表并转换
        if (count > 0) {
            serviceRegisters = BeanConvertUtil.covertBeanList(
                routeInnerServiceSMOIMpl.queryRoutes(routeDto), 
                ApiServiceRegisterDataVo.class
            );
        } else {
            // 无数据时返回空列表
            serviceRegisters = new ArrayList<>();
        }

        // 创建返回VO对象
        ApiServiceRegisterVo apiServiceRegisterVo = new ApiServiceRegisterVo();

        // 设置总记录数
        apiServiceRegisterVo.setTotal(count);
        // 计算总页数：总记录数/每页记录数，向上取整
        apiServiceRegisterVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置服务注册数据列表
        apiServiceRegisterVo.setServiceRegisters(serviceRegisters);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiServiceRegisterVo), 
            HttpStatus.OK
        );

        // 设置响应到上下文
        context.setResponseEntity(responseEntity);
    }
}