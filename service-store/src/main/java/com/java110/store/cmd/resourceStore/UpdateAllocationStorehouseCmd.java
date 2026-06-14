package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.resource.ResourceStoreDto;
import com.java110.dto.resource.ResourceStoreTimesDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.IAllocationStorehouseUserInnerServiceSMO;
import com.java110.intf.store.*;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.purchase.AllocationStorehouseApplyPo;
import com.java110.po.purchase.AllocationStorehousePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 修改调拨申请命令类
 * 
 * 该类负责处理调拨申请的修改操作，包括参数验证、业务逻辑处理和数据库事务管理
 * 主要功能包括：验证调拨申请数据的有效性、更新调拨申请信息、保存调拨物品明细等
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode resourceStore.updateAllocationStorehouse
 */
@Java110Cmd(serviceCode = "resourceStore.updateAllocationStorehouse")
public class UpdateAllocationStorehouseCmd extends Cmd {

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseUserInnerServiceSMO allocationStorehouseUserInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseApplyV1InnerServiceSMO allocationStorehouseApplyV1InnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseV1InnerServiceSMO allocationStorehouseV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreV1InnerServiceSMO resourceStoreV1InnerServiceSMOImpl;

    @Autowired
    private IUserStorehouseV1InnerServiceSMO userStorehouseV1InnerServiceSMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IStorehouseV1InnerServiceSMO storehouseV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    /**
     * 验证调拨申请参数的有效性
     * 
     * 该方法用于验证请求参数是否完整有效，包括：
     * 1. 必填字段验证
     * 2. 物品信息验证
     * 3. 库存数量验证
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "remark", "请求报文中未包含申请信息");
        Assert.hasKeyAndValue(reqJson, "storeId", "请求报文中未包含storeId");
        
        // 验证物品信息是否存在
        if (!reqJson.containsKey("resourceStores")) {
            throw new IllegalArgumentException("请求报文中未包含物品信息");
        }
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        if (resourceStores == null || resourceStores.size() < 1) {
            throw new IllegalArgumentException("请求报文中未包含物品信息");
        }
        
        // 遍历所有物品信息，进行库存验证
        for (int resIndex = 0; resIndex < resourceStores.size(); resIndex++) {
            // 查询物品基本信息
            ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
            resourceStoreDto.setStoreId(reqJson.getString("storeId"));
            resourceStoreDto.setResId(resourceStores.getJSONObject(resIndex).getString("resId"));
            resourceStoreDto.setShId(resourceStores.getJSONObject(resIndex).getString("shId"));
            List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
            Assert.listOnlyOne(resourceStoreDtos, "未包含 物品信息");

            // 查询物品批次信息，用于库存验证
            ResourceStoreTimesDto resourceStoreTimesDto = new ResourceStoreTimesDto();
            resourceStoreTimesDto.setTimesId(resourceStores.getJSONObject(resIndex).getString("timesId"));
            List<ResourceStoreTimesDto> resourceStoreTimesDtos = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimess(resourceStoreTimesDto);
            Assert.listOnlyOne(resourceStoreTimesDtos, "查询物品批次表错误！");
            
            // 验证库存数量是否足够
            double stockA = Double.parseDouble(resourceStoreTimesDtos.get(0).getStock());
            double stockB = Double.parseDouble(resourceStores.getJSONObject(resIndex).getString("curStock"));
            if (stockA < stockB) {
                throw new IllegalArgumentException("该批次价格下库存数量不足！");
            }
            
            // 补充物品名称和库存信息到请求参数中
            resourceStores.getJSONObject(resIndex).put("resName", resourceStoreDtos.get(0).getResName());
            resourceStores.getJSONObject(resIndex).put("stockA", stockA);
        }
    }

    /**
     * 执行调拨申请修改操作
     * 
     * 该方法负责处理调拨申请的核心业务逻辑，包括：
     * 1. 查询用户信息
     * 2. 封装调拨申请对象
     * 3. 删除原有调拨物品明细
     * 4. 保存新的调拨物品明细
     * 5. 更新调拨申请信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        int flag = 0;
        
        // 查询用户信息并验证用户是否存在
        UserDto userDto = new UserDto();
        userDto.setUserId(reqJson.getString("userId"));
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户不存在");
        reqJson.put("userName", userDtos.get(0).getName());
        
        // 封装调拨申请对象
        AllocationStorehouseApplyPo allocationStorehouseApplyPo = covertAllocationStorehouseApply(reqJson);
        // 初始化申请数量，后续会累加计算
        allocationStorehouseApplyPo.setApplyCount("0.0");
        
        // 删除原有的调拨物品明细
        AllocationStorehousePo allocationStorehousePo = new AllocationStorehousePo();
        allocationStorehousePo.setApplyId(allocationStorehouseApplyPo.getApplyId());
        allocationStorehouseV1InnerServiceSMOImpl.deleteAllocationStorehouse(allocationStorehousePo);
        
        // 遍历所有物品信息，保存新的调拨物品明细
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        JSONObject resObj = null;
        for (int resIndex = 0; resIndex < resourceStores.size(); resIndex++) {
            // 处理单个物品信息
            resObj = resourceStores.getJSONObject(resIndex);
            // 保存调拨物品明细
            saveAllocationStorehouse(reqJson, allocationStorehouseApplyPo, resObj);
        }
        
        // 更新调拨申请信息
        flag = allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
        if (flag < 1) {
            throw new CmdException("保存修改物品失败");
        }
        
        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }

    /**
     * 保存调拨物品明细信息
     * 
     * 该方法用于保存单个调拨物品的详细信息，包括：
     * 1. 生成调拨物品ID
     * 2. 设置调拨物品的基本信息
     * 3. 保存到数据库
     * 4. 累加申请总数量
     * 
     * @param reqJson 请求参数JSON对象
     * @param allocationStorehouseApplyPo 调拨申请对象
     * @param resObj 物品信息JSON对象
     * @throws CmdException 当保存失败时抛出异常
     */
    private void saveAllocationStorehouse(JSONObject reqJson, AllocationStorehouseApplyPo allocationStorehouseApplyPo, JSONObject resObj) {
        // 创建调拨物品对象并设置基本信息
        AllocationStorehousePo allocationStorehousePo = new AllocationStorehousePo();
        allocationStorehousePo.setAsId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_allocationStorehouseId));
        allocationStorehousePo.setApplyId(allocationStorehouseApplyPo.getApplyId());
        allocationStorehousePo.setResId(resObj.getString("resId"));
        allocationStorehousePo.setResName(resObj.getString("resName"));
        allocationStorehousePo.setShIda(resObj.getString("shId")); // 调出仓库ID
        allocationStorehouseApplyPo.setShId(resObj.getString("shId"));
        allocationStorehousePo.setShIdz(resObj.getString("shzId")); // 调入仓库ID
        allocationStorehousePo.setStoreId(reqJson.getString("storeId"));
        
        // 设置调拨数量和原有库存
        allocationStorehousePo.setStock(resObj.getString("curStock")); // 调拨数量
        allocationStorehousePo.setOriginalStock(resObj.getString("stock")); // 原有库存
        
        // 设置备注和申请人信息
        allocationStorehousePo.setRemark(reqJson.getString("remark"));
        allocationStorehousePo.setStartUserId(reqJson.getString("userId"));
        allocationStorehousePo.setStartUserName(reqJson.getString("userName"));
        allocationStorehousePo.setTimesId(resObj.getString("timesId")); // 批次ID
        
        // 保存调拨物品信息
        int flag = allocationStorehouseV1InnerServiceSMOImpl.saveAllocationStorehouse(allocationStorehousePo);
        if (flag < 1) {
            throw new CmdException("保存调拨物品失败");
        }
        
        // 累加申请总数量
        double applyCount = Double.parseDouble(allocationStorehouseApplyPo.getApplyCount());
        applyCount += resObj.getDoubleValue("curStock");
        allocationStorehouseApplyPo.setApplyCount(applyCount + "");
    }

    /**
     * 封装调拨申请对象
     * 
     * 该方法用于将请求参数转换为调拨申请对象
     * 
     * @param reqJson 请求参数JSON对象
     * @return AllocationStorehouseApplyPo 封装好的调拨申请对象
     */
    private AllocationStorehouseApplyPo covertAllocationStorehouseApply(JSONObject reqJson) {
        AllocationStorehouseApplyPo allocationStorehouseApplyPo = new AllocationStorehouseApplyPo();
        allocationStorehouseApplyPo.setApplyId(reqJson.getString("applyId")); // 申请ID
        allocationStorehouseApplyPo.setRemark(reqJson.getString("remark")); // 备注
        allocationStorehouseApplyPo.setStoreId(reqJson.getString("storeId")); // 店铺ID
        allocationStorehouseApplyPo.setCommunityId(reqJson.getString("communityId")); // 社区ID
        return allocationStorehouseApplyPo;
    }
}