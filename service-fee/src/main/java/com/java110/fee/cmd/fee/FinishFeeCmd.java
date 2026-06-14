/**
 * 费用结束命令类
 * 
 * 该类用于处理结束费用的业务逻辑，主要功能包括：
 * 1. 验证请求参数的合法性
 * 2. 更新费用状态为结束状态
 * 3. 删除相关的月费用离散数据
 * 4. 重新生成或刷新月费用数据
 * 5. 删除对应的欠费信息记录
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDto;
import com.java110.fee.feeMonth.IPayFeeMonth;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.intf.report.IReportOweFeeInnerServiceSMO;
import com.java110.po.fee.PayFeePo;
import com.java110.po.reportFee.ReportOweFeePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 结束费用命令类
 * 服务代码：fee.finishFee
 * 负责处理费用结束的相关业务操作
 */
@Java110Cmd(serviceCode = "fee.finishFee")
public class FinishFeeCmd extends Cmd {

    /**
     * 房间内部服务接口
     */
    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /**
     * 费用内部服务接口
     */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /**
     * 支付费用V1内部服务接口
     */
    @Autowired
    private IPayFeeV1InnerServiceSMO feeV1InnerServiceSMOImpl;

    /**
     * 支付费用月处理接口
     */
    @Autowired
    private IPayFeeMonth payFeeMonthImpl;

    /**
     * 欠费报表内部服务接口
     */
    @Autowired
    private IReportOweFeeInnerServiceSMO reportOweFeeInnerServiceSMOImpl;

    /**
     * 验证请求参数
     * 
     * 该方法用于验证结束费用请求的必需参数，包括：
     * 1. 小区ID
     * 2. 费用ID
     * 3. 验证费用信息的唯一性
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 验证必需参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "feeId", "未包含feeId");

        // 构建费用查询条件
        FeeDto feeDto = new FeeDto();
        feeDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        feeDto.setFeeId(reqJson.getString("feeId")); // 设置费用ID

        // 查询费用信息
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        // 验证查询结果是否唯一
        Assert.listOnlyOne(feeDtos, "未查询到费用信息 或查询到多条" + reqJson);
    }

    /**
     * 执行结束费用命令
     * 
     * 该方法执行结束费用的核心业务逻辑，包括：
     * 1. 更新费用状态为结束状态
     * 2. 删除离散的月费用数据
     * 3. 重新生成或刷新月费用数据
     * 4. 删除对应的欠费信息
     * 
     * @param event 命令事件对象，包含事件相关信息
     * @param context 命令数据流上下文，包含请求和响应数据
     * @param reqJson 请求参数的JSON对象
     * @throws CmdException 当结束费用操作失败时抛出异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {
        // 将请求JSON转换为支付费用PO对象
        PayFeePo payFeePo = BeanConvertUtil.covertBean(reqJson, PayFeePo.class);
        // 设置费用状态为结束状态
        payFeePo.setState(FeeDto.STATE_FINISH);
        
        // 更新费用状态
        int flag = feeV1InnerServiceSMOImpl.updatePayFee(payFeePo);
        if (flag < 1) {
            throw new CmdException("结束费用失败");
        }

        // 处理离散的月费用数据 - 删除原有的月费用数据
        payFeeMonthImpl.deleteFeeMonth(payFeePo.getFeeId(), payFeePo.getCommunityId());
        // 重新生成或刷新月费用数据
        payFeeMonthImpl.doGeneratorOrRefreshFeeMonth(payFeePo.getFeeId(), payFeePo.getCommunityId());

        // 删除欠费信息记录
        ReportOweFeePo reportOweFeePo = new ReportOweFeePo();
        reportOweFeePo.setFeeId(payFeePo.getFeeId()); // 设置费用ID
        reportOweFeePo.setCommunityId(payFeePo.getCommunityId()); // 设置小区ID
        reportOweFeeInnerServiceSMOImpl.deleteReportOweFee(reportOweFeePo);
    }
}