/**
 * 删除物品库存命令类
 * 
 * 该命令用于处理删除物品库存的业务逻辑，包括验证物品是否可删除和执行删除操作。
 * 在删除物品前会检查是否存在相关的采购、领用和调拨记录，确保数据完整性。
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
import com.java110.dto.purchase.AllocationStorehouseDto;
import com.java110.dto.file.FileRelDto;
import com.java110.dto.purchase.PurchaseApplyDetailDto;
import com.java110.intf.common.IFileInnerServiceSMO;
import com.java110.intf.common.IFileRelInnerServiceSMO;
import com.java110.intf.store.IAllocationStorehouseInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyDetailInnerServiceSMO;
import com.java110.intf.store.IResourceStoreInnerServiceSMO;
import com.java110.intf.store.IResourceStoreV1InnerServiceSMO;
import com.java110.po.file.FileRelPo;
import com.java110.po.purchase.ResourceStorePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "resourceStore.deleteResourceStore")
public class DeleteResourceStoreCmd extends Cmd {

    /** 物品库存服务接口 */
    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    /** 文件关联服务接口 */
    @Autowired
    private IFileRelInnerServiceSMO fileRelInnerServiceSMOImpl;

    /** 文件服务接口 */
    @Autowired
    private IFileInnerServiceSMO fileInnerServiceSMOImpl;

    /** 采购申请明细服务接口 */
    @Autowired
    private IPurchaseApplyDetailInnerServiceSMO purchaseApplyDetailInnerServiceSMOImpl;

    /** 调拨记录服务接口 */
    @Autowired
    private IAllocationStorehouseInnerServiceSMO allocationStorehouseInnerServiceSMOImpl;

    /** 物品库存V1服务接口 */
    @Autowired
    private IResourceStoreV1InnerServiceSMO resourceStoreV1InnerServiceSMOImpl;

    /**
     * 验证删除物品库存的请求参数和业务规则
     * 
     * 该方法用于验证删除物品库存的请求是否合法，包括：
     * 1. 检查必要的参数是否存在
     * 2. 验证物品是否存在于采购或领用记录中
     * 3. 验证物品是否存在于调拨记录中
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求参数中必须包含物品ID和商户ID
        Assert.hasKeyAndValue(reqJson, "resId", "物品ID不能为空");
        Assert.hasKeyAndValue(reqJson, "storeId", "商户信息不能为空");
        
        // 根据物品ID查询采购明细表，检查是否存在采购或领用记录
        String resId = reqJson.getString("resId");
        PurchaseApplyDetailDto purchaseApplyDetailDto = new PurchaseApplyDetailDto();
        purchaseApplyDetailDto.setResId(resId);
        List<PurchaseApplyDetailDto> purchaseApplyDetailDtos = purchaseApplyDetailInnerServiceSMOImpl.queryPurchaseApplyDetails(purchaseApplyDetailDto);
        // 如果存在采购或领用记录，则不允许删除物品
        Assert.listIsNull(purchaseApplyDetailDtos, "该物品存在采购或领用记录，不能删除！");
        
        // 根据物品ID查询调拨记录，检查是否存在调拨记录
        AllocationStorehouseDto allocationStorehouseDto = new AllocationStorehouseDto();
        allocationStorehouseDto.setResId(resId);
        List<AllocationStorehouseDto> allocationStorehouseDtos = allocationStorehouseInnerServiceSMOImpl.queryAllocationStorehouses(allocationStorehouseDto);
        // 如果存在调拨记录，则不允许删除物品
        Assert.listIsNull(allocationStorehouseDtos, "该物品存在调拨记录，不能删除！");
    }

    /**
     * 执行删除物品库存的命令
     * 
     * 该方法负责执行实际的删除操作，包括：
     * 1. 删除物品库存记录
     * 2. 删除与物品相关的文件关联记录
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当删除操作失败时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        String resId = reqJson.getString("resId");
        // 设置状态为删除状态（1表示已删除）
        reqJson.put("statusCd", "1");
        // 将JSON对象转换为资源库存PO对象
        ResourceStorePo resourceStorePo = BeanConvertUtil.covertBean(reqJson, ResourceStorePo.class);

        // 调用服务删除物品库存记录
        int flag = resourceStoreV1InnerServiceSMOImpl.deleteResourceStore(resourceStorePo);
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }
        
        // 查询与物品相关的文件关联记录
        FileRelDto fileRelDto = new FileRelDto();
        fileRelDto.setObjId(resId);
        List<FileRelDto> fileRelDtos = fileRelInnerServiceSMOImpl.queryFileRels(fileRelDto);
        
        // 如果存在文件关联记录，则逐个删除
        if (fileRelDtos != null && fileRelDtos.size() > 0) {
            // 遍历所有文件关联记录，删除文件表图片
            for (FileRelDto fileRel : fileRelDtos) {
                FileRelPo fileRelpo = new FileRelPo();
                fileRelpo.setFileRelId(fileRel.getFileRelId());
                // 删除文件关联记录
                flag = fileRelInnerServiceSMOImpl.deleteFileRel(fileRelpo);
                if (flag < 1) {
                    throw new CmdException("保存数据失败");
                }
            }
        }
    }
}