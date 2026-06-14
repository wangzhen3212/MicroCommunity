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
package com.java110.fee.cmd.returnPayFee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeAccountDetailDto;
import com.java110.dto.onlinePayRefund.OnlinePayRefundDto;
import com.java110.dto.payFee.PayFeeDetailDiscountDto;
import com.java110.intf.acct.IOnlinePayRefundV1InnerServiceSMO;
import com.java110.intf.fee.IFeeAccountDetailServiceSMO;
import com.java110.intf.fee.IPayFeeDetailDiscountInnerServiceSMO;
import com.java110.intf.fee.IReturnPayFeeInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.returnPayFee.ApiReturnPayFeeDataVo;
import com.java110.vo.api.returnPayFee.ApiReturnPayFeeVo;
import org.springframework.beans.factory.annotation.Autowired;
import com.java110.dto.payFee.ReturnPayFeeDto;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 类表述：查询退费信息列表命令类
 * 服务编码：returnPayFee.listReturnPayFee
 * 请求路劲：/app/returnPayFee.ListReturnPayFee
 * 功能描述：该命令类用于查询退费信息列表，包括退费的基本信息、账户明细、折扣信息以及在线支付退款状态等
 * add by 吴学文 at 2022-02-21 12:20:03 mail: 928255095@qq.com
 * open source address: https://gitee.com/wuxw7/MicroCommunity
 * 官网：http://www.homecommunity.cn
 * 温馨提示：如果您对此文件进行修改 请不要删除原有作者及注释信息，请补充您的 修改的原因以及联系邮箱如下
 * // modify by 张三 at 2021-09-12 第10行在某种场景下存在某种bug 需要修复，注释10至20行 加入 20行至30行
 */
@Java110Cmd(serviceCode = "returnPayFee.listReturnPayFees")
public class ListReturnPayFeesCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListReturnPayFeesCmd.class);

    @Autowired
    private IReturnPayFeeInnerServiceSMO returnPayFeeInnerServiceSMOImpl;

    @Autowired
    private IFeeAccountDetailServiceSMO feeAccountDetailServiceSMOImpl;

    @Autowired
    private IPayFeeDetailDiscountInnerServiceSMO payFeeDetailDiscountInnerServiceSMOImpl;

    @Autowired
    private IOnlinePayRefundV1InnerServiceSMO onlinePayRefundV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 功能描述：验证请求参数的有效性，包括分页信息和必要参数
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证小区ID参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
    }

    /**
     * 命令执行方法
     * 功能描述：执行查询退费信息列表的主要逻辑，包括查询退费信息、关联查询账户明细、折扣信息和在线支付退款状态
     * @param event 命令事件对象
     * @param cmdDataFlowContext 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令执行异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext cmdDataFlowContext, JSONObject reqJson) throws CmdException {

        // 将请求参数转换为ReturnPayFeeDto对象
        ReturnPayFeeDto returnPayFeeDto = BeanConvertUtil.covertBean(reqJson, ReturnPayFeeDto.class);

        // 查询退费信息总数
        int count = returnPayFeeInnerServiceSMOImpl.queryReturnPayFeesCount(returnPayFeeDto);

        List<ReturnPayFeeDto> returnPayFees;

        // 如果存在退费记录，则查询详细信息
        if (count > 0) {
            // 注意这里处理 记得测试退费逻辑
            // 查询退费信息列表
            returnPayFees = returnPayFeeInnerServiceSMOImpl.queryReturnPayFees(returnPayFeeDto);
            
            // 遍历每个退费记录，查询关联的账户明细和折扣信息
            for (ReturnPayFeeDto returnPayFee : returnPayFees) {
                // 查询账户明细信息
                FeeAccountDetailDto feeAccountDetailDto = new FeeAccountDetailDto();
                feeAccountDetailDto.setDetailId(returnPayFee.getDetailId());
                List<FeeAccountDetailDto> feeAccountDetailDtos = feeAccountDetailServiceSMOImpl.queryFeeAccountDetails(feeAccountDetailDto);
                if (!ListUtil.isNull(feeAccountDetailDtos)) {
                    returnPayFee.setFeeAccountDetailDtoList(feeAccountDetailDtos);
                }
                
                // 查询折扣信息
                PayFeeDetailDiscountDto payFeeDetailDiscountDto = new PayFeeDetailDiscountDto();
                payFeeDetailDiscountDto.setDetailId(returnPayFee.getDetailId());
                List<PayFeeDetailDiscountDto> payFeeDetailDiscountDtos = payFeeDetailDiscountInnerServiceSMOImpl.queryPayFeeDetailDiscounts(payFeeDetailDiscountDto);
                if (!ListUtil.isNull(payFeeDetailDiscountDtos)) {
                    returnPayFee.setPayFeeDetailDiscountDtoList(payFeeDetailDiscountDtos);
                }
            }
        } else {
            // 如果没有退费记录，返回空列表
            returnPayFees = new ArrayList<>();
        }
        
        // 查询在线支付退款系统的返回内容
        computeOnlinePayRefundRemark(returnPayFees);

        // 构建返回结果
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) reqJson.getInteger("row")), count, returnPayFees);

        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        cmdDataFlowContext.setResponseEntity(responseEntity);
    }

    /**
     * 计算在线支付退款备注信息
     * 功能描述：查询在线支付退款系统的状态和备注信息，并设置到退费记录中
     * @param returnPayFees 退费信息列表
     */
    private void computeOnlinePayRefundRemark(List<ReturnPayFeeDto> returnPayFees) {

        // 如果退费列表为空，直接返回
        if (ListUtil.isNull(returnPayFees)) {
            return;
        }

        // 收集所有退费记录的明细ID
        List<String> detailIds = new ArrayList<>();
        for (ReturnPayFeeDto returnPayFeeDto : returnPayFees) {
            detailIds.add(returnPayFeeDto.getDetailId());
        }

        // 构建查询条件，查询在线支付退款信息
        OnlinePayRefundDto onlinePayRefundDto = new OnlinePayRefundDto();
        onlinePayRefundDto.setBusiIds(detailIds.toArray(new String[detailIds.size()]));
        List<OnlinePayRefundDto> onlinePayRefundDtos = onlinePayRefundV1InnerServiceSMOImpl.queryOnlinePayRefunds(onlinePayRefundDto);

        // 如果没有在线支付退款信息，直接返回
        if (ListUtil.isNull(onlinePayRefundDtos)) {
            return;
        }

        // 将在线支付退款信息设置到对应的退费记录中
        for (ReturnPayFeeDto returnPayFeeDto : returnPayFees) {
            for(OnlinePayRefundDto tmpOnlinePayRefundDto:onlinePayRefundDtos){
                // 根据业务ID匹配对应的退费记录
                if(returnPayFeeDto.getDetailId().equals(tmpOnlinePayRefundDto.getBusiId())){
                    // 设置退款状态
                    returnPayFeeDto.setRefundState(tmpOnlinePayRefundDto.getState());
                    // 设置退款备注信息
                    returnPayFeeDto.setRefundRemark(tmpOnlinePayRefundDto.getMessage());
                }
            }
        }
    }
}