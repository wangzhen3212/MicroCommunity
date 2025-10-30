/**
 * 删除物品类型命令类
 * 
 * 该命令类负责处理删除物品类型的业务逻辑，包括验证删除条件和执行删除操作。
 * 主要功能：
 * 1. 验证删除请求参数
 * 2. 检查物品类型是否可以被删除（是否存在子分类或正在使用）
 * 3. 执行物品类型的删除操作
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
import com.java110.dto.resource.ResourceStoreTypeDto;
import com.java110.intf.community.IResourceStoreServiceSMO;
import com.java110.intf.store.IResourceStoreTypeV1InnerServiceSMO;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resource.ResourceStoreTypePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "resourceStoreType.deleteResourceStoreType")
public class DeleteResourceStoreTypeCmd extends Cmd {

    /**
     * 物品信息服务接口
     */
    @Autowired
    private IResourceStoreServiceSMO resourceStoreServiceSMOImpl;

    /**
     * 物品类型服务接口
     */
    @Autowired
    private IResourceStoreTypeV1InnerServiceSMO resourceStoreTypeV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证删除物品类型请求的必要参数是否完整
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求/响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含rstId字段
        Assert.hasKeyAndValue(reqJson, "rstId", "rstId不能为空");
    }

    /**
     * 执行删除物品类型命令
     * 
     * 该方法负责执行删除物品类型的核心业务逻辑，包括：
     * 1. 检查物品类型是否可以删除（是否存在子分类或正在使用）
     * 2. 执行删除操作
     * 3. 处理删除结果
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     * @throws ParseException 当数据解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建物品信息查询对象
        ResourceStorePo resourceStorePo = new ResourceStorePo();
        resourceStorePo.setRstId(reqJson.getString("rstId"));
        
        // 检查是否为顶级分类（parentId为0）
        if (!StringUtil.isEmpty(reqJson.getString("parentId")) && "0".equals(reqJson.getString("parentId"))) {
            // 对于顶级分类，需要检查是否存在子分类
            ResourceStoreTypeDto resourceStoreTypeDto = new ResourceStoreTypeDto();
            resourceStoreTypeDto.setParentId(reqJson.getString("rstId")); // 设置父级ID为当前要删除的分类ID
            // 查询是否存在子分类
            List<ResourceStoreTypeDto> resourceStores = resourceStoreTypeV1InnerServiceSMOImpl.queryResourceStoreTypes(resourceStoreTypeDto);
            // 如果存在子分类，则不允许删除
            Assert.listIsNull(resourceStores, "该物品类型下有二级分类，不能删除！");
        } else {
            // 对于非顶级分类，需要检查该分类是否正在被物品信息使用
            List<ResourceStorePo> resourceStores = resourceStoreServiceSMOImpl.getResourceStores(resourceStorePo);
            // 如果该分类正在被使用，则不允许删除
            Assert.listIsNull(resourceStores, "物品信息中该类型正在使用，不能删除！");
        }

        // 将请求JSON对象转换为物品类型持久化对象
        ResourceStoreTypePo resourceStoreTypePo = BeanConvertUtil.covertBean(reqJson, ResourceStoreTypePo.class);

        // 执行删除操作
        int flag = resourceStoreTypeV1InnerServiceSMOImpl.deleteResourceStoreType(resourceStoreTypePo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除失败");
        }
    }
}