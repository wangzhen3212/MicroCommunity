package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.purchase.AllocationStorehouseDto;
import com.java110.dto.purchase.AllocationStorehouseApplyDto;
import com.java110.dto.resource.ResourceStoreDto;
import com.java110.dto.resource.ResourceStoreTimesDto;
import com.java110.intf.common.IAllocationStorehouseUserInnerServiceSMO;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.store.*;
import com.java110.po.purchase.AllocationStorehouseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resource.ResourceStoreTimesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

/**
 * 调拨审核命令类
 * 负责处理仓库资源调拨申请的审核流程，包括审核通过、退回、转单等操作
 * 通过工作流引擎实现审核流程的控制和状态管理
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "resourceStore.auditAllocationStoreOrder")
public class AuditAllocationStoreOrderCmd extends Cmd {

    @Autowired
    private IAllocationStorehouseUserInnerServiceSMO allocationStorehouseUserInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseInnerServiceSMO allocationStorehouseInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseApplyInnerServiceSMO allocationStorehouseApplyInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreV1InnerServiceSMO resourceStoreV1InnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseApplyV1InnerServiceSMO allocationStorehouseApplyV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowUserInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数是否完整，确保必要的参数都存在
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求JSON数据
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        Assert.hasKeyAndValue(reqJson, "taskId", "未包含任务");
        Assert.hasKeyAndValue(reqJson, "id", "订单号不能为空");
        Assert.hasKeyAndValue(reqJson, "auditCode", "未包含状态");
        Assert.hasKeyAndValue(reqJson, "auditMessage", "未包含状态说明");
    }

    /**
     * 主命令执行方法
     * 处理调拨申请审核的核心业务逻辑，包括各种审核状态的处理
     *
     * @param event   命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求JSON数据
     * @throws CmdException    命令执行异常
     * @throws ParseException  解析异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        String storeId = context.getReqHeaders().get("store-id");

        // 查询调拨申请单信息
        AllocationStorehouseApplyDto allocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
        allocationStorehouseApplyDto.setApplyId(reqJson.getString("id"));
        List<AllocationStorehouseApplyDto> allocationStorehouseDtos
                = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(allocationStorehouseApplyDto);
        Assert.listOnlyOne(allocationStorehouseDtos, "调拨申请单存在多条");

        // 准备更新调拨申请单状态的对象
        // 状态说明：W待审核 D 审核中 C 审核完成 D 审核失败
        AllocationStorehouseApplyPo allocationStorehouseApplyPo = new AllocationStorehouseApplyPo();
        allocationStorehouseApplyPo.setApplyId(allocationStorehouseDtos.get(0).getApplyId());
        reqJson.put("id", reqJson.getString("id"));
        reqJson.put("storeId", storeId);

        // 根据审核码处理不同的审核操作
        // 1100: 办理操作, 1500: 办理操作
        if ("1100".equals(reqJson.getString("auditCode"))
                || "1500".equals(reqJson.getString("auditCode"))) { 
            // 设置下一处理人
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            // 完成当前任务
            boolean isLastTask = oaWorkflowUserInnerServiceSMOImpl.completeTask(reqJson);
            if (isLastTask) {
                // 如果是最后一个任务，状态设为完成
                allocationStorehouseApplyPo.setState(AllocationStorehouseApplyDto.STATE_END);
            } else {
                // 否则状态设为处理中
                allocationStorehouseApplyPo.setState(AllocationStorehouseApplyDto.STATE_DEALING);
            }
            allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
            
        } else if ("1300".equals(reqJson.getString("auditCode"))) { // 转单操作
            // 设置下一处理人
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            // 将任务转给其他用户处理
            oaWorkflowUserInnerServiceSMOImpl.changeTaskToOtherUser(reqJson);
            // 状态设为处理中
            allocationStorehouseApplyPo.setState(AllocationStorehouseApplyDto.STATE_DEALING);
            allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
            
        } else if ("1200".equals(reqJson.getString("auditCode"))
                || "1400".equals(reqJson.getString("auditCode"))
        ) { // 退回操作
            // 设置发起人
            reqJson.put("startUserId", allocationStorehouseDtos.get(0).getStartUserId());
            // 退回任务
            oaWorkflowUserInnerServiceSMOImpl.goBackTask(reqJson);
            // 状态设为未通过
            allocationStorehouseApplyPo.setState(AllocationStorehouseApplyDto.STATE_NOT_PASS);
            allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
            
        } else if ("1600".equals(reqJson.getString("auditCode"))) { // 重新提交操作
            // 设置下一处理人
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            // 完成任务
            boolean isLastTask = oaWorkflowUserInnerServiceSMOImpl.completeTask(reqJson);
            if (isLastTask) {
                // 如果是最后一个任务，状态设为完成
                allocationStorehouseApplyPo.setState(AllocationStorehouseApplyDto.STATE_END);
            } else {
                // 否则状态设为申请中
                allocationStorehouseApplyPo.setState(AllocationStorehouseApplyDto.STATE_APPLY);
            }
            allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
        } else {
            throw new IllegalArgumentException("不支持的类型");
        }
    }

    /**
     * 调拨申请-物品调拨审核方法
     * 处理调拨申请的详细审核逻辑，包括状态更新和库存调整
     *
     * @param event   事件对象
     * @param context 数据上文对象
     * @param reqJson 请求报文
     * @throws CmdException    命令执行异常
     * @throws ParseException  解析异常
     */
    public void doCmd1(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        int flag = 0;
        // 构建调拨申请数据传输对象
        AllocationStorehouseApplyDto allocationStorehouseDto = new AllocationStorehouseApplyDto();
        allocationStorehouseDto.setTaskId(reqJson.getString("taskId"));
        allocationStorehouseDto.setApplyId(reqJson.getString("applyId"));
        allocationStorehouseDto.setStoreId(reqJson.getString("storeId"));
        allocationStorehouseDto.setAuditCode(reqJson.getString("state"));
        allocationStorehouseDto.setAuditMessage(reqJson.getString("remark"));
        allocationStorehouseDto.setCurrentUserId(reqJson.getString("userId"));
        allocationStorehouseDto.setNoticeState(reqJson.getString("noticeState"));
        allocationStorehouseDto.setNextUserId(reqJson.getString("nextUserId"));
        
        // 查询调拨申请单信息
        AllocationStorehouseApplyDto tmpAllocationStorehouseDto = new AllocationStorehouseApplyDto();
        tmpAllocationStorehouseDto.setApplyId(reqJson.getString("applyId"));
        tmpAllocationStorehouseDto.setStoreId(reqJson.getString("storeId"));
        List<AllocationStorehouseApplyDto> allocationStorehouseDtos = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(tmpAllocationStorehouseDto);
        Assert.listOnlyOne(allocationStorehouseDtos, "调拨申请单存在多条");
        allocationStorehouseDto.setStartUserId(allocationStorehouseDtos.get(0).getStartUserId());
        
        // 完成任务并检查是否为最后一个任务
        boolean isLastTask = allocationStorehouseUserInnerServiceSMOImpl.completeTask(allocationStorehouseDto);
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("成功", HttpStatus.OK);
        
        if (isLastTask) {
            // 如果是最后一个任务，更新调拨仓库信息
            updateAllocationStorehouse(reqJson, context);
        } else if (reqJson.getString("state").equals("1100")) {  // 审核同意时，状态变为调拨审核  1100同意状态
            String procure = reqJson.getString("procure");
            AllocationStorehouseApplyPo allocationStorehouseApplyPo = new AllocationStorehouseApplyPo();
            allocationStorehouseApplyPo.setApplyId(allocationStorehouseDtos.get(0).getApplyId());
            allocationStorehouseApplyPo.setState(AllocationStorehouseDto.STATE_AUDIT);
            if (!StringUtil.isEmpty(procure) && procure.equals("true")) {
                // 如果是采购流程，状态设为已审核
                allocationStorehouseApplyPo.setState(AllocationStorehouseDto.STATE_REVIEWED);
            }
            flag = allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
            if (flag < 1) {
                throw new CmdException("修改失败");
            }
        } else if (reqJson.getString("state").equals("1200")) {  // 审核拒绝时，状态变为调拨失败  1200拒绝状态
            // 撤销调拨申请
            revokeAllocationStorehouse(reqJson);
        }
    }

    /**
     * 更新调拨仓库信息
     * 处理调拨申请审核通过后的库存调整和状态更新
     *
     * @param paramInJson 接口调用方传入参数
     * @param context     数据流上下文
     */
    private void updateAllocationStorehouse(JSONObject paramInJson, ICmdDataFlowContext context) {
        // 查询调拨申请单信息
        AllocationStorehouseApplyDto tmpAllocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
        tmpAllocationStorehouseApplyDto.setApplyId(paramInJson.getString("applyId"));
        tmpAllocationStorehouseApplyDto.setStoreId(paramInJson.getString("storeId"));
        List<AllocationStorehouseApplyDto> allocationStorehouseApplyDtos = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(tmpAllocationStorehouseApplyDto);

        Assert.listOnlyOne(allocationStorehouseApplyDtos, "存在多条记录，或不存在数据" + tmpAllocationStorehouseApplyDto.getApplyId());

        // 构建更新对象
        JSONObject businessComplaint = new JSONObject();
        businessComplaint.putAll(BeanConvertUtil.beanCovertMap(allocationStorehouseApplyDtos.get(0)));
        businessComplaint.put("state", AllocationStorehouseDto.STATE_SUCCESS);
        AllocationStorehouseApplyPo allocationStorehouseApplyPo = BeanConvertUtil.covertBean(businessComplaint, AllocationStorehouseApplyPo.class);
        int flag = 0;
        
        // 处理调拨成功的情况
        if (allocationStorehouseApplyDtos.get(0).getState().equals("1201") || allocationStorehouseApplyDtos.get(0).getState().equals("1204")) {
            flag = allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
            if (flag < 1) {
                throw new CmdException("修改失败");
            }
            
            // 查询调拨详情信息
            AllocationStorehouseDto tmpAllocationStorehouseDto = new AllocationStorehouseDto();
            tmpAllocationStorehouseDto.setApplyId(paramInJson.getString("applyId"));
            tmpAllocationStorehouseDto.setStoreId(paramInJson.getString("storeId"));
            List<AllocationStorehouseDto> allocationStorehouseDtos = allocationStorehouseInnerServiceSMOImpl.queryAllocationStorehouses(tmpAllocationStorehouseDto);

            // 遍历调拨详情，处理每个调拨项
            for (AllocationStorehouseDto allocationStorehouseDto : allocationStorehouseDtos) {
                // 查询目标仓库是否已有此物品
                ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
                resourceStoreDto.setShId(allocationStorehouseDto.getShIdz()); // 目标仓库ID
                resourceStoreDto.setResCode(allocationStorehouseDto.getResCode());
                List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
                ResourceStorePo resourceStorePo = new ResourceStorePo();

                if (resourceStoreDtos != null && resourceStoreDtos.size() == 1) {
                    // 目标仓库已有此物品，更新库存
                    // 查询原仓库该物品记录
                    ResourceStoreDto originalResourceStoreDto = new ResourceStoreDto();
                    originalResourceStoreDto.setShId(allocationStorehouseDto.getShIda()); // 原仓库ID
                    originalResourceStoreDto.setResId(allocationStorehouseDto.getResId());
                    List<ResourceStoreDto> originalResourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(originalResourceStoreDto);
                    Assert.listOnlyOne(originalResourceStoreDtos, "原仓库记录不存在");
                    
                    ResourceStoreDto resourceStoreDto1 = resourceStoreDtos.get(0);
                    // 计算库存：目标仓库库存 + 调拨数量
                    BigDecimal stock1 = new BigDecimal(resourceStoreDtos.get(0).getStock());
                    BigDecimal stock2 = new BigDecimal(allocationStorehouseDto.getStock());
                    resourceStoreDto1.setStock(stock1.add(stock2).toString());
                    
                    // 计算最小计量总数
                    if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniStock())) {
                        throw new IllegalArgumentException("最小计量总数不能为空！");
                    }
                    BigDecimal miniStock = new BigDecimal(resourceStoreDtos.get(0).getMiniStock()); // 获取原最小计量总数
                    if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniUnitStock())) {
                        throw new IllegalArgumentException("最小计量单位数量不能为空！");
                    }
                    BigDecimal miniUnitStock = new BigDecimal(resourceStoreDtos.get(0).getMiniUnitStock()); // 获取最小计量单位数量
                    BigDecimal nowMiniStock = stock2.multiply(miniUnitStock); // 计算调拨的最小计量总数
                    BigDecimal newMiniStock = miniStock.add(nowMiniStock);
                    resourceStoreDto1.setMiniStock(String.valueOf(newMiniStock));
                    
                    resourceStorePo = BeanConvertUtil.covertBean(resourceStoreDto1, ResourceStorePo.class);
                    // 设置价格信息
                    resourceStorePo.setAveragePrice(originalResourceStoreDtos.get(0).getAveragePrice());
                    resourceStorePo.setOutLowPrice(originalResourceStoreDtos.get(0).getOutLowPrice());
                    resourceStorePo.setOutHighPrice(originalResourceStoreDtos.get(0).getOutHighPrice());
                    resourceStorePo.setRstId(originalResourceStoreDtos.get(0).getRstId());
                    resourceStorePo.setParentRstId(originalResourceStoreDtos.get(0).getParentRstId());
                    resourceStorePo.setRssId(originalResourceStoreDtos.get(0).getRssId());
                    if (!StringUtil.isEmpty(originalResourceStoreDtos.get(0).getMiniUnitCode()) && !StringUtil.isEmpty(originalResourceStoreDtos.get(0).getMiniUnitStock())) {
                        resourceStorePo.setMiniUnitCode(originalResourceStoreDtos.get(0).getMiniUnitCode());
                        resourceStorePo.setMiniUnitStock(originalResourceStoreDtos.get(0).getMiniUnitStock());
                    }
                    flag = resourceStoreV1InnerServiceSMOImpl.updateResourceStore(resourceStorePo);
                    if (flag < 1) {
                        throw new CmdException("修改失败");
                    }
                } else if (resourceStoreDtos != null && resourceStoreDtos.size() > 1) {
                    // 目标仓库存在多条相同物品记录，返回错误
                    ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "物品信息查询到多条，请核实后再处理！");
                    context.setResponseEntity(responseEntity);
                    return;
                } else {
                    // 目标仓库没有此物品，新增记录
                    // 查询原仓库物品信息
                    resourceStoreDto = new ResourceStoreDto();
                    resourceStoreDto.setShId(allocationStorehouseDto.getShIda()); // 原仓库ID
                    resourceStoreDto.setResId(allocationStorehouseDto.getResId());
                    resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
                    Assert.listOnlyOne(resourceStoreDtos, "原仓库记录不存在");
                    
                    resourceStorePo = BeanConvertUtil.covertBean(allocationStorehouseDto, resourceStorePo);
                    resourceStorePo.setResId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_resId));
                    resourceStorePo.setStock(allocationStorehouseDto.getStock());
                    
                    // 计算最小计量单位总数
                    if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniUnitStock())) {
                        throw new IllegalArgumentException("最小计量单位数量不能为空！");
                    }
                    BigDecimal miniUnitStock = new BigDecimal(resourceStoreDtos.get(0).getMiniUnitStock());
                    BigDecimal stock = new BigDecimal(allocationStorehouseDto.getStock());
                    BigDecimal miniStock = stock.multiply(miniUnitStock); // 计算最小计量单位总数
                    resourceStorePo.setMiniStock(String.valueOf(miniStock));
                    
                    // 设置仓库和基本信息
                    resourceStorePo.setShId(allocationStorehouseDto.getShIdz()); // 目标仓库ID
                    resourceStorePo.setPrice(resourceStoreDtos.get(0).getPrice());
                    resourceStorePo.setDescription("");
                    resourceStorePo.setUnitCode(resourceStoreDtos.get(0).getUnitCode());
                    resourceStorePo.setOutLowPrice(resourceStoreDtos.get(0).getOutLowPrice());
                    resourceStorePo.setOutHighPrice(resourceStoreDtos.get(0).getOutHighPrice());
                    resourceStorePo.setShowMobile(resourceStoreDtos.get(0).getShowMobile());
                    resourceStorePo.setWarningStock(resourceStoreDtos.get(0).getWarningStock());
                    resourceStorePo.setAveragePrice(resourceStoreDtos.get(0).getAveragePrice());
                    resourceStorePo.setRstId(resourceStoreDtos.get(0).getRstId());
                    resourceStorePo.setParentRstId(resourceStoreDtos.get(0).getParentRstId());
                    resourceStorePo.setRssId(resourceStoreDtos.get(0).getRssId());
                    resourceStorePo.setMiniUnitCode(resourceStoreDtos.get(0).getMiniUnitCode());
                    resourceStorePo.setMiniUnitStock(resourceStoreDtos.get(0).getMiniUnitStock());
                    flag = resourceStoreV1InnerServiceSMOImpl.saveResourceStore(resourceStorePo);
                    if (flag < 1) {
                        throw new CmdException("添加失败");
                    }
                }
                
                // 保存至物品批次表
                // 查询调拨批次价格
                ResourceStoreTimesDto resourceStoreTimesDto = new ResourceStoreTimesDto();
                resourceStoreTimesDto.setTimesId(allocationStorehouseDto.getTimesId());
                List<ResourceStoreTimesDto> resourceStoreTimesDtos = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimess(resourceStoreTimesDto);
                
                ResourceStoreTimesPo resourceStoreTimesPo = new ResourceStoreTimesPo();
                resourceStoreTimesPo.setApplyOrderId(tmpAllocationStorehouseDto.getApplyId());
                resourceStoreTimesPo.setPrice(resourceStoreTimesDtos.get(0).getPrice());
                resourceStoreTimesPo.setStock(allocationStorehouseDto.getStock());
                resourceStoreTimesPo.setResCode(allocationStorehouseDto.getResCode());
                resourceStoreTimesPo.setStoreId(allocationStorehouseDto.getStoreId());
                resourceStoreTimesPo.setTimesId(GenerateCodeFactory.getGeneratorId("10"));
                resourceStoreTimesPo.setShId(allocationStorehouseDto.getShIdz()); // 目标仓库ID
                resourceStoreTimesV1InnerServiceSMOImpl.saveOrUpdateResourceStoreTimes(resourceStoreTimesPo);
            }
        } else if (allocationStorehouseApplyDtos.get(0).getState().equals("1203")) {
            // 处理调拨失败的情况
            allocationStorehouseApplyPo.setState(AllocationStorehouseDto.STATE_FAIL);
            flag = allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
            if (flag < 1) {
                throw new CmdException("修改失败");
            }
        }
    }

    /**
     * 撤销调拨申请
     * 处理调拨申请审核不通过的情况，恢复原仓库库存
     *
     * @param paramInJson 接口调用方传入参数
     */
    private void revokeAllocationStorehouse(JSONObject paramInJson) {
        // 查询调拨申请单信息
        AllocationStorehouseApplyDto tmpAllocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
        tmpAllocationStorehouseApplyDto.setApplyId(paramInJson.getString("applyId"));
        tmpAllocationStorehouseApplyDto.setStoreId(paramInJson.getString("storeId"));
        List<AllocationStorehouseApplyDto> allocationStorehouseApplyDtos = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(tmpAllocationStorehouseApplyDto);
        Assert.listOnlyOne(allocationStorehouseApplyDtos, "存在多条记录，或不存在数据" + tmpAllocationStorehouseApplyDto.getApplyId());
        
        // 构建更新对象，状态设为失败
        JSONObject businessComplaint = new JSONObject();
        businessComplaint.putAll(BeanConvertUtil.beanCovertMap(allocationStorehouseApplyDtos.get(0)));
        businessComplaint.put("state", AllocationStorehouseDto.STATE_FAIL);
        AllocationStorehouseApplyPo allocationStorehouseApplyPo = BeanConvertUtil.covertBean(businessComplaint, AllocationStorehouseApplyPo.class);
        int flag = allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
        if (flag < 1) {
            throw new CmdException("修改失败");
        }
        
        // 查询调拨详情数据
        AllocationStorehouseDto tmpAllocationStorehouseDto = new AllocationStorehouseDto();
        tmpAllocationStorehouseDto.setApplyId(paramInJson.getString("applyId"));
        tmpAllocationStorehouseDto.setStoreId(paramInJson.getString("storeId"));
        List<AllocationStorehouseDto> allocationStorehouseDtos = allocationStorehouseInnerServiceSMOImpl.queryAllocationStorehouses(tmpAllocationStorehouseDto);
        
        // 遍历调拨详情，恢复原仓库库存
        for (AllocationStorehouseDto allocationStorehouseDto : allocationStorehouseDtos) {
            // 查询原仓库物品信息
            ResourceStoreDto resourceStoreDto = new ResourceStoreDto();
            resourceStoreDto.setResId(allocationStorehouseDto.getResId());
            List<ResourceStoreDto> resourceStoreDtos = resourceStoreInnerServiceSMOImpl.queryResourceStores(resourceStoreDto);
            Assert.listOnlyOne(resourceStoreDtos, "资源物品信息错误");
            
            ResourceStorePo resourceStorePo = new ResourceStorePo();
            resourceStorePo.setResId(allocationStorehouseDto.getResId());
            
            // 恢复库存：原库存 + 调拨数量
            BigDecimal resourceStoreStock = new BigDecimal(resourceStoreDtos.get(0).getStock());
            BigDecimal storehouseStock = new BigDecimal(allocationStorehouseDto.getStock());
            BigDecimal stock = resourceStoreStock.add(storehouseStock);
            resourceStorePo.setStock(String.valueOf(stock));
            
            // 计算最小计量总数
            if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniStock())) {
                throw new IllegalArgumentException("最小计量总数不能为空！");
            }
            BigDecimal miniStock = new BigDecimal(resourceStoreDtos.get(0).getMiniStock()); // 获取物品表的最小计量总数
            if (StringUtil.isEmpty(resourceStoreDtos.get(0).getMiniUnitStock())) {
                throw new IllegalArgumentException("最小计量单位数量不能为空！");
            }
            BigDecimal miniUnitStock = new BigDecimal(resourceStoreDtos.get(0).getMiniUnitStock()); // 获取最小计量单位数量
            BigDecimal stock1 = new BigDecimal(allocationStorehouseDto.getStock()); // 获取调拨数量
            BigDecimal nowMiniStock = stock1.multiply(miniUnitStock); // 计算当前的最小计量总数
            BigDecimal newMiniStock = miniStock.add(nowMiniStock);
            resourceStorePo.setMiniStock(String.valueOf(newMiniStock));
            flag = resourceStoreV1InnerServiceSMOImpl.updateResourceStore(resourceStorePo);

            // 保存至物品批次表
            ResourceStoreTimesDto resourceStoreTimesDto = new ResourceStoreTimesDto();
            resourceStoreTimesDto.setTimesId(allocationStorehouseDto.getTimesId());
            List<ResourceStoreTimesDto> resourceStoreTimesDtos = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimess(resourceStoreTimesDto);
            
            ResourceStoreTimesPo resourceStoreTimesPo = new ResourceStoreTimesPo();
            resourceStoreTimesPo.setApplyOrderId(tmpAllocationStorehouseDto.getApplyId());
            resourceStoreTimesPo.setPrice(resourceStoreTimesDtos.get(0).getPrice());
            resourceStoreTimesPo.setStock(allocationStorehouseDto.getStock());
            resourceStoreTimesPo.setResCode(allocationStorehouseDto.getResCode());
            resourceStoreTimesPo.setStoreId(allocationStorehouseDto.getStoreId());
            resourceStoreTimesPo.setTimesId(GenerateCodeFactory.getGeneratorId("10"));
            resourceStoreTimesPo.setShId(allocationStorehouseDto.getShIda()); // 原仓库ID
            resourceStoreTimesV1InnerServiceSMOImpl.saveOrUpdateResourceStoreTimes(resourceStoreTimesPo);
            
            if (flag < 1) {
                throw new CmdException("修改失败");
            }
        }
    }
}