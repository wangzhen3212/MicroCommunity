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

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.dto.purchase.PurchaseApplyDetailDto;
import com.java110.intf.common.IPurchaseApplyUserInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyDetailInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyDetailV1InnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyV1InnerServiceSMO;
import com.java110.po.purchase.PurchaseApplyDetailPo;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 采购申请删除命令类
 * 
 * 该类负责处理采购申请单的删除操作，包括验证删除条件、删除采购申请主表数据、
 * 删除采购申请明细数据以及取消相关的审批流程任务。
 * 
 * 类表述：删除
 * 服务编码：purchaseApply.deletePurchaseApply
 * 请求路劲：/app/purchaseApply.DeletePurchaseApply
 * add by 吴学文 at 2022-08-08 13:21:13 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "purchaseApply.deletePurchaseApply")
public class DeletePurchaseApplyCmd extends Cmd {
    private static Logger logger = LoggerFactory.getLogger(DeletePurchaseApplyCmd.class);

    @Autowired
    private IPurchaseApplyV1InnerServiceSMO purchaseApplyV1InnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyInnerServiceSMO purchaseApplyInnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyDetailInnerServiceSMO purchaseApplyDetailInnerServiceSMOImpl;

    @Autowired
    private IPurchaseApplyUserInnerServiceSMO iPurchaseApplyUserInnerServiceSMO;

    @Autowired
    private IPurchaseApplyDetailV1InnerServiceSMO purchaseApplyDetailV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的订单号信息
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求参数中必须包含applyOrderId字段
        Assert.hasKeyAndValue(reqJson, "applyOrderId", "订单号不能为空");
    }

    /**
     * 执行删除采购申请命令
     * 
     * 该方法执行采购申请单的删除操作，包括：
     * 1. 验证采购申请单状态是否允许删除
     * 2. 删除采购申请主表数据
     * 3. 删除采购申请明细数据
     * 4. 取消相关的审批流程任务
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 从请求参数中获取采购申请单号
        String applyOrderId = reqJson.getString("applyOrderId");
        
        // 根据订单号查询采购申请单信息
        PurchaseApplyDto purchaseApplyDto = new PurchaseApplyDto();
        purchaseApplyDto.setApplyOrderId(applyOrderId);
        List<PurchaseApplyDto> purchaseApplyDtos = purchaseApplyInnerServiceSMOImpl.queryPurchaseApplys(purchaseApplyDto);
        
        // 验证查询结果，必须存在且仅存在一条记录
        if (purchaseApplyDtos.size() != 1) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "采购申请单出现多条或者未找到采购申请单！");
            context.setResponseEntity(responseEntity);
            return;
        }
        
        // 检查采购申请订单状态：只有状态为"1000"（待处理）的订单才能被删除
        // 对于采购申请类型的订单进行状态验证
        if (!"1000".equals(purchaseApplyDtos.get(0).getState()) && PurchaseApplyDto.RES_ORDER_TYPE_ENTER.equals(purchaseApplyDtos.get(0).getResOrderType())) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "您的采购申请订单状态已改变，无法进行取消操作！");
            context.setResponseEntity(responseEntity);
            return;
        }
        
        // 对于物品领用类型的订单进行状态验证
        if (!"1000".equals(purchaseApplyDtos.get(0).getState()) && PurchaseApplyDto.RES_ORDER_TYPE_OUT.equals(purchaseApplyDtos.get(0).getResOrderType()) ) {
            ResponseEntity<String> responseEntity = ResultVo.createResponseEntity(ResultVo.CODE_BUSINESS_VERIFICATION, "您的物品领用订单状态已改变，无法进行取消操作！");
            context.setResponseEntity(responseEntity);
            return;
        }

        // 执行删除操作：删除采购申请主表和明细表数据
        deletePurchaseApply(reqJson);
        deletePurchaseApplyDetail(reqJson);

        // 取消审批流程任务
        PurchaseApplyPo purchaseApplyPo = BeanConvertUtil.covertBean(reqJson, PurchaseApplyPo.class);
        iPurchaseApplyUserInnerServiceSMO.deleteTask(purchaseApplyPo);

        // 返回成功响应
        context.setResponseEntity(ResultVo.success());
    }

    /**
     * 删除采购申请主表数据
     * 
     * 根据传入的参数删除采购申请主表记录
     * 
     * @param paramInJson 接口调用方传入的参数，包含采购申请单信息
     * @throws CmdException 当删除操作失败时抛出异常
     */
    private void deletePurchaseApply(JSONObject paramInJson) {
        // 将JSON参数转换为采购申请PO对象
        PurchaseApplyPo purchaseApplyPo = BeanConvertUtil.covertBean(paramInJson, PurchaseApplyPo.class);
        // 调用服务删除采购申请记录
        int flag = purchaseApplyV1InnerServiceSMOImpl.deletePurchaseApply(purchaseApplyPo);

        // 验证删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }
    }

    /**
     * 删除采购申请明细数据
     * 
     * 根据采购申请单号删除对应的所有明细记录，并取消相关的审批流程任务
     * 
     * @param paramInJson 接口调用方传入的参数，包含采购申请单号
     * @throws CmdException 当删除操作失败时抛出异常
     */
    private void deletePurchaseApplyDetail(JSONObject paramInJson) {
        // 根据订单号查询采购申请明细列表
        PurchaseApplyDetailDto purchaseApplyDetailDto = new PurchaseApplyDetailDto();
        purchaseApplyDetailDto.setApplyOrderId(paramInJson.getString("applyOrderId"));
        List<PurchaseApplyDetailDto> purchaseApplyDetailDtos = purchaseApplyDetailInnerServiceSMOImpl.queryPurchaseApplyDetails(purchaseApplyDetailDto);
        
        int flag = 0;
        // 遍历明细列表，逐个删除明细记录
        if (purchaseApplyDetailDtos.size() > 0) {
            for (PurchaseApplyDetailDto purchaseApplyDetail : purchaseApplyDetailDtos) {
                // 将明细DTO转换为PO对象
                PurchaseApplyDetailPo purchaseApplyPo = BeanConvertUtil.covertBean(purchaseApplyDetail, PurchaseApplyDetailPo.class);
                // 调用服务删除明细记录
                flag = purchaseApplyDetailV1InnerServiceSMOImpl.deletePurchaseApplyDetail(purchaseApplyPo);

                // 验证删除操作是否成功
                if (flag < 1) {
                    throw new CmdException("删除数据失败");
                }
                
                // 取消流程审批任务
                // 根据业务键（订单号）查询审批任务
                PurchaseApplyDto purchaseDto = new PurchaseApplyDto();
                purchaseDto.setBusinessKey(purchaseApplyDetail.getApplyOrderId());
                List<PurchaseApplyDto> purchaseApplyDtoList = purchaseApplyInnerServiceSMOImpl.getActRuTaskId(purchaseDto);
                
                // 如果存在审批任务，则更新任务处理人为"999999"（系统用户），实现任务取消
                if (purchaseApplyDtoList != null && purchaseApplyDtoList.size() > 0) {
                    PurchaseApplyDto purchaseDto1 = new PurchaseApplyDto();
                    purchaseDto1.setActRuTaskId(purchaseApplyDtoList.get(0).getActRuTaskId());
                    purchaseDto1.setAssigneeUser("999999"); // 设置为系统用户，表示任务已取消
                    purchaseApplyInnerServiceSMOImpl.updateActRuTaskById(purchaseDto1);
                }
            }
        }
    }
}