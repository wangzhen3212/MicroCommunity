package com.java110.fee.cmd.fee;

import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Cmd;
import com.java110.core.annotation.Java110Transactional;
import com.java110.core.context.ICmdDataFlowContext;
import com.java110.core.event.cmd.Cmd;
import com.java110.core.event.cmd.CmdEvent;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeConfigDto;
import com.java110.dto.fee.FeeDto;
import com.java110.fee.feeMonth.IPayFeeMonth;
import com.java110.fee.smo.impl.FeeAttrInnerServiceSMOImpl;
import com.java110.intf.community.IRoomInnerServiceSMO;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.intf.fee.IPayFeeV1InnerServiceSMO;
import com.java110.po.fee.FeeAttrPo;
import com.java110.po.fee.PayFeePo;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.Assert;
import com.java110.utils.util.BeanConvertUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * 费用更新命令类
 * 
 * 该类负责处理费用信息的更新操作，包括费用基本信息更新、费用离散月重新计算、
 * 欠费重新生成以及费用属性（如费率、费率周期等）的维护
 * 
 * @author Java110
 * @version 1.0
 * @since 2023
 */
@Java110Cmd(serviceCode = "fee.updateFee")
public class UpdateFeeCmd extends Cmd {

    /** 默认添加费用数量常量 */
    private static final int DEFAULT_ADD_FEE_COUNT = 200;

    /** 房间内部服务接口 */
    @Autowired
    private IRoomInnerServiceSMO roomInnerServiceSMOImpl;

    /** 费用内部服务接口 */
    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    /** 支付费用V1内部服务接口 */
    @Autowired
    private IPayFeeV1InnerServiceSMO payFeeV1InnerServiceSMOImpl;

    /** 费用属性内部服务实现 */
    @Autowired
    private FeeAttrInnerServiceSMOImpl feeAttrInnerServiceSMOImpl;

    /** 支付费用月份处理接口 */
    @Autowired
    private IPayFeeMonth payFeeMonthImpl;

    /**
     * 参数验证方法
     * 
     * 验证请求参数的有效性，包括必填字段检查、费用信息查询验证等
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当参数验证失败时抛出异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 验证必填参数是否存在
        Assert.hasKeyAndValue(reqJson, "communityId", "未包含小区ID");
        Assert.hasKeyAndValue(reqJson, "feeId", "未包含feeId");
        Assert.hasKeyAndValue(reqJson, "startTime", "未包含开始时间");
        Assert.hasKeyAndValue(reqJson, "endTime", "未包含结束时间");
        Assert.hasKeyAndValue(reqJson, "maxEndTime", "未包含结束时间");
        
        // 格式化最大结束时间，如果没有包含时间部分则添加默认时间
        if(!reqJson.getString("maxEndTime").contains(":")){
            reqJson.put("maxEndTime",reqJson.getString("maxEndTime")+" 23:59:59");
        }

        // 查询费用信息进行验证
        FeeDto feeDto = new FeeDto();
        feeDto.setCommunityId(reqJson.getString("communityId"));
        feeDto.setFeeId(reqJson.getString("feeId"));

        List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);

        // 验证查询结果是否唯一
        Assert.listOnlyOne(feeDtos, "未查询到费用信息 或查询到多条" + reqJson);
    }

    /**
     * 执行费用更新命令
     * 
     * 主要功能包括：
     * 1. 更新费用基本信息
     * 2. 重新计算离散月份
     * 3. 重新生成欠费信息
     * 4. 更新费用相关属性（如截止时间、费率等）
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 当费用更新失败时抛出异常
     */
    @Override
    @Java110Transactional
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException {

        // 将请求JSON转换为支付费用PO对象
        PayFeePo payFeePo = BeanConvertUtil.covertBean(reqJson, PayFeePo.class);
        
        // 更新支付费用信息
        int flag = payFeeV1InnerServiceSMOImpl.updatePayFee(payFeePo);

        // 检查更新是否成功
        if (flag < 1) {
            throw new CmdException("修改费用");
        }

        // 重新计算离散月份：先删除原有的月份数据，再重新生成
        payFeeMonthImpl.deleteFeeMonth(payFeePo.getFeeId(),payFeePo.getCommunityId());
        payFeeMonthImpl.doGeneratorOrRefreshFeeMonth(payFeePo.getFeeId(),payFeePo.getCommunityId());

        // 重新生成欠费信息
        List<String> feeIds= new ArrayList<>();
        feeIds.add(payFeePo.getFeeId());
        payFeeMonthImpl.doGeneratorOweFees(feeIds,payFeePo.getCommunityId());

        // 处理一次性费用截止时间属性
        if (reqJson.containsKey("maxEndTime") && !StringUtil.isEmpty(reqJson.getString("maxEndTime"))) {
            FeeAttrDto feeAttrDto = new FeeAttrDto();
            feeAttrDto.setFeeId(payFeePo.getFeeId());
            feeAttrDto.setSpecCd(FeeAttrDto.SPEC_CD_ONCE_FEE_DEADLINE_TIME);
            List<FeeAttrDto> feeAttrDtos = feeAttrInnerServiceSMOImpl.queryFeeAttrs(feeAttrDto);
            
            FeeAttrPo feeAttrPo = new FeeAttrPo();
            feeAttrPo.setFeeId(payFeePo.getFeeId());
            feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_ONCE_FEE_DEADLINE_TIME);
            feeAttrPo.setValue(reqJson.getString("maxEndTime"));
            feeAttrPo.setCommunityId(reqJson.getString("communityId"));
            
            // 根据属性是否存在决定是新增还是更新
            if (ListUtil.isNull(feeAttrDtos)) {
                feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                feeAttrInnerServiceSMOImpl.saveFeeAttr(feeAttrPo);
            } else {
                feeAttrInnerServiceSMOImpl.updateFeeAttr(feeAttrPo);
            }
        }

