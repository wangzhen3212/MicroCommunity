/**
 * 删除调拨仓库命令类
 * 
 * 该命令类负责处理调拨仓库的删除操作，包括验证删除条件、执行删除逻辑以及相关的业务处理。
 * 主要功能包括验证调拨申请状态、删除调拨记录、取消审批流程等。
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.purchase.AllocationStorehouseDto;
import com.java110.dto.purchase.AllocationStorehouseApplyDto;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.dto.resource.ResourceStoreTimesDto;
import com.java110.intf.community.IResourceStoreServiceSMO;
import com.java110.intf.store.*;
import com.java110.po.purchase.AllocationStorehousePo;
import com.java110.po.purchase.AllocationStorehouseApplyPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.po.resource.ResourceStoreTimesPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

@Java110Cmd(serviceCode = "resourceStore.deleteAllocationStorehouse")
public class DeleteAllocationStorehouseCmd extends Cmd {

    @Autowired
    private IAllocationStorehouseApplyInnerServiceSMO allocationStorehouseApplyInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseInnerServiceSMO allocationStorehouseInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseV1InnerServiceSMO allocationStorehouseV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreServiceSMO resourceStoreServiceSMOImpl;

    @Autowired
    private IResourceStoreV1InnerServiceSMO resourceStoreV1InnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseApplyV1InnerServiceSMO allocationStorehouseApplyV1InnerServiceSMOImpl;

    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证删除调拨仓库请求的必需参数是否完整
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含applyId（调拨编号）
        Assert.hasKeyAndValue(reqJson, "applyId", "调拨编号不能为空");
    }

    /**
     * 执行删除调拨仓库命令
     * 
     * 该方法根据调拨申请ID查询调拨申请信息，验证状态后执行删除操作
     * 只有状态为"1200"的调拨申请才能被删除
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当命令执行过程中出现错误时抛出
     * @throws ParseException 当数据解析出现错误时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求参数中获取调拨申请ID
        String applyId = reqJson.getString("applyId");
        
        // 创建调拨申请查询对象
        AllocationStorehouseApplyDto allocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
        allocationStorehouseApplyDto.setApplyId(applyId);
        
        // 查询调拨申请信息
        List<AllocationStorehouseApplyDto> allocationStorehouseApplyDtos = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(allocationStorehouseApplyDto);
        
        // 验证查询结果，确保只返回一条记录
        Assert.listOnlyOne(allocationStorehouseApplyDtos, "查询调拨申请表错误！");
        
        // 获取调拨申请的状态
        String state = allocationStorehouseApplyDtos.get(0).getState();
        
        // 检查状态是否为"1200"（可删除状态）
        if (!StringUtil.isEmpty(state) && state.equals("1200")) {
            // 状态符合条件，执行删除操作
            deleteAllocationStorehouse(reqJson);
        } else {
            // 状态不符合条件，返回错误信息
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "您的调拨订单状态已改变，无法进行取消操作！！");
            context.setResponseEntity(responseEntity);
            return;
        }
    }

    /**
     * 删除调拨仓库记录
     * 
     * 该方法执行具体的删除逻辑，包括：
     * 1. 删除调拨仓库记录
     * 2. 取消相关的审批流程
     * 3. 更新调拨申请状态
     * 
     * @param paramInJson 包含删除参数的JSON对象，必须包含applyId和storeId
     * @throws CmdException 当删除操作失败时抛出命令异常
     */
    public void deleteAllocationStorehouse(JSONObject paramInJson) {
        // 创建调拨仓库查询条件对象
        AllocationStorehouseDto allocationStorehouseDto = new AllocationStorehouseDto();
        allocationStorehouseDto.setApplyId(paramInJson.getString("applyId"));
        allocationStorehouseDto.setStoreId(paramInJson.getString("storeId"));

        // 查询符合条件的调拨仓库记录
        List<AllocationStorehouseDto> allocationStorehouseDtos = allocationStorehouseInnerServiceSMOImpl.queryAllocationStorehouses(allocationStorehouseDto);
        
        int flag = 0; // 操作结果标志
        
        // 遍历所有调拨仓库记录，逐条删除
        for (AllocationStorehouseDto tmpAllocationStorehouseDto : allocationStorehouseDtos) {
            // 将DTO对象转换为PO对象，用于删除操作
            AllocationStorehousePo allocationStorehousePo = BeanConvertUtil.covertBean(tmpAllocationStorehouseDto, AllocationStorehousePo.class);
            allocationStorehousePo.setStatusCd("1"); // 设置状态为删除状态
            
            // 执行删除操作
            flag = allocationStorehouseV1InnerServiceSMOImpl.deleteAllocationStorehouse(allocationStorehousePo);

            // 检查删除操作是否成功
            if (flag < 1) {
                throw new CmdException("删除失败");
            }
            
            // 取消相关的审批流程
            // 查询审批任务信息
            PurchaseApplyDto purchaseDto = new PurchaseApplyDto();
            purchaseDto.setBusinessKey(tmpAllocationStorehouseDto.getApplyId());
            List<PurchaseApplyDto> purchaseApplyDtoList = purchaseApplyInnerServiceSMOImpl.getActRuTaskId(purchaseDto);
            
            // 如果存在审批任务，则取消该任务
            if (purchaseApplyDtoList != null && purchaseApplyDtoList.size() > 0) {
                PurchaseApplyDto purchaseDto1 = new PurchaseApplyDto();
                purchaseDto1.setActRuTaskId(purchaseApplyDtoList.get(0).getActRuTaskId());
                purchaseDto1.setAssigneeUser("999999"); // 设置特定的处理人员标识
                purchaseApplyInnerServiceSMOImpl.updateActRuTaskById(purchaseDto1);
            }
        }

        // 更新调拨申请状态为删除状态
        AllocationStorehouseApplyPo allocationStorehouseApplyPo = new AllocationStorehouseApplyPo();
        allocationStorehouseApplyPo.setApplyId(allocationStorehouseDto.getApplyId());
        allocationStorehouseApplyPo.setStoreId(allocationStorehouseDto.getStoreId());
        allocationStorehouseApplyPo.setStatusCd("1"); // 设置状态为删除状态
        
        // 执行更新操作
        flag = allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);
        
        // 检查更新操作是否成功
        if (flag < 1) {
            throw new CmdException("修改失败");
        }
    }
}