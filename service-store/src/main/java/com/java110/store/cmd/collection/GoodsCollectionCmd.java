package com.java110.store.cmd.collection;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.oaWorkflow.OaWorkflowDto;
import com.java110.dto.purchase.PurchaseApplyDetailDto;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.dto.resource.ResourceStoreDto;
import com.java110.dto.resource.ResourceStoreTimesDto;
import com.java110.dto.store.StorehouseDto;
import com.java110.dto.user.UserDto;
import com.java110.dto.user.UserStorehouseDto;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowInnerServiceSMO;
import com.java110.intf.store.*;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.purchase.PurchaseApplyDetailPo;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resource.ResourceStoreTimesPo;
import com.java110.po.user.UserStorehousePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 物品领用申请命令类
 * 负责处理物品领用申请的业务逻辑，包括参数验证、申请单创建、库存检查、工作流启动等功能
 * 支持直接出库和审批流程两种处理方式
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "/collection/goodsCollection")
public class GoodsCollectionCmd extends Cmd {

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    @Autowired
    private IStorehouseV1InnerServiceSMO storehouseV1InnerServiceSMOImpl;

    @Autowired
    private IOaWorkflowInnerServiceSMO oaWorkflowInnerServiceSMOImpl;

    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowActivitiInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyDetailInnerServiceSMO purchaseApplyDetailInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    @Autowired
    private IUserStorehouseInnerServiceSMO userStorehouseInnerServiceSMOImpl;

