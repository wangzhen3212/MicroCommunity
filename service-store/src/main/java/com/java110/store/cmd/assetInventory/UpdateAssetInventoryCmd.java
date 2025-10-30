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
import com.java110.dto.resource.ResourceStoreTimesDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.*;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.assetInventory.AssetInventoryPo;
import com.java110.po.assetInventory.AssetInventoryDetailPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resource.ResourceStoreTimesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;


/**
 * 资产盘点更新命令类
 * 
 * 该类负责处理资产盘点的更新操作，包括盘点数据的验证、保存、审核以及库存更新等业务逻辑。
 * 服务编码：assetInventory.updateAssetInventory
 * 请求路径：/app/assetInventory.UpdateAssetInventory
 * 
 * @author 吴学文 at 2022-09-22 14:23:28 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 */
@Java110Cmd(serviceCode = "assetInventory.updateAssetInventory")
public class UpdateAssetInventoryCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateAssetInventoryCmd.class);

    /** ID生成前缀 */
    public static final String CODE_PREFIX_ID = "10";

    /** 资产盘点服务接口 */
    @Autowired
    private IAssetInventoryV1InnerServiceSMO assetInventoryV1InnerServiceSMOImpl;

    /** 资产盘点明细服务接口 */
    @Autowired
    private IAssetInventoryDetailV1InnerServiceSMO assetInventoryDetailV1InnerServiceSMOImpl;

    /** 资源仓库服务接口 */
    @Autowired
    private IResourceStoreV1InnerServiceSMO resourceStoreV1InnerServiceSMOImpl;

    /** 采购申请服务接口 */
    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    /** 仓库调拨服务接口 */
    @Autowired
    private IAllocationStorehouseInnerServiceSMO allocationStorehouseInnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMO;

    /** 资源仓库批次服务接口 */
    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含aiId字段
        Assert.hasKeyAndValue(reqJson, "aiId", "请求报文中未包含aiId");
        // 验证请求参数中必须包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行资产盘点更新命令
     * 
     * 处理资产盘点的更新逻辑，包括：
     * 1. 保存盘点明细数据
     * 2. 处理审核相关逻辑
     * 3. 更新盘点主表信息
     * 4. 审核通过时更新库存信息
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

        // 获取资源仓库列表数据
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        
        // 处理盘点明细数据：当有资源数据且状态不是"4000"（审核通过）时，重新保存明细数据
        if (resourceStores != null && resourceStores.size() > 0 && assetInventoryPo.getState() != null && !assetInventoryPo.getState().equals("4000")) {
            // 删除原有的盘点明细数据
            AssetInventoryDetailPo assetInventoryDetailPo = new AssetInventoryDetailPo();
            assetInventoryDetailPo.setApplyOrderId(assetInventoryPo.getAiId());
            assetInventoryDetailV1InnerServiceSMOImpl.deleteAssetInventoryDetail(assetInventoryDetailPo);
            
            // 遍历资源列表，保存新的盘点明细数据
            for (int resourceStoreIndex = 0; resourceStoreIndex < resourceStores.size(); resourceStoreIndex++) {
                JSONObject resourceStore = resourceStores.getJSONObject(resourceStoreIndex);
                // 保存原始库存信息
                resourceStore.put("originalStock", resourceStore.getString("stock"));
                // 转换为盘点明细PO对象
                assetInventoryDetailPo = BeanConvertUtil.covertBean(resourceStore, AssetInventoryDetailPo.class);
                assetInventoryDetailPo.setId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
                assetInventoryDetailPo.setApplyOrderId(assetInventoryPo.getAiId());
                // 保存盘点明细数据
                int flag = assetInventoryDetailV1InnerServiceSMOImpl.saveAssetInventoryDetail(assetInventoryDetailPo);
                if (flag < 1) {
                    throw new CmdException("保存数据盘点明细失败");
                }
            }
        }
        
        // 处理审核相关逻辑：当状态为"3000"（审核中）或"4000"（审核通过）时，设置审核人信息
        if (assetInventoryPo.getState() != null && (assetInventoryPo.getState().equals("4000")||assetInventoryPo.getState().equals("3000"))){
            String currentUserId = reqJson.getString("userId");
            UserDto userDto =  new UserDto();
            userDto.setUserId(currentUserId);
            // 查询用户信息
            List<UserDto> userDtos = userV1InnerServiceSMO.queryUsers(userDto);
            Assert.listOnlyOne(userDtos,"用户不存在");
            // 设置审核人信息
            assetInventoryPo.setAuditId(currentUserId);
            assetInventoryPo.setAuditName(userDtos.get(0).getName());
            assetInventoryPo.setAuditTel(userDtos.get(0).getTel());
            assetInventoryPo.setAuditTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        }
        
        // 更新资产盘点主表信息
        int flag = assetInventoryV1InnerServiceSMOImpl.updateAssetInventory(assetInventoryPo);
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 审核通过时更新库存信息
        if (assetInventoryPo.getState() != null && assetInventoryPo.getState().equals("4000")){ 
            if (resourceStores != null && resourceStores.size() > 0 ) {
                for (int resourceStoreIndex = 0; resourceStoreIndex < resourceStores.size(); resourceStoreIndex++) {
                    AssetInventoryDetailPo assetInventoryDetailPo = new AssetInventoryDetailPo();
                    JSONObject resourceStore = resourceStores.getJSONObject(resourceStoreIndex);
                    // 保存原始库存信息
                    resourceStore.put("originalStock", resourceStore.getString("stock"));
                    assetInventoryDetailPo = BeanConvertUtil.covertBean(resourceStore, AssetInventoryDetailPo.class);
                    assetInventoryDetailPo.setId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
                    assetInventoryDetailPo.setApplyOrderId(assetInventoryPo.getAiId());

                    // 更新批次库存信息
                    ResourceStoreTimesDto resourceStoreTimesDto = new ResourceStoreTimesDto();
                    resourceStoreTimesDto.setTimesId(assetInventoryDetailPo.getTimesId());
                    List<ResourceStoreTimesDto> resourceStoreTimesDtos = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimess(resourceStoreTimesDto);
                    if(resourceStoreTimesDtos.size()>0){
                        ResourceStoreTimesPo resourceStoreTimesPo = new ResourceStoreTimesPo();
                        resourceStoreTimesPo.setTimesId(resourceStoreTimesDtos.get(0).getTimesId());
                        resourceStoreTimesPo.setStock(assetInventoryDetailPo.getQuantity());
                        resourceStoreTimesV1InnerServiceSMOImpl.updateResourceStoreTimes(resourceStoreTimesPo);
                    }

                    // 查询批次库存总和
                    ResourceStoreTimesDto resourceStoreTimesDto1 = new ResourceStoreTimesDto();
                    resourceStoreTimesDto1.setShId(assetInventoryDetailPo.getShId());
                    resourceStoreTimesDto1.setResCode(assetInventoryDetailPo.getResCode());
                    Integer timessCountStock = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimessCountStock(resourceStoreTimesDto1);

                    // 更新某个仓库某个商品的库存信息
                    ResourceStorePo resourceStorePo = new ResourceStorePo();
                    resourceStorePo.setResId(assetInventoryDetailPo.getResId());
                    resourceStorePo.setStock(Integer.toString(timessCountStock));

                    // 计算最小计量单位库存
                    BigDecimal quantity = new BigDecimal(timessCountStock);
                    BigDecimal miniUnitStock = new BigDecimal(resourceStore.getString("miniUnitStock"));
                    // 计算最小计量总数：数量 × 最小单位库存
                    BigDecimal miniStock = quantity.multiply(miniUnitStock);
                    resourceStorePo.setMiniStock(String.valueOf(miniStock));
                    resourceStorePo.setStatusCd("0");
                    // 更新资源仓库库存信息
                    resourceStoreV1InnerServiceSMOImpl.updateResourceStore(resourceStorePo);
                }
            }
        }
        
        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}