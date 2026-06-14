package com.java110.store.cmd.purchase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.purchase.AllocationStorehouseDto;
import com.java110.dto.purchase.AllocationStorehouseApplyDto;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.dto.resource.ResourceStoreDto;
import com.java110.dto.store.StorehouseDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.store.*;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.purchase.PurchaseApplyDetailPo;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resource.ResourceStoreTimesPo;
import com.java110.store.bmo.purchase.IPurchaseApplyBMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 紧急采购申请命令类
 * 处理紧急采购申请的业务逻辑，包括参数验证、紧急采购次数限制检查、物资入库、库存调整等操作
 * 支持从集团仓库紧急采购物资并自动调拨到小区仓库
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "/purchase/urgentPurchaseApply")
public class UrgentPurchaseApplyCmd extends Cmd {

    //域
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    //键
    public static final String URGRNT_NUMBER = "URGRNT_NUMBER";

    @Autowired
    private IAllocationStorehouseInnerServiceSMO allocationStorehouseInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseApplyInnerServiceSMO allocationStorehouseApplyInnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    @Autowired
    private IStorehouseInnerServiceSMO storehouseInnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyBMO purchaseApplyBMOImpl;

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    @Autowired
    private IStorehouseV1InnerServiceSMO storehouseV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括必填字段检查、采购物资检查、仓库验证等
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "resourceStores", "必填，请填写申请采购的物资");
        Assert.hasKeyAndValue(reqJson, "description", "必填，请填写采购申请说明");
        Assert.hasKeyAndValue(reqJson, "resOrderType", "必填，请填写申请类型");
        Assert.hasKeyAndValue(reqJson, "shId", "必填，请填写仓库");
        
        // 验证采购物资列表
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        if (resourceStores == null || resourceStores.size() < 1) {
            throw new CmdException("未包含采购物品");
        }
        
        // 验证仓库是否存在
        StorehouseDto storehouseDto = new StorehouseDto();
        storehouseDto.setShId(reqJson.getString("shId"));
        List<StorehouseDto> storehouseDtos = storehouseV1InnerServiceSMOImpl.queryStorehouses(storehouseDto);
        Assert.listOnlyOne(storehouseDtos, "仓库不存在");
        
        // 验证仓库是否允许采购
        if (!"ON".equals(storehouseDtos.get(0).getAllowPurchase())) {
            throw new CmdException(storehouseDtos.get(0).getShName() + "不允许采购");
        }
    }

    /**
     * 紧急采购-仓库物品入库执行方法
     * 处理紧急采购的核心业务逻辑，包括用户验证、采购次数限制检查、采购申请创建、
     * 物资入库、库存调整、调拨记录生成等操作
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求JSON对象
     * @throws CmdException 当业务处理失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 获取用户信息
        String userId = context.getReqHeaders().get("user-id");
        String userName = context.getReqHeaders().get("user-name");
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        userDto.setRow(1);
        userDto.setPage(1);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "用户不存在");
        userName = userDtos.get(0).getName();
        String storeId = context.getReqHeaders().get("store-id");
        
        // 构建采购申请查询条件，用于检查当月紧急采购次数
        PurchaseApplyDto purchaseApplyDto = new PurchaseApplyDto();
        purchaseApplyDto.setResOrderType(reqJson.getString("resOrderType"));
        // 设置入库方式为紧急采购
        purchaseApplyDto.setWarehousingWay(PurchaseApplyDto.WAREHOUSING_TYPE_URGENT);
        purchaseApplyDto.setCommunityId(reqJson.getString("communityId"));
        purchaseApplyDto.setUserId(userId);
        // 设置紧急采购标识，查询当月数据
        purchaseApplyDto.setUrgentFlag("1");
        
        // 查询当月紧急采购次数
        int count = purchaseApplyInnerServiceSMOImpl.queryPurchaseApplysCount(purchaseApplyDto);
        
        // 获取紧急采购次数限制配置
        String value = CommunitySettingFactory.getValue(purchaseApplyDto.getCommunityId(), URGRNT_NUMBER);
        if (StringUtil.isEmpty(value)) {
            value = MappingCache.getValue(DOMAIN_COMMON, URGRNT_NUMBER);
        }
        if (StringUtil.isEmpty(value)) {
            throw new IllegalArgumentException("映射值为空！");
        }
        int number = Integer.parseInt(value);
        
        // 检查紧急采购次数是否超限
        if (count >= number) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "本月紧急采购次数已超过" + number + "次，请下月再使用！");
            context.setResponseEntity(responseEntity);
            return;
        }
        
        // 创建采购申请主记录
        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
        purchaseApplyPo.setApplyOrderId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
        purchaseApplyPo.setDescription(reqJson.getString("description"));
        purchaseApplyPo.setUserId(userId);
        purchaseApplyPo.setUserName(userName);
        purchaseApplyPo.setEndUserName(reqJson.getString("endUserName"));
        purchaseApplyPo.setEndUserTel(reqJson.getString("endUserTel"));
        purchaseApplyPo.setStoreId(storeId);
        purchaseApplyPo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_ENTER);
        purchaseApplyPo.setState(PurchaseApplyDto.STATE_END);
        purchaseApplyPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        purchaseApplyPo.setDescription(reqJson.getString("description"));
        purchaseApplyPo.setCreateUserId(userId);
        purchaseApplyPo.setCreateUserName(userName);
        purchaseApplyPo.setWarehousingWay(PurchaseApplyDto.WAREHOUSING_TYPE_URGENT);
        purchaseApplyPo.setCommunityId(reqJson.getString("communityId"));
        
        // 处理采购物资明细
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        List<PurchaseApplyDetailPo> purchaseApplyDetailPos = new ArrayList<>();
        
        for (int resourceStoreIndex = 0; resourceStoreIndex < resourceStores.size(); resourceStoreIndex++) {
            JSONObject resourceStore = resourceStores.getJSONObject(resourceStoreIndex);
            
            // 创建采购申请明细记录
            PurchaseApplyDetailPo purchaseApplyDetailPo = BeanConvertUtil.covertBean(resourceStore, PurchaseApplyDetailPo.class);
            purchaseApplyDetailPo.setId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
            purchaseApplyDetailPo.setPurchaseQuantity(resourceStore.get("quantity").toString());
            purchaseApplyDetailPo.setPurchaseRemark(resourceStore.get("remark").toString());
            purchaseApplyDetailPo.setOriginalStock(resourceStore.getString("stock"));
            purchaseApplyDetailPo.setPrice(resourceStore.getString("urgentPrice"));
            purchaseApplyDetailPos.add(purchaseApplyDetailPo);
            
            // 查询目标仓库中是否已存在该物资
            ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
            resourceStoreDto.setResCode(resourceStore.getString("resCode"));
            if (StringUtil.isEmpty(resourceStore.getString("shzId"))) {
                resourceStore.put("shzId", resourceStore.getString("shId"));
            }
            resourceStoreDto.setShId(resourceStore.getString("shzId")); // 小区目标仓库
            List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
            
            // 调整集团仓库物品信息（价格、库存等）
            ResourceStorePo resourceStorePo = new ResourceStorePo();
            resourceStorePo.setResId(purchaseApplyDetailPo.getResId());
            resourceStorePo.setPurchasePrice(resourceStore.getString("urgentPrice"));
            resourceStorePo.setStock(purchaseApplyDetailPo.getQuantity());
            resourceStorePo.setResOrderType(PurchaseApplyDto.WAREHOUSING_TYPE_URGENT);
            resourceStorePo.setOperationType(PurchaseApplyDto.WEIGHTED_MEAN_TRUE);
            resourceStorePo.setAdjustmentType("1001");
            resourceStoreInnerServiceSMOImpl.updateResourceStore(resourceStorePo);
            
            // 保存至物品批次表（调整原仓库批次）
            ResourceStoreTimesPo resourceStoreTimesPo1 = new ResourceStoreTimesPo();
            resourceStoreTimesPo1.setApplyOrderId(purchaseApplyPo.getApplyOrderId());
            resourceStoreTimesPo1.setPrice(purchaseApplyDetailPo.getPrice());// 采购价
            resourceStoreTimesPo1.setResCode(resourceStore.getString("resCode"));
            resourceStoreTimesPo1.setStock("0");
            resourceStoreTimesPo1.setStoreId(storeId);
            resourceStoreTimesPo1.setShId(resourceStore.getString("shId"));
            resourceStoreTimesPo1.setCommunityId(reqJson.getString("communityId"));
            resourceStoreTimesV1InnerServiceSMOImpl.saveOrUpdateResourceStoreTimes(resourceStoreTimesPo1);
            
            AllocationStorehouseDto allocationStorehouseDto = new AllocationStorehouseDto();
            
            if (resourceStoreDtos != null && resourceStoreDtos.size() == 1) {
                // 目标仓库已存在该物品的情况
                
                // 生成调拨详情记录
                allocationStorehouseDto.setAsId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_allocationStorehouseId));
                allocationStorehouseDto.setbId("-1");
                allocationStorehouseDto.setShIda(resourceStore.getString("shId"));// 原仓库
                allocationStorehouseDto.setShIdz(resourceStore.getString("shzId"));// 目标仓库
                allocationStorehouseDto.setResId(resourceStoreDtos.get(0).getResId());
                allocationStorehouseDto.setResName(resourceStoreDtos.get(0).getResName());
                allocationStorehouseDto.setStoreId(storeId);
                allocationStorehouseDto.setStock(purchaseApplyDetailPo.getQuantity());
                allocationStorehouseDto.setStartUserId(userId);
                allocationStorehouseDto.setStartUserName(userName);
                allocationStorehouseDto.setCreateTime(new Date());
                allocationStorehouseDto.setRemark("紧急采购入库并调拨");
                allocationStorehouseDto.setApplyId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyId));
                allocationStorehouseDto.setOriginalStock(resourceStoreDtos.get(0).getStock());
                allocationStorehouseInnerServiceSMOImpl.saveAllocationStorehouses(allocationStorehouseDto);
                
                // 生成调拨申请记录
                AllocationStorehouseApplyDto allocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
                allocationStorehouseApplyDto.setApplyId(allocationStorehouseDto.getApplyId());
                allocationStorehouseApplyDto.setbId("-1");
                allocationStorehouseApplyDto.setStartUserId(allocationStorehouseDto.getStartUserId());
                allocationStorehouseApplyDto.setStartUserName(allocationStorehouseDto.getStartUserName());
                allocationStorehouseApplyDto.setRemark("紧急采购入库并调拨");
                allocationStorehouseApplyDto.setApplyCount(purchaseApplyDetailPo.getQuantity());
                allocationStorehouseApplyDto.setStoreId(storeId);
                allocationStorehouseApplyDto.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                allocationStorehouseApplyDto.setState(AllocationStorehouseDto.STATE_SUCCESS);
                allocationStorehouseApplyDto.setCommunityId(reqJson.getString("communityId"));
                // 设置调拨类型
                allocationStorehouseApplyDto.setApplyType("30000");
                allocationStorehouseApplyInnerServiceSMOImpl.saveAllocationStorehouseApplys(allocationStorehouseApplyDto);

                // 生成被调拨详情明细记录
                AllocationStorehouseDto allocationStorehouseDto1 = allocationStorehouseDto;
                allocationStorehouseDto1.setAsId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_allocationStorehouseId));
                allocationStorehouseDto1.setApplyId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyId));
                allocationStorehouseDto1.setResId(resourceStore.getString("resId"));
                allocationStorehouseDto1.setCreateTime(new Date());
                allocationStorehouseInnerServiceSMOImpl.saveAllocationStorehouses(allocationStorehouseDto1);

                // 加入被调拨申请记录
                AllocationStorehouseApplyDto allocationStorehouseApplyDto1 = allocationStorehouseApplyDto;
                allocationStorehouseApplyDto1.setApplyId(allocationStorehouseDto1.getApplyId());
                allocationStorehouseApplyDto1.setApplyType("40000");// 被调拨记录
                allocationStorehouseApplyDto1.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                allocationStorehouseApplyDto1.setCommunityId(resourceStore.getString("communityId"));
                allocationStorehouseApplyInnerServiceSMOImpl.saveAllocationStorehouseApplys(allocationStorehouseApplyDto1);

                // 调整小区仓库物品均价、数量
                ResourceStoreDto resourceStoreDto2 = new ResourceStoreDto();
                resourceStoreDto2.setResCode(resourceStore.getString("resCode"));
                resourceStoreDto2.setResId(purchaseApplyDetailPo.getResId());
                List<ResourceStoreDto> resourceStoreDtoList = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto2);
                ResourceStorePo resourceStorePo1 = new ResourceStorePo();
                resourceStorePo1.setPurchasePrice(resourceStore.getString("urgentPrice"));
                resourceStorePo1.setResId(resourceStoreDtos.get(0).getResId());
                resourceStorePo1.setStock(purchaseApplyDetailPo.getQuantity());
                resourceStorePo1.setAveragePrice(resourceStoreDtoList.get(0).getAveragePrice());
                resourceStorePo1.setPrice(resourceStoreDtoList.get(0).getPrice());
                resourceStorePo1.setResOrderType(PurchaseApplyDto.WAREHOUSING_TYPE_URGENT);
                resourceStorePo1.setOperationType(PurchaseApplyDto.WEIGHTED_MEAN_FALSE);
                
                // 计算最小计量总数
                // 获取紧急采购数量
                BigDecimal purchaseQuantity = new BigDecimal(purchaseApplyDetailPo.getQuantity());
                // 获取原有最小计量总数
                if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniStock())) {
                    throw new IllegalArgumentException("最小计量总数不能为空！");
                }
                BigDecimal miniStock = new BigDecimal(resourceStoreDtos.get(0).getMiniStock());
                // 获取最小计量单位数量
                if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniUnitStock())) {
                    throw new IllegalArgumentException("最小计量单位数量不能为空！");
                }
                BigDecimal miniUnitStock = new BigDecimal(resourceStoreDtos.get(0).getMiniUnitStock());
                // 计算新的最小计量总数 = 采购数量 × 最小计量单位数量 + 原有最小计量总数
                BigDecimal newMiniStock = purchaseQuantity.multiply(miniUnitStock).add(miniStock);
                resourceStorePo1.setMiniStock(String.valueOf(newMiniStock));
                resourceStoreInnerServiceSMOImpl.updateResourceStore(resourceStorePo1);
                
            } else if (resourceStoreDtos != null && resourceStoreDtos.size() > 1) {
                // 查询到多个相同物资记录，数据异常
                throw new IllegalArgumentException("查询商品错误！");
            } else {
                // 目标仓库不存在该物品的情况，需要新增物资记录
                
                // 生成新的物资ID
                String newResId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_resId);
                
                // 生成调拨详情记录
                allocationStorehouseDto.setAsId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_allocationStorehouseId));
                allocationStorehouseDto.setbId("-1");
                allocationStorehouseDto.setShIda(resourceStore.getString("shId"));
                allocationStorehouseDto.setShIdz(resourceStore.getString("shzId"));// 小区目标仓库
                allocationStorehouseDto.setResId(newResId);
                allocationStorehouseDto.setResName(resourceStore.getString("resName"));
                allocationStorehouseDto.setStoreId(storeId);
                allocationStorehouseDto.setStock(purchaseApplyDetailPo.getQuantity());
                allocationStorehouseDto.setStartUserId(userId);
                allocationStorehouseDto.setStartUserName(userName);
                allocationStorehouseDto.setCreateTime(new Date());
                allocationStorehouseDto.setRemark("紧急采购入库并调拨");
                allocationStorehouseDto.setApplyId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyId));
                allocationStorehouseDto.setOriginalStock("0");
                allocationStorehouseInnerServiceSMOImpl.saveAllocationStorehouses(allocationStorehouseDto);
                
                // 生成调拨申请记录
                AllocationStorehouseApplyDto allocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
                allocationStorehouseApplyDto.setApplyId(allocationStorehouseDto.getApplyId());
                allocationStorehouseApplyDto.setbId("-1");
                allocationStorehouseApplyDto.setStartUserId(allocationStorehouseDto.getStartUserId());
                allocationStorehouseApplyDto.setStartUserName(allocationStorehouseDto.getStartUserName());
                allocationStorehouseApplyDto.setRemark("紧急采购入库并调拨");
                allocationStorehouseApplyDto.setApplyCount(purchaseApplyDetailPo.getQuantity());
                allocationStorehouseApplyDto.setStoreId(storeId);
                allocationStorehouseApplyDto.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                allocationStorehouseApplyDto.setState(AllocationStorehouseDto.STATE_SUCCESS);
                allocationStorehouseApplyDto.setCommunityId(reqJson.getString("communityId"));
                // 设置调拨类型
                allocationStorehouseApplyDto.setApplyType("30000");
                allocationStorehouseApplyInnerServiceSMOImpl.saveAllocationStorehouseApplys(allocationStorehouseApplyDto);

                // 加入被调拨详情记录
                AllocationStorehouseDto allocationStorehouseDto1 = allocationStorehouseDto;
                allocationStorehouseDto1.setAsId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_allocationStorehouseId));
                allocationStorehouseDto1.setApplyId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyId));
                allocationStorehouseDto1.setResId(resourceStore.getString("resId"));
                allocationStorehouseDto1.setCreateTime(new Date());
                allocationStorehouseInnerServiceSMOImpl.saveAllocationStorehouses(allocationStorehouseDto1);

                // 加入被调拨申请记录
                AllocationStorehouseApplyDto allocationStorehouseApplyDto1 = allocationStorehouseApplyDto;
                allocationStorehouseApplyDto1.setApplyId(allocationStorehouseDto1.getApplyId());
                allocationStorehouseApplyDto1.setApplyType("40000");// 被调拨记录
                allocationStorehouseApplyDto1.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
                allocationStorehouseApplyDto1.setCommunityId(resourceStore.getString("communityId"));
                allocationStorehouseApplyInnerServiceSMOImpl.saveAllocationStorehouseApplys(allocationStorehouseApplyDto1);

                // 小区仓库进行入库插入（新增物资记录）
                ResourceStoreDto resourceStoreDto2 = new ResourceStoreDto();
                resourceStoreDto2.setResCode(resourceStore.getString("resCode"));
                resourceStoreDto2.setResId(purchaseApplyDetailPo.getResId());
                List<ResourceStoreDto> resourceStoreDtoList = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto2);
                ResourceStoreDto resourceStoreDto1 = BeanConvertUtil.covertBean(resourceStoreDtoList.get(0), ResourceStoreDto.class);
                resourceStoreDto1.setResId(newResId);
                resourceStoreDto1.setShId(resourceStore.getString("shzId"));
                resourceStoreDto1.setbId("-1");
                resourceStoreDto1.setStock(purchaseApplyDetailPo.getQuantity());
                resourceStoreDto1.setCreateTime(new Date());
                resourceStoreDto1.setCommunityId(resourceStoreTimesPo1.getCommunityId());
                
                // 计算最小计量总数
                // 获取紧急采购数量
                BigDecimal purchaseQuantity = new BigDecimal(purchaseApplyDetailPo.getQuantity());
                // 获取最小计量单位数量
                if (StringUtil.isEmpty(resourceStore.getString("miniUnitStock"))) {
                    throw new IllegalArgumentException("最小计量单位数量不能为空！");
                }
                BigDecimal miniUnitStock = new BigDecimal(resourceStore.getString("miniUnitStock"));
                // 计算最小计量总数 = 采购数量 × 最小计量单位数量
                BigDecimal miniStock = purchaseQuantity.multiply(miniUnitStock);
                resourceStoreDto1.setMiniStock(String.valueOf(miniStock));
                resourceStoreInnerServiceSMOImpl.saveResourceStore(resourceStoreDto1);
            }
            
            // 查询模板仓库所属CommunityId
            StorehouseDto storehouseDto = new StorehouseDto();
            storehouseDto.setShId(resourceStore.getString("shzId"));
            List<StorehouseDto> storehouseDtoList = storehouseV1InnerServiceSMOImpl.queryStorehouses(storehouseDto);
            String storehouseCommunityId = "";
            if (storehouseDtoList.size() > 0) {
                storehouseCommunityId = storehouseDtoList.get(0).getCommunityId();
            }
            
            // 保存至物品批次表（调整目标仓库批次）
            ResourceStoreTimesPo resourceStoreTimesPo = new ResourceStoreTimesPo();
            resourceStoreTimesPo.setApplyOrderId(allocationStorehouseDto.getApplyId());
            resourceStoreTimesPo.setPrice(purchaseApplyDetailPo.getPrice());
            resourceStoreTimesPo.setResCode(resourceStore.getString("resCode"));
            resourceStoreTimesPo.setStock(purchaseApplyDetailPo.getQuantity());
            resourceStoreTimesPo.setStoreId(storeId);
            resourceStoreTimesPo.setShId(resourceStore.getString("shzId"));
            resourceStoreTimesPo.setCommunityId(storehouseCommunityId);
            resourceStoreTimesV1InnerServiceSMOImpl.saveOrUpdateResourceStoreTimes(resourceStoreTimesPo);
        }
        
        // 设置采购申请明细并执行采购申请
        purchaseApplyPo.setPurchaseApplyDetailPos(purchaseApplyDetailPos);
        ResponseEntity responseEntity = purchaseApplyBMOImpl.apply(purchaseApplyPo, reqJson);
        context.setResponseEntity(responseEntity);
    }
}