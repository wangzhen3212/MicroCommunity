/**
 * 资源库类型查询命令类
 * 
 * 该类用于处理资源库类型列表查询请求，支持分页查询和条件筛选功能
 * 通过注解@Java110Cmd声明服务代码为"resourceStoreType.listResourceStoreTypes"
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.resource.ResourceStoreTypeDto;
import com.java110.intf.store.IResourceStoreTypeInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

@Java110Cmd(serviceCode = "resourceStoreType.listResourceStoreTypes")
public class ListResourceStoreTypesCmd extends Cmd {

    /**
     * 资源库类型内部服务接口
     */
    @Autowired
    private IResourceStoreTypeInnerServiceSMO resourceStoreTypeInnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证请求参数，确保必要的参数存在，并设置分页信息
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头信息和设置响应
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 如果请求JSON中不包含storeId，则从请求头中获取并设置
        if (!reqJson.containsKey("storeId")) {
            String storeId = context.getReqHeaders().get("store-id");
            reqJson.put("storeId", storeId);
        }
        // 调用父类的分页信息验证方法
        super.validatePageInfo(reqJson);
    }

    /**
     * 执行命令的主方法
     * 
     * 处理资源库类型查询业务逻辑，包括参数转换、数据查询和响应构建
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中出现错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 处理标志位逻辑：当flag为0时，将rstId作为parentId，并清空rstId和name
        if (reqJson.containsKey("flag") && reqJson.getString("flag").equals("0")) {
            reqJson.put("parentId", reqJson.getString("rstId"));
            reqJson.put("rstId", null);
            reqJson.put("name", null);
        }
        
        // 如果存在rsId参数且不为空，将其赋值给rstId
        if (reqJson.containsKey("rsId") && !StringUtil.isEmpty(reqJson.getString("rsId"))) {
            reqJson.put("rstId", reqJson.getString("rsId"));
        }
        
        // 如果存在rstName参数且不为空，将其赋值给name
        if (reqJson.containsKey("rstName") && !StringUtil.isEmpty(reqJson.getString("rstName"))) {
            reqJson.put("name", reqJson.getString("rstName"));
        }
        
        // 将请求JSON对象转换为ResourceStoreTypeDto对象
        ResourceStoreTypeDto resourceStoreTypeDto = BeanConvertUtil.covertBean(reqJson, ResourceStoreTypeDto.class);

        // 查询资源库类型总数
        int count = resourceStoreTypeInnerServiceSMOImpl.queryResourceStoreTypesCount(resourceStoreTypeDto);

        List<ResourceStoreTypeDto> resourceStoreTypeDtos = null;

        // 根据查询结果数量决定返回的数据
        if (count > 0) {
            // 查询数量大于0时，查询具体的资源库类型列表
            resourceStoreTypeDtos = resourceStoreTypeInnerServiceSMOImpl.queryResourceStoreTypes(resourceStoreTypeDto);
        } else {
            // 查询数量为0时，返回空列表
            resourceStoreTypeDtos = new ArrayList<>();
        }

        // 构建结果对象，计算总页数
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, resourceStoreTypeDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        context.setResponseEntity(responseEntity);
    }
}