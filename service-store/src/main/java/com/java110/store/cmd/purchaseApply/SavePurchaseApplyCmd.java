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
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.purchase.PurchaseApplyDto;
import com.java110.intf.common.IPurchaseApplyUserInnerServiceSMO;
import com.java110.intf.store.IPurchaseApplyV1InnerServiceSMO;
import com.java110.po.purchase.PurchaseApplyPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 采购申请保存命令类
 * 
 * 该类负责处理采购申请的保存操作，包括参数验证、数据保存和流程启动等功能。
 * 服务编码：purchaseApply.savePurchaseApply
 * 请求路径：/app/purchaseApply.SavePurchaseApply
 * 
 * @author 吴学文 at 2022-08-08 13:21:13 mail: 928255095@qq.com
 * @open source address: https://gitee.com/wuxw7/MicroCommunity
 * @官网：http://www.homecommunity.cn
 * @温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "purchaseApply.savePurchaseApply")
public class SavePurchaseApplyCmd extends Cmd {

    /**
     * 日志记录器
     */
    private static Logger logger = LoggerFactory.getLogger(SavePurchaseApplyCmd.class);

    /**
     * 采购申请ID前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 采购申请服务接口
     */
    @Autowired
    private IPurchaseApplyV1InnerServiceSMO purchaseApplyV1InnerServiceSMOImpl;

    /**
     * 采购申请流程服务接口
     */
    @Autowired
    private IPurchaseApplyUserInnerServiceSMO iPurchaseApplyUserInnerServiceSMO;

    /**
     * 参数验证方法
     * 
     * 验证请求参数是否完整，确保必要的采购申请信息都存在
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws IllegalArgumentException 当必要参数缺失时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证申请采购的物资信息是否存在
        Assert.hasKeyAndValue(reqJson, "resourceStores", "必填，请填写申请采购的物资");
        // 验证采购申请说明是否存在
        Assert.hasKeyAndValue(reqJson, "description", "必填，请填写采购申请说明");
        // 验证申请类型是否存在
        Assert.hasKeyAndValue(reqJson, "resOrderType", "必填，请填写申请类型");
    }

    /**
     * 执行采购申请保存命令
     * 
     * 处理采购申请的保存逻辑，包括设置初始状态、生成订单ID、保存数据到数据库，
     * 并启动采购申请审批流程
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求的JSON数据对象
     * @throws CmdException 当保存数据失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 设置采购申请初始状态为待处理(1000)
        reqJson.put("state", "1000");
        // 设置创建时间为当前时间
        reqJson.put("createTime", DateUtil.getCurrentDate());
        // 生成申请订单ID
        reqJson.put("applyOrderId", GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));
        
        // 将JSON对象转换为采购申请PO对象
        PurchaseApplyPo purchaseApplyPo = BeanConvertUtil.covertBean(reqJson, PurchaseApplyPo.class);
        // 设置采购申请ID，使用前缀生成唯一ID
        purchaseApplyPo.setApplyOrderId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务保存采购申请数据到数据库
        int flag = purchaseApplyV1InnerServiceSMOImpl.savePurchaseApply(purchaseApplyPo);

        // 检查保存操作是否成功，失败则抛出异常
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 将JSON对象转换为采购申请DTO对象，用于流程启动
        PurchaseApplyDto purchaseApplyDto = BeanConvertUtil.covertBean(reqJson, PurchaseApplyDto.class);
        // 设置当前用户ID
        purchaseApplyDto.setCurrentUserId(reqJson.getString("userId"));
        // 启动采购申请审批流程
        iPurchaseApplyUserInnerServiceSMO.startProcess(purchaseApplyDto);

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}