        // 如果计算方式不是租金率，则直接返回
        if (!reqJson.containsKey("computingFormula")
                || !FeeConfigDto.COMPUTING_FORMULA_RANT_RATE.equals(reqJson.getString("computingFormula"))) {
            return;
        }

        // 处理费率属性
        if (reqJson.containsKey("rate")) {
            FeeAttrDto feeAttrDto = new FeeAttrDto();
            feeAttrDto.setFeeId(payFeePo.getFeeId());
            feeAttrDto.setSpecCd(FeeAttrDto.SPEC_CD_RATE);
            List<FeeAttrDto> feeAttrDtos = feeAttrInnerServiceSMOImpl.queryFeeAttrs(feeAttrDto);
            
            FeeAttrPo feeAttrPo = new FeeAttrPo();
            feeAttrPo.setFeeId(payFeePo.getFeeId());
            feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_RATE);
            feeAttrPo.setValue(reqJson.getString("rate"));
            feeAttrPo.setCommunityId(reqJson.getString("communityId"));
            
            // 根据属性是否存在决定是新增还是更新
            if (feeAttrDtos == null || feeAttrDtos.size() < 1) {
                feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                feeAttrInnerServiceSMOImpl.saveFeeAttr(feeAttrPo);
            } else {
                feeAttrInnerServiceSMOImpl.updateFeeAttr(feeAttrPo);
            }
        }

        // 处理费率周期属性
        if (reqJson.containsKey("rateCycle")) {
            FeeAttrDto feeAttrDto = new FeeAttrDto();
            feeAttrDto.setFeeId(payFeePo.getFeeId());
            feeAttrDto.setSpecCd(FeeAttrDto.SPEC_CD_RATE_CYCLE);
            List<FeeAttrDto> feeAttrDtos = feeAttrInnerServiceSMOImpl.queryFeeAttrs(feeAttrDto);
            
            FeeAttrPo feeAttrPo = new FeeAttrPo();
            feeAttrPo.setFeeId(payFeePo.getFeeId());
            feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_RATE_CYCLE);
            feeAttrPo.setValue(reqJson.getString("rateCycle"));
            feeAttrPo.setCommunityId(reqJson.getString("communityId"));
            
            // 根据属性是否存在决定是新增还是更新
            if (feeAttrDtos == null || feeAttrDtos.size() < 1) {
                feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                feeAttrInnerServiceSMOImpl.saveFeeAttr(feeAttrPo);
            } else {
                feeAttrInnerServiceSMOImpl.updateFeeAttr(feeAttrPo);
            }
        }

        // 处理费率开始时间属性
        if (reqJson.containsKey("rateStartTime")) {
            FeeAttrDto feeAttrDto = new FeeAttrDto();
            feeAttrDto.setFeeId(payFeePo.getFeeId());
            feeAttrDto.setSpecCd(FeeAttrDto.SPEC_CD_RATE_START_TIME);
            List<FeeAttrDto> feeAttrDtos = feeAttrInnerServiceSMOImpl.queryFeeAttrs(feeAttrDto);
            
            FeeAttrPo feeAttrPo = new FeeAttrPo();
            feeAttrPo.setFeeId(payFeePo.getFeeId());
            feeAttrPo.setSpecCd(FeeAttrDto.SPEC_CD_RATE_START_TIME);
            feeAttrPo.setValue(reqJson.getString("rateStartTime"));
            feeAttrPo.setCommunityId(reqJson.getString("communityId"));
            
            // 根据属性是否存在决定是新增还是更新
            if (feeAttrDtos == null || feeAttrDtos.size() < 1) {
                feeAttrPo.setAttrId(GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_attrId));
                feeAttrInnerServiceSMOImpl.saveFeeAttr(feeAttrPo);
            } else {
                feeAttrInnerServiceSMOImpl.updateFeeAttr(feeAttrPo);
            }
        }
    }
}