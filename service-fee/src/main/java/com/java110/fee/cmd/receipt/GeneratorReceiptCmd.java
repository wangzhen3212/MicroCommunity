package com.java110.fee.cmd.receipt;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.dto.fee.FeeDetailDto;
import com.java110.dto.fee.FeeReceiptDetailDto;
import com.java110.intf.fee.*;
import com.java110.po.fee.PayFeeDetailPo;
import com.java110.utils.cache.CommonCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.List;

/**
 * 手工生成收据命令类
 * 该命令用于处理手工生成收据的业务逻辑，包括参数验证和收据生成操作
 * 通过校验缴费明细和收据状态，确保收据生成的准确性和唯一性
 * 
 * @author Java110
 * @version 1.0
 * @serviceCode receipt.generatorReceipt
 */
@Java110Cmd(serviceCode = "receipt.generatorReceipt")
public class GeneratorReceiptCmd extends Cmd {

    /**
     * 缴费明细内部服务接口
     * 用于查询和处理缴费明细相关数据
     */
    @Autowired
    private IFeeDetailInnerServiceSMO feeDetailInnerServiceSMOImpl;

    /**
     * 收据明细内部服务接口
     * 用于查询和处理收据明细相关数据
     */
    @Autowired
    private IFeeReceiptDetailInnerServiceSMO feeReceiptDetailInnerServiceSMOImpl;

    /**
     * 收据生成内部服务接口
     * 用于执行实际的收据生成操作
     */
    @Autowired
    private IGeneratorFeeReceiptInnerServiceSMO generatorFeeReceiptInnerServiceSMOImpl;

    /**
     * 参数验证方法
     * 验证请求参数的完整性和业务规则的合法性
     * 
     * @param event 命令事件对象，包含请求相关信息
     * @param context 命令数据流上下文，用于获取和设置数据流
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败或业务规则不满足时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证必填参数：小区ID和明细ID
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区信息");
        Assert.hasKeyAndValue(reqJson, "detailId", "未包含收据ID");

        // 查询收费明细是否存在
        FeeDetailDto feeDetailDto = new FeeDetailDto();
        feeDetailDto.setDetailId(reqJson.getString("detailId"));
        feeDetailDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeDetailDto> feeDetailDtos = feeDetailInnerServiceSMOImpl.queryFeeDetails(feeDetailDto);
        // 验证缴费明细必须存在且唯一
        Assert.listOnlyOne(feeDetailDtos, "缴费明细不存在");

        // 检查收据是否已存在，避免重复生成
        FeeReceiptDetailDto feeReceiptDetailDto = new FeeReceiptDetailDto();
        feeReceiptDetailDto.setDetailId(reqJson.getString("detailId"));
        feeReceiptDetailDto.setCommunityId(reqJson.getString("communityId"));
        List<FeeReceiptDetailDto> feeReceiptDetailDtos = feeReceiptDetailInnerServiceSMOImpl.queryFeeReceiptDetails(feeReceiptDetailDto);

        // 如果收据已存在，抛出异常
        if (feeReceiptDetailDtos != null && feeReceiptDetailDtos.size() > 0) {
            throw new CmdException("收据已存在");
        }

        // 将查询到的缴费明细存入请求参数中，供后续处理使用
        reqJson.put("feeDetailDto", feeDetailDtos.get(0));
    }

    /**
     * 执行收据生成命令
     * 处理收据生成的核心业务逻辑，包括收据码缓存和收据生成
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当命令执行失败时抛出
     * @throws ParseException 当参数解析异常时抛出
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 从请求参数中获取缴费明细数据
        FeeDetailDto feeDetailDto = (FeeDetailDto) reqJson.get("feeDetailDto");

        // 将FeeDetailDto转换为PayFeeDetailPo，用于收据生成
        PayFeeDetailPo payFeeDetailPo = BeanConvertUtil.covertBean(feeDetailDto, PayFeeDetailPo.class);

        // 处理收据码逻辑：如果提供了收据码，则缓存到公共缓存中
        String receiptCode = reqJson.getString("receiptCode");
        if (!StringUtil.isEmpty(receiptCode)) {
            // 将收据码缓存，键为明细ID+收据码后缀
            CommonCache.setValue(payFeeDetailPo.getDetailId() + CommonCache.RECEIPT_CODE, receiptCode);
        }

        // 调用收据生成服务，执行实际的收据生成操作
        generatorFeeReceiptInnerServiceSMOImpl.generator(payFeeDetailPo);

        // 收据生成完成后，清理缓存的收据码
        if (!StringUtil.isEmpty(receiptCode)) {
            CommonCache.removeValue(payFeeDetailPo.getDetailId() + CommonCache.RECEIPT_CODE);
        }

        // 设置响应结果为成功
        context.setResponseEntity(ResultVo.success());
    }
}