    /**
     * 验证请求参数的有效性
     * 检查必填字段、物品库存、仓库信息和审批流程配置
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求JSON数据
     * @throws CmdException    当命令执行异常时抛出
     * @throws ParseException  当数据解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "resourceStores", "必填，请填写物品领用的物资");
        Assert.hasKeyAndValue(reqJson, "description", "必填，请填写采购申请说明");
        Assert.hasKeyAndValue(reqJson, "shId", "必填，请填写采购申请说明");
        Assert.hasKeyAndValue(reqJson, "endUserName", "必填，请填写采购联系人");
        Assert.hasKeyAndValue(reqJson, "endUserTel", "必填，请填写采购联系电话");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        
        // 验证物品列表
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        String storeId = CmdContextUtils.getStoreId(context);
        if (resourceStores == null || resourceStores.size() < 1) {
            throw new IllegalArgumentException("未包含领用物品");
        }
        
        // 验证每个物品的库存和价格信息
        double quanitity = 0;
        double stock = 0;
        for (int resourceStoreIndex = 0; resourceStoreIndex < resourceStores.size(); resourceStoreIndex++) {
            JSONObject resourceStore = resourceStores.getJSONObject(resourceStoreIndex);
            Assert.hasKeyAndValue(resourceStore, "timesId", "必填，未选择价格");
            
            // 查询价格信息
            ResourceStoreTimesDto resourceStoreTimesDto = new ResourceStoreTimesDto();
            resourceStoreTimesDto.setTimesId(resourceStore.getString("timesId"));
            resourceStoreTimesDto.setStoreId(storeId);
            List<ResourceStoreTimesDto> resourceStoreTimesDtos = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimess(resourceStoreTimesDto);
            Assert.listOnlyOne(resourceStoreTimesDtos, "价格不存在");
            
            // 验证申请数量
            if(StringUtil.isEmpty(resourceStore.getString("quantity"))){
                throw new IllegalArgumentException("申请数量不能为空");
            }
            quanitity = Double.parseDouble(resourceStore.getString("quantity"));
            if (quanitity <= 0) {
                throw new IllegalArgumentException("申请数量不正确");
            }
            
            // 验证库存是否充足
            stock = Double.parseDouble(resourceStoreTimesDtos.get(0).getStock());
            if (quanitity > stock) {
                throw new IllegalArgumentException("该批次价格下库存数量不足！");
            }
            resourceStore.put("resourceStoreTimesDtos", resourceStoreTimesDtos);
        }
        
        // 验证仓库信息
        StorehouseDto storehouseDto = new StorehouseDto();
        storehouseDto.setShId(reqJson.getString("shId"));
        List<StorehouseDto> storehouseDtos = storehouseV1InnerServiceSMOImpl.queryStorehouses(storehouseDto);
        Assert.listOnlyOne(storehouseDtos, "仓库不存在");
        
        // 如果仓库不使用审批流程，直接返回
        if (!StorehouseDto.SWITCH_ON.equals(storehouseDtos.get(0).getUseSwitch())) {
            return;
        }
        
        // 验证审批流程配置
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setStoreId(storeId);
        oaWorkflowDto.setFlowId(storehouseDtos.get(0).getUseFlowId());
        List<OaWorkflowDto> oaWorkflowDtos = oaWorkflowInnerServiceSMOImpl.queryOaWorkflows(oaWorkflowDto);
        Assert.listOnlyOne(oaWorkflowDtos, "流程不存在");
        
        // 验证流程状态和部署情况
        if (!OaWorkflowDto.STATE_COMPLAINT.equals(oaWorkflowDtos.get(0).getState())) {
            throw new IllegalArgumentException(oaWorkflowDtos.get(0).getFlowName() + "流程未部署");
        }
        if (StringUtil.isEmpty(oaWorkflowDtos.get(0).getProcessDefinitionKey())) {
            throw new IllegalArgumentException(oaWorkflowDtos.get(0).getFlowName() + "流程未部署");
        }
    }

    /**
     * 执行物品领用申请命令
     * 创建领用申请单，处理物品信息，启动审批流程或直接出库
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求JSON数据
     * @throws CmdException    当命令执行异常时抛出
     * @throws ParseException  当数据解析异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        String storeId = context.getReqHeaders().get("store-id");
        String userId = context.getReqHeaders().get("user-id");
        String acceptStaffId = reqJson.getString("acceptStaffId");
        
        // 如果指定了受理人员，使用指定人员ID
        if (!StringUtil.isEmpty(acceptStaffId)) {
            userId = acceptStaffId;
        }
        
        // 查询仓库信息
        StorehouseDto storehouseDto = new StorehouseDto();
        storehouseDto.setShId(reqJson.getString("shId"));
        List<StorehouseDto> storehouseDtos = storehouseV1InnerServiceSMOImpl.queryStorehouses(storehouseDto);
        Assert.listOnlyOne(storehouseDtos, "仓库不存在");
        
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "未包含用户");
        String userName = userDtos.get(0).getName();
        
        // 创建采购申请单
        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
        purchaseApplyPo.setApplyOrderId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
        purchaseApplyPo.setDescription(reqJson.getString("description"));
        purchaseApplyPo.setUserId(userId);
        purchaseApplyPo.setUserName(userName);
        purchaseApplyPo.setEndUserName(reqJson.getString("endUserName"));
        purchaseApplyPo.setEndUserTel(reqJson.getString("endUserTel"));
        purchaseApplyPo.setStoreId(storeId);
        purchaseApplyPo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_OUT); // 出库类型
        purchaseApplyPo.setState(PurchaseApplyDto.STATE_WAIT_DEAL); // 待处理状态
        purchaseApplyPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        purchaseApplyPo.setCreateUserId(userId);
        purchaseApplyPo.setCreateUserName(userName);
        purchaseApplyPo.setWarehousingWay(PurchaseApplyDto.WAREHOUSING_TYPE_APPLY); // 申请入库方式
        
        // 如果仓库设置为直接出库，修改入库方式
        if (!StringUtil.isEmpty(reqJson.getString("useSwitch")) && StorehouseDto.SWITCH_OFF.equals(reqJson.getString("useSwitch"))) {
            purchaseApplyPo.setWarehousingWay(PurchaseApplyDto.WAREHOUSING_TYPE_DIRECT); // 直接入库方式
        }
        purchaseApplyPo.setCommunityId(reqJson.getString("communityId"));
        
        // 处理物品详细信息
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        List<PurchaseApplyDetailPo> purchaseApplyDetailPos = new ArrayList<>();
        List<ResourceStoreTimesDto> resourceStoreTimesDtos = null;
        
        for (int resourceStoreIndex = 0; resourceStoreIndex < resourceStores.size(); resourceStoreIndex++) {
            JSONObject resourceStore = resourceStores.getJSONObject(resourceStoreIndex);
            resourceStoreTimesDtos = (List<ResourceStoreTimesDto>) resourceStore.get("resourceStoreTimesDtos");
            resourceStore.put("originalStock", resourceStore.get("stock"));
            
            // 转换物品信息为采购申请详情对象
            PurchaseApplyDetailPo purchaseApplyDetailPo = BeanConvertUtil.covertBean(resourceStore, PurchaseApplyDetailPo.class);
            purchaseApplyDetailPo.setId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
            purchaseApplyDetailPo.setPrice(resourceStoreTimesDtos.get(0).getPrice());
            purchaseApplyDetailPo.setTimesId(resourceStoreTimesDtos.get(0).getTimesId());
            purchaseApplyDetailPo.setOriginalStock(resourceStoreTimesDtos.get(0).getStock());
            purchaseApplyDetailPo.setPurchaseQuantity(resourceStore.getString("quantity"));
            
            // 获取批次采购参考价格
            String consultPrice = null;
            JSONArray timeList = resourceStore.getJSONArray("times");
            if (resourceStore.containsKey("timesId") && !StringUtil.isEmpty(resourceStore.getString("timesId"))) {
                for (int timesIndex = 0; timesIndex < timeList.size(); timesIndex++) {
                    JSONObject times = timeList.getJSONObject(timesIndex);
                    if (times.getString("timesId").toString().equals(resourceStore.getString("timesId").toString())) {
                        consultPrice = times.getString("price");
                    }
                }
            }
            purchaseApplyDetailPo.setConsultPrice(consultPrice);
            purchaseApplyDetailPos.add(purchaseApplyDetailPo);
        }
        
        purchaseApplyPo.setPurchaseApplyDetailPos(purchaseApplyDetailPos);
        
        // 保存采购申请单
        int saveFlag = purchaseApplyInnerServiceSMOImpl.savePurchaseApply(purchaseApplyPo);
        if (saveFlag < 1) {
            throw new CmdException("物品领用申请失败");
        }
        
        // 转换对象并设置审批相关信息
        PurchaseApplyDto purchaseApplyDto = BeanConvertUtil.covertBean(purchaseApplyPo, PurchaseApplyDto.class);
        purchaseApplyDto.setCurrentUserId(purchaseApplyPo.getUserId());
        purchaseApplyDto.setNextStaffId(reqJson.getString("staffId"));
        
        // 如果仓库设置为直接出库，更新开关状态
        if (!StringUtil.isEmpty(reqJson.getString("useSwitch")) && StorehouseDto.SWITCH_OFF.equals(reqJson.getString("useSwitch"))) {
            storehouseDtos.get(0).setUseSwitch(StorehouseDto.SWITCH_OFF);
        }
        
        // 启动审批流程
        toStartWorkflow(purchaseApplyDto, storehouseDtos.get(0), reqJson);
        context.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_OK, "物品领用成功"));
    }

    /**
     * 启动审批流程
     * 根据仓库配置决定是启动OA审批流程还是直接出库
     *
     * @param purchaseApplyDto 采购申请数据传输对象
     * @param storehouseDto    仓库数据传输对象
     * @param reqJson          请求JSON数据
     */
    private void toStartWorkflow(PurchaseApplyDto purchaseApplyDto, StorehouseDto storehouseDto, JSONObject reqJson) {
        // 如果仓库不使用审批流程，直接出库
        if (!StorehouseDto.SWITCH_ON.equals(storehouseDto.getUseSwitch())) {
            toPurchaseOutStorehouse(purchaseApplyDto, storehouseDto, reqJson);
            return;
        }
        
        // 查询OA工作流信息
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setStoreId(purchaseApplyDto.getStoreId());
        oaWorkflowDto.setFlowId(storehouseDto.getUseFlowId());
        List<OaWorkflowDto> oaWorkflowDtos = oaWorkflowInnerServiceSMOImpl.queryOaWorkflows(oaWorkflowDto);
        
        // 准备流程启动参数
        JSONObject flowJson = new JSONObject();
        flowJson.put("processDefinitionKey", oaWorkflowDtos.get(0).getProcessDefinitionKey());
        flowJson.put("createUserId", purchaseApplyDto.getCurrentUserId());
        flowJson.put("flowId", oaWorkflowDtos.get(0).getFlowId());
        flowJson.put("id", purchaseApplyDto.getApplyOrderId());
        flowJson.put("auditMessage", "提交审核");
        flowJson.put("storeId", purchaseApplyDto.getStoreId());
        reqJson.put("processDefinitionKey", oaWorkflowDtos.get(0).getProcessDefinitionKey());
        
        // 启动流程
        JSONObject result = oaWorkflowActivitiInnerServiceSMOImpl.startProcess(flowJson);
        
        // 获取下一处理人信息
        JSONObject audit = reqJson.getJSONObject("audit");
        String nextUserId = "-1";
        if (audit != null) {
            String staffId = reqJson.getJSONObject("audit").getString("staffId");
            if (!StringUtil.isEmpty(staffId)) {
                nextUserId = reqJson.getJSONObject("audit").getString("staffId");
            } else {
                nextUserId = reqJson.getJSONObject("audit").getString("assignee");
            }
        }
        
        // 自动完成第一个任务
        flowJson = new JSONObject();
        flowJson.put("processInstanceId", result.getString("processInstanceId"));
        flowJson.put("createUserId", purchaseApplyDto.getCurrentUserId());
        flowJson.put("nextUserId", nextUserId);
        flowJson.put("storeId", purchaseApplyDto.getStoreId());
        flowJson.put("id", purchaseApplyDto.getApplyOrderId());
        flowJson.put("flowId", oaWorkflowDtos.get(0).getFlowId());
        oaWorkflowActivitiInnerServiceSMOImpl.autoFinishFirstTask(flowJson);
    }

