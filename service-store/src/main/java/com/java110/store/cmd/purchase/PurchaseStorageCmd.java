/**
 * 采购入库命令类
 * 
 * 功能：处理直接采购入库业务，包括采购申请创建、库存更新、物品流水记录等操作
 * 支持直接采购入库模式，将采购物资直接入库并更新库存信息
 * 
 * @author Java110
 * @version 1.0
 * @see Cmd
 */
package com.java110.store.cmd.purchase;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.dto.user.UserDto;
import com.java110.intf.common.IPurchaseApplyUserInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyInnerServiceSMO;
import com.java110.intf.store.IResourceStoreInnerServiceSMO;
import com.java110.intf.store.IResourceStoreTimesV1InnerServiceSMO;
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
 * 直接入库
 *      * {"resourceStores":[{"resId":"852020061636590016","resName":"橡皮擦","resCode":"003","price":"100.00","stock":"0","description":"ada","quantity":"1"},
 *      * {"resId":"852020061729120031","resName":"文档柜","resCode":"002","price":"33.00","stock":"0","description":"蓝色","quantity":"1"}],
 *      * "description":"123123","endUserName":"1","endUserTel":"17797173942","file":"","resOrderType":"10000","staffId":"","staffName":""}
 */
@Java110Cmd(serviceCode = "/purchase/purchaseStorage")
public class PurchaseStorageCmd extends Cmd {

    /** 资源库存服务接口 */
    @Autowired
    private IResourceStoreInnerServiceSMO resourceStoreInnerServiceSMOImpl;

    /** 用户服务接口 */
    @Autowired
    private IUserV1InnerServiceSMO userV1InnerServiceSMOImpl;

    /** 资源库存流水服务接口 */
    @Autowired
    private IResourceStoreTimesV1InnerServiceSMO resourceStoreTimesV1InnerServiceSMOImpl;

