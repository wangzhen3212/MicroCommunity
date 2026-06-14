package com.java110.store.cmd.purchase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.doc.annotation.*;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.dto.resource.ResourceStoreDto;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyDetailInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyInnerServiceSMO;
import com.java110.intf.store.IResourceStoreInnerServiceSMO;
import com.java110.intf.store.IResourceStoreTimesV1InnerServiceSMO;
import com.java110.po.purchase.PurchaseApplyDetailPo;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resource.ResourceStoreTimesPo;
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

@Java110CmdDoc(title = "采购入库",
        description = "主要用于 采购人员入库",
        httpMethod = "post",
        url = "http://{ip}:{port}/app/purchase/resourceEnter",
        resource = "storeDoc",
        author = "吴学文",
        serviceCode = "purchase.resourceEnter",
        seq = 10
)

@Java110ParamsDoc(params = {
        @Java110ParamDoc(name = "applyOrderId", length = 30, remark = "采购申请单订单ID"),
        @Java110ParamDoc(name = "purchaseApplyDetailVo", type = "Array", length = 30, remark = "采购物品信息"),
        @Java110ParamDoc(parentNodeName = "purchaseApplyDetailVo", name = "purchaseQuantity", type = "Int", length = 30, remark = "数量"),
        @Java110ParamDoc(parentNodeName = "purchaseApplyDetailVo", name = "price", type = "String", length = 30, remark = "价格"),
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
        reqBody = "{'applyOrderId':'123123','purchaseApplyDetailVo':[{'purchaseQuantity':'10','price':'1.3','id':'123123','resId':'343434'}]}",
        resBody = "{'code':0,'msg':'成功'}"
)

/**
 * 采购入库命令类
 * 负责处理采购人员入库的业务逻辑，包括采购申请单的验证、物品库存更新、采购明细处理等
 * 该类继承自Cmd基类，实现了采购入库的完整业务流程
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "/purchase/resourceEnter")
public class ResourceEnterCmd extends Cmd {

    /**
     * 采购申请服务接口
     */
    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    /**
     * 采购申请明细服务接口
     */
    @Autowired
    private IPurchaseApplyDetailInnerServiceSMO purchaseApplyDetailInnerServiceSMOImpl;

    /**
     * 物品库存服务接口
     */
    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    /**
     * 物品库存记录服务接口
     */
    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    /**
     * 工作流服务接口
     */
    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowUserInnerServiceSMOImpl;

    /**
     * 验证采购入库请求参数
     * 检查订单ID是否存在，验证订单状态是否可入库，检查采购明细数据的完整性
     *
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求JSON数据
     * @throws CmdException 当验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证订单ID是否为空
        Assert.hasKeyAndValue(reqJson, "applyOrderId", "订单ID为空");
        
        // 查询采购申请单信息
        PurchaseApplyDto purchaseApplyDto = new PurchaseApplyDto();
        purchaseApplyDto.setApplyOrderId(reqJson.getString("applyOrderId"));
        purchaseApplyDto.setStatusCd("0");
        List<PurchaseApplyDto> purchaseApplyDtoList = purchaseApplyInnerServiceSMOImpl.queryPurchaseApplys(purchaseApplyDto);
        
        // 检查订单是否已经处理过
        if (purchaseApplyDtoList != null && PurchaseApplyDto.STATE_AUDITED.equals(purchaseApplyDtoList.get(0).getState())) {
            throw new IllegalArgumentException("该订单已经处理，请刷新确认订单状态！");
        }
        
        // 验证采购明细数据
        JSONArray purchaseApplyDetails = reqJson.getJSONArray("purchaseApplyDetailVo");
        for (int detailIndex = 0; detailIndex < purchaseApplyDetails.size(); detailIndex++) {
            JSONObject purchaseApplyDetail = purchaseApplyDetails.getJSONObject(detailIndex);
            // 检查采购数量、单价和明细ID是否填写
            Assert.hasKeyAndValue(purchaseApplyDetail, "purchaseQuantity", "采购数量未填写");
            Assert.hasKeyAndValue(purchaseApplyDetail, "price", "采购单价未填写");
            Assert.hasKeyAndValue(purchaseApplyDetail, "id", "明细ID为空");
        }
    }

    /**
     * 执行采购入库操作
     * 处理采购申请单的入库流程，包括更新采购明细、更新物品库存、记录库存变更历史等
     *
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求JSON数据
     * @throws CmdException 当命令执行失败时抛出异常
     * @throws ParseException 当数据解析失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 获取采购明细数据并转换为PO对象列表
        JSONArray purchaseApplyDetails = reqJson.getJSONArray("purchaseApplyDetailVo");
        List<PurchaseApplyDetailPo> purchaseApplyDetailPos = new ArrayList<>();
        for (int detailIndex = 0; detailIndex < purchaseApplyDetails.size(); detailIndex++) {
            JSONObject purchaseApplyDetail = purchaseApplyDetails.getJSONObject(detailIndex);
            // 将JSON对象转换为采购明细PO对象
            PurchaseApplyDetailPo purchaseApplyDetailPo = BeanConvertUtil.covertBean(purchaseApplyDetail, PurchaseApplyDetailPo.class);
            purchaseApplyDetailPos.add(purchaseApplyDetailPo);
        }
        
        // 创建采购申请PO对象并设置相关数据
        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
        purchaseApplyPo.setApplyOrderId(reqJson.getString("applyOrderId"));
        purchaseApplyPo.setPurchaseApplyDetailPos(purchaseApplyDetailPos);

        // 处理每个采购明细项
        purchaseApplyDetailPos = purchaseApplyPo.getPurchaseApplyDetailPos();
        for (PurchaseApplyDetailPo purchaseApplyDetailPo : purchaseApplyDetailPos) {
            // 更新采购申请明细
            purchaseApplyDetailInnerServiceSMOImpl.updatePurchaseApplyDetail(purchaseApplyDetailPo);
            
            // 创建物品库存PO对象并设置采购信息
            ResourceStorePo resourceStorePo = new ResourceStorePo();
            resourceStorePo.setResId(purchaseApplyDetailPo.getResId());
            resourceStorePo.setPurchasePrice(purchaseApplyDetailPo.getPrice());
            resourceStorePo.setStock(purchaseApplyDetailPo.getPurchaseQuantity());
            resourceStorePo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_ENTER);
            
            // 查询物品资源信息
            ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
            resourceStoreDto.setResId(purchaseApplyDetailPo.getResId());
            List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
            Assert.listOnlyOne(resourceStoreDtos, "查询物品资源信息错误！");
            
            // 验证最小计量单位数量是否为空
            if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniUnitStock())) {
                throw new IllegalArgumentException("最小计量单位数量不能为空！");
            }
            
            // 获取最小计量单位数量
            BigDecimal miniUnitStock = new BigDecimal(resourceStoreDtos.get(0).getMiniUnitStock());
            
            // 验证最小计量总数是否为空
            if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniStock())) {
                throw new IllegalArgumentException("最小计量总数不能为空！");
            }
            
            // 获取采购前物品最小计量总数
            BigDecimal miniStock = new BigDecimal(resourceStoreDtos.get(0).getMiniStock());
            // 计算采购的物品最小计量总数
            BigDecimal purchaseQuantity = new BigDecimal(purchaseApplyDetailPo.getPurchaseQuantity());
            BigDecimal purchaseMiniStock = purchaseQuantity.multiply(miniUnitStock);
            // 计算采购后物品最小计量总数
            BigDecimal nowMiniStock = miniStock.add(purchaseMiniStock);
            resourceStorePo.setMiniStock(String.valueOf(nowMiniStock));
            
            // 更新物品库存信息
            resourceStoreInnerServiceSMOImpl.updateResourceStore(resourceStorePo);

            // 保存至物品库存记录表
            ResourceStoreTimesPo resourceStoreTimesPo = new ResourceStoreTimesPo();
            resourceStoreTimesPo.setApplyOrderId(purchaseApplyPo.getApplyOrderId());
            resourceStoreTimesPo.setPrice(purchaseApplyDetailPo.getPrice());
            resourceStoreTimesPo.setStock(purchaseApplyDetailPo.getPurchaseQuantity());
            resourceStoreTimesPo.setResCode(resourceStoreDtos.get(0).getResCode());
            resourceStoreTimesPo.setStoreId(resourceStoreDtos.get(0).getStoreId());
            resourceStoreTimesPo.setTimesId(GenerateCodeFactory.getGeneratorId("10"));
            resourceStoreTimesPo.setShId(purchaseApplyDetailPo.getShId());
            resourceStoreTimesPo.setCommunityId(resourceStoreDtos.get(0).getCommunityId());
            resourceStoreTimesV1InnerServiceSMOImpl.saveOrUpdateResourceStoreTimes(resourceStoreTimesPo);
        }
        
        // 获取订单号并更新采购申请状态
        String applyOrderId = purchaseApplyPo.getApplyOrderId();
        PurchaseApplyPo purchaseApply = new PurchaseApplyPo();
        purchaseApply.setApplyOrderId(applyOrderId);
        
        // 如果包含taskId，则处理工作流相关逻辑
        if (reqJson.containsKey("taskId")) {
            reqJson.put("auditCode", "1100");
            reqJson.put("auditMessage", "入库成功");
            reqJson.put("id", reqJson.getString("applyOrderId"));
            reqJson.put("storeId", CmdContextUtils.getStoreId(context));
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            
            // 完成任务并判断是否为最后一个任务
            boolean isLastTask = oaWorkflowUserInnerServiceSMOImpl.completeTask(reqJson);
            if (isLastTask) {
                purchaseApply.setState(PurchaseApplyDto.STATE_END);
            } else {
                purchaseApply.setState(PurchaseApplyDto.STATE_DEALING);
            }
        } else {
            // 无工作流时直接设置为已审核状态
            purchaseApply.setState(PurchaseApplyDto.STATE_AUDITED);
        }
        
        purchaseApply.setStatusCd("0");
        // 更新采购申请状态
        purchaseApplyInnerServiceSMOImpl.updatePurchaseApply(purchaseApply);

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_OK, "采购申请成功"));
    }
}