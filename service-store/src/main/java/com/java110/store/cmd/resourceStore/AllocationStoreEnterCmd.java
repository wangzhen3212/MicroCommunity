package com.java110.store.cmd.resourceStore;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.CmdContextUtils;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.purchase.AllocationStorehouseApplyDto;
import com.java110.dto.purchase.AllocationStorehouseDto;
import com.java110.dto.resource.ResourceStoreTimesDto;
import com.java110.dto.store.StorehouseDto;
import com.java110.intf.common.IOaWorkflowActivitiInnerServiceSMO;
import com.java110.intf.store.*;
import com.java110.po.purchase.AllocationStorehouseApplyPo;
import com.java110.po.purchase.AllocationStorehousePo;
import com.java110.store.bmo.allocation.IAllocationBMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * 调拨入库确认命令类
 * 负责处理调拨申请单的入库确认操作，包括数据验证、库存调拨、流程状态更新等功能
 * 该类继承自Cmd基类，通过注解@Java110Cmd声明服务代码为resourceStore.allocationStoreEnter
 */
@Java110Cmd(serviceCode = "resourceStore.allocationStoreEnter")
public class AllocationStoreEnterCmd extends Cmd {

    @Autowired
    private IAllocationStorehouseApplyInnerServiceSMO allocationStorehouseApplyInnerServiceSMOImpl;

    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseApplyV1InnerServiceSMO allocationStorehouseApplyV1InnerServiceSMOImpl;

    @Autowired
    private IAllocationStorehouseV1InnerServiceSMO allocationStorehouseV1InnerServiceSMOImpl;

    @Autowired
    private IAllocationBMO allocationBMOImpl;

    @Autowired
    private IOaWorkflowActivitiInnerServiceSMO oaWorkflowUserInnerServiceSMOImpl;

    @Autowired
    private IStorehouseInnerServiceSMO storehouseInnerServiceSMOImpl;

