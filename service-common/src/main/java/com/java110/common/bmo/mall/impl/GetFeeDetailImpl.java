package com.java110.common.bmo.mall.impl;

import com.alibaba.fastjson.JSONObject;
import com.java110.common.bmo.mall.IMallCommonApiBmo;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.dto.fee.FeeAccountDetailDto;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.payFee.PayFeeDetailDiscountDto;
import com.java110.intf.fee.IFeeAccountDetailServiceSMO;
import com.java110.intf.fee.IFeeDetailInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailDiscountInnerServiceSMO;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import com.java110.vo.api.ApiFeeDetailDataVo;
import com.java110.vo.api.ApiFeeDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("getFeeDetailImpl")
public class GetFeeDetailImpl implements IMallCommonApiBmo {

    /**
     * 缴费明细服务接口
     */
    @Autowired
    private IFeeDetailInnerServiceSMO feeDetailInnerServiceSMOImpl;

    /**
     * 账户明细服务接口
     */
    @Autowired
    private IFeeAccountDetailServiceSMO feeAccountDetailServiceSMOImpl;

    /**
     * 缴费折扣服务接口
     */
    @Autowired
    private IPayFeeDetailDiscountInnerServiceSMO payFeeDetailDiscountInnerServiceSMOImpl;

    @Override
    public void validate(ICmdDataFlowContext context, JSONObject reqJson) {
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");

    }

    @Override
    public void doCmd(ICmdDataFlowContext context, JSONObject reqJson) {
// 处理开始时间参数，如果存在则添加时间后缀
        if (!StringUtil.isEmpty(reqJson.getString("startTime"))) {
            String startTime = reqJson.getString("startTime") + " 00:00:00";
            reqJson.put("startTime", startTime);
        } else {
            reqJson.put("startTime", null);
        }

        // 处理结束时间参数，如果存在则添加时间后缀
        if (!StringUtil.isEmpty(reqJson.getString("endTime"))) {
            String endTime = reqJson.getString("endTime") + " 23:59:59";
            reqJson.put("endTime", endTime);
        } else {
            reqJson.put("endTime", null);
        }

        // 将请求参数转换为数据传输对象
        FeeDetailDto feeDetailDto = BeanConvertUtil.covertBean(reqJson, FeeDetailDto.class);

        // 查询总记录数
        int total = feeDetailInnerServiceSMOImpl.queryFeeDetailsCount(feeDetailDto);

        // 如果存在记录，则查询详细数据
        List<FeeDetailDto> feeDetailDtos = null;
        if (total > 0) {
            feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(BeanConvertUtil.covertBean(reqJson, FeeDetailDto.class));
            // 遍历处理每条缴费明细记录
            refreshFeeDetails(feeDetailDtos);
        }else{
            feeDetailDtos = new ArrayList<>();
        }

        // 计算总页数：总记录数 / 每页记录数，向上取整
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) total / (double) reqJson.getInteger("row")), total, feeDetailDtos);

        // 设置HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);
        // 构建响应实体
        context.setResponseEntity(responseEntity);
    }

    private void refreshFeeDetails(List<FeeDetailDto> feeDetailDtos) {
        for (FeeDetailDto feeDetail : feeDetailDtos) {
            // 获取状态并处理退费相关状态的金额显示格式
            String state = feeDetail.getState();
            // 处理退费单、已退费、退费失败状态的金额显示
            if ("1300".equals(state) || "1100".equals(state) || "1200".equals(state)) {
                // 处理周期字段，移除负号前缀
                String cycles = feeDetail.getCycles();
                if (!StringUtil.isEmpty(cycles) && cycles.contains("-")) {
                    feeDetail.setCycles(cycles.substring(1));
                }
                // 处理应收金额字段，移除负号前缀
                String receivableAmount = feeDetail.getReceivableAmount();
                if (!StringUtil.isEmpty(receivableAmount) && receivableAmount.contains("-")) {
                    feeDetail.setReceivableAmount(receivableAmount.substring(1));
                }
                // 处理实收金额字段，移除负号前缀
                String receivedAmount = feeDetail.getReceivedAmount();
                if (!StringUtil.isEmpty(receivedAmount) && receivedAmount.contains("-")) {
                    feeDetail.setReceivedAmount(receivedAmount.substring(1));
                }
            }

            // 查询账户明细信息
            FeeAccountDetailDto feeAccountDetailDto = new FeeAccountDetailDto();
            feeAccountDetailDto.setDetailId(feeDetail.getDetailId());
            List<FeeAccountDetailDto> feeAccountDetailDtos = feeAccountDetailServiceSMOImpl.queryFeeAccountDetails(feeAccountDetailDto);
            feeDetail.setFeeAccountDetailDtoList(feeAccountDetailDtos);

            // 查询折扣信息
            PayFeeDetailDiscountDto payFeeDetailDiscountDto = new PayFeeDetailDiscountDto();
            payFeeDetailDiscountDto.setDetailId(feeDetail.getDetailId());
            List<PayFeeDetailDiscountDto> payFeeDetailDiscountDtos = payFeeDetailDiscountInnerServiceSMOImpl.queryPayFeeDetailDiscounts(payFeeDetailDiscountDto);
            feeDetail.setPayFeeDetailDiscountDtoList(payFeeDetailDiscountDtos);

        }
    }
}
