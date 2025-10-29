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
package com.java110.fee.cmd.feeSub;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.payFee.PayFeeSubDto;
import com.java110.intf.fee.IFeeDetailInnerServiceSMO;
import com.java110.intf.fee.IPayFeeSubV1InnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.po.fee.PayFeePo;
import com.java110.po.payFee.PayFeeSubPo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 类表述：合并费用
 * 服务编码：payFeeSub.deletePayFeeSub
 * 请求路劲：/app/payFeeSub.DeletePayFeeSub
 * add by 吴学文 at 2024-01-05 12:10:47 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */

/**
 * 费用合并命令类
 * 该命令用于处理费用合并业务，将已拆分的费用重新合并回原费用
 * 主要功能包括：验证合并条件、删除拆分记录、结束拆分费用、启用原费用
 * @author 吴学文
 * @version 1.0
 * @since 2024-01-05
 */
@Java110Cmd(serviceCode = "feeSub.mergePayFee")
public class MergePayFeeCmd extends Cmd {
    private static Logger logger = LoggerFactory.getLogger(MergePayFeeCmd.class);

    @Autowired
    private IPayFeeSubV1InnerServiceSMO payFeeSubV1InnerServiceSMOImpl;

    @Autowired
    private IFeeDetailInnerServiceSMO feeDetailInnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，检查费用是否存在、是否满足合并条件
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "feeId", "feeId不能为空");
        Assert.hasKeyAndValue(reqJson, "communityId", "communityId不能为空");

        // 检查费用是否存在 - 通过feeId查询拆分费用
        PayFeeSubDto payFeeSubDto = new PayFeeSubDto();
        payFeeSubDto.setFeeId(reqJson.getString("feeId"));
        payFeeSubDto.setCommunityId(reqJson.getString("communityId"));
        List<PayFeeSubDto> payFeeSubDtos = payFeeSubV1InnerServiceSMOImpl.queryPayFeeSubs(payFeeSubDto);

        // 如果没有找到拆分费用，抛出异常
        if (ListUtil.isNull(payFeeSubDtos)) {
            throw new CmdException("没有需要合并的费用");
        }

        // 通过preFeeId查询所有相关的拆分费用
        payFeeSubDto = new PayFeeSubDto();
        payFeeSubDto.setPreFeeId(payFeeSubDtos.get(0).getPreFeeId());
        payFeeSubDto.setCommunityId(reqJson.getString("communityId"));
        payFeeSubDtos = payFeeSubV1InnerServiceSMOImpl.queryPayFeeSubs(payFeeSubDto);

        // 如果没有找到相关的拆分费用，抛出异常
        if (ListUtil.isNull(payFeeSubDtos)) {
            throw new CmdException("没有需要合并的费用");
        }

        // 检查拆分费用是否存在缴费记录，如果存在缴费记录则不能合并
        for (PayFeeSubDto tmpPayFeeSubDto : payFeeSubDtos) {
            FeeDetailDto feeDetailDto = new FeeDetailDto();
            feeDetailDto.setState(FeeDetailDto.STATE_NORMAL); // 设置状态为正常状态
            feeDetailDto.setFeeId(tmpPayFeeSubDto.getFeeId());
            feeDetailDto.setCommunityId(tmpPayFeeSubDto.getCommunityId());
            int count = feeDetailInnerServiceSMOImpl.queryFeeDetailsCount(feeDetailDto);
            // 如果存在缴费记录，抛出异常阻止合并
            if (count > 0) {
                throw new CmdException("拆分后费用存在缴费记录 不能合并处理");
            }
        }

        // 将查询到的拆分费用列表存入请求参数中，供后续处理使用
        reqJson.put("payFeeSubDtos",payFeeSubDtos);
    }

    /**
     * 执行费用合并命令
     * 该方法在事务中执行，包含删除拆分记录、结束拆分费用、启用原费用等操作
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当合并操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {
        // 从请求参数中获取拆分费用列表
        List<PayFeeSubDto> payFeeSubDtos = (List<PayFeeSubDto>)reqJson.get("payFeeSubDtos");

        // 删除所有相关的拆分费用记录
        PayFeeSubPo payFeeSubPo = new PayFeeSubPo();
        payFeeSubPo.setPreFeeId(payFeeSubDtos.get(0).getPreFeeId()); // 设置原费用ID
        payFeeSubPo.setCommunityId(payFeeSubDtos.get(0).getCommunityId()); // 设置小区ID
        int flag =  payFeeSubV1InnerServiceSMOImpl.deletePayFeeSub(payFeeSubPo);

        // 检查删除操作是否成功
        if (flag < 1) {
            throw new CmdException("删除数据失败");
        }

        // 遍历所有拆分费用，将其状态设置为结束状态
        for (PayFeeSubDto tmpPayFeeSubDto : payFeeSubDtos) {
            PayFeePo payFeePo = new PayFeePo();
            payFeePo.setFeeId(tmpPayFeeSubDto.getFeeId()); // 设置费用ID
            payFeePo.setState(FeeDto.STATE_FINISH); // 设置状态为结束状态
            payFeePo.setCommunityId(tmpPayFeeSubDto.getCommunityId()); // 设置小区ID
            flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
            // 检查更新操作是否成功
            if (flag < 1) {
                throw new CmdException("结束费用失败");
            }
        }

        // 启用原费用，将其状态设置为进行中状态
        PayFeePo payFeePo = new PayFeePo();
        payFeePo.setFeeId(payFeeSubDtos.get(0).getPreFeeId()); // 设置原费用ID
        payFeePo.setState(FeeDto.STATE_DOING); // 设置状态为进行中状态
        payFeePo.setCommunityId(payFeeSubDtos.get(0).getCommunityId()); // 设置小区ID
        flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
        // 检查启用操作是否成功
        if (flag < 1) {
            throw new CmdException("启用原费用失败");
        }

        // 设置响应结果为成功
        cmdDataFlowContext.setResponseEntity(ResultVo.success());
    }
}