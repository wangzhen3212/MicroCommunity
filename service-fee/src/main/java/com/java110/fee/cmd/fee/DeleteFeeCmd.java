/**
 * 费用删除命令类
 * 
 * 该类负责处理费用删除相关的业务逻辑，包括费用信息的验证、删除操作以及相关数据的清理
 * 实现了Cmd接口，通过注解@Java110Cmd声明服务代码为"fee.deleteFee"
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.Environment;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeDto;
import com.java110.dto.payFee.PayFeeRuleBillDto;
import com.java110.fee.feeMonth.IPayFeeMonth;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.*;
import com.java110.intf.report.IReportOweFeeInnerServiceSMO;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.fee.PayFeePo;
import com.java110.po.payFee.PayFeeRuleBillPo;
import com.java110.po.reportFee.ReportOweFeePo;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Java110Cmd(serviceCode = "fee.deleteFee")
public class DeleteFeeCmd extends Cmd {

    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    @Autowired
    private IPayFeeDetailV1InnerServiceSMO payFeeDetailV1InnerServiceSMOImpl;

    @Autowired
    private IFeeDetailInnerServiceSMO feeDetailInnerServiceSMOImpl;

    @Autowired
    private IPayFeeMonth payFeeMonthImpl;

    @Autowired
    private IReportOweFeeInnerServiceSMO reportOweFeeInnerServiceSMOImpl;

    @Autowired
    private IPayFeeRuleBillV1InnerServiceSMO payFeeRuleBillV1InnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 
     * 验证删除费用请求的合法性，包括：
     * 1. 检查必要参数是否存在
     * 2. 验证费用信息是否存在且唯一
     * 3. 检查是否存在缴费记录
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 检查当前环境是否为开发环境
        Environment.isDevEnv();

        // 验证必要参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "feeId", "未包含feeId");

        // 构建费用查询条件
        FeeDto feeDto = new FeeDto();
        feeDto.setCommunityId(reqJson.getString("communityId"));
        feeDto.setFeeId(reqJson.getString("feeId"));

        // 查询费用信息
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        // 验证费用信息是否存在且唯一
        Assert.listOnlyOne(feeDtos, "未查询到费用信息 或查询到多条" + reqJson);

        // 检查是否开启费用删除验证
        String feeValidate = MappingCache.getValue("DELETE_FEE_VALIDATE");
        if ("ON".equals(feeValidate)) {
            return; // 如果开启验证，直接返回，跳过后续检查
        }

        // 检查是否存在缴费记录
        FeeDetailDto feeDetailDto = new FeeDetailDto();
        feeDetailDto.setCommunityId(reqJson.getString("communityId"));
        feeDetailDto.setFeeId(reqJson.getString("feeId"));
        feeDetailDto.setStates(new String[]{"1400", "1000"}); // 设置缴费状态：1400-已缴费，1000-待缴费
        List<FeeDetailDto> feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(feeDetailDto);
        
        // 如果存在缴费记录，则不允许删除
        if (feeDetailDtos != null && feeDetailDtos.size() > 0) {
            throw new IllegalArgumentException("存在缴费记录，不能取消，如果需要取消，请先退费");
        }
    }

    /**
     * 执行费用删除命令
     * 
     * 该方法负责执行费用删除的核心业务逻辑，包括：
     * 1. 删除费用主表信息
     * 2. 删除费用明细信息
     * 3. 删除费用账单规则信息
     * 4. 删除欠费报表信息
     * 5. 删除费用月信息
     * 
     * @param event 命令事件对象
     * @param context 数据流上下文对象
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当删除操作失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 构建业务数据对象
        JSONObject businessUnit = new JSONObject();
        businessUnit.put("feeId", reqJson.getString("feeId"));
        businessUnit.put("communityId", reqJson.getString("communityId"));
        
        // 转换为费用PO对象
        PayFeePo payFeePo = BeanConvertUtil.covertBean(businessUnit, PayFeePo.class);

        // 删除费用主表信息
        int flag = payFeeV1InnerServiceSMOImpl.deletePayFee(payFeePo);
        if (flag < 1) {
            throw new IllegalArgumentException("删除失败");
        }

        // 删除费用明细信息
        PayFeeDetailPo payFeeDetailPo = BeanConvertUtil.covertBean(businessUnit, PayFeeDetailPo.class);
        List<FeeDetailDto> feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(BeanConvertUtil.covertBean(payFeeDetailPo, FeeDetailDto.class));
        if(feeDetailDtos != null && !feeDetailDtos.isEmpty()) {
            int flag2 = payFeeDetailV1InnerServiceSMOImpl.deletePayFeeDetailNew(payFeeDetailPo);
            if (flag2 < 1) {
                throw new IllegalArgumentException("删除失败");
            }
        }

        // 删除费用账单规则信息
        PayFeeRuleBillDto payFeeRuleBillDto = new PayFeeRuleBillDto();
        payFeeRuleBillDto.setFeeId(payFeePo.getFeeId());
        payFeeRuleBillDto.setCommunityId(payFeePo.getCommunityId());
        List<PayFeeRuleBillDto> payFeeRuleBillDtos = payFeeRuleBillV1InnerServiceSMOImpl.queryPayFeeRuleBills(payFeeRuleBillDto);
        if(payFeeRuleBillDtos != null && !payFeeRuleBillDtos.isEmpty()) {
            PayFeeRuleBillPo payFeeRuleBillPo = new PayFeeRuleBillPo();
            payFeeRuleBillPo.setBillId(payFeeRuleBillDtos.get(0).getBillId());
            payFeeRuleBillPo.setCommunityId(payFeeRuleBillDtos.get(0).getCommunityId());
            payFeeRuleBillV1InnerServiceSMOImpl.deletePayFeeRuleBill(payFeeRuleBillPo);
        }
        
        // 删除欠费报表信息
        ReportOweFeePo reportOweFeePo = new ReportOweFeePo();
        reportOweFeePo.setFeeId(payFeePo.getFeeId());
        reportOweFeePo.setCommunityId(payFeePo.getCommunityId());
        reportOweFeeInnerServiceSMOImpl.deleteReportOweFee(reportOweFeePo);

        // 删除费用月信息
        payFeeMonthImpl.deleteFeeMonth(payFeePo.getFeeId(),payFeePo.getCommunityId());
    }
}