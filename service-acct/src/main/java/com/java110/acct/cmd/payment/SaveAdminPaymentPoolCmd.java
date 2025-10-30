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
package com.java110.acct.cmd.payment;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.payment.PaymentPoolDto;
import com.java110.intf.acct.IPaymentPoolConfigV1InnerServiceSMO;
import com.java110.intf.acct.IPaymentPoolV1InnerServiceSMO;
import com.java110.intf.acct.IPaymentPoolValueV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeConfigV1InnerServiceSMO;
import com.java110.po.payment.PaymentPoolConfigPo;
import com.java110.po.payment.PaymentPoolPo;
import com.java110.po.payment.PaymentPoolValuePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 支付池管理保存命令类
 * 
 * 该类负责处理支付池配置的保存操作，包括支付池基本信息、动态字段配置和费用项关联关系的保存
 * 支持不同类型的支付配置，包括全局支付和费用项关联支付
 * 
 * @author 吴学文 at 2023-10-25 11:52:42 mail: 928255095@qq.com
 * @version 1.0
 * @see Cmd
 */
@Java110Cmd(serviceCode = "payment.saveAdminPaymentPool")
public class SaveAdminPaymentPoolCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(SaveAdminPaymentPoolCmd.class);

    /**
     * ID生成前缀
     */
    public static final String CODE_PREFIX_ID = "10";

    /**
     * 支付池服务接口
     */
    @Autowired
    private IPaymentPoolV1InnerServiceSMO paymentPoolV1InnerServiceSMOImpl;

    /**
     * 支付池字段值服务接口
     */
    @Autowired
    private IPaymentPoolValueV1InnerServiceSMO paymentPoolValueV1InnerServiceSMOImpl;

    /**
     * 费用配置服务接口
     */
    @Autowired
    private IPayFeeConfigV1InnerServiceSMO payFeeConfigV1InnerServiceSMOImpl;

    /**
     * 支付池配置服务接口
     */
    @Autowired
    private IPaymentPoolConfigV1InnerServiceSMO paymentPoolConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括必填字段检查、动态字段验证和业务规则校验
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
        
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "paymentName", "请求报文中未包含paymentName");
        Assert.hasKeyAndValue(reqJson, "paymentType", "请求报文中未包含paymentType");
        Assert.hasKeyAndValue(reqJson, "communityId", "请求报文中未包含communityId");
        Assert.hasKeyAndValue(reqJson, "state", "请求报文中未包含state");
        Assert.hasKeyAndValue(reqJson, "payType", "请求报文中未包含payType");

        // 验证动态字段配置
        Assert.hasKey(reqJson, "paymentKeys", "未包含动态字段");
        JSONArray paymentKeys = reqJson.getJSONArray("paymentKeys");
        if (ListUtil.isNull(paymentKeys)) {
            throw new CmdException("未包含字段");
        }
        
        // 验证动态字段的完整性
        JSONObject columns = null;
        for (int keyIndex = 0; keyIndex < paymentKeys.size(); keyIndex++) {
            columns = paymentKeys.getJSONObject(keyIndex);
            Assert.hasKeyAndValue(columns, "columnKey", "未包含key");
            Assert.hasKeyAndValue(columns, "columnValue", reqJson.getString("name") + "未填写值");
        }

        // 如果是非费用项支付类型，检查是否已存在相同配置
        if (!PaymentPoolDto.PAY_TYPE_FEE_CONFIG.equals(reqJson.getString("payType"))) {
            PaymentPoolDto paymentPoolDto = new PaymentPoolDto();
            paymentPoolDto.setCommunityId(reqJson.getString("communityId"));
            paymentPoolDto.setPayType(reqJson.getString("payType"));
            int count = paymentPoolV1InnerServiceSMOImpl.queryPaymentPoolsCount(paymentPoolDto);
            if (count > 0) {
                throw new CmdException("已经存在，请勿重复添加，只有支付范围为费用项的可以添加多个支付");
            }
            return;
        }

        // 费用项支付类型必须包含费用项配置
        JSONArray configIds = reqJson.getJSONArray("configIds");
        if (ListUtil.isNull(configIds)) {
            throw new CmdException("未包含费用项");
        }
    }

    /**
     * 执行保存支付池配置命令
     * 
     * 该方法负责保存支付池的完整配置信息，包括：
     * 1. 支付池基本信息
     * 2. 动态字段配置值
     * 3. 费用项关联关系（仅限费用项支付类型）
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当保存操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 保存支付池基本信息
        PaymentPoolPo paymentPoolPo = BeanConvertUtil.covertBean(reqJson, PaymentPoolPo.class);
        paymentPoolPo.setPpId(GenerateCodeFactory.getGeneratorId(CODE_PREFIX_ID));
        int flag = paymentPoolV1InnerServiceSMOImpl.savePaymentPool(paymentPoolPo);

        if (flag < 1) {
            throw new CmdException("保存数据失败");
        }

        // 保存动态字段配置值
        JSONArray paymentKeys = reqJson.getJSONArray("paymentKeys");
        JSONObject columns = null;
        PaymentPoolValuePo paymentPoolValuePo = null;
        for (int keyIndex = 0; keyIndex < paymentKeys.size(); keyIndex++) {
            columns = paymentKeys.getJSONObject(keyIndex);
            paymentPoolValuePo = new PaymentPoolValuePo();
            paymentPoolValuePo.setColumnValue(columns.getString("columnValue"));
            paymentPoolValuePo.setColumnKey(columns.getString("columnKey"));
            paymentPoolValuePo.setCommunityId(paymentPoolPo.getCommunityId());
            paymentPoolValuePo.setPpId(paymentPoolPo.getPpId());
            paymentPoolValuePo.setValueId(GenerateCodeFactory.getGeneratorId("11"));
            paymentPoolValueV1InnerServiceSMOImpl.savePaymentPoolValue(paymentPoolValuePo);
        }

        // 如果是非费用项支付类型，直接返回
        if (!PaymentPoolDto.PAY_TYPE_FEE_CONFIG.equals(reqJson.getString("payType"))) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }

        // 保存费用项关联关系
        JSONArray configIds = reqJson.getJSONArray("configIds");
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigIds(configIds.toArray(new String[configIds.size()]));
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeConfigDto> feeConfigDtos = payFeeConfigV1InnerServiceSMOImpl.queryPayFeeConfigs(feeConfigDto);

        if (ListUtil.isNull(feeConfigDtos)) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }

        // 遍历费用项配置，保存支付池与费用项的关联关系
        PaymentPoolConfigPo paymentPoolConfigPo = null;
        for (FeeConfigDto tmpFeeConfigDto : feeConfigDtos) {
            paymentPoolConfigPo = new PaymentPoolConfigPo();
            paymentPoolConfigPo.setConfigId(tmpFeeConfigDto.getConfigId());
            paymentPoolConfigPo.setFeeName(tmpFeeConfigDto.getFeeName());
            paymentPoolConfigPo.setCommunityId(paymentPoolPo.getCommunityId());
            paymentPoolConfigPo.setPpId(paymentPoolPo.getPpId());
            paymentPoolConfigPo.setPpcId(GenerateCodeFactory.getGeneratorId("11"));
            paymentPoolConfigV1InnerServiceSMOImpl.savePaymentPoolConfig(paymentPoolConfigPo);
        }

        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}