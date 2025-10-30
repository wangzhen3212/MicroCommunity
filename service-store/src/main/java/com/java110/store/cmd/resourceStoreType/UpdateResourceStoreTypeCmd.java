/**
 * 资源库类型更新命令类
 * 
 * 该类负责处理资源库类型的更新操作，包括参数验证和业务逻辑执行。
 * 通过注解标识服务代码，与前端请求进行映射，实现资源库类型信息的修改功能。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.resourceStoreType;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.intf.store.IResourceStoreTypeV1InnerServiceSMO;
import com.java110.po.resource.ResourceStoreTypePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;

/**
 * 资源库类型更新命令实现类
 * 
 * 继承自Cmd基类，实现资源库类型信息的更新功能。
 * 通过服务码"resourceStoreType.updateResourceStoreType"与前端请求进行绑定。
 */
@Java110Cmd(serviceCode = "resourceStoreType.updateResourceStoreType")
public class UpdateResourceStoreTypeCmd extends Cmd {

    /**
     * 资源库类型服务接口
     * 
     * 通过Spring自动注入，提供资源库类型相关的数据操作服务
     */
    @Autowired
    private IResourceStoreTypeV1InnerServiceSMO resourceStoreTypeV1InnerServiceSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * 验证更新资源库类型所需的必要参数是否完整，包括：
     * 1. 资源库类型ID(rstId)
     * 2. 资源库类型名称(name)
     * 3. 店铺ID(storeId)
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求报文中必须包含资源库类型ID
        Assert.hasKeyAndValue(reqJson, "rstId", "请求报文中未包含rstId");
        // 验证请求报文中必须包含资源库类型名称
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        
        // 如果请求报文中没有店铺ID，则从请求头中获取并添加到请求JSON中
        if (!reqJson.containsKey("storeId")) {
            String storeId = context.getReqHeaders().get("store-id");
            reqJson.put("storeId", storeId);
        }
        
        // 验证请求报文中必须包含店铺ID
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含storeId");
    }

    /**
     * 命令执行方法
     * 
     * 执行资源库类型的更新操作，将请求参数转换为实体对象并调用服务层进行数据更新。
     * 如果更新失败，抛出运行时异常。
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出
     * @throws ParseException 当数据解析失败时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求JSON对象转换为资源库类型实体对象
        ResourceStoreTypePo resourceStoreTypePo = BeanConvertUtil.covertBean(reqJson, ResourceStoreTypePo.class);
        
        // 调用服务层接口更新资源库类型信息，返回影响的行数
        int flag = resourceStoreTypeV1InnerServiceSMOImpl.updateResourceStoreType(resourceStoreTypePo);
        
        // 如果更新失败（影响行数小于1），抛出异常
        if (flag < 1) {
            throw new IllegalArgumentException("保存类型失败");
        }
    }
}