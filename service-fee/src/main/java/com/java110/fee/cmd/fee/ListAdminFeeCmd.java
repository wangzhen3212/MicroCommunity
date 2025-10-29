/**
 * 费用管理命令类 - 管理员费用列表查询
 * 
 * 该类负责处理管理员费用列表查询请求，包括费用信息的查询、计算和格式化处理。
 * 主要功能包括：费用数据查询、费用计算、费用属性更新等。
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
import com.java110.core.factory.CommunitySettingFactory;
import com.java110.core.log.LoggerFactory;
import com.java110.core.smo.IComputeFeeSMO;
import com.java110.dto.fee.FeeDto;
import com.java110.intf.fee.IFeeInnerServiceSMO;
import com.java110.utils.cache.MappingCache;
import com.java110.utils.exception.CmdException;
import com.java110.utils.util.*;
import com.java110.vo.api.fee.ApiFeeDataVo;
import com.java110.vo.api.fee.ApiFeeVo;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 管理员费用列表查询命令类
 * 服务代码：fee.listAdminFee
 */
@Java110Cmd(serviceCode = "fee.listAdminFee")
public class ListAdminFeeCmd extends Cmd {

    private static Logger logger = LoggerFactory.getLogger(ListAdminFeeCmd.class);

    @Autowired
    private IFeeInnerServiceSMO feeInnerServiceSMOImpl;

    @Autowired
    private IComputeFeeSMO computeFeeSMOImpl;

    /**
     * 请求参数验证方法
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void validate(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 验证分页信息
        super.validatePageInfo(reqJson);
        // 验证管理员权限
        super.validateAdmin(context);
    }

    /**
     * 执行费用列表查询命令
     * 
     * @param event 命令事件对象
     * @param context 命令数据流上下文
     * @param reqJson 请求参数JSON对象
     * @throws CmdException 命令异常
     * @throws ParseException 解析异常
     */
    @Override
    public void doCmd(CmdEvent event, ICmdDataFlowContext context, JSONObject reqJson) throws CmdException, ParseException {
        // 将请求参数转换为FeeDto对象
        FeeDto feeDto = BeanConvertUtil.covertBean(reqJson, FeeDto.class);
        // 处理多付费对象ID
        morePayerObjIds(reqJson, feeDto);
        
        List<ApiFeeDataVo> fees = new ArrayList<>();
        // 查询费用总数
        int count = feeInnerServiceSMOImpl.queryFeesCount(feeDto);
        
        if (count > 0) {
            // 查询费用明细数据
            List<FeeDto> feeDtos = feeInnerServiceSMOImpl.queryFees(feeDto);
            // 计算费用价格
            computeFeePrice(feeDtos);
            // 转换为API响应对象
            List<ApiFeeDataVo> apiFeeDataVos = BeanConvertUtil.covertBeanList(feeDtos, ApiFeeDataVo.class);
            
            for (ApiFeeDataVo apiFeeDataVo : apiFeeDataVos) {
                // 获取付费对象类型
                String payerObjType = apiFeeDataVo.getPayerObjType();
                // 如果是车辆类型，设置车辆类型编码
                if (FeeDto.PAYER_OBJ_TYPE_CAR.equals(payerObjType)) {
                    apiFeeDataVo.setCarTypeCd("1001");
                }
                fees.add(apiFeeDataVo);
            }
            // 更新费用属性信息
            freshFeeAttrs(fees, feeDtos);
        } else {
            fees = new ArrayList<>();
        }
        
        // 构建API响应对象
        ApiFeeVo apiFeeVo = new ApiFeeVo();
        apiFeeVo.setTotal(count);
        // 计算总页数
        apiFeeVo.setRecords((int) Math.ceil((double) count / (double) reqJson.getInteger("row")));
        apiFeeVo.setFees(fees);
        
        // 设置响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(JSONObject.toJSONString(apiFeeVo), HttpStatus.OK);
        context.setResponseEntity(responseEntity);
    }

    /**
     * 处理多付费对象ID
     * 将逗号分隔的付费对象ID字符串转换为数组
     * 
     * @param reqJson 请求参数JSON对象
     * @param feeDto 费用数据传输对象
     */
    private void morePayerObjIds(JSONObject reqJson, FeeDto feeDto) {
        // 检查是否存在payerObjIds参数
        if (!reqJson.containsKey("payerObjIds") || StringUtil.isEmpty(reqJson.getString("payerObjIds"))) {
            return;
        }
        String payerObjIds = reqJson.getString("payerObjIds");
        // 将逗号分隔的字符串拆分为数组
        feeDto.setPayerObjIds(payerObjIds.split(","));
    }

