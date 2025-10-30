/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.store.cmd.assetInventory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.store.IAssetInventoryDetailV1InnerServiceSMO;
import com.java110.intf.store.IAssetInventoryV1InnerServiceSMO;
import com.java110.intf.store.IAllocationStorehouseApplyInnerServiceSMO;
import com.java110.intf.store.IAllocationStorehouseInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyInnerServiceSMO;
import com.java110.po.assetInventory.AssetInventoryPo;
import com.java110.po.assetInventory.AssetInventoryDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 资产盘点保存命令类
 * 
 * 该类负责处理资产盘点的保存操作，包括参数验证、数据转换和业务逻辑处理
 * 主要功能：验证盘点参数、保存盘点主信息和明细信息
 * 
 * @author 吴学文 at 2022-09-22 14:23:28 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "assetInventory.saveAssetInventory")
public class SaveAssetInventoryCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveAssetInventoryCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 采购申请服务接口
     */
    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    /**
     * 资产盘点服务接口
     */
    @Autowired
    private IAssetInventoryV1InnerServiceSMO assetInventoryV1InnerServiceSMOImpl;

    /**
     * 资产盘点明细服务接口
     */
    @Autowired
    private IAssetInventoryDetailV1InnerServiceSMO assetInventoryDetailV1InnerServiceSMOImpl;

    /**
     * 仓库调拨服务接口
     */
    @Autowired
    private IAllocationStorehouseInnerServiceSMO allocationStorehouseInnerServiceSMOImpl;

    /**
     * 仓库调拨申请服务接口
     */
    @Autowired
    private IAllocationStorehouseApplyInnerServiceSMO allocationStorehouseApplyInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，并设置默认值
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "invTime", "请求报文中未包含盘点时间");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "shId", "请求报文中未包含shId");
        
        // 设置操作人员ID，使用当前用户ID
        reqJson.put("staffId", reqJson.getString("userId"));
    }

    /**
     * 执行资产盘点保存命令
     * 
     * 处理资产盘点的核心业务逻辑，包括数据转换、明细保存和主信息保存
     * 使用事务注解确保数据一致性
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为资产盘点PO对象
        AssetInventoryPo assetInventoryPo = BeanConvertUtil.covertBean(reqJson, AssetInventoryPo.class);

        // 注释掉的业务逻辑：检查采购申请和调拨状态
        // 这些代码被注释掉，可能是为了避免在某些业务场景下的冲突检查
//        PurchaseApplyDto purchaseApplyDto = new PurchaseApplyDto();
//        purchaseApplyDto.setCommunityId(assetInventoryPo.getCommunityId());
//        purchaseApplyDto.setState(purchaseApplyDto.STATE_DEALING);//审核中
//        List<PurchaseApplyDto> purchaseApplyDtos = purchaseApplyInnerServiceSMOImpl.queryPurchaseApplyAndDetails(purchaseApplyDto);
//        for (PurchaseApplyDto applyDto : purchaseApplyDtos){
//            List<PurchaseApplyDetailVo> purchaseApplyDetailVos = applyDto.getPurchaseApplyDetailVo();
//            for (PurchaseApplyDetailVo applyDetailVo : purchaseApplyDetailVos) {
//                 if (applyDetailVo.getShId() != null && assetInventoryPo.getShId() != null && applyDetailVo.getShId().equals(assetInventoryPo.getShId())) {
//                    String resOrderType =  applyDto.getResOrderType().equals("10000")? "入库":"出库";
//                    throw new CmdException("盘存失败"+applyDetailVo.getShName() + "有"+ resOrderType +"物品");
//                }
//            }
//        }
//        AllocationStorehouseDto allocationStorehouseDto = new AllocationStorehouseDto();
//        allocationStorehouseDto.setShIda(assetInventoryPo.getShId());
//        allocationStorehouseDto.setState(allocationStorehouseDto.STATE_AUDIT);
//        List<AllocationStorehouseDto> allocationStorehouseDtosa = allocationStorehouseInnerServiceSMOImpl.queryAllocationStorehouses(allocationStorehouseDto);
//        if (allocationStorehouseDtosa.size() > 0){
//            //cmdDataFlowContext.setResponseEntity(ResultVo.error("盘存失败"+assetInventoryPo.getShName() + "有调拨物品。" ));
//            throw new CmdException("盘存失败"+assetInventoryPo.getShName() + "有调拨物品。");
//        }
//        allocationStorehouseDto.setShIdz(assetInventoryPo.getShId());
//        allocationStorehouseDtosa = allocationStorehouseInnerServiceSMOImpl.queryAllocationStorehouses(allocationStorehouseDto);
//        if (allocationStorehouseDtosa.size() > 0){
//            //cmdDataFlowContext.setResponseEntity(ResultVo.error("盘存失败"+assetInventoryPo.getShName() + "有调拨物品。" ));
//            throw new CmdException("盘存失败"+assetInventoryPo.getShName() + "有调拨物品。");
//        }

        // 验证盘点物品数据是否存在
        if (!reqJson.containsKey("resourceStores")) {
            throw new IllegalArgumentException("未包含盘点物品");
        }
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        if (resourceStores == null || resourceStores.size() < 1) {
            throw new IllegalArgumentException("未包含盘点物品");
        }

        // 生成盘点主表ID
        assetInventoryPo.setAiId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 遍历盘点物品明细，保存每个明细记录
        for (int resourceStoreIndex = 0; resourceStoreIndex < resourceStores.size(); resourceStoreIndex++) {
            JSONObject resourceStore = resourceStores.getJSONObject(resourceStoreIndex);
            
            // 设置原始库存数量
            resourceStore.put("originalStock", resourceStore.getString("stock"));
            
            // 转换为盘点明细PO对象
            AssetInventoryDetailPo assetInventoryDetailPo = BeanConvertUtil.covertBean(resourceStore, AssetInventoryDetailPo.class);
            
            // 生成明细记录ID并设置关联主表ID
            assetInventoryDetailPo.setId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
            assetInventoryDetailPo.setApplyOrderId(assetInventoryPo.getAiId());
            
            // 保存盘点明细记录
            int flag = assetInventoryDetailV1InnerServiceSMOImpl.saveAssetInventoryDetail(assetInventoryDetailPo);
            if (flag < 1) {
                throw new CmdException("保存数据盘点明细失败");
            }
        }
        
        // 保存盘点主表信息
        //assetInventoryPo.setState("4488"); //4488 盘点中，4477 盘点结束，4466 盘点入库
        int flag = assetInventoryV1InnerServiceSMOImpl.saveAssetInventory(assetInventoryPo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}