package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeAccountDetailDto;
import com.java110.dto.payFee.PayFeeDetailDiscountDto;
import com.java110.intf.fee.IFeeAccountDetailServiceSMO;
import com.java110.intf.fee.IFeeDetailInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailDiscountInnerServiceSMO;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.api.ApiFeeDetailDataVo;
import com.java110.vo.api.ApiFeeDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询缴费历史命令类
 * 负责处理缴费明细查询请求，包括缴费记录、账户明细和折扣信息的查询与处理
 * 服务代码：fee.queryFeeDetail
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "fee.queryFeeDetail")
public class QueryFeeDetailCmd extends Cmd {

    /** 缴费明细服务接口 */
    @Autowired
    private IFeeDetailInnerServiceSMO feeDetailInnerServiceSMOImpl;

    /** 账户明细服务接口 */
    @Autowired
    private IFeeAccountDetailServiceSMO feeAccountDetailServiceSMOImpl;

    /** 缴费折扣服务接口 */
    @Autowired
    private IPayFeeDetailDiscountInnerServiceSMO payFeeDetailDiscountInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数中是否包含必要的communityId信息
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当参数验证失败时抛出命令异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证请求中必须包含communityId参数
        Assert.jsonObjectHaveKey(reqJson, "communityId", "请求中未包含communityId信息");
    }

    /**
     * 执行命令方法
     * 处理缴费明细查询业务逻辑，包括时间范围处理、数据查询、金额格式处理和响应数据组装
     *
     * @param event   命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求JSON对象
     * @throws CmdException 当业务处理过程中发生异常时抛出命令异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
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
        
        // 初始化返回对象
        ApiFeeDetailVo apiFeeDetailVo = new ApiFeeDetailVo();
        // 将请求参数转换为数据传输对象
        FeeDetailDto feeDetailDto = BeanConvertUtil.covertBean(reqJson, FeeDetailDto.class);

        // 查询总记录数
        int total = feeDetailInnerServiceSMOImpl.queryFeeDetailsCount(feeDetailDto);
        apiFeeDetailVo.setTotal(total);
        
        // 如果存在记录，则查询详细数据
        if (total > 0) {
            List<FeeDetailDto> feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(BeanConvertUtil.covertBean(reqJson, FeeDetailDto.class));
            List<FeeDetailDto> feeDetailList = new ArrayList<>();
            
            // 遍历处理每条缴费明细记录
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
                
                feeDetailList.add(feeDetail);
            }
            
            // 将数据传输对象转换为视图对象
            List<ApiFeeDetailDataVo> feeDetails = BeanConvertUtil.covertBeanList(feeDetailList, ApiFeeDetailDataVo.class);

            // 刷新创建时间格式（当前被注释掉，可根据需要启用）
            // reFreshCreateTime(feeDetails, feeDetailDtos);

            apiFeeDetailVo.setFeeDetails(feeDetails);
        }
        
        // 计算总页数
        int row = reqJson.getInteger("row");
        apiFeeDetailVo.setRecords((int) Math.ceil((double) total / (double) row));
        
        // 构建响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFeeDetailVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 刷新创建时间格式
     * 将数据传输对象中的时间格式转换为视图对象需要的格式
     *
     * @param feeDetails    返回的视图对象列表
     * @param feeDetailDtos 原始的数据传输对象列表
     */
    private void reFreshCreateTime(List<ApiFeeDetailDataVo> feeDetails, List<FeeDetailDto> feeDetailDtos) {
        // 遍历视图对象列表，匹配对应的数据传输对象并设置格式化后的时间
        for (ApiFeeDetailDataVo feeDetailDataVo : feeDetails) {
            for (FeeDetailDto feeDetailDto : feeDetailDtos) {
                // 根据detailId匹配记录
                if (feeDetailDataVo.getDetailId().equals(feeDetailDto.getDetailId())) {
                    // 将时间格式化为标准日期时间字符串
                    feeDetailDataVo.setCreateTime(DateUtil.getFormatTimeString(feeDetailDto.getCreateTime(), DateUtil.DATE_FORMATE_STRING_A));
                }
            }
        }
    }
}