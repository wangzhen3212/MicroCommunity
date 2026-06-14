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
import com.java110.doc.annotation.*;
import com.java110.dto.purchase.PurchaseApplyDetailDto;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.dto.resource.ResourceStoreDto;
import com.java110.dto.resource.ResourceStoreTimesDto;
import com.java110.dto.user.UserStorehouseDto;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.store.*;
import com.java110.po.purchase.PurchaseApplyDetailPo;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resource.ResourceStoreTimesPo;
import com.java110.po.user.UserStorehousePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购人员入库 功能
 * 请求地址为/app/purchase/resourceEnter
 */

@Java110CmdDoc(title = "领用出库",
        description = "主要用于 仓库管理员领用出库",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/collection/resourceOut",
        resource = "storeDoc",
        author = "吴学文",
        serviceCode = "collection.resourceOut",
        seq = 11
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "applyOrderId", length = 30, remark = "采购申请单订单ID"),
        @Java110ParamDoc(name = "purchaseApplyDetailVo", type = "Array", length = 30, remark = "采购物品信息"),
        @Java110ParamDoc(parentNodeName = "purchaseApplyDetailVo", name = "purchaseQuantity", type = "Double", length = 30, remark = "数量"),
        @Java110ParamDoc(parentNodeName = "purchaseApplyDetailVo", name = "id", type = "String", length = 30, remark = "采购明细ID"),
        @Java110ParamDoc(parentNodeName = "purchaseApplyDetailVo", name = "resId", type = "String", length = 30, remark = "物品ID"),
})

@Java110ResponseDoc(
        params = {
                @Java110ParamDoc(name = "code", type = "int", length = 11, defaultValue = "0", remark = "返回编号，0 成功 其他失败"),
                @Java110ParamDoc(name = "msg", type = "String", length = 250, defaultValue = "成功", remark = "描述"),
        }
)

@Java110ExampleDoc(
        reqBody = "{'applyOrderId':'123123','purchaseApplyDetailVo':[{'purchaseQuantity':'10.00','id':'123123','resId':'343434'}]}",
        resBody = "{'code':0,'msg':'成功'}"
)

/**
 * 物品领用申请出库命令类
 * 负责处理物品领用出库的业务逻辑，包括库存校验、库存扣减、个人仓库更新等操作
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2024
 */
@Java110Cmd(serviceCode = "/collection/resourceOut")
public class ResourceOutCmd extends Cmd {

    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyDetailV1InnerServiceSMO purchaseApplyDetailV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    @Autowired
    private IUserStorehouseInnerServiceSMO userStorehouseInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowUserInnerServiceSMOImpl;

