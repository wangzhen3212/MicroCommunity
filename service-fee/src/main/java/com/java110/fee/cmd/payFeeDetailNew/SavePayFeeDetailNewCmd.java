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
package com.java110.fee.cmd.payFeeDetailNew;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.fee.IPayFeeDetailV1InnerServiceSMO;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 类表述：保存
 * 服务编码：payFeeDetailNew.savePayFeeDetailNew
 * 请求路劲：/app/payFeeDetailNew.SavePayFeeDetailNew
 * add by 吴学文 at 2021-12-06 21:10:16 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 支付费用明细新增命令类
 * 负责处理支付费用明细的新增操作，包括参数验证和数据保存
 * 通过注解@Java110Cmd定义服务编码，对应前端请求路由
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-12-06
 */
@Java110Cmd(serviceCode = "payFeeDetailNew.savePayFeeDetailNew")
public class SavePayFeeDetailNewCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SavePayFeeDetailNewCmd.class);

    /**
     * 主键ID前缀，用于生成明细ID
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 支付费用明细服务接口，用于数据持久化操作
     */
    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailNewV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求报文中的必填字段是否完整
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求报文JSON对象，包含前端传递的参数
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证明细ID字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "detailId", "请求报文中未包含detailId");
        // 验证费用ID字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "feeId", "请求报文中未包含feeId");
        // 验证小区ID字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证周期字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "cycles", "请求报文中未包含cycles");
        // 验证应收金额字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "receivableAmount", "请求报文中未包含receivableAmount");
        // 验证实收金额字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "receivedAmount", "请求报文中未包含receivedAmount");
        // 验证优惠率字段是否存在且不为空
        Assert.hasKeyAndValue(reqJson, "primeRate", "请求报文中未包含primeRate");
    }

    /**
     * 执行命令方法
     * 处理支付费用明细的新增业务逻辑，包括数据转换、ID生成和数据保存
     * 使用@Java110Transactional注解确保事务性操作
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求报文JSON对象
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为支付费用明细PO对象
        PayFeeDetailPo payFeeDetailNewPo = BeanConvertUtil.covertBean(reqJson, PayFeeDetailPo.class);
        
        // 生成明细ID，使用前缀+序列号的方式
        payFeeDetailNewPo.setDetailId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 设置默认不开具发票标识
        payFeeDetailNewPo.setOpenInvoice("N");
        
        // 调用服务层保存支付费用明细数据
        int flag = payFeeDetailNewV1InnerServiceSMOImpl.savePayFeeDetailNew(payFeeDetailNewPo);

        // 检查保存结果，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}