    /** 采购申请服务接口 */
    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    /** 采购申请用户服务接口 */
    @Autowired
    private IPurchaseApplyUserInnerServiceSMO purchaseApplyUserInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的参数都存在
     * 
     * @param event   命令事件对象
     * @param context 数据流上下文对象，包含请求和响应信息
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException    当命令执行异常时抛出
     * @throws ParseException  当数据解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必须包含采购物资信息
        Assert.hasKeyAndValue(reqJson, "resourceStores", "必填，请填写申请采购的物资");
        // 验证必须包含采购说明
        Assert.hasKeyAndValue(reqJson, "description", "必填，请填写采购申请说明");
        // 验证必须包含申请类型
        Assert.hasKeyAndValue(reqJson, "resOrderType", "必填，请填写申请类型");
    }

    /**
     * 直接采购入库执行方法
     * 
     * 处理直接采购入库的核心业务逻辑，包括：
     * 1. 创建采购申请记录
     * 2. 更新物资库存信息
     * 3. 记录物资流水信息
     * 4. 启动审批流程（非直接入库模式）
     * 
     * @param event    命令事件对象
     * @param context  数据流上下文对象
     * @param reqJson  请求的JSON数据对象
     * @throws CmdException    当命令执行异常时抛出
     * @throws ParseException  当数据解析异常时抛出
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {

        // 从请求头中获取店铺ID和用户ID
        String storeId = context.getReqHeaders().get("store-id");
        String userId = context.getReqHeaders().get("user-id");
        
        // 查询用户信息
        UserDto userDto = new UserDto();
        userDto.setUserId(userId);
        List<UserDto> userDtos = userV1InnerServiceSMOImpl.queryUsers(userDto);

        // 验证用户信息唯一性
        Assert.listOnlyOne(userDtos, "未包含用户");

        // 获取用户名
        String userName = userDtos.get(0).getName();
        
        // 创建采购申请主对象
        PurchaseApplyPo purchaseApplyPo = new PurchaseApplyPo();
        // 生成采购申请单ID
        purchaseApplyPo.setApplyOrderId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
        purchaseApplyPo.setDescription(reqJson.getString("description"));
        purchaseApplyPo.setUserId(userId);
        purchaseApplyPo.setUserName(userName);
        purchaseApplyPo.setEndUserName(reqJson.getString("endUserName"));
        purchaseApplyPo.setEndUserTel(reqJson.getString("endUserTel"));
        purchaseApplyPo.setStoreId(storeId);
        // 设置订单类型为入库类型
        purchaseApplyPo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_ENTER);
        // 设置状态为已完成
        purchaseApplyPo.setState(PurchaseApplyDto.STATE_END);
        purchaseApplyPo.setCreateTime(DateUtil.getNow(DateUtil.DATE_FORMATE_STRING_A));
        purchaseApplyPo.setDescription("直接采购入库");
        purchaseApplyPo.setCreateUserId(userId);
        purchaseApplyPo.setCreateUserName(userName);
        // 设置入库方式为直接入库
        purchaseApplyPo.setWarehousingWay(PurchaseApplyDto.WAREHOUSING_TYPE_DIRECT);
        purchaseApplyPo.setCommunityId(reqJson.getString("communityId"));
        
        // 获取采购物资列表
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        List<PurchaseApplyDetailPo> purchaseApplyDetailPos = new ArrayList<>();
        
        // 遍历所有采购物资，处理每个物资的入库逻辑
        for (int resourceStoreIndex = 0; resourceStoreIndex < resourceStores.size(); resourceStoreIndex++) {
            JSONObject resourceStore = resourceStores.getJSONObject(resourceStoreIndex);
            
            // 创建采购申请明细对象
            PurchaseApplyDetailPo purchaseApplyDetailPo = BeanConvertUtil.covertBean(resourceStore, PurchaseApplyDetailPo.class);
            purchaseApplyDetailPo.setId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
            purchaseApplyDetailPo.setRemark("直接采购入库");
            purchaseApplyDetailPo.setOriginalStock(resourceStore.getString("stock"));
            // 设置采购数量
            purchaseApplyDetailPo.setQuantity(purchaseApplyDetailPo.getPurchaseQuantity());
            purchaseApplyDetailPos.add(purchaseApplyDetailPo);
            
            // 更新库存信息
            ResourceStorePo resourceStorePo = new ResourceStorePo();
            resourceStorePo.setPurchasePrice(purchaseApplyDetailPo.getPrice());
            resourceStorePo.setResId(purchaseApplyDetailPo.getResId());
            resourceStorePo.setStock(purchaseApplyDetailPo.getPurchaseQuantity());
            resourceStorePo.setResOrderType(PurchaseApplyDto.RES_ORDER_TYPE_ENTER);
            
            // 获取采购数量
            BigDecimal purchaseQuantity = new BigDecimal(purchaseApplyDetailPo.getPurchaseQuantity());
            // 获取原有最小计量总数
            BigDecimal miniStock = new BigDecimal(resourceStore.getString("miniStock"));
            // 获取最小单位数量
            BigDecimal newMiniStock = new BigDecimal(0);
            
            // 验证最小计量单位数量不能为空
            if (StringUtil.isEmpty(resourceStore.getString("miniUnitStock"))) {
                throw new IllegalArgumentException("最小计量单位数量不能为空！");
            }
            BigDecimal miniUnitStock = new BigDecimal(resourceStore.getString("miniUnitStock"));
            
            // 计算新的最小计量总数：采购数量 × 最小单位数量 + 原有最小计量总数
            if (StringUtil.isEmpty(resourceStore.getString("miniStock"))) {
                newMiniStock = purchaseQuantity.multiply(miniUnitStock);
            } else {
                newMiniStock = (purchaseQuantity.multiply(miniUnitStock)).add(miniStock);
            }
            resourceStorePo.setMiniStock(String.valueOf(newMiniStock));
            
            // 更新资源库存
            resourceStoreInnerServiceSMOImpl.updateResourceStore(resourceStorePo);

            // 保存至物品流水表
            ResourceStoreTimesPo resourceStoreTimesPo = new ResourceStoreTimesPo();
            resourceStoreTimesPo.setApplyOrderId(purchaseApplyPo.getApplyOrderId());
            resourceStoreTimesPo.setPrice(purchaseApplyDetailPo.getPrice());
            resourceStoreTimesPo.setStock(purchaseApplyDetailPo.getPurchaseQuantity());
            resourceStoreTimesPo.setResCode(resourceStore.getString("resCode"));
            resourceStoreTimesPo.setStoreId(storeId);
            resourceStoreTimesPo.setShId(purchaseApplyDetailPo.getShId());
            resourceStoreTimesV1InnerServiceSMOImpl.saveOrUpdateResourceStoreTimes(resourceStoreTimesPo);

        }
        
        // 设置采购申请明细列表
        purchaseApplyPo.setPurchaseApplyDetailPos(purchaseApplyDetailPos);
        
        // 保存采购申请记录
        int saveFlag = purchaseApplyInnerServiceSMOImpl.savePurchaseApply(purchaseApplyPo);
        if (saveFlag < 1) {
            context.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_ERROR, "采购申请失败"));
            return;
        }
        
        // 转换采购申请对象并设置当前用户ID
        PurchaseApplyDto purchaseApplyDto = BeanConvertUtil.covertBean(purchaseApplyPo, PurchaseApplyDto.class);
        purchaseApplyDto.setCurrentUserId(purchaseApplyPo.getUserId());
        
        // 如果不是直接入库和紧急入库模式，启动审批流程
        if (!purchaseApplyPo.getWarehousingWay().equals(PurchaseApplyDto.WAREHOUSING_TYPE_DIRECT) && !purchaseApplyPo.getWarehousingWay().equals(PurchaseApplyDto.WAREHOUSING_TYPE_URGENT)) {
            purchaseApplyDto.setNextStaffId(reqJson.getString("staffId"));
            purchaseApplyUserInnerServiceSMOImpl.startProcess(purchaseApplyDto);
        }
        
        // 返回成功响应
        context.setResponseEntity(ResultVo.createResponseEntity(ResultVo.CODE_OK, "采购申请成功"));

    }
}