    /**
     * 更新费用属性信息
     * 将FeeDto中的费用属性信息设置到ApiFeeDataVo中
     * 
     * @param fees API费用数据视图对象列表
     * @param feeDtos 费用数据传输对象列表
     */
    private void freshFeeAttrs(List<ApiFeeDataVo> fees, List<FeeDto> feeDtos) {
        for (ApiFeeDataVo apiFeeDataVo : fees) {
            for (FeeDto feeDto : feeDtos) {
                // 根据费用ID匹配对应的费用属性
                if (apiFeeDataVo.getFeeId().equals(feeDto.getFeeId())) {
                    apiFeeDataVo.setFeeAttrs(feeDto.getFeeAttrDtos());
                }
            }
        }
    }

    /**
     * 计算费用价格
     * 对费用列表中的每个费用项进行计算，包括到期时间、欠费金额等
     * 
     * @param feeDtos 费用数据传输对象列表
     */
    private void computeFeePrice(List<FeeDto> feeDtos) {
        // 检查费用列表是否为空
        if (ListUtil.isNull(feeDtos)) {
            return;
        }

        for (FeeDto feeDto : feeDtos) {
            try {
                // 计算目标到期时间和欠费月数
                // 轮数 * 周期 * 30 + 开始时间 = 目标到期时间
                Map<String, Object> targetEndDateAndOweMonth = computeFeeSMOImpl.getTargetEndDateAndOweMonth(feeDto);
                Date targetEndDate = (Date) targetEndDateAndOweMonth.get("targetEndDate");
                double oweMonth = (double) targetEndDateAndOweMonth.get("oweMonth");
                feeDto.setCycle(feeDto.getPaymentCycle());
                
                // 如果是单次收费模式，设置周期为欠费月数
                // 考虑账单模式的场景
                if (FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())) {
                    feeDto.setCycle(oweMonth + "");
                }
                feeDto.setDeadlineTime(targetEndDate);
                
                // 执行费用计算
                doComputeFeePrice(feeDto, oweMonth);

            } catch (Exception e) {
                logger.error("查询费用信息 ，费用信息错误", e);
            }
            // 格式化价格，去掉多余的0
            feeDto.setSquarePrice(Double.parseDouble(feeDto.getSquarePrice()) + "");
            feeDto.setAdditionalAmount(Double.parseDouble(feeDto.getAdditionalAmount()) + "");
        }
    }

    /**
     * 执行具体的费用价格计算
     * 根据计算公式计算费用单价、总价和欠费金额
     * 
     * @param feeDto 费用数据传输对象
     * @param oweMonth 欠费月数
     */
    private void doComputeFeePrice(FeeDto feeDto, double oweMonth) {
        String computingFormula = feeDto.getComputingFormula();
        // 获取费用价格信息
        Map feePriceAll = computeFeeSMOImpl.getFeePrice(feeDto);
        feeDto.setFeePrice(Double.parseDouble(feePriceAll.get("feePrice").toString()));
        
        // 计算费用总价，考虑小数位数和精度
        feeDto.setFeeTotalPrice(MoneyUtil.computePriceScale(Double.parseDouble(feePriceAll.get("feeTotalPrice").toString()),
                feeDto.getScale(),
                Integer.parseInt(feeDto.getDecimalPlace())));
        
        // 计算欠费金额 = 费用单价 × 欠费月数
        BigDecimal curFeePrice = new BigDecimal(feeDto.getFeePrice());
        curFeePrice = curFeePrice.multiply(new BigDecimal(oweMonth));
        feeDto.setAmountOwed(MoneyUtil.computePriceScale(curFeePrice.doubleValue(), feeDto.getScale(), Integer.parseInt(feeDto.getDecimalPlace())) + "");
        
        // 处理动态费用场景
        // 4004: 动态费用计算公式
        if ("4004".equals(computingFormula)
                && FeeDto.FEE_FLAG_ONCE.equals(feeDto.getFeeFlag())
                && !FeeDto.STATE_FINISH.equals(feeDto.getState())
                && feeDto.getDeadlineTime() == null) {
            // 动态费用且未完成时，设置到期时间为当前时间
            feeDto.setDeadlineTime(DateUtil.getCurrentDate());
        }
        // 处理租金递增逻辑
        computeFeeSMOImpl.dealRentRate(feeDto);
    }

}