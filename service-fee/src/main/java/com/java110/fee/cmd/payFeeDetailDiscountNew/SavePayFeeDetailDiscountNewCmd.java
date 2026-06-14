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
package com.java110.fee.cmd.payFeeDetailDiscountNew;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.intf.fee.IPayFeeDetailDiscountNewV1InnerServiceSMO;
import com.java110.po.payFee.PayFeeDetailDiscountPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import com.java110.core.log.LoggerFactory;

/**
 * 类表述：保存
 * 服务编码：payFeeDetailDiscountNew.savePayFeeDetailDiscountNew
 * 请求路劲：/app/payFeeDetailDiscountNew.SavePayFeeDetailDiscountNew
 * add by 吴学文 at 2021-12-06 21:52:43 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 费用明细折扣信息保存命令类
 * 
 * 该类负责处理费用明细折扣信息的保存请求，包括参数验证和业务逻辑处理
 * 通过注解@Java110Cmd指定服务编码，用于路由请求到对应的处理器
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2021-12-06
 */
@Java110Cmd(serviceCode = "payFeeDetailDiscountNew.savePayFeeDetailDiscountNew")
public class SavePayFeeDetailDiscountNewCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SavePayFeeDetailDiscountNewCmd.class);

    /**
     * 主键ID前缀，用于生成唯一标识
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 费用明细折扣服务接口，用于数据持久化操作
     */
    @Autowired
    private IPayFeeDetailDiscountNewV1InnerServiceSMO payFeeDetailDiscountNewV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数中是否包含必要的字段，确保请求数据的完整性
     * 
     * @param event CMD事件对象，包含请求相关信息
     * @param cmdDataFlowContext 数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证请求报文中是否包含detailDiscountId字段
        Assert.hasKeyAndValue(reqJson, "detailDiscountId", "请求报文中未包含detailDiscountId");
        // 验证请求报文中是否包含feeId字段
        Assert.hasKeyAndValue(reqJson, "feeId", "请求报文中未包含feeId");
        // 验证请求报文中是否包含detailId字段
        Assert.hasKeyAndValue(reqJson, "detailId", "请求报文中未包含detailId");
        // 验证请求报文中是否包含communityId字段
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        // 验证请求报文中是否包含discountId字段
        Assert.hasKeyAndValue(reqJson, "discountId", "请求报文中未包含discountId");
        // 验证请求报文中是否包含discountPrice字段
        Assert.hasKeyAndValue(reqJson, "discountPrice", "请求报文中未包含discountPrice");
    }

    /**
     * 执行保存费用明细折扣信息的核心方法
     * 
     * 该方法使用@Java110Transactional注解确保事务性操作
     * 将请求参数转换为实体对象，生成主键ID，并调用服务层进行数据保存
     * 
     * @param event CMD事件对象，包含请求相关信息
     * @param cmdDataFlowContext 数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当数据保存失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求JSON对象转换为费用明细折扣实体对象
        PayFeeDetailDiscountPo payFeeDetailDiscountNewPo = BeanConvertUtil.covertBean(reqJson, PayFeeDetailDiscountPo.class);
        
        // 生成唯一的主键ID，使用预设的前缀
        payFeeDetailDiscountNewPo.setDetailDiscountId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        
        // 调用服务层接口保存数据，返回影响的行数
        int flag = payFeeDetailDiscountNewV1InnerServiceSMOImpl.savePayFeeDetailDiscountNew(payFeeDetailDiscountNewPo);

        // 检查保存操作是否成功，如果影响行数小于1则认为保存失败
        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 设置响应结果为成功状态
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}