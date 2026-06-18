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
package com.java110.fee.cmd.payFeeRule;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.fee.FeeDto;
import com.java110.intf.fee.IPayFeeRuleBillV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.payFee.PayFeeRuleBillDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询缴费规则账单列表的命令类
 * 服务编码：payFeeRuleBill.listPayFeeRuleBill
 * 请求路劲：/app/payFeeRuleBill.ListPayFeeRuleBill
 * add by 吴学文 at 2023-10-23 16:59:08 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "payFeeRule.listPayFeeRuleBill")
public class ListPayFeeRuleBillCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListPayFeeRuleBillCmd.class);
    
    /**
     * 缴费规则账单服务接口
     */
    @Autowired
    private IPayFeeRuleBillV1InnerServiceSMO payFeeRuleBillV1InnerServiceSMOImpl;

    /**
     * 费用计算服务接口
     */
    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    /**
     * 参数验证方法
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        super.validatePageInfo(reqJson);
        // 验证小区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行命令方法 - 查询缴费规则账单列表
     * @param event 命令事件
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 如果请求中包含费用ID，则根据费用ID查询账单规则
        ifHasFeeId(reqJson);
        
        // 将请求参数转换为PayFeeRuleBillDto对象
        PayFeeRuleBillDto payFeeRuleBillDto = BeanConvertUtil.covertBean(reqJson, PayFeeRuleBillDto.class);

        // 查询缴费规则账单总数
        int count = payFeeRuleBillV1InnerServiceSMOImpl.queryPayFeeRuleBillsCount(payFeeRuleBillDto);

        List<PayFeeRuleBillDto> payFeeRuleBillDtos = null;

        if (count > 0) {
            // 查询缴费规则账单列表
            payFeeRuleBillDtos = payFeeRuleBillV1InnerServiceSMOImpl.queryPayFeeRuleBills(payFeeRuleBillDto);
        } else {
            payFeeRuleBillDtos = new ArrayList<>();
        }

        // 计算费用价格
        computeFeePrice(payFeeRuleBillDtos);

        // 构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, payFeeRuleBillDtos);

        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 计算费用价格
     * @param payFeeRuleBillDtos 缴费规则账单列表
     */
    private void computeFeePrice(List<PayFeeRuleBillDto> payFeeRuleBillDtos) {

        if (payFeeRuleBillDtos == null || payFeeRuleBillDtos.isEmpty()) {
            return;
        }

        // 遍历每个缴费规则账单，计算费用价格
        for (PayFeeRuleBillDto payFeeRuleBillDto : payFeeRuleBillDtos) {
            // 设置计费周期为1
            payFeeRuleBillDto.setCycle("1");

            try {
                // 计算单个账单的费用价格
                doComputeFeePrice(payFeeRuleBillDto, 1);
            } catch (Exception e) {
                logger.error("查询费用信息 ，费用信息错误", e);
            }
        }
    }

    /**
     * 计算单个费用价格
     * @param feeDto 费用数据传输对象
     * @param oweMonth 欠费月数
     */
    private void doComputeFeePrice(FeeDto feeDto, double oweMonth) {
        String computingFormula = feeDto.getComputingFormula();
        // 获取费用价格信息
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(feeDto);
        // 设置单价
        feeDto.setFeePrice(Double.parseDouble(feePriceAll.get("feePrice").toString()));
        // 设置总价，并进行精度处理
        feeDto.setFeeTotalPrice(MoneyUtil.computePriceScale(Double.parseDouble(feePriceAll.get("feeTotalPrice").toString()),
                feeDto.getScale(),
                Integer.parseInt(feeDto.getDecimalPlace())));
        
        // 计算欠费金额
        BigDecimal curFeePrice = new BigDecimal(feeDto.getFeePrice());
        curFeePrice = curFeePrice.multiply(new BigDecimal(oweMonth));
        feeDto.setAmountOwed(MoneyUtil.computePriceScale(curFeePrice.doubleValue(), feeDto.getScale(), Integer.parseInt(feeDto.getDecimalPlace())) + "");
        
        // 处理动态费用逻辑
        if ("4004".equals(computingFormula)
                && FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())
                && !FeeDto.STATE_FINISH.equals(feeDto.getState())
                && feeDto.getDeadlineTime() == null) {
            // 设置动态费用的截止时间为当前时间
            feeDto.setDeadlineTime(DateUtil.getCurrentDate());
        }
        // 处理租金递增逻辑
        computeFeeSMOImpl.dealRentRate(feeDto);
    }

    /**
     * 如果请求中包含费用ID，则根据费用ID查询规则ID
     * @param reqJson 请求参数JSON对象
     */
    private void ifHasFeeId(JSONObject reqJson) {
        // 检查请求中是否包含费用ID
        if (!reqJson.containsKey("feeId")) {
            return;
        }

        String feeId = reqJson.getString("feeId");

        if (StringUtil.isEmpty(feeId)) {
            return;
        }

        // 根据费用ID查询缴费规则账单
        PayFeeRuleBillDto payFeeRuleBillDto = new PayFeeRuleBillDto();
        payFeeRuleBillDto.setFeeId(feeId);
        payFeeRuleBillDto.setCommunityId(reqJson.getString("communityId"));
        List<PayFeeRuleBillDto> payFeeRuleBillDtos = payFeeRuleBillV1InnerServiceSMOImpl.queryPayFeeRuleBills(payFeeRuleBillDto);

        if (ListUtil.isNull(payFeeRuleBillDtos)) {
            return;
        }
        
        // 用规则ID替换费用ID
        reqJson.remove("feeId");
        reqJson.put("ruleId", payFeeRuleBillDtos.get(0).getRuleId());
    }
}