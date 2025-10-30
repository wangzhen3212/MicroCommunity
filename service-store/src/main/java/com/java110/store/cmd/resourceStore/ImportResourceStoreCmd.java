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
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.dict.DictDto;
import com.java110.dto.dict.DictQueryDto;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.dto.resource.ResourceStoreDto;
import com.java110.dto.resource.ResourceStoreTypeDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.importData.ImportResourceStoreDto;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.community.DictInnerServiceSMO;
import com.java110.intf.store.*;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.purchase.PurchaseApplyDetailPo;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resource.ResourceStoreTimesPo;
import com.java110.po.resource.ResourceStoreTypePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 物品资源导入命令类
 * 功能：处理物品资源的批量导入，包括新增物品和更新现有物品库存，并生成相应的入库记录
 * 服务编码：resourceStore.importResourceStore
 * 请求路径：/app/resourceStore.SaveResourceStore
 * 
 * @author 吴学文 at 2022-06-30 12:03:19 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "resourceStore.importResourceStore")
public class ImportResourceStoreCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ImportResourceStoreCmd.class);

    /** 编码前缀常量 */
    public static final String CODE_PREFIX_ID = "10";

    /** 物品资源服务接口 */
    @Autowired
    private IResourceStoreV1InnerServiceSMO resourceStoreV1InnerServiceSMOImpl;

    /** 物品资源查询服务接口 */
    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    /** 文件关联服务接口 */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /** 文件服务接口 */
    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /** 采购申请服务接口 */
    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    /** 字典服务接口 */
    @Autowired
    private DictInnerServiceSMO dictInnerServiceSMOImpl;

    /** 物品资源类型服务接口 */
    @Autowired
    private IResourceStoreTypeV1InnerServiceSMO resourceStoreTypeV1InnerServiceSMOImpl;

    /** 物品资源库存记录服务接口 */
    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 目前该方法为空实现，可根据需要添加具体的参数验证逻辑
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        //Assert.hasKeyAndValue(reqJson, "xxx", "xxx");
    }

    /**
     * 执行物品资源导入命令
     * 主要功能：处理导入的物品数据，包括新增物品、更新库存，并生成入库记录
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象，包含用户信息、店铺信息和导入数据
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求参数中获取用户和店铺信息
        String userId = reqJson.getString("userId");
        String userName = reqJson.getString("userName");
        String storeId = reqJson.getString("storeId");

        // 解析导入的物品数据
        JSONArray importResourceStoreDtos = reqJson.getJSONArray("importResourceStoreDtos");
        List<ImportResourceStoreDto> tmpImportResourceStoreDtos = JSONArray.parseArray(importResourceStoreDtos.toJSONString(), ImportResourceStoreDto.class);
        
        // 如果导入数据为空，直接返回成功响应
        if (ListUtil.isNull(tmpImportResourceStoreDtos)) {
            cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(0));
            return;
        }
        
        // 查询单位字典信息
        DictQueryDto dictDto = new DictQueryDto();
        dictDto.setName("resource_store");
        dictDto.setType("unit_code");
        List<DictDto> dictDtos = dictInnerServiceSMOImpl.queryDict(dictDto);

        List<ResourceStorePo> resourceStorePos = new ArrayList<>();
        // 遍历处理每个导入的物品
        for (ImportResourceStoreDto importResourceStoreDto : tmpImportResourceStoreDtos) {
            // 根据物品编码查询物品资源表，检查是否已存在
            ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
            resourceStoreDto.setResCode(importResourceStoreDto.getResName());
            resourceStoreDto.setShId(reqJson.getString("shId"));
            List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
            
            // 构建物品资源基础信息
            JSONObject businessResourceStore = new JSONObject();
            //businessResourceStore.put("stock", "0");
            businessResourceStore.put("miniStock", "100"); // 设置最小库存
            businessResourceStore.put("outLowPrice", importResourceStoreDto.getOutLowPrice()); // 设置最低售价
            businessResourceStore.put("outHighPrice", importResourceStoreDto.getOutHighPrice()); // 设置最高售价
            businessResourceStore.put("showMobile", "N"); // 设置是否在移动端显示
            businessResourceStore.put("description", importResourceStoreDto.getResName()); // 设置物品描述
            businessResourceStore.put("isFixed", importResourceStoreDto.getIsFixed()); // 设置是否为固定资产
            businessResourceStore.put("miniUnitCode", "1"); // 设置最小单位编码
            businessResourceStore.put("miniUnitStock", "1"); // 设置最小单位库存
            businessResourceStore.put("storeId", storeId); // 设置店铺ID
            businessResourceStore.put("createTime", new Date()); // 设置创建时间
            
            // 将JSON对象转换为物品资源PO对象
            ResourceStorePo resourceStorePo = BeanConvertUtil.covertBean(businessResourceStore, ResourceStorePo.class);
            resourceStorePo.setShId(reqJson.getString("shId")); // 设置供应商ID
            resourceStorePo.setStock(importResourceStoreDto.getStock()); // 设置库存数量
            resourceStorePo.setResName(importResourceStoreDto.getResName()); // 设置物品名称
            resourceStorePo.setResCode(importResourceStoreDto.getResCode()); // 设置物品编码
            resourceStorePo.setRstId(getRstId(importResourceStoreDto.getRstName(), storeId)); // 获取或创建物品类型ID
            resourceStorePo.setParentRstId(resourceStorePo.getRstId()); // 设置父类型ID
            resourceStorePo.setPrice(importResourceStoreDto.getPrice()); // 设置价格
            resourceStorePo.setUnitCode(getUnitCode(importResourceStoreDto.getUnitCode(), dictDtos)); // 获取单位编码
            resourceStorePo.setRemark(importResourceStoreDto.getRemark()); // 设置备注
            resourceStorePo.setWarningStock(importResourceStoreDto.getWarningStock()); // 设置预警库存
            resourceStorePo.setAveragePrice(importResourceStoreDto.getPrice()); // 设置平均价格
            resourceStorePo.setCommunityId(reqJson.getString("communityId")); // 设置社区ID
            
            int flag = 0;
            // 根据物品是否存在决定是新增还是更新
            if (ListUtil.isNull(resourceStoreDtos)) {
                // 新增物品：生成物品ID并保存
                resourceStorePo.setResId(GenerateCodeFactory.getResId(GenerateCodeFactory.CODE_PREFIX_resId));
                flag = resourceStoreV1InnerServiceSMOImpl.saveResourceStore(resourceStorePo);
            } else {
                // 更新现有物品：累加库存并更新
                resourceStorePo.setResId(resourceStoreDtos.get(0).getResId());
                double stock = Double.parseDouble(resourceStoreDtos.get(0).getStock()) + Double.parseDouble(importResourceStoreDto.getStock());
                resourceStorePo.setStock(stock + "");
                // 清空部分字段，避免更新时覆盖原有值
                resourceStorePo.setOutLowPrice("");
                resourceStorePo.setOutHighPrice("");
                resourceStorePo.setMiniUnitCode("");
                resourceStorePo.setMiniStock("");
                flag = resourceStoreV1InnerServiceSMOImpl.updateResourceStore(resourceStorePo);
                // 恢复原始库存值用于后续入库记录
                resourceStorePo.setStock(importResourceStoreDto.getStock());
            }

            // 检查操作是否成功
            if (flag < 1) {
                throw new CmdException("保存数据失败");
            }

            resourceStorePos.add(resourceStorePo);
        }

        // 执行入库操作
        inStore(reqJson, userId, userName, storeId, resourceStorePos);
        // 设置响应结果
        cmdDataFlowContext.setResponseEntity(ResultVo.createResponseEntity(tmpImportResourceStoreDtos.size()));
    }

    /**
     * 获取物品类型ID
     * 如果类型不存在则自动创建
     *
     * @param rstName 物品类型名称
     * @param storeId 店铺ID
     * @return 物品类型ID
     */
    private String getRstId(String rstName, String storeId) {
        // 查询是否已存在该物品类型
        ResourceStoreTypeDto resourceStoreTypeDto = new ResourceStoreTypeDto();
        resourceStoreTypeDto.setStoreId(storeId);
        resourceStoreTypeDto.setName(rstName);
        List<ResourceStoreTypeDto> resourceStoreTypeDtos = resourceStoreTypeV1InnerServiceSMOImpl.queryResourceStoreTypes(resourceStoreTypeDto);
        
        // 如果类型存在，直接返回类型ID
        if (resourceStoreTypeDtos != null && resourceStoreTypeDtos.size() > 0) {
            return resourceStoreTypeDtos.get(0).getRstId();
        }

        // 类型不存在，创建新的物品类型
        ResourceStoreTypePo resourceStoreTypePo = new ResourceStoreTypePo();
        resourceStoreTypePo.setDescription(rstName); // 设置类型描述
        resourceStoreTypePo.setName(rstName); // 设置类型名称
        resourceStoreTypePo.setStoreId(storeId); // 设置店铺ID
        resourceStoreTypePo.setRstId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_rstId)); // 生成类型ID
        resourceStoreTypePo.setParentId("0"); // 设置父级ID为0（根节点）
        resourceStoreTypeV1InnerServiceSMOImpl.saveResourceStoreType(resourceStoreTypePo); // 保存物品类型
        return resourceStoreTypePo.getRstId(); // 返回新创建的类型ID
    }

    /**
     * 获取单位编码
     * 根据单位名称从字典中查找对应的编码
     *
     * @param unitCode 单位名称
     * @param dictDtos 字典数据列表
     * @return 单位编码，如果找不到则返回"无"
     */
    private String getUnitCode(String unitCode, List<DictDto> dictDtos) {
        // 遍历字典数据，匹配单位名称
        for (DictDto dictDto : dictDtos) {
            if (unitCode.equals(dictDto.getName())) {
                return dictDto.getStatusCd(); // 返回状态编码作为单位编码
            }
        }

        return "无"; // 未找到对应单位，返回默认值
    }

    /**
     * 入库处理方法
     * 为导入的物品生成入库记录和采购申请
     *
     * @param reqJson 请求参数JSON对象
     * @param userId 用户ID
     * @param userName 用户名称
     * @param storeId 店铺ID
     * @param resourceStorePos 物品资源列表
     */
    private void inStore(JSONObject reqJson, String userId, String userName, String storeId, List<ResourceStorePo> resourceStorePos) {
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setPage(1);
        userDto.setRow(1);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证用户存在且唯一
        Assert.listOnlyOne(userDtos, "用户不存在");

        // 创建采购申请主记录
        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
        purchaseApplyPo.setApplyOrderId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId)); // 生成申请单ID
        purchaseApplyPo.setDescription("入库单（物品导入）"); // 设置申请单描述
        purchaseApplyPo.setUserId(userId); // 设置用户ID
        purchaseApplyPo.setUserName(userDtos.get(0).getName()); // 设置用户名称
        purchaseApplyPo.setEndUserName(userDtos.get(0).getName()); // 设置最终用户名称
        purchaseApplyPo.setEndUserTel(userDtos.get(0).getTel()); // 设置最终用户电话
        purchaseApplyPo.setStoreId(storeId); // 设置店铺ID
        purchaseApplyPo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_ENTER); // 设置资源订单类型为入库
        purchaseApplyPo.setState(PurchaseApplyDto.STATE_END); // 设置状态为已完成
        purchaseApplyPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A)); // 设置创建时间
        purchaseApplyPo.setCreateUserId(userId); // 设置创建用户ID
        purchaseApplyPo.setCreateUserName(userName); // 设置创建用户名称
        purchaseApplyPo.setWarehousingWay(PurchaseApplyDto.WAREHOUSING_TYPE_URGENT); // 设置入库方式为紧急入库
        purchaseApplyPo.setCommunityId(reqJson.getString("communityId")); // 设置社区ID
        
        // 创建采购申请明细列表
        List<PurchaseApplyDetailPo> purchaseApplyDetailPos = new ArrayList<>();
        for (ResourceStorePo resourceStorePo : resourceStorePos) {
            // 创建采购申请明细
            PurchaseApplyDetailPo purchaseApplyDetailPo = new PurchaseApplyDetailPo();
            purchaseApplyDetailPo.setApplyOrderId(purchaseApplyPo.getApplyOrderId()); // 设置申请单ID
            purchaseApplyDetailPo.setId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId)); // 生成明细ID
            purchaseApplyDetailPo.setPurchaseQuantity(resourceStorePo.getStock()); // 设置采购数量
            purchaseApplyDetailPo.setPurchaseRemark(resourceStorePo.getRemark()); // 设置采购备注
            purchaseApplyDetailPo.setOriginalStock(resourceStorePo.getStock()); // 设置原始库存
            purchaseApplyDetailPo.setQuantity(resourceStorePo.getStock()); // 设置数量
            purchaseApplyDetailPo.setPrice(resourceStorePo.getPrice()); // 设置价格
            purchaseApplyDetailPo.setResId(resourceStorePo.getResId()); // 设置物品ID
            purchaseApplyDetailPo.setRsId(resourceStorePo.getRssId()); // 设置物品资源ID
            purchaseApplyDetailPos.add(purchaseApplyDetailPo);
            purchaseApplyPo.setPurchaseApplyDetailPos(purchaseApplyDetailPos); // 设置采购申请明细列表

            // 保存物品库存记录
            ResourceStoreTimesPo resourceStoreTimesPo = new ResourceStoreTimesPo();
            resourceStoreTimesPo.setApplyOrderId(purchaseApplyPo.getApplyOrderId()); // 设置申请单ID
            resourceStoreTimesPo.setPrice(purchaseApplyDetailPo.getPrice()); // 设置价格
            resourceStoreTimesPo.setStock(purchaseApplyDetailPo.getPurchaseQuantity()); // 设置库存数量
            resourceStoreTimesPo.setResCode(resourceStorePo.getResCode()); // 设置物品编码
            resourceStoreTimesPo.setStoreId(resourceStorePo.getStoreId()); // 设置店铺ID
            resourceStoreTimesPo.setTimesId(GenerateCodeFactory.getGeneratorId("10")); // 生成库存记录ID
            resourceStoreTimesPo.setShId(resourceStorePo.getShId()); // 设置供应商ID
            resourceStoreTimesPo.setCommunityId(resourceStorePo.getCommunityId()); // 设置社区ID

            // 保存或更新库存记录
            resourceStoreTimesV1InnerServiceSMOImpl.saveOrUpdateResourceStoreTimes(resourceStoreTimesPo);
        }
        
        // 保存采购申请记录
        int saveFlag = purchaseApplyInnerServiceSMOImpl.savePurchaseApply(purchaseApplyPo);
        if (saveFlag < 1) {
            throw new CmdException("采购申请失败");
        }
    }
}