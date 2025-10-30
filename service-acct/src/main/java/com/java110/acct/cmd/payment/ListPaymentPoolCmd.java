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

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.payment.PaymentPoolConfigDto;
import com.java110.dto.payment.PaymentPoolValueDto;
import com.java110.intf.acct.IPaymentPoolConfigV1InnerServiceSMO;
import com.java110.intf.acct.IPaymentPoolV1InnerServiceSMO;
import com.java110.intf.acct.IPaymentPoolValueV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.payment.PaymentPoolDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询支付池列表命令类
 * 服务编码：paymentPool.listPaymentPool
 * 请求路劲：/app/paymentPool.ListPaymentPool
 * 功能描述：该命令类用于处理支付池列表查询请求，包括参数验证、数据查询和结果返回
 * add by 吴学文 at 2023-10-25 11:52:42 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "payment.listPaymentPool")
public class ListPaymentPoolCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListPaymentPoolCmd.class);
    
    /**
     * 支付池服务接口
     */
    @Autowired
    private IPaymentPoolV1InnerServiceSMO paymentPoolV1InnerServiceSMOImpl;

    /**
     * 支付池值服务接口
     */
    @Autowired
    private IPaymentPoolValueV1InnerServiceSMO paymentPoolValueV1InnerServiceSMOImpl;

    /**
     * 支付池配置服务接口
     */
    @Autowired
    private IPaymentPoolConfigV1InnerServiceSMO paymentPoolConfigV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，包括分页参数和必填字段
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证属性参数
        super.validateProperty(cmdDataFlowContext);
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 命令执行方法
     * 功能描述：执行支付池列表查询逻辑，包括数据查询、结果处理和响应返回
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为支付池DTO对象
        PaymentPoolDto paymentPoolDto = BeanConvertUtil.covertBean(reqJson, PaymentPoolDto.class);

        // 查询支付池总数
        int count = paymentPoolV1InnerServiceSMOImpl.queryPaymentPoolsCount(paymentPoolDto);

        List<PaymentPoolDto> paymentPoolDtos = null;

        if (count > 0) {
            // 如果存在数据，查询支付池列表
            paymentPoolDtos = paymentPoolV1InnerServiceSMOImpl.queryPaymentPools(paymentPoolDto);
        } else {
            // 如果没有数据，返回空列表
            paymentPoolDtos = new ArrayList<>();
        }

        // 补充配置信息和值信息
        computeConfigAndValues(paymentPoolDtos);

        // 构建返回结果对象，包含分页信息和数据列表
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, paymentPoolDtos);

        // 创建HTTP响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        // 设置响应实体到上下文
        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 计算配置和值信息方法
     * 功能描述：为支付池列表补充配置信息和值信息
     * @param paymentPoolDtos 支付池数据列表
     */
    private void computeConfigAndValues(List<PaymentPoolDto> paymentPoolDtos) {
        // 检查支付池列表是否为空
        if(paymentPoolDtos == null || paymentPoolDtos.isEmpty()){
            return;
        }

        // 查询支付池值信息
        PaymentPoolValueDto paymentPoolValueDto = new PaymentPoolValueDto();
        paymentPoolValueDto.setPpId(paymentPoolDtos.get(0).getPpId());
        paymentPoolValueDto.setCommunityId(paymentPoolDtos.get(0).getCommunityId());
        List<PaymentPoolValueDto> values = paymentPoolValueV1InnerServiceSMOImpl.queryPaymentPoolValues(paymentPoolValueDto);
        // 设置值信息到支付池对象
        paymentPoolDtos.get(0).setValues(values);

        // 查询支付池配置信息
        PaymentPoolConfigDto paymentPoolConfigDto = new PaymentPoolConfigDto();
        paymentPoolConfigDto.setPpId(paymentPoolDtos.get(0).getPpId());
        paymentPoolConfigDto.setCommunityId(paymentPoolDtos.get(0).getCommunityId());
        List<PaymentPoolConfigDto> configs = paymentPoolConfigV1InnerServiceSMOImpl.queryPaymentPoolConfigs(paymentPoolConfigDto);
        // 设置配置信息到支付池对象
        paymentPoolDtos.get(0).setConfigs(configs);
    }
}