    /**
     * 执行出库操作
     * 更新库存信息，记录出库流水，更新个人仓库
     *
     * @param purchaseApplyDto 采购申请数据传输对象
     * @param storehouseDto    仓库数据传输对象
     * @param reqJson          请求JSON数据
     */
    private void toPurchaseOutStorehouse(PurchaseApplyDto purchaseApplyDto, StorehouseDto storehouseDto, JSONObject reqJson) {
        // 查询采购申请详情
        PurchaseApplyDetailDto purchaseApplyDetailDto = new PurchaseApplyDetailDto();
        purchaseApplyDetailDto.setApplyOrderId(purchaseApplyDto.getApplyOrderId());
        purchaseApplyDetailDto.setStoreId(purchaseApplyDto.getStoreId());
        List<PurchaseApplyDetailDto> purchaseApplyDetailDtos = purchaseApplyDetailInnerServiceSMOImpl.queryPurchaseApplyDetails(purchaseApplyDetailDto);
        if (purchaseApplyDetailDtos == null || purchaseApplyDetailDtos.size() < 1) {
            return;
        }
        
        double stock = 0;
        // 处理每个出库物品
        for (PurchaseApplyDetailDto tmpPurchaseApplyDetailDto : purchaseApplyDetailDtos) {
            // 更新物品库存
            ResourceStorePo resourceStorePo = new ResourceStorePo();
            resourceStorePo.setResId(tmpPurchaseApplyDetailDto.getResId());
            resourceStorePo.setPurchasePrice(tmpPurchaseApplyDetailDto.getPrice());
            resourceStorePo.setStock("-" + tmpPurchaseApplyDetailDto.getPurchaseQuantity()); // 出库，库存减少
            resourceStorePo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_OUT); // 出库类型
            
            // 查询物品资源信息
            ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
            resourceStoreDto.setResId(tmpPurchaseApplyDetailDto.getResId());
            resourceStoreDto.setShId(tmpPurchaseApplyDetailDto.getShId());
            List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
            Assert.listOnlyOne(resourceStoreDtos, "查询物品资源信息错误！");
            
            // 验证最小计量单位数量
            if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniUnitStock())) {
                throw new IllegalArgumentException("最小计量单位数量不能为空！");
            }
            
            // 获取最小计量单位数量
            BigDecimal miniUnitStock = new BigDecimal(resourceStoreDtos.get(0).getMiniUnitStock());
            if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniStock())) {
                throw new IllegalArgumentException("最小计量总数不能为空！");
            }
            
            // 更新物品库存
            resourceStoreInnerServiceSMOImpl.updateResourceStore(resourceStorePo);
            
            // 保存出库流水记录
            ResourceStoreTimesPo resourceStoreTimesPo = new ResourceStoreTimesPo();
            resourceStoreTimesPo.setApplyOrderId(tmpPurchaseApplyDetailDto.getApplyOrderId());
            resourceStoreTimesPo.setPrice(tmpPurchaseApplyDetailDto.getPrice());
            resourceStoreTimesPo.setStock("-" + tmpPurchaseApplyDetailDto.getPurchaseQuantity()); // 出库数量
            resourceStoreTimesPo.setResCode(resourceStoreDtos.get(0).getResCode());
            resourceStoreTimesPo.setStoreId(resourceStoreDtos.get(0).getStoreId());
            resourceStoreTimesPo.setTimesId(GenerateCodeFactory.getGeneratorId("10"));
            resourceStoreTimesPo.setShId(tmpPurchaseApplyDetailDto.getShId());
            resourceStoreTimesV1InnerServiceSMOImpl.saveOrUpdateResourceStoreTimes(resourceStoreTimesPo);
            
            // 添加到个人仓库
            addPersonStorehouse(purchaseApplyDto, resourceStoreDtos, tmpPurchaseApplyDetailDto);
        }
        
        // 更新采购申请单状态为已完成
        PurchaseApplyPo purchaseApplyPo = null;
        purchaseApplyPo = new PurchaseApplyPo();
        purchaseApplyPo.setApplyOrderId(purchaseApplyDto.getApplyOrderId());
        purchaseApplyPo.setState(PurchaseApplyDto.STATE_END); // 完成状态
        purchaseApplyPo.setStatusCd("0");
        purchaseApplyInnerServiceSMOImpl.updatePurchaseApply(purchaseApplyPo);
    }

    /**
     * 向个人仓库中添加数据
     * 更新或新增个人仓库中的物品库存信息
     *
     * @param purchaseApplyDto       采购申请数据传输对象
     * @param resourceStoreDtos      物品资源数据传输对象列表
     * @param purchaseApplyDetailDto 采购申请详情数据传输对象
     */
    private void addPersonStorehouse(PurchaseApplyDto purchaseApplyDto, List<ResourceStoreDto> resourceStoreDtos, PurchaseApplyDetailDto purchaseApplyDetailDto) {
        // 获取物品单位信息
        String unitCode = resourceStoreDtos.get(0).getUnitCode();
        String miniUnitCode = resourceStoreDtos.get(0).getMiniUnitCode();
        String miniUnitStock = resourceStoreDtos.get(0).getMiniUnitStock();
        
        // 创建个人仓库记录
        UserStorehousePo userStorehousePo = new UserStorehousePo();
        userStorehousePo.setUsId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_usId));
        userStorehousePo.setResId(resourceStoreDtos.get(0).getResId());
        userStorehousePo.setResCode(resourceStoreDtos.get(0).getResCode());
        userStorehousePo.setResName(resourceStoreDtos.get(0).getResName());
        userStorehousePo.setStoreId(resourceStoreDtos.get(0).getStoreId());
        userStorehousePo.setUserId(purchaseApplyDto.getUserId());
        userStorehousePo.setTimesId(purchaseApplyDetailDto.getTimesId());
        
        // 查询是否已存在个人仓库记录
        UserStorehouseDto userStorehouseDto = new UserStorehouseDto();
        userStorehouseDto.setResCode(resourceStoreDtos.get(0).getResCode());
        userStorehouseDto.setUserId(purchaseApplyDto.getUserId());
        userStorehouseDto.setStoreId(resourceStoreDtos.get(0).getStoreId());
        List<UserStorehouseDto> userStorehouseDtos = userStorehouseInnerServiceSMOImpl.queryUserStorehouses(userStorehouseDto);
        
        if (userStorehouseDtos == null || userStorehouseDtos.size() < 1) {
            // 新增个人仓库记录
            userStorehousePo.setStock(purchaseApplyDetailDto.getPurchaseQuantity());
            
            // 计算最小计量单位库存
            if (!StringUtil.isEmpty(unitCode) && !StringUtil.isEmpty(miniUnitCode) && !StringUtil.isEmpty(miniUnitStock) && !unitCode.equals(miniUnitCode)) {
                BigDecimal purchaseQuantity2 = new BigDecimal(purchaseApplyDetailDto.getPurchaseQuantity());
                BigDecimal miniUnitStock2 = new BigDecimal(miniUnitStock);
                BigDecimal quantity = purchaseQuantity2.multiply(miniUnitStock2);
                userStorehousePo.setMiniStock(String.valueOf(quantity));
            } else {
                userStorehousePo.setMiniStock(purchaseApplyDetailDto.getPurchaseQuantity());
            }
            userStorehouseInnerServiceSMOImpl.saveUserStorehouses(userStorehousePo);
        } else {
            // 更新现有个人仓库记录
            BigDecimal purchaseQuantity3 = new BigDecimal(purchaseApplyDetailDto.getPurchaseQuantity());
            BigDecimal stock3 = new BigDecimal(userStorehouseDtos.get(0).getStock());
            BigDecimal total = purchaseQuantity3.add(stock3); // 累加库存
            userStorehousePo.setStock(total.toString());
            userStorehousePo.setUsId(userStorehouseDtos.get(0).getUsId());
            
            // 计算最小计量单位库存
            if (!StringUtil.isEmpty(unitCode) && !StringUtil.isEmpty(miniUnitCode) && !StringUtil.isEmpty(miniUnitStock) && !unitCode.equals(miniUnitCode)) {
                BigDecimal miniUnitStock3 = new BigDecimal(miniUnitStock);
                BigDecimal quantity = purchaseQuantity3.multiply(miniUnitStock3);
                BigDecimal miniStock = new BigDecimal(0);
                
                // 获取原有最小计量库存
                if (StringUtil.isEmpty(userStorehouseDtos.get(0).getMiniStock())) {
                    throw new IllegalArgumentException("信息错误，个人物品最小计量总数不能为空！");
                } else {
                    miniStock = new BigDecimal(userStorehouseDtos.get(0).getMiniStock());
                }
                
                BigDecimal miniQuantity = quantity.add(miniStock); // 累加最小计量库存
                userStorehousePo.setMiniStock(String.valueOf(miniQuantity));
            } else {
                userStorehousePo.setMiniStock(String.valueOf(total));
            }
            userStorehouseInnerServiceSMOImpl.updateUserStorehouses(userStorehousePo);
        }
    }
}