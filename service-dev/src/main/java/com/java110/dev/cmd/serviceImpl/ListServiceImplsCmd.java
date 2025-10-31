package com.java110.dev.cmd.serviceImpl;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.service.ServiceBusinessDto;
import com.java110.intf.community.IServiceBusinessInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.api.serviceImpl.ApiServiceImplDataVo;
import com.java110.vo.api.serviceImpl.ApiServiceImplVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询服务实现列表命令类
 * 负责处理服务实现列表查询请求
 * 
 * @Java110Cmd 注解声明这是一个Java110命令，serviceCode指定服务编码
 */
@Java110Cmd(serviceCode = "serviceImpl.listServiceImpls")
public class ListServiceImplsCmd extends Cmd {

    /**
     * 服务业务内部服务接口，用于查询服务业务信息
     */
    @Autowired
    private IServiceBusinessInnerServiceSMO serviceBusinessInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 主要验证分页参数
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 调用父类方法验证分页信息
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令方法
     * 查询服务实现列表并返回结果
     * 
     * @param event 命令事件
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON数据
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为ServiceBusinessDto对象
        ServiceBusinessDto serviceImplDto = BeanConvertUtil.covertBean(reqJson, ServiceBusinessDto.class);

        // 查询服务业务总数
        int count = serviceBusinessInnerServiceSMOImpl.queryServiceBusinesssCount(serviceImplDto);

        // 服务实现数据列表
        List<ApiServiceImplDataVo> serviceImpls = null;

        // 如果有数据，则查询详细列表；否则返回空列表
        if (count > 0) {
            // 查询服务业务列表并转换为ApiServiceImplDataVo对象列表
            serviceImpls = BeanConvertUtil.covertBeanList(
                serviceBusinessInnerServiceSMOImpl.queryServiceBusinesss(serviceImplDto), 
                ApiServiceImplDataVo.class
            );
        } else {
            // 没有数据时返回空列表
            serviceImpls = new ArrayList<>();
        }

        // 创建返回VO对象
        ApiServiceImplVo apiServiceImplVo = new ApiServiceImplVo();

        // 设置总记录数
        apiServiceImplVo.setTotal(count);
        // 计算总页数：总记录数/每页记录数，向上取整
        apiServiceImplVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        // 设置服务实现数据列表
        apiServiceImplVo.setServiceImpls(serviceImpls);

        // 创建响应实体，将VO对象转换为JSON字符串
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(
            JSONObject.toJSONString(apiServiceImplVo), 
            HttpStatus.OK
        );

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}