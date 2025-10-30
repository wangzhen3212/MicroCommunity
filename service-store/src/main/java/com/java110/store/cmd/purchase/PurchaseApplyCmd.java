package com.java110.store.cmd.purchase;

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
import com.java110.dto.store.StorehouseDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.common.IPurchaseApplyUserInnerServiceSMO;
import com.java110.intf.oa.IOaWorkflowInnerServiceSMO;
import com.java110.intf.store.*;
import com.java110.intf.user.IUserV1InnerServiceSMO;
import com.java110.po.purchase.PurchaseApplyDetailPo;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resource.ResourceStoreTimesPo;
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
 * 采购申请命令类
 * 
 * <p>处理采购申请相关的业务逻辑，包括参数验证、采购申请创建、审批流程启动和直接入库等功能</p>
 * 
 * <p>请求示例：
 * {"resourceStores":[{"resId":"852020061636590016","resName":"橡皮擦","resCode":"003","price":"100.00","stock":"0","description":"ada","quantity":"1"},
 * {"resId":"852020061729120031","resName":"文档柜","resCode":"002","price":"33.00","stock":"0","description":"蓝色","quantity":"1"}],
 * "description":"123123","endUserName":"1","endUserTel":"17797173942","file":"","resOrderType":"10000","staffId":"","staffName":""}
 * </p>
 */
@Java110Cmd(serviceCode = "/purchase/purchaseApply")
public class PurchaseApplyCmd extends Cmd {

    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyUserInnerServiceSMO purchaseApplyUserInnerServiceSMOImpl;

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

