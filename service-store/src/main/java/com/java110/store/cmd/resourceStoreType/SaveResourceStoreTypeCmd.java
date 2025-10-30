/**
 * 资源库类型保存命令类
 * 
 * 该类用于处理资源库类型的新增保存操作，包括参数验证和业务逻辑处理。
 * 通过注解@Java110Cmd声明服务代码，作为命令模式的一部分处理具体业务。
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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.resource.ResourceStoreTypeDto;
import com.java110.intf.store.IResourceStoreTypeV1InnerServiceSMO;
import com.java110.po.resource.ResourceStoreTypePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.Date;

@Java110Cmd(serviceCode = "resourceStoreType.saveResourceStoreType")
public class SaveResourceStoreTypeCmd extends Cmd {

    /**
     * 资源库类型服务接口
     */
    @Autowired
    private IResourceStoreTypeV1InnerServiceSMO resourceStoreTypeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括必填字段检查、默认值设置和业务规则验证。
     * 主要验证内容：
     * 1. 名称字段必填验证
     * 2. 父级ID默认值设置
     * 3. 店铺ID获取和验证
     * 4. 类型名称唯一性验证
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求头和响应处理
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证名称字段必填
        Assert.hasKeyAndValue(reqJson, "name", "请求报文中未包含name");
        
        // 设置父级ID默认值
        if(!reqJson.containsKey("parentId")){
            reqJson.put("parentId", "0"); // 如果没有父级ID，默认设置为0（根节点）
        }

        String parentId = reqJson.getString("parentId");

        // 处理空字符串的父级ID
        if(StringUtil.isEmpty(parentId)){
            reqJson.put("parentId", "0"); // 如果父级ID为空字符串，也设置为0
        }

        // 从请求头获取店铺ID并设置到参数中
        if (!reqJson.containsKey("storeId")) {
            String storeId = context.getReqHeaders().get("store-id");
            reqJson.put("storeId", storeId);
        }
        
        // 验证店铺ID必填
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含storeId");
        
        // 构建查询条件对象，验证类型名称唯一性
        ResourceStoreTypeDto resourceStoreTypeDto = new ResourceStoreTypeDto();
        resourceStoreTypeDto.setStoreId(reqJson.getString("storeId"));
        resourceStoreTypeDto.setName(reqJson.getString("name"));
        
        // 查询相同店铺下是否存在相同名称的类型
        int flag = resourceStoreTypeV1InnerServiceSMOImpl.queryResourceStoreTypesCount(resourceStoreTypeDto);

        // 如果已存在相同名称的类型，抛出异常
        if (flag > 0) {
            throw new IllegalArgumentException("类型名称已存在");
        }
    }

    /**
     * 执行命令方法
     * 
     * 处理资源库类型的新增保存业务逻辑，包括：
     * 1. 生成唯一ID
     * 2. 设置创建时间
     * 3. 数据对象转换
     * 4. 调用服务保存数据
     * 5. 处理保存结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出异常
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 生成资源库类型唯一ID
        reqJson.put("rstId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_rstId));
        
        // 设置创建时间为当前时间
        reqJson.put("createTime", new Date());
        
        // 将JSON对象转换为持久化对象
        ResourceStoreTypePo resourceStoreTypePo = BeanConvertUtil.covertBean(reqJson, ResourceStoreTypePo.class);
        
        // 调用服务保存资源库类型数据
        int flag = resourceStoreTypeV1InnerServiceSMOImpl.saveResourceStoreType(resourceStoreTypePo);

        // 检查保存结果，如果影响行数小于1表示保存失败
        if (flag < 1) {
            throw new IllegalArgumentException("保存类型失败");
        }
    }
}