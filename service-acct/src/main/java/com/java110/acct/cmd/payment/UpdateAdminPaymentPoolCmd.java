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
 * 类表述：更新
 * 服务编码：paymentPool.updatePaymentPool
 * 请求路劲：/app/paymentPool.UpdatePaymentPool
 * add by 吴学文 at 2023-10-25 11:52:42 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 支付池管理更新命令类
 * 负责处理管理员对支付池配置的更新操作，包括支付池基本信息、动态字段值和费用配置关联关系的更新
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-10-25
 */
@Java110Cmd(serviceCode = "payment.updateAdminPaymentPool")
public class UpdateAdminPaymentPoolCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(UpdateAdminPaymentPoolCmd.class);

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
     * 验证请求参数的有效性和完整性，包括必填字段检查、动态字段验证和费用配置验证
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param cmdDataFlowContext 命令数据流上下文，用于获取和设置请求响应数据
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证管理员权限
        super.validateAdmin(cmdDataFlowContext);
        
        // 验证必填字段
        Assert.hasKeyAndValue(reqJson, "ppId", "ppId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");

        // 验证是否包含动态字段
        Assert.hasKey(reqJson, "paymentKeys", "未包含动态字段");

        // 获取动态字段数组
        JSONArray paymentKeys = reqJson.getJSONArray("paymentKeys");

        // 验证动态字段是否为空
        if (ListUtil.isNull(paymentKeys)) {
            throw new CmdException("未包含字段");
        }
        
        JSONObject columns = null;
        // 遍历动态字段数组，验证每个字段的完整性
        // 字段格式示例：{"columnKey":"WECHAT_KEY","keyId":"2","name":"支付密码","page":-1,"paymentType":"WECHAT",
        // "records":0,"remark":"请输入微信v2支付秘钥","row":0,"statusCd":"0","total":0,"columnValue":"1"}
        for (int keyIndex = 0; keyIndex < paymentKeys.size(); keyIndex++) {
            columns = paymentKeys.getJSONObject(keyIndex);
            // 验证字段键和值是否存在
            Assert.hasKeyAndValue(columns, "columnKey", "未包含key");
            Assert.hasKeyAndValue(columns, "columnValue", reqJson.getString("name") + "未填写值");
        }

        // 如果支付类型不是费用配置类型，则不需要验证费用配置
        if (!PaymentPoolDto.PAY_TYPE_FEE_CONFIG.equals(reqJson.getString("payType"))) {
            return;
        }

        // 验证费用配置ID数组
        JSONArray configIds = reqJson.getJSONArray("configIds");
        if (configIds == null || configIds.isEmpty()) {
            throw new CmdException("未包含费用项");
        }
    }

    /**
     * 执行支付池更新命令
     * 更新支付池基本信息、动态字段值和费用配置关联关系
     *
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当更新操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为支付池PO对象
        PaymentPoolPo paymentPoolPo = BeanConvertUtil.covertBean(reqJson, PaymentPoolPo.class);
        
        // 更新支付池基本信息
        int flag = paymentPoolV1InnerServiceSMOImpl.updatePaymentPool(paymentPoolPo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("更新数据失败");
        }
        
        // 删除原有的支付池字段值
        PaymentPoolValuePo paymentPoolValuePo = new PaymentPoolValuePo();
        paymentPoolValuePo.setPpId(paymentPoolPo.getPpId());
        paymentPoolValuePo.setCommunityId(paymentPoolPo.getCommunityId());
        paymentPoolValueV1InnerServiceSMOImpl.deletePaymentPoolValue(paymentPoolValuePo);

        // 保存新的动态字段值
        JSONArray paymentKeys = reqJson.getJSONArray("paymentKeys");
        JSONObject columns = null;

        // 遍历动态字段数组，逐个保存字段值
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

        // 如果支付类型不是费用配置类型，则不需要处理费用配置关联关系
        if (!PaymentPoolDto.PAY_TYPE_FEE_CONFIG.equals(reqJson.getString("payType"))) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }
        
        // 获取费用配置ID数组
        JSONArray configIds = reqJson.getJSONArray("configIds");
        
        // 查询费用配置详细信息
        FeeConfigDto feeConfigDto = new FeeConfigDto();
        feeConfigDto.setConfigIds(configIds.toArray(new String[configIds.size()]));
        feeConfigDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeConfigDto> feeConfigDtos = payFeeConfigV1InnerServiceSMOImpl.queryPayFeeConfigs(feeConfigDto);

        // 如果查询结果为空，直接返回成功
        if (feeConfigDtos == null || feeConfigDtos.isEmpty()) {
            cmdDataFlowContext.setResponseEntity(ResultVo.success());
            return;
        }

        // 删除原有的支付池配置关联关系
        PaymentPoolConfigPo paymentPoolConfigPo = new PaymentPoolConfigPo();
        paymentPoolConfigPo.setPpId(paymentPoolPo.getPpId());
        paymentPoolConfigPo.setCommunityId(paymentPoolPo.getCommunityId());
        paymentPoolConfigV1InnerServiceSMOImpl.deletePaymentPoolConfig(paymentPoolConfigPo);

        // 保存新的支付池配置关联关系
        for (FeeConfigDto tmpFeeConfigDto : feeConfigDtos) {
            paymentPoolConfigPo = new PaymentPoolConfigPo();
            paymentPoolConfigPo.setConfigId(tmpFeeConfigDto.getConfigId());
            paymentPoolConfigPo.setFeeName(tmpFeeConfigDto.getFeeName());
            paymentPoolConfigPo.setCommunityId(paymentPoolPo.getCommunityId());
            paymentPoolConfigPo.setPpId(paymentPoolPo.getPpId());
            paymentPoolConfigPo.setPpcId(GenerateCodeFactory.getGeneratorId("11"));
            paymentPoolConfigV1InnerServiceSMOImpl.savePaymentPoolConfig(paymentPoolConfigPo);
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}