    /**
     * 验证采购申请参数
     *
     * @param event   命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException    命令异常
     * @throws ParseException  解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数
        Assert.hasKeyAndValue(reqJson, "description", "必填，请填写采购申请说明");
        Assert.hasKeyAndValue(reqJson, "endUserName", "必填，请填写采购联系人");
        Assert.hasKeyAndValue(reqJson, "endUserTel", "必填，请填写采购联系电话");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        Assert.hasKeyAndValue(reqJson, "shId", "必填，请填写仓库");
        Assert.hasKey(reqJson, "resourceStores", "必填，请填写申请采购的物资");

        // 验证采购物品列表
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

        // 如果采购开关关闭，则直接返回（无需流程审批）
        if (!StorehouseDto.SWITCH_ON.equals(storehouseDtos.get(0).getPurchaseSwitch())) {
            return;
        }
        
        // 获取店铺ID并验证审批流程
        String storeId = CmdContextUtils.getStoreId(context);
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setStoreId(storeId);
        oaWorkflowDto.setFlowId(storehouseDtos.get(0).getPurchaseFlowId());
        List<OaWorkflowDto> oaWorkflowDtos = oaWorkflowInnerServiceSMOImpl.queryOaWorkflows(oaWorkflowDto);
        
        // 验证流程是否存在且已部署
        Assert.listOnlyOne(oaWorkflowDtos, "流程不存在");
        if (!OaWorkflowDto.STATE_COMPLAINT.equals(oaWorkflowDtos.get(0).getState())) {
            throw new IllegalArgumentException(oaWorkflowDtos.get(0).getFlowName() + "流程未部署");
        }

        if (StringUtil.isEmpty(oaWorkflowDtos.get(0).getProcessDefinitionKey())) {
            throw new IllegalArgumentException(oaWorkflowDtos.get(0).getFlowName() + "流程未部署");
        }
    }

    /**
     * 执行采购申请命令
     *
     * @param event   命令事件
     * @param context 数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException    命令异常
     * @throws ParseException  解析异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 获取用户和店铺信息
        String userId = CmdContextUtils.getUserId(context);
        String storeId = CmdContextUtils.getStoreId(context);

        // 验证仓库信息
        StorehouseDto storehouseDto = new StorehouseDto();
        storehouseDto.setShId(reqJson.getString("shId"));
        List<StorehouseDto> storehouseDtos = storehouseV1InnerServiceSMOImpl.queryStorehouses(storehouseDto);
        Assert.listOnlyOne(storehouseDtos, "仓库不存在");
        
        // 处理采购开关设置
        if (!StringUtil.isEmpty(reqJson.getString("purchaseSwitch")) && StorehouseDto.SWITCH_OFF.equals(reqJson.getString("purchaseSwitch"))) {
            storehouseDtos.get(0).setPurchaseSwitch(StorehouseDto.SWITCH_OFF);
        }
        
        // 获取用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);
        Assert.listOnlyOne(userDtos, "未包含用户");

        String userName = userDtos.get(0).getName();
        
        // 封装采购申请主表信息
        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
        purchaseApplyPo.setApplyOrderId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
        purchaseApplyPo.setDescription(reqJson.getString("description"));
        purchaseApplyPo.setUserId(userId);
        purchaseApplyPo.setUserName(userName);
        purchaseApplyPo.setEndUserName(reqJson.getString("endUserName"));
        purchaseApplyPo.setEndUserTel(reqJson.getString("endUserTel"));
        purchaseApplyPo.setStoreId(storeId);
        purchaseApplyPo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_ENTER);
        purchaseApplyPo.setState(PurchaseApplyDto.STATE_WAIT_DEAL);
        purchaseApplyPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        purchaseApplyPo.setCreateUserId(userId);
        purchaseApplyPo.setCreateUserName(userName);
        purchaseApplyPo.setWarehousingWay(PurchaseApplyDto.WAREHOUSING_TYPE_APPLY);
        
        // 根据采购开关设置入库方式
        if (!StorehouseDto.SWITCH_ON.equals(storehouseDtos.get(0).getPurchaseSwitch())) {
            purchaseApplyPo.setWarehousingWay(PurchaseApplyDto.WAREHOUSING_TYPE_DIRECT);
        }
        purchaseApplyPo.setCommunityId(reqJson.getString("communityId"));
        
        // 封装采购物品详情信息
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        List<PurchaseApplyDetailPo> purchaseApplyDetailPos = new ArrayList<>();
        for (int resourceStoreIndex = 0; resourceStoreIndex < resourceStores.size(); resourceStoreIndex++) {
            JSONObject resourceStore = resourceStores.getJSONObject(resourceStoreIndex);
            resourceStore.put("originalStock", resourceStore.getString("stock"));
            JSONArray timeList = resourceStore.getJSONArray("times");
            
            // 转换采购详情对象
            PurchaseApplyDetailPo purchaseApplyDetailPo = BeanConvertUtil.covertBean(resourceStore, PurchaseApplyDetailPo.class);
            purchaseApplyDetailPo.setPurchaseQuantity(resourceStore.getString("quantity"));
            purchaseApplyDetailPo.setId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
            
            // 获取采购参考价格
            String consultPrice = null;
            if (resourceStore.containsKey("timesId") && !StringUtil.isEmpty(resourceStore.getString("timesId"))) {
                for (int timesIndex = 0; timesIndex < timeList.size(); timesIndex++) {
                    JSONObject times = timeList.getJSONObject(timesIndex);
                    if (times.getString("timesId").equals(resourceStore.getString("timesId"))) {
                        consultPrice = times.getString("price");
                    }
                }
            }
            purchaseApplyDetailPo.setConsultPrice(consultPrice);
            purchaseApplyDetailPos.add(purchaseApplyDetailPo);
        }
        purchaseApplyPo.setPurchaseApplyDetailPos(purchaseApplyDetailPos);

        // 保存采购申请
        int saveFlag = purchaseApplyInnerServiceSMOImpl.savePurchaseApply(purchaseApplyPo);
        if (saveFlag < 1) {
            context.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "采购申请失败"));
            return;
        }
        
        // 转换对象并设置审批相关信息
        PurchaseApplyDto purchaseApplyDto = BeanConvertUtil.covertBean(purchaseApplyPo, PurchaseApplyDto.class);
        purchaseApplyDto.setCurrentUserId(purchaseApplyPo.getUserId());
        purchaseApplyDto.setNextStaffId(reqJson.getString("staffId"));

        // 启动审核流程
        toStartWorkflow(purchaseApplyDto, storehouseDtos.get(0), reqJson);

        context.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_OK, "采购申请成功"));
    }

    /**
     * 启动审批流程
     *
     * @param purchaseApplyDto 采购申请数据传输对象
     * @param storehouseDto    仓库数据传输对象
     * @param reqJson          请求JSON对象
     */
    private void toStartWorkflow(PurchaseApplyDto purchaseApplyDto, StorehouseDto storehouseDto, JSONObject reqJson) {
        // 如果采购开关关闭，则直接入库
        if (!StorehouseDto.SWITCH_ON.equals(storehouseDto.getPurchaseSwitch())) {
            toPurchaseEnterStorehouse(purchaseApplyDto, storehouseDto, reqJson);
            return;
        }

        // 查询审批流程信息
        OaWorkflowDto oaWorkflowDto = new OaWorkflowDto();
        oaWorkflowDto.setStoreId(purchaseApplyDto.getStoreId());
        oaWorkflowDto.setFlowId(storehouseDto.getPurchaseFlowId());
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
        
        // 启动审批流程
        JSONObject result = oaWorkflowActivitiInnerServiceSMOImpl.startProcess(flowJson);

        // 获取下一处理人ID
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
        
        // 自动完成第一个任务节点
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
     * 执行采购入库操作
     *
     * @param purchaseApplyDto 采购申请数据传输对象
     * @param storehouseDto    仓库数据传输对象
     * @param reqJson          请求JSON对象
     */
    private void toPurchaseEnterStorehouse(PurchaseApplyDto purchaseApplyDto, StorehouseDto storehouseDto, JSONObject reqJson) {
        // 查询采购申请详情
        PurchaseApplyDetailDto purchaseApplyDetailDto = new PurchaseApplyDetailDto();
        purchaseApplyDetailDto.setApplyOrderId(purchaseApplyDto.getApplyOrderId());
        purchaseApplyDetailDto.setStoreId(purchaseApplyDto.getStoreId());
        List<PurchaseApplyDetailDto> purchaseApplyDetailDtos = purchaseApplyDetailInnerServiceSMOImpl.queryPurchaseApplyDetails(purchaseApplyDetailDto);

        if (purchaseApplyDetailDtos == null || purchaseApplyDetailDtos.size() < 1) {
            return;
        }

        int stock = 0;
        // 遍历采购物品详情，更新库存信息
        for (PurchaseApplyDetailDto tmpPurchaseApplyDetailDto : purchaseApplyDetailDtos) {
            ResourceStorePo resourceStorePo = new ResourceStorePo();
            resourceStorePo.setResId(tmpPurchaseApplyDetailDto.getResId());
            resourceStorePo.setPurchasePrice(tmpPurchaseApplyDetailDto.getPrice());
            resourceStorePo.setStock(tmpPurchaseApplyDetailDto.getPurchaseQuantity());
            resourceStorePo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_ENTER);
            
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
            
            // 计算最小计量单位相关数量
            BigDecimal miniUnitStock = new BigDecimal(resourceStoreDtos.get(0).getMiniUnitStock());
            if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniStock())) {
                throw new IllegalArgumentException("最小计量总数不能为空！");
            }
            
