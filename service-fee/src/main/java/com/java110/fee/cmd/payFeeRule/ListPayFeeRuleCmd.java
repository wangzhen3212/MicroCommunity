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
import com.java110.dto.payFee.PayFeeRuleBillDto;
import com.java110.intf.fee.IPayFeeRuleBillV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeRuleV1InnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.payFee.PayFeeRuleDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 类表述：查询
 * 服务编码：payFeeRule.listPayFeeRule
 * 请求路劲：/app/payFeeRule.ListPayFeeRule
 * add by 吴学文 at 2023-10-18 18:08:44 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 支付费用规则查询命令类
 * 
 * 该类负责处理支付费用规则的列表查询请求，支持根据社区ID等条件查询费用规则信息，
 * 同时支持根据费用ID查询关联的账单规则信息
 * 
 * @author 吴学文
 * @version 1.0
 * @since 2023-10-18
 */
@Java110Cmd(serviceCode = "payFeeRule.listPayFeeRule")
public class ListPayFeeRuleCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListPayFeeRuleCmd.class);
    
    /**
     * 支付费用规则服务接口
     */
    @Autowired
    private IPayFeeRuleV1InnerServiceSMO payFeeRuleV1InnerServiceSMOImpl;

    /**
     * 支付费用规则账单服务接口
     */
    @Autowired
    private IPayFeeRuleBillV1InnerServiceSMO payFeeRuleBillV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括分页信息和必填字段
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证社区ID不能为空
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");
    }

    /**
     * 执行命令方法
     * 
     * 处理支付费用规则查询业务逻辑，包括参数转换、数据查询和结果返回
     * 
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 将请求参数转换为PayFeeRuleDto对象
        PayFeeRuleDto payFeeRuleDto = BeanConvertUtil.covertBean(reqJson, PayFeeRuleDto.class);

        // 如果请求中包含费用ID，则查询对应的账单规则
        ifHasFeeId(reqJson, payFeeRuleDto);

        // 查询支付费用规则总数
        int count = payFeeRuleV1InnerServiceSMOImpl.queryPayFeeRulesCount(payFeeRuleDto);

        List<PayFeeRuleDto> payFeeRuleDtos = null;

        if (count > 0) {
            // 查询支付费用规则列表
            payFeeRuleDtos = payFeeRuleV1InnerServiceSMOImpl.queryPayFeeRules(payFeeRuleDto);
        } else {
            // 如果没有数据，返回空列表
            payFeeRuleDtos = new ArrayList<>();
        }

        // 计算总页数并构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, payFeeRuleDtos);

        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 根据费用ID查询规则ID
     * 
     * 如果请求参数中包含feeId，则通过费用ID查询对应的规则ID，并设置到查询条件中
     * 
     * @param reqJson 请求参数JSON对象
     * @param payFeeRuleDto 支付费用规则数据传输对象
     */
    private void ifHasFeeId(JSONObject reqJson, PayFeeRuleDto payFeeRuleDto) {
        // 检查请求参数中是否包含feeId
        if (!reqJson.containsKey("feeId")) {
            return;
        }

        String feeId = reqJson.getString("feeId");

        // 检查feeId是否为空
        if (StringUtil.isEmpty(feeId)) {
            return;
        }

        // 构建账单规则查询条件
        PayFeeRuleBillDto payFeeRuleBillDto = new PayFeeRuleBillDto();
        payFeeRuleBillDto.setFeeId(feeId);
        payFeeRuleBillDto.setCommunityId(payFeeRuleDto.getCommunityId());
        
        // 查询账单规则列表
        List<PayFeeRuleBillDto> payFeeRuleBillDtos = payFeeRuleBillV1InnerServiceSMOImpl.queryPayFeeRuleBills(payFeeRuleBillDto);

        // 如果查询结果为空，直接返回
        if (payFeeRuleBillDtos == null || payFeeRuleBillDtos.isEmpty()) {
            return;
        }

        // 将查询到的规则ID设置到查询条件中
        payFeeRuleDto.setRuleId(payFeeRuleBillDtos.get(0).getRuleId());
    }
}