    /**
     * 验证调拨入库请求参数的合法性
     * 包括申请单验证、物品信息验证、库存验证等
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException   当验证失败时抛出命令异常
     * @throws ParseException 当解析数据时发生错误抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证申请单ID是否存在
        Assert.hasKeyAndValue(reqJson, "applyId", "订单ID为空");
        
        // 查询调拨申请单信息，确保申请单存在且唯一
        AllocationStorehouseApplyDto allocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
        allocationStorehouseApplyDto.setApplyId(reqJson.getString("applyId"));
        List<AllocationStorehouseApplyDto> allocationStorehouseDtos
                = allocationStorehouseApplyInnerServiceSMOImpl.queryAllocationStorehouseApplys(allocationStorehouseApplyDto);
        Assert.listOnlyOne(allocationStorehouseDtos, "调拨申请单存在多条");

        // 验证是否包含物品信息
        if (!reqJson.containsKey("resourceStores")) {
            throw new CmdException("未包含物品");
        }
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");

        if (resourceStores == null || resourceStores.size() < 1) {
            throw new CmdException("未包含物品");
        }
        
        // 遍历所有物品信息，进行详细验证
        JSONObject resourceStore = null;
        ResourceStoreTimesDto resourceStoreTimesDto = null;
        List<ResourceStoreTimesDto> resourceStoreTimesDtos = null;
        double originStock = 0;
        for (int detailIndex = 0; detailIndex < resourceStores.size(); detailIndex++) {
            resourceStore = resourceStores.getJSONObject(detailIndex);
            
            // 验证物品必填字段
            Assert.hasKeyAndValue(resourceStore, "quantity", "调拨数量未包含");
            Assert.hasKeyAndValue(resourceStore, "shIda", "原仓库不存在");
            Assert.hasKeyAndValue(resourceStore, "shIdz", "目标仓库不存在");
            Assert.hasKeyAndValue(resourceStore, "timesId", "单价未填写");
            Assert.hasKeyAndValue(resourceStore, "resCode", "物品编码未填写");
            Assert.hasKeyAndValue(resourceStore, "asId", "调拨明细未填写");
            
            // 查询物品库存信息
            resourceStoreTimesDto = new ResourceStoreTimesDto();
            resourceStoreTimesDto.setTimesId(resourceStore.getString("timesId"));
            resourceStoreTimesDto.setShId(resourceStore.getString("shIda"));
            resourceStoreTimesDtos = resourceStoreTimesV1InnerServiceSMOImpl.queryResourceStoreTimess(resourceStoreTimesDto);
            Assert.listOnlyOne(resourceStoreTimesDtos, "物品不存在");
            
            // 验证库存是否充足
            originStock = Double.parseDouble(resourceStoreTimesDtos.get(0).getStock());
            double quantity = Double.parseDouble(resourceStore.getString("quantity"));
            if (originStock < quantity) {
                throw new CmdException(reqJson.getString("resCode") + "库存不足");
            }
        }
    }

    /**
     * 执行调拨入库操作
     * 包括库存调拨、申请单状态更新、工作流处理等业务逻辑
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException   当执行过程中发生错误时抛出命令异常
     * @throws ParseException 当解析数据时发生错误抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        JSONObject resourceStore = null;
        double quantity = 0;
        
        // 生成新的调拨申请ID，用于记录调拨操作
        String newApplyId = GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyId);
        
        // 遍历所有物品，执行调拨操作
        for (int detailIndex = 0; detailIndex < resourceStores.size(); detailIndex++) {
            resourceStore = resourceStores.getJSONObject(detailIndex);
            
            // 查询调拨记录信息
            AllocationStorehouseDto allocationStorehouseDto = new AllocationStorehouseDto();
            allocationStorehouseDto.setApplyId(reqJson.getString("applyId"));
            allocationStorehouseDto.setAsId(resourceStore.getString("asId"));
            List<AllocationStorehouseDto> allocationStorehouseDtos = allocationStorehouseV1InnerServiceSMOImpl.queryAllocationStorehouses(allocationStorehouseDto);
            
            quantity = Double.parseDouble(resourceStore.getString("quantity"));
            
            // 设置新的申请ID并执行调拨操作
            allocationStorehouseDtos.get(0).setApplyIda(newApplyId);
            allocationBMOImpl.doToAllocationStorehouse(allocationStorehouseDtos.get(0), quantity);
            
            // 注释掉的代码：更新调拨明细中的实际调拨库存
            // AllocationStorehousePo allocationStorehousePo = new AllocationStorehousePo();
            // allocationStorehousePo.setAsId(resourceStore.getString("asId"));
            // allocationStorehousePo.setStock(resourceStore.getString("quantity"));
            // allocationStorehouseV1InnerServiceSMOImpl.updateAllocationStorehouse(allocationStorehousePo);
        }
        
        String applyId = reqJson.getString("applyId");
        AllocationStorehouseApplyPo allocationStorehouseApplyPo = new AllocationStorehouseApplyPo();
        allocationStorehouseApplyPo.setApplyId(applyId);
        
        // 处理工作流任务，如果包含taskId则提交流程
        if (reqJson.containsKey("taskId")) {
            reqJson.put("auditCode", "1100");
            reqJson.put("auditMessage", "调拨审核入库成功");
            reqJson.put("id", reqJson.getString("applyId"));
            reqJson.put("storeId", CmdContextUtils.getStoreId(context));
            reqJson.put("nextUserId", reqJson.getString("staffId"));
            
            // 完成任务并判断是否为最后一个任务
            boolean isLastTask = oaWorkflowUserInnerServiceSMOImpl.completeTask(reqJson);
            if (isLastTask) {
                // 如果是最后一个任务，设置状态为结束
                allocationStorehouseApplyPo.setState(AllocationStorehouseApplyDto.STATE_END);
            } else {
                // 否则设置为处理中
                allocationStorehouseApplyPo.setState(AllocationStorehouseApplyDto.STATE_DEALING);
            }
        } else {
            // 不包含taskId时设置为处理中状态
            allocationStorehouseApplyPo.setState(AllocationStorehouseApplyDto.STATE_DEALING);
        }
        
        // 更新调拨申请单状态
        allocationStorehouseApplyPo.setStatusCd("0");
        allocationStorehouseApplyV1InnerServiceSMOImpl.updateAllocationStorehouseApply(allocationStorehouseApplyPo);

        // 创建新的调拨申请记录，用于记录调拨操作
        AllocationStorehouseApplyDto allocationStorehouseApplyDto = new AllocationStorehouseApplyDto();
        allocationStorehouseApplyDto.setApplyId(applyId);
        List<AllocationStorehouseApplyDto> allocationStorehouseApplyDtoList = allocationStorehouseApplyV1InnerServiceSMOImpl.queryAllocationStorehouseApplys(allocationStorehouseApplyDto);
        AllocationStorehouseApplyDto allocationStorehouseApplyDto1 = allocationStorehouseApplyDtoList.get(0);
        
        // 设置新的调拨记录信息
        allocationStorehouseApplyDto1.setApplyId(newApplyId);
        allocationStorehouseApplyDto1.setApplyType("40000"); // 设置申请类型为调拨记录
        allocationStorehouseApplyDto1.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        
        // 查询被调拨仓库的归属小区信息
        String shIda = resourceStore.getString("shIda");
        StorehouseDto storehouseDto = new StorehouseDto();
        storehouseDto.setShId(shIda);
        List<StorehouseDto> storehouseList = storehouseInnerServiceSMOImpl.queryStorehouses(storehouseDto);
        allocationStorehouseApplyDto1.setCommunityId(storehouseList.get(0).getCommunityId());
        allocationStorehouseApplyDto1.setbId("-1");
        
        // 保存新的调拨申请记录
        allocationStorehouseApplyInnerServiceSMOImpl.saveAllocationStorehouseApplys(allocationStorehouseApplyDto1);

        // 设置响应结果
        context.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_OK, "采购申请成功"));
    }
}