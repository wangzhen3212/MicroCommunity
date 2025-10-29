package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeAccountDetailDto;
import com.java110.dto.fee.FeeDetailDto;
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
 * 查询管理员缴费历史命令类
 * 该命令用于查询管理员的缴费历史记录，包括费用明细、账户明细和折扣信息
 * 支持分页查询和时间范围筛选功能
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "fee.queryAdminFeeDetail")
public class QueryAdminFeeDetailCmd extends Cmd {

    /**
     * 费用明细服务接口
     */
    @Autowired
    private IFeeDetailInnerServiceSMO feeDetailInnerServiceSMOImpl;

    /**
     * 费用账户明细服务接口
     */
    @Autowired
    private IFeeAccountDetailServiceSMO feeAccountDetailServiceSMOImpl;

    /**
     * 缴费明细折扣服务接口
     */
    @Autowired
    private IPayFeeDetailDiscountInnerServiceSMO payFeeDetailDiscountInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的有效性，包括分页参数、管理员权限和时间范围处理
     *
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证分页参数
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
        
        // 处理开始时间参数
        if (!StringUtil.isEmpty(reqJson.getString("startTime"))) {
            // 将开始时间格式化为当天的00:00:00
            String startTime = reqJson.getString("startTime") + " 00:00:00";
            reqJson.put("startTime", startTime);
        } else {
            reqJson.put("startTime", null);
        }
        
        // 处理结束时间参数
        if (!StringUtil.isEmpty(reqJson.getString("endTime"))) {
            // 将结束时间格式化为当天的23:59:59
            String endTime = reqJson.getString("endTime") + " 23:59:59";
            reqJson.put("endTime", endTime);
        } else {
            reqJson.put("endTime", null);
        }
    }

    /**
     * 执行查询管理员缴费历史命令
     * 查询费用明细数据，包括账户明细和折扣信息，并返回分页结果
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当命令执行过程中发生错误时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 初始化返回对象
        ApiFeeDetailVo apiFeeDetailVo = new ApiFeeDetailVo();
        // 将请求参数转换为费用明细DTO对象
        FeeDetailDto feeDetailDto = BeanConvertUtil.covertBean(reqJson, FeeDetailDto.class);

        // 查询总记录数
        int total = feeDetailInnerServiceSMOImpl.queryFeeDetailsCount(feeDetailDto);
        apiFeeDetailVo.setTotal(total);
        
        // 如果有数据，则查询详细记录
        if (total > 0) {
            // 查询费用明细列表
            List<FeeDetailDto> feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(BeanConvertUtil.covertBean(reqJson, FeeDetailDto.class));
            List<FeeDetailDto> feeDetailList = new ArrayList<>();
            
            // 遍历处理每条费用明细记录
            for (FeeDetailDto feeDetail : feeDetailDtos) {
                // 获取状态
                String state = feeDetail.getState();
                // 处理退费相关状态的金额显示（去除负号）
                if ("1300".equals(state) || "1100".equals(state) || "1200".equals(state)) { //退费单、已退费、退费失败状态
                    // 处理周期字段，去除开头的负号
                    String cycles = feeDetail.getCycles();
                    if (!StringUtil.isEmpty(cycles) && cycles.contains("-")) {
                        feeDetail.setCycles(cycles.substring(1));
                    }
                    // 处理应收金额字段，去除开头的负号
                    String receivableAmount = feeDetail.getReceivableAmount();
                    if (!StringUtil.isEmpty(receivableAmount) && receivableAmount.contains("-")) {
                        feeDetail.setReceivableAmount(receivableAmount.substring(1));
                    }
                    // 处理实收金额字段，去除开头的负号
                    String receivedAmount = feeDetail.getReceivedAmount();
                    if (!StringUtil.isEmpty(receivedAmount) && receivedAmount.contains("-")) {
                        feeDetail.setReceivedAmount(receivedAmount.substring(1));
                    }
                }
                
                // 查询费用账户明细信息
                FeeAccountDetailDto feeAccountDetailDto = new FeeAccountDetailDto();
                feeAccountDetailDto.setDetailId(feeDetail.getDetailId());
                List<FeeAccountDetailDto> feeAccountDetailDtos = feeAccountDetailServiceSMOImpl.queryFeeAccountDetails(feeAccountDetailDto);
                feeDetail.setFeeAccountDetailDtoList(feeAccountDetailDtos);
                
                // 查询缴费明细折扣信息
                PayFeeDetailDiscountDto payFeeDetailDiscountDto = new PayFeeDetailDiscountDto();
                payFeeDetailDiscountDto.setDetailId(feeDetail.getDetailId());
                List<PayFeeDetailDiscountDto> payFeeDetailDiscountDtos = payFeeDetailDiscountInnerServiceSMOImpl.queryPayFeeDetailDiscounts(payFeeDetailDiscountDto);
                feeDetail.setPayFeeDetailDiscountDtoList(payFeeDetailDiscountDtos);
                
                // 将处理后的费用明细添加到列表
                feeDetailList.add(feeDetail);
            }
            
            // 将费用明细DTO列表转换为API返回VO列表
            List<ApiFeeDetailDataVo> feeDetails = BeanConvertUtil.covertBeanList(feeDetailList, ApiFeeDetailDataVo.class);
            apiFeeDetailVo.setFeeDetails(feeDetails);
        }
        
        // 计算总页数
        int row = reqJson.getInteger("row");
        apiFeeDetailVo.setRecords((int) Math.ceil((double) total / (double) row));
        
        // 构建HTTP响应
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFeeDetailVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }
}