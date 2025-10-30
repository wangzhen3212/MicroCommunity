/*
 * Copyright 2017-2020 吴学文 and java110 team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.java110.store.cmd.purchaseApply;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.store.IPurchaseApplyDetailV1InnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyV1InnerServiceSMO;
import com.java110.po.purchase.PurchaseApplyDetailPo;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 采购申请更新命令类
 * 
 * 该类负责处理采购申请单的更新操作，包括验证请求参数、更新采购申请主信息、
 * 删除原有采购明细并重新添加新的采购明细数据。
 * 
 * 服务编码：purchaseApply.updatePurchaseApply
 * 请求路径：/app/purchaseApply.UpdatePurchaseApply
 * add by 吴学文 at 2022-08-08 13:21:13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "purchaseApply.updatePurchaseApply")
public class UpdatePurchaseApplyCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdatePurchaseApplyCmd.class);

    /**
     * 采购申请服务接口
     */
    @Autowired
    private IPurchaseApplyV1InnerServiceSMO purchaseApplyV1InnerServiceSMOImpl;

    /**
     * 采购申请明细服务接口
     */
    @Autowired
    private IPurchaseApplyDetailV1InnerServiceSMO purchaseApplyDetailV1InnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证更新采购申请所需的必要参数是否完整有效
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必填字段是否存在
        Assert.hasKeyAndValue(reqJson, "applyOrderId", "订单号不能为空");
        Assert.hasKeyAndValue(reqJson, "description", "必填，请填写采购申请说明");
        Assert.hasKeyAndValue(reqJson, "endUserName", "必填，请填写采购联系人");
        Assert.hasKeyAndValue(reqJson, "endUserTel", "必填，请填写采购联系电话");
        Assert.hasKeyAndValue(reqJson, "communityId", "必填，请填写小区信息");
        Assert.hasKey(reqJson, "resourceStores", "必填，请填写申请采购的物资");
        
        // 验证采购物品列表不能为空
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        if (resourceStores == null || resourceStores.size() < 1) {
            throw new CmdException("未包含采购物品");
        }
    }

    /**
     * 执行采购申请更新命令
     * 
     * 该方法负责执行采购申请单的完整更新流程，包括：
     * 1. 更新采购申请主信息
     * 2. 删除原有的采购明细数据
     * 3. 重新添加新的采购明细数据
     * 4. 设置响应结果
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 获取采购物品列表并从请求中移除，避免影响主表数据转换
        JSONArray resourceStores = reqJson.getJSONArray("resourceStores");
        reqJson.remove("resourceStores");
        
        // 将请求JSON转换为采购申请PO对象
        PurchaseApplyPo purchaseApplyPo = BeanConvertUtil.covertBean(reqJson, PurchaseApplyPo.class);
        
        // 更新采购申请主信息
        int flag = purchaseApplyV1InnerServiceSMOImpl.updatePurchaseApply(purchaseApplyPo);
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 删除原有的采购申请明细数据
        PurchaseApplyDetailPo purchaseApplyDetailPo = new PurchaseApplyDetailPo();
        purchaseApplyDetailPo.setApplyOrderId(purchaseApplyPo.getApplyOrderId());
        purchaseApplyDetailV1InnerServiceSMOImpl.deletePurchaseApplyDetail(purchaseApplyDetailPo);
        
        // 遍历新的采购物品列表，重新添加采购明细
        for (int resourceStoreIndex = 0; resourceStoreIndex < resourceStores.size(); resourceStoreIndex++) {
            JSONObject resourceStore = resourceStores.getJSONObject(resourceStoreIndex);
            
            // 设置原始库存信息
            resourceStore.put("originalStock", resourceStore.getString("stock"));
            JSONArray timeList = resourceStore.getJSONArray("times");
            
            // 将采购物品JSON转换为采购明细PO对象
            purchaseApplyDetailPo = BeanConvertUtil.covertBean(resourceStore, PurchaseApplyDetailPo.class);
            purchaseApplyDetailPo.setPurchaseQuantity(resourceStore.getString("quantity"));
            
            // 生成明细ID并设置关联的申请订单ID
            purchaseApplyDetailPo.setId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
            purchaseApplyDetailPo.setApplyOrderId(purchaseApplyPo.getApplyOrderId());
            
            // 获取采购参考价格
            String consultPrice = null;
            if (resourceStore.containsKey("timesId") && !StringUtil.isEmpty(resourceStore.getString("timesId"))) {
                // 遍历时间列表，匹配对应的价格信息
                for (int timesIndex = 0; timesIndex < timeList.size(); timesIndex++) {
                    JSONObject times = timeList.getJSONObject(timesIndex);
                    if (times.getString("timesId").equals(resourceStore.getString("timesId"))) {
                        consultPrice = times.getString("price");
                    }
                }
            }
            
            // 设置参考价格和业务ID
            purchaseApplyDetailPo.setConsultPrice(consultPrice);
            purchaseApplyDetailPo.setbId("-1");
            
            // 保存采购申请明细
            purchaseApplyDetailV1InnerServiceSMOImpl.savePurchaseApplyDetail(purchaseApplyDetailPo);
        }
        
        // 设置成功响应
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}