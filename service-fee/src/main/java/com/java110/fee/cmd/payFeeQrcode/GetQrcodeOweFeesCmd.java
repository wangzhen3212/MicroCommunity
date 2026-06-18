package com.java110.fee.cmd.payFeeQrcode;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeDto;
import com.java110.fee.bmo.impl.QueryOweFeeImpl;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.MoneyUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询二维码欠费费用命令类
 * 该命令用于通过二维码方式查询业主的欠费费用信息
 * 主要功能包括：验证请求参数、查询欠费费用、计算欠费金额、格式化金额显示、处理业主信息脱敏等
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode payFeeQrcode.getQrcodeOweFees
 */
@Java110Cmd(serviceCode = "payFeeQrcode.getQrcodeOweFees")
public class GetQrcodeOweFeesCmd extends Cmd {

    private final static Logger logger = LoggerFactory.getLogger(GetQrcodeOweFeesCmd.class);
    
    /**
     * 费用内部服务接口，用于查询费用相关信息
     */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /**
     * 费用计算服务接口，用于计算欠费金额
     */
    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    /**
     * 公共配置域常量
     */
    public static final String DOMAIN_COMMON = "DOMAIN.COMMON";

    /**
     * 总费用价格配置键
     */
    public static final String TOTAL_FEE_PRICE = "TOTAL_FEE_PRICE";

    /**
     * 收款金额开关配置键
     */
    public static final String RECEIVED_AMOUNT_SWITCH = "RECEIVED_AMOUNT_SWITCH";

    /**
     * 线下支付费用开关配置键
     */
    public static final String OFFLINE_PAY_FEE_SWITCH = "OFFLINE_PAY_FEE_SWITCH";

    /**
     * 验证请求参数方法
     * 检查请求中是否包含必要的小区ID和业主ID参数
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当参数验证失败时抛出命令异常
     * @throws ParseException 当数据解析异常时抛出解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证请求中必须包含小区ID和业主ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区");
        Assert.hasKeyAndValue(reqJson, "ownerId", "未包含业主");
    }

    /**
     * 执行查询二维码欠费费用命令
     * 主要流程：查询业主欠费费用 -> 计算欠费金额 -> 格式化金额显示 -> 处理业主信息脱敏 -> 返回结果
     *
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求的JSON数据
     * @throws CmdException 当命令执行过程中发生错误时抛出命令异常
     * @throws ParseException 当数据解析异常时抛出解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 创建费用查询条件对象
        FeeDto feeDto = new FeeDto();
        feeDto.setOwnerId(reqJson.getString("ownerId")); // 设置业主ID
        feeDto.setCommunityId(reqJson.getString("communityId")); // 设置小区ID
        feeDto.setArrearsEndTime(DateUtil.getCurrentDate()); // 设置欠费截止时间为当前日期
        feeDto.setState(FeeDto.STATE_DOING); // 设置费用状态为进行中

        // 查询符合条件的费用列表
        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        // 如果没有查询到费用数据，返回空列表
        if (feeDtos == null || feeDtos.size() < 1) {
            feeDtos = new ArrayList<>();
            context.setResponseEntity(ResultVo.createResponseEntity(feeDtos));
            return;
        }

        // 获取总费用价格配置，优先从小区设置获取，如果没有则从公共配置获取
        String val = CommunitySettingFactory.getValue(feeDtos.get(0).getCommunityId(), TOTAL_FEE_PRICE);
        if (StringUtil.isEmpty(val)) {
            val = MappingCache.getValue(DOMAIN_COMMON, TOTAL_FEE_PRICE);
        }

        // 创建临时费用列表，用于存储处理后的费用数据
        List<FeeDto> tmpFeeDtos = new ArrayList<>();
        for (FeeDto tmpFeeDto : feeDtos) {
            try {
                // 计算每项费用的欠费金额
                computeFeeSMOImpl.computeEveryOweFee(tmpFeeDto);
                
                // 格式化费用总价格，根据小数位数和精度进行格式化
                tmpFeeDto.setFeeTotalPrice(
                        MoneyUtil.computePriceScale(
                                tmpFeeDto.getFeeTotalPrice(),
                                tmpFeeDto.getScale(),
                                Integer.parseInt(tmpFeeDto.getDecimalPlace())
                        )
                );

                // 设置配置值
                tmpFeeDto.setVal(val);
                
                // 只处理金额不为0的费用，排除金额为0的费用项
                // 注意：这里考虑负数金额的情况，有些可能需要红冲处理
                if (tmpFeeDto.getFeePrice() != 0) {
                    // 获取业主姓名并进行脱敏处理
                    String ownerName = FeeAttrDto.getFeeAttrValue(tmpFeeDto, FeeAttrDto.SPEC_CD_OWNER_NAME);
                    ownerName = StringUtil.maskName(ownerName);
                    
                    // 清空费用属性数据，避免信息泄露风险
                    tmpFeeDto.setFeeAttrDtos(null);
                    
                    // 设置脱敏后的业主姓名
                    tmpFeeDto.setOwnerName(ownerName);
                    
                    // 将处理后的费用对象添加到临时列表
                    tmpFeeDtos.add(tmpFeeDto);
                }
            } catch (Exception e) {
                // 记录计算费用过程中出现的异常，可能是费用资料有问题导致的算费失败
                logger.error("可能费用资料有问题导致算费失败", e);
            }
        }

        // 设置响应结果，返回处理后的费用列表
        context.setResponseEntity(ResultVo.createResponseEntity(feeDtos));
    }
}