    /**
     * 参数校验方法
     * 验证请求参数的有效性，包括订单ID、领用物品信息、库存数量等
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求JSON数据
     * @throws CmdException 当参数校验失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 校验订单ID是否存在
        Assert.hasKeyAndValue(reqJson, "applyOrderId", "订单ID为空");
        
        // 校验领用物品列表是否存在且不为空
        JSONArray purchaseApplyDetails = reqJson.getJSONArray("purchaseApplyDetailVo");
        if (purchaseApplyDetails == null || purchaseApplyDetails.size() < 1) {
            throw new CmdException("未包含领用物品");
        }
        
        String storeId = CmdContextUtils.getStoreId(context);
        double quanitity = 0;
        double stock = 0;
        
        // 遍历所有领用物品明细进行详细校验
        for (int detailIndex = 0; detailIndex < purchaseApplyDetails.size(); detailIndex++) {
            JSONObject purchaseApplyDetail = purchaseApplyDetails.getJSONObject(detailIndex);
            
            // 校验必填字段
            Assert.hasKeyAndValue(purchaseApplyDetail, "purchaseQuantity", "采购数量未填写");
            Assert.hasKeyAndValue(purchaseApplyDetail, "id", "明细ID为空");
            Assert.hasKeyAndValue(purchaseApplyDetail, "timesId", "价格为空");
            
            // 获取领用数量
            quanitity = Double.parseDouble(purchaseApplyDetail.getString("quantity"));
            
            // 查询物品价格信息
            ResourceStoreTimesDto resourceStoreTimesDto = new ResourceStoreTimesDto();
            resourceStoreTimesDto.setTimesId(purchaseApplyDetail.getString("timesId"));
            resourceStoreTimesDto.setStoreId(storeId);
            List<ResourceStoreTimesDto> resourceStoreTimesDtos = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimess(resourceStoreTimesDto);
            
            // 校验价格信息是否存在且唯一
            Assert.listOnlyOne(resourceStoreTimesDtos, "价格不存在");
            
            // 校验领用数量是否合法
            if (quanitity < 1) {
                throw new CmdException("申请数量不正确");
            }
            
            // 校验库存是否充足
            stock = Double.parseDouble(resourceStoreTimesDtos.get(0).getStock());
            if (quanitity > stock) {
                throw new CmdException(resourceStoreTimesDtos.get(0).getResCode() + "出库不足,库存为=" + stock + ",申请数为=" + quanitity);
            }
        }
    }

    /**
     * 执行领用出库命令
     * 处理物品领用出库的核心业务逻辑，包括库存扣减、个人仓库更新、状态更新等
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求JSON数据
     * @throws CmdException    当业务处理失败时抛出异常
     * @throws ParseException 当数据解析失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取领用物品明细列表
        JSONArray purchaseApplyDetails = reqJson.getJSONArray("purchaseApplyDetailVo");
        List<PurchaseApplyDetailDto> purchaseApplyDetailPos = new ArrayList<>();
        
        // 转换JSON对象为DTO对象
        for (int detailIndex = 0; detailIndex < purchaseApplyDetails.size(); detailIndex++) {
            JSONObject purchaseApplyDetail = purchaseApplyDetails.getJSONObject(detailIndex);
            PurchaseApplyDetailDto purchaseApplyDetailDto = BeanConvertUtil.covertBean(purchaseApplyDetail, PurchaseApplyDetailDto.class);
            purchaseApplyDetailPos.add(purchaseApplyDetailDto);
        }
        
        // 构建采购申请PO对象
        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
        purchaseApplyPo.setApplyOrderId(reqJson.getString("applyOrderId"));
        
        // 查询采购申请信息
        PurchaseApplyDto purchaseApplyDto = new PurchaseApplyDto();
        purchaseApplyDto.setApplyOrderId(purchaseApplyPo.getApplyOrderId());
        List<PurchaseApplyDto> purchaseApplyDtos = purchaseApplyInnerServiceSMOImpl.queryPurchaseApplys(purchaseApplyDto);
        Assert.listOnlyOne(purchaseApplyDtos, "出库单不存在");
        
        // 处理每个领用物品明细
        for (PurchaseApplyDetailDto tmpPurchaseApplyDetailDto : purchaseApplyDetailPos) {
            // 构建库存更新对象
            ResourceStorePo resourceStorePo = new ResourceStorePo();
            resourceStorePo.setResId(tmpPurchaseApplyDetailDto.getResId());
            resourceStorePo.setPurchasePrice(tmpPurchaseApplyDetailDto.getPrice());
            resourceStorePo.setStock("-" + tmpPurchaseApplyDetailDto.getPurchaseQuantity()); // 负号表示出库
            resourceStorePo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_OUT);
            
            // 查询物品资源信息
            ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
            resourceStoreDto.setResId(tmpPurchaseApplyDetailDto.getResId());
            resourceStoreDto.setShId(tmpPurchaseApplyDetailDto.getShId());
            List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
            Assert.listOnlyOne(resourceStoreDtos, "查询物品资源信息错误！");
            
            // 校验最小计量单位数量
            if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniUnitStock())) {
                throw new IllegalArgumentException("最小计量单位数量不能为空！");
            }
            
            // 获取最小计量单位数量
            BigDecimal miniUnitStock = new BigDecimal(resourceStoreDtos.get(0).getMiniUnitStock());
            
            // 校验最小计量总数
            if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniStock())) {
                throw new IllegalArgumentException("最小计量总数不能为空！");
            }
            
            // 获取领用发放的数量
            BigDecimal quantity = new BigDecimal(tmpPurchaseApplyDetailDto.getQuantity());
            
            // 原库存总数
            BigDecimal stock = new BigDecimal(resourceStoreDtos.get(0).getStock());
            
            // 计算领用成功后剩余的库存总数
            BigDecimal newStock = stock.subtract(quantity);
            
            // 计算领用成功后剩余的最小计量总数
            BigDecimal nowMiniStock = newStock.multiply(miniUnitStock);
            resourceStorePo.setMiniStock(String.valueOf(nowMiniStock));
            
            // 更新物品库存
            resourceStoreInnerServiceSMOImpl.updateResourceStore(resourceStorePo);
            
            // 保存至物品times表（出入库记录）
            ResourceStoreTimesPo resourceStoreTimesPo = new ResourceStoreTimesPo();
            resourceStoreTimesPo.setApplyOrderId(tmpPurchaseApplyDetailDto.getApplyOrderId());
            resourceStoreTimesPo.setPrice(tmpPurchaseApplyDetailDto.getPrice());
            resourceStoreTimesPo.setStock("-" + tmpPurchaseApplyDetailDto.getPurchaseQuantity()); // 负号表示出库
            resourceStoreTimesPo.setResCode(resourceStoreDtos.get(0).getResCode());
            resourceStoreTimesPo.setStoreId(resourceStoreDtos.get(0).getStoreId());
            resourceStoreTimesPo.setTimesId(GenerateCodeFactory.getGeneratorId("10")); // 生成唯一ID
            resourceStoreTimesPo.setShId(tmpPurchaseApplyDetailDto.getShId());
            resourceStoreTimesV1InnerServiceSMOImpl.saveOrUpdateResourceStoreTimes(resourceStoreTimesPo);
            
            // 向个人仓库中添加物品
            addPersonStorehouse(purchaseApplyDtos.get(0), resourceStoreDtos, tmpPurchaseApplyDetailDto);
            
            // 更新领用明细表中的实际领用数量
            PurchaseApplyDetailPo purchaseApplyDetailPo = new PurchaseApplyDetailPo();
            purchaseApplyDetailPo.setApplyOrderId(tmpPurchaseApplyDetailDto.getApplyOrderId());
            purchaseApplyDetailPo.setPurchaseQuantity(tmpPurchaseApplyDetailDto.getPurchaseQuantity());
            purchaseApplyDetailV1InnerServiceSMOImpl.updatePurchaseApplyDetail(purchaseApplyDetailPo);
        }
        
        // 获取订单号并更新采购申请状态
        String applyOrderId = purchaseApplyPo.getApplyOrderId();
        PurchaseApplyPo purchaseApply = new PurchaseApplyPo();
        purchaseApply.setApplyOrderId(applyOrderId);
        
        // 处理工作流相关逻辑
        if (reqJson.containsKey("taskId")) {
            reqJson.put("auditCode", "1100");
            reqJson.put("auditMessage", "入库成功");
            reqJson.put("id", reqJson.getString("applyOrderId"));
            reqJson.put("storeId", CmdContextUtils.getStoreId(context));
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            
            // 完成工作流任务
            boolean isLastTask = oaWorkflowUserInnerServiceSMOImpl.completeTask(reqJson);
            if (isLastTask) {
                purchaseApply.setState(PurchaseApplyDto.STATE_END); // 最终状态
            } else {
                purchaseApply.setState(PurchaseApplyDto.STATE_DEALING); // 处理中状态
            }
        } else {
            purchaseApply.setState(PurchaseApplyDto.STATE_AUDITED); // 已审核状态
        }
        
        // 更新采购申请状态
        purchaseApply.setStatusCd("0");
        purchaseApplyInnerServiceSMOImpl.updatePurchaseApply(purchaseApply);
        
        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_OK, "出库成功"));
    }

    /**
     * 向个人仓库中添加数据
     * 处理个人仓库的物品入库逻辑，包括新增物品和更新现有物品数量
     *
     * @param purchaseApplyDto      采购申请DTO对象
     * @param resourceStoreDtos     物品资源信息列表
     * @param purchaseApplyDetailDto 采购申请明细DTO对象
     */
    private void addPersonStorehouse(PurchaseApplyDto purchaseApplyDto, List<ResourceStoreDto> resourceStoreDtos, PurchaseApplyDetailDto purchaseApplyDetailDto) {
        // 获取物品单位信息
        String unitCode = resourceStoreDtos.get(0).getUnitCode();
        String miniUnitCode = resourceStoreDtos.get(0).getMiniUnitCode();
        String miniUnitStock = resourceStoreDtos.get(0).getMiniUnitStock();
        
        // 构建个人仓库PO对象
        UserStorehousePo userStorehousePo = new UserStorehousePo();
        userStorehousePo.setUsId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_usId));
        userStorehousePo.setResId(resourceStoreDtos.get(0).getResId());
        userStorehousePo.setResCode(resourceStoreDtos.get(0).getResCode());
        userStorehousePo.setResName(resourceStoreDtos.get(0).getResName());
        userStorehousePo.setStoreId(resourceStoreDtos.get(0).getStoreId());
        userStorehousePo.setUserId(purchaseApplyDto.getUserId());
        userStorehousePo.setTimesId(purchaseApplyDetailDto.getTimesId());
        
        // 查询物品是否已经存在于个人仓库中
        UserStorehouseDto userStorehouseDto = new UserStorehouseDto();
        userStorehouseDto.setResCode(resourceStoreDtos.get(0).getResCode());
        userStorehouseDto.setUserId(purchaseApplyDto.getUserId());
        userStorehouseDto.setStoreId(resourceStoreDtos.get(0).getStoreId());
        List<UserStorehouseDto> userStorehouseDtos = userStorehouseInnerServiceSMOImpl.queryUserStorehouses(userStorehouseDto);
        
        if (userStorehouseDtos == null || userStorehouseDtos.size() < 1) {
            // 个人仓库中不存在该物品，新增记录
            userStorehousePo.setStock(purchaseApplyDetailDto.getPurchaseQuantity());
            
            // 处理单位转换逻辑
            if (!StringUtil.isEmpty(unitCode) && !StringUtil.isEmpty(miniUnitCode) && 
                !StringUtil.isEmpty(miniUnitStock) && !unitCode.equals(miniUnitCode)) {
                // 计算最小计量单位的总数量
                BigDecimal purchaseQuantity2 = new BigDecimal(purchaseApplyDetailDto.getPurchaseQuantity());
                BigDecimal miniUnitStock2 = new BigDecimal(miniUnitStock);
                BigDecimal quantity = purchaseQuantity2.multiply(miniUnitStock2);
                userStorehousePo.setMiniStock(String.valueOf(quantity));
            } else {
                userStorehousePo.setMiniStock(purchaseApplyDetailDto.getPurchaseQuantity());
            }
            userStorehouseInnerServiceSMOImpl.saveUserStorehouses(userStorehousePo);
        } else {
            // 个人仓库中已存在该物品，更新数量
            BigDecimal purchaseQuantity3 = new BigDecimal(purchaseApplyDetailDto.getPurchaseQuantity());
            BigDecimal stock3 = new BigDecimal(userStorehouseDtos.get(0).getStock());
            BigDecimal total = purchaseQuantity3.add(stock3); // 计算新的总库存
            userStorehousePo.setStock(String.valueOf(total));
            userStorehousePo.setUsId(userStorehouseDtos.get(0).getUsId());
            
            // 处理单位转换逻辑
            if (!StringUtil.isEmpty(unitCode) && !StringUtil.isEmpty(miniUnitCode) && 
                !StringUtil.isEmpty(miniUnitStock) && !unitCode.equals(miniUnitCode)) {
                // 计算本次领取的最小计量总数
                BigDecimal miniUnitStock3 = new BigDecimal(miniUnitStock);
                BigDecimal quantity = purchaseQuantity3.multiply(miniUnitStock3);
                
                BigDecimal miniStock = new BigDecimal(0);
                // 获取原有的最小计量总数
                if (StringUtil.isEmpty(userStorehouseDtos.get(0).getMiniStock())) {
                    throw new IllegalArgumentException("信息错误，个人物品最小计量总数不能为空！");
                } else {
                    miniStock = new BigDecimal(userStorehouseDtos.get(0).getMiniStock());
                }
                
                // 计算新的最小计量总数
                BigDecimal miniQuantity = quantity.add(miniStock);
                userStorehousePo.setMiniStock(String.valueOf(miniQuantity));
            } else {
                userStorehousePo.setMiniStock(String.valueOf(total));
            }
            userStorehouseInnerServiceSMOImpl.updateUserStorehouses(userStorehousePo);
        }
    }
}