            // 计算采购后的最小计量总数
            BigDecimal miniStock = new BigDecimal(resourceStoreDtos.get(0).getMiniStock());
            BigDecimal purchaseQuantity = new BigDecimal(tmpPurchaseApplyDetailDto.getPurchaseQuantity());
            BigDecimal purchaseMiniStock = purchaseQuantity.multiply(miniUnitStock);
            BigDecimal nowMiniStock = miniStock.add(purchaseMiniStock);
            resourceStorePo.setMiniStock(String.valueOf(nowMiniStock));
            
            // 更新物品资源库存
            resourceStoreInnerServiceSMOImpl.updateResourceStore(resourceStorePo);

            // 保存物品入库记录到times表
            ResourceStoreTimesPo resourceStoreTimesPo = new ResourceStoreTimesPo();
            resourceStoreTimesPo.setApplyOrderId(tmpPurchaseApplyDetailDto.getApplyOrderId());
            resourceStoreTimesPo.setPrice(tmpPurchaseApplyDetailDto.getPrice());
            resourceStoreTimesPo.setStock(tmpPurchaseApplyDetailDto.getPurchaseQuantity());
            resourceStoreTimesPo.setResCode(resourceStoreDtos.get(0).getResCode());
            resourceStoreTimesPo.setStoreId(resourceStoreDtos.get(0).getStoreId());
            resourceStoreTimesPo.setTimesId(GenerateCodeFactory.getGeneratorId("10"));
            resourceStoreTimesPo.setShId(tmpPurchaseApplyDetailDto.getShId());
            resourceStoreTimesPo.setCommunityId(resourceStoreDtos.get(0).getCommunityId());
            resourceStoreTimesV1InnerServiceSMOImpl.saveOrUpdateResourceStoreTimes(resourceStoreTimesPo);
        }
        
        // 更新采购申请状态为已完成
        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
        purchaseApplyPo.setApplyOrderId(purchaseApplyDto.getApplyOrderId());
        purchaseApplyPo.setState(PurchaseApplyDto.STATE_END);
        purchaseApplyPo.setStatusCd("0");
        purchaseApplyInnerServiceSMOImpl.updatePurchaseApply(purchaseApplyPo);
    }
}