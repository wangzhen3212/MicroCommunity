package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.resource.ResourceStoreTypeDto;
import com.java110.intf.store.IResourceStoreTypeInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询资源库类型树形结构命令类
 * 该命令用于查询资源库类型并以树形结构返回，支持多级分类展示
 * 主要功能包括：参数验证、查询类型数据、构建树形结构
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode resourceStore.listResourceStoreTypeTree
 */
@Java110Cmd(serviceCode = "resourceStore.listResourceStoreTypeTree")
public class ListResourceStoreTypeTreeCmd extends Cmd {

    @Autowired
    private IResourceStoreTypeInnerServiceSMO resourceStoreTypeInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求中是否包含商户ID，并将商户ID设置到请求JSON中
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置上下文信息
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从上下文中获取商户ID
        String storeId = CmdContextUtils.getStoreId(context);
        // 验证商户ID不能为空
        Assert.hasLength(storeId, "未包含商户ID");
        // 将商户ID设置到请求JSON中
        reqJson.put("storeId", storeId);
    }

    /**
     * 执行命令方法
     * 查询资源库类型数据并构建树形结构返回
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为资源库类型DTO对象
        ResourceStoreTypeDto resourceStoreTypeDto = BeanConvertUtil.covertBean(reqJson, ResourceStoreTypeDto.class);

        // 调用服务层查询资源库类型树形数据
        List<ResourceStoreTypeDto> storeTypeDtos = resourceStoreTypeInnerServiceSMOImpl.queryResourceStoreTypeTree(resourceStoreTypeDto);

        // 如果查询结果为空，直接返回
        if (storeTypeDtos == null || storeTypeDtos.size() < 1) {
            return;
        }

        // 初始化存储树形结构的列表
        List<ResourceStoreTypeDto> storeTypes = new ArrayList<>();

        // 计算一级分类：筛选出没有父级的分类作为根节点
        for (ResourceStoreTypeDto tmpResourceStoreTypeDto : storeTypeDtos) {
            // 跳过有父级的分类（非一级分类）
            if (!StringUtil.isEmpty(tmpResourceStoreTypeDto.getParentRstId())) {
                continue;
            }
            // 复制当前分类对象
            ResourceStoreTypeDto selfResourceStoreTypeDto = BeanConvertUtil.covertBean(tmpResourceStoreTypeDto, ResourceStoreTypeDto.class);
            // 设置父级信息：将自身ID作为父级ID，便于后续子分类关联
            selfResourceStoreTypeDto.setParentRstId(tmpResourceStoreTypeDto.getRstId());
            selfResourceStoreTypeDto.setParentName(tmpResourceStoreTypeDto.getName());
            selfResourceStoreTypeDto.setParentDescription(tmpResourceStoreTypeDto.getDescription());
            // 添加到一级分类列表
            storeTypes.add(selfResourceStoreTypeDto);
        }

        // 计算二级分类：将子分类关联到对应的父分类下
        ResourceStoreTypeDto tResourceStoreTypeDto = null;
        List<ResourceStoreTypeDto> subStoreTypes = null;
        for (ResourceStoreTypeDto tmpResourceStoreTypeDto : storeTypeDtos) {
            // 跳过一级分类（没有父级的分类）
            if (StringUtil.isEmpty(tmpResourceStoreTypeDto.getParentRstId())) {
                continue;
            }

            // 根据父级ID查找对应的一级分类
            tResourceStoreTypeDto = getParentResoureceStoreTypes(storeTypes, tmpResourceStoreTypeDto);

            // 如果没有找到对应的一级分类，跳过该子分类（数据异常情况）
            if (tResourceStoreTypeDto == null) {
                continue;
            }

            // 如果父分类的子分类列表为空，初始化子分类列表
            if (tResourceStoreTypeDto.getSubTypes() == null) {
                tResourceStoreTypeDto.setSubTypes(new ArrayList<>());
            }
            // 获取子分类列表并添加当前子分类
            subStoreTypes = tResourceStoreTypeDto.getSubTypes();
            subStoreTypes.add(tmpResourceStoreTypeDto);
        }

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(storeTypes));
    }

    /**
     * 根据父级ID在分类列表中查找对应的父分类
     * 用于将子分类关联到正确的父分类下
     *
     * @param storeTypes 一级分类列表
     * @param tmpResourceStoreTypeDto 当前处理的子分类对象
     * @return 对应的父分类对象，如果未找到则返回null
     */
    private ResourceStoreTypeDto getParentResoureceStoreTypes(List<ResourceStoreTypeDto> storeTypes, ResourceStoreTypeDto tmpResourceStoreTypeDto) {
        // 如果一级分类列表为空，直接返回null
        if (storeTypes.size() < 1) {
            return null;
        }

        // 遍历一级分类列表，查找与子分类父级ID匹配的分类
        for (ResourceStoreTypeDto storeType : storeTypes) {
            if (storeType.getRstId().equals(tmpResourceStoreTypeDto.getParentRstId())) {
                return storeType;
            }
        }

        // 未找到匹配的父分类
        return null;
    }
}