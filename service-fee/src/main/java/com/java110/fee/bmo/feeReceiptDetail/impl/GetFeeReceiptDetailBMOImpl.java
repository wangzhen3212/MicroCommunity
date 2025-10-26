package com.java110.fee.bmo.feeReceiptDetail.impl;

import com.java110.dto.fee.FeeDto;
import com.java110.dto.fee.FeeReceiptDetailDto;
import com.java110.dto.fee.FeeReceiptDto;
import com.java110.dto.payFee.PayFeeDetailDiscountDto;
import com.java110.fee.bmo.feeReceiptDetail.IGetFeeReceiptDetailBMO;
import com.java110.intf.fee.IFeeReceiptDetailInnerServiceSMO;
import com.java110.intf.fee.IPayFeeDetailDiscountInnerServiceSMO;
import com.java110.utils.util.DateUtil;
import com.java110.utils.util.ListUtil;
import com.java110.utils.util.StringUtil;
import com.java110.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 费用收据明细查询业务实现类
 * 
 * <p>该类主要负责处理费用收据明细的查询、优惠金额计算和一次性费用合并等业务逻辑</p>
 * 
 * @author Java110
 * @version 1.0
 * @since 2024
 */
@Service("getFeeReceiptDetailBMOImpl")
public class GetFeeReceiptDetailBMOImpl implements IGetFeeReceiptDetailBMO {

    /**
     * 费用收据明细内部服务接口
     */
    @Autowired
    private IFeeReceiptDetailInnerServiceSMO feeReceiptDetailInnerServiceSMOImpl;

    /**
     * 支付费用明细折扣内部服务接口
     */
    @Autowired
    private IPayFeeDetailDiscountInnerServiceSMO payFeeDetailDiscountInnerServiceSMOImpl;

    /**
     * 查询费用收据明细列表
     * 
     * <p>该方法根据查询条件获取费用收据明细，计算优惠金额，并根据配置合并一次性费用</p>
     * 
     * @param feeReceiptDetailDto 费用收据明细查询条件DTO
     * @param mergeFee 费用合并标识，"ON"表示开启合并，其他值不合并
     * @return ResponseEntity<String> 包含查询结果的响应实体，格式为订单服务能够接受的报文
     */
    public ResponseEntity<String> get(FeeReceiptDetailDto feeReceiptDetailDto, String mergeFee) {
        // 查询符合条件的费用收据明细总数
        int count = feeReceiptDetailInnerServiceSMOImpl.queryFeeReceiptDetailsCount(feeReceiptDetailDto);

        List<FeeReceiptDetailDto> feeReceiptDetailDtos = null;
        if (count > 0) {
            // 查询费用收据明细列表
            feeReceiptDetailDtos = feeReceiptDetailInnerServiceSMOImpl.queryFeeReceiptDetails(feeReceiptDetailDto);
        } else {
            feeReceiptDetailDtos = new ArrayList<>();
        }

        // 计算优惠金额
        computeDiscountFee(feeReceiptDetailDtos);

        // 合并一次性费用，按费用项名称和单价合并
        feeReceiptDetailDtos = mergeOnceFee(feeReceiptDetailDtos, mergeFee);

        // 构建分页结果对象
        ResultVo resultVo = new ResultVo((int) Math.ceil((double) count / (double) feeReceiptDetailDto.getRow()), count, feeReceiptDetailDtos);

        // 返回响应实体
        ResponseEntity<String> responseEntity = new ResponseEntity<String>(resultVo.toString(), HttpStatus.OK);

        return responseEntity;
    }

    /**
     * 计算优惠金额
     * 
     * <p>该方法根据费用明细ID列表查询对应的折扣信息，并设置到费用收据明细中</p>
     * 
     * @param feeReceiptDetailDtos 费用收据明细列表
     */
    private void computeDiscountFee(List<FeeReceiptDetailDto> feeReceiptDetailDtos) {
        // 检查参数有效性
        if (feeReceiptDetailDtos == null || feeReceiptDetailDtos.size() < 1) {
            return;
        }

        // 收集所有费用明细ID
        List<String> detailIds = new ArrayList<>();
        for (FeeReceiptDetailDto feeReceiptDetailDto : feeReceiptDetailDtos) {
            detailIds.add(feeReceiptDetailDto.getDetailId());
        }

        // 构建折扣查询条件
        PayFeeDetailDiscountDto payFeeDetailDiscountDto = new PayFeeDetailDiscountDto();
        payFeeDetailDiscountDto.setCommunityId(feeReceiptDetailDtos.get(0).getCommunityId());
        payFeeDetailDiscountDto.setDetailIds(detailIds.toArray(new String[detailIds.size()]));

        // 查询折扣信息
        List<PayFeeDetailDiscountDto> payFeeDetailDiscountDtos = payFeeDetailDiscountInnerServiceSMOImpl.computeDiscountFee(payFeeDetailDiscountDto);

        // 将折扣信息设置到对应的费用收据明细中
        for (FeeReceiptDetailDto feeReceiptDetailDto : feeReceiptDetailDtos) {
            for (PayFeeDetailDiscountDto tmpPayFeeDetailDiscountDto : payFeeDetailDiscountDtos) {
                if (!feeReceiptDetailDto.getDetailId().equals(tmpPayFeeDetailDiscountDto.getDetailId())) {
                    continue;
                }
                feeReceiptDetailDto.setDiscountPrice(tmpPayFeeDetailDiscountDto.getDiscountPrice());
            }
        }
    }

    /**
     * 合并一次性费用
     * 
     * <p>该方法根据配置将符合条件的一次性费用进行合并，合并条件包括费用名称、单价、缴费对象和时间连续性</p>
     * 
     * @param feeReceiptDetailDtos 原始费用收据明细列表
     * @param mergeFee 费用合并标识，"ON"表示开启合并
     * @return List<FeeReceiptDetailDto> 合并后的费用收据明细列表
     */
    private List<FeeReceiptDetailDto> mergeOnceFee(List<FeeReceiptDetailDto> feeReceiptDetailDtos, String mergeFee) {
        // 检查参数有效性
        if (ListUtil.isNull(feeReceiptDetailDtos)) {
            return feeReceiptDetailDtos;
        }

        // 检查是否开启费用合并
        if (!"ON".equals(mergeFee)) {
            return feeReceiptDetailDtos;
        }

        // 初始化费用分类列表
        List<FeeReceiptDetailDto> cycleFeeReceiptDetailDtos = new ArrayList<>();
        List<FeeReceiptDetailDto> onceFeeReceiptDetailDtos = new ArrayList<>();

        // 将所有费用明细都视为一次性费用进行处理
        // 注释掉的代码原本用于区分周期性和一次性费用
//        for (FeeReceiptDetailDto feeReceiptDetailDto : feeReceiptDetailDtos) {
//            if (FeeDto.FEE_FLAG_ONCE.equals(feeReceiptDetailDto.getFeeFlag())) {
//                onceFeeReceiptDetailDtos.add(feeReceiptDetailDto);
//                continue;
//            }
//            cycleFeeReceiptDetailDtos.add(feeReceiptDetailDto);
//        }
        for (FeeReceiptDetailDto feeReceiptDetailDto : feeReceiptDetailDtos) {
            onceFeeReceiptDetailDtos.add(feeReceiptDetailDto);
        }
        
        // 如果没有一次性费用，直接返回原始列表
        if (ListUtil.isNull(onceFeeReceiptDetailDtos)) {
            return feeReceiptDetailDtos;
        }

        // 按结束时间升序排序
        Collections.sort(onceFeeReceiptDetailDtos, new Comparator<FeeReceiptDetailDto>() {
            @Override
            public int compare(FeeReceiptDetailDto receiptDto1, FeeReceiptDetailDto receiptDto2) {
                Date endTime1 = DateUtil.getDateFromStringA(receiptDto1.getEndTime());
                Date endTime2 = DateUtil.getDateFromStringA(receiptDto2.getEndTime());
                return Long.compare(endTime1.getTime(), endTime2.getTime());
            }
        });
        
        // 合并后的费用明细列表
        List<FeeReceiptDetailDto> nOnceFeeReceiptDetailDtos = new ArrayList<>();

        // 遍历所有一次性费用进行合并处理
        for (FeeReceiptDetailDto feeReceiptDetailDto : onceFeeReceiptDetailDtos) {
            doMergeOnceFee(nOnceFeeReceiptDetailDtos, feeReceiptDetailDto);
        }

        // 将合并后的费用明细添加到结果列表中
        cycleFeeReceiptDetailDtos.addAll(nOnceFeeReceiptDetailDtos);

        return cycleFeeReceiptDetailDtos;
    }

    /**
     * 执行一次性费用合并操作
     * 
     * <p>该方法根据费用名称、单价、缴费对象和时间连续性等条件，将符合条件的费用明细进行合并</p>
     * 
     * @param nOnceFeeReceiptDetailDtos 已合并的费用明细列表
     * @param feeReceiptDetailDto 当前待合并的费用明细
     */
    private void doMergeOnceFee(List<FeeReceiptDetailDto> nOnceFeeReceiptDetailDtos, FeeReceiptDetailDto feeReceiptDetailDto) {
        // 如果目标列表为空，直接添加当前费用明细
        if (ListUtil.isNull(nOnceFeeReceiptDetailDtos)) {
            nOnceFeeReceiptDetailDtos.add(feeReceiptDetailDto);
            return;
        }
        
        FeeReceiptDetailDto nOnceFeeReceiptDetailDto = null;
        
        // 遍历已合并的费用明细，查找符合条件的合并目标
        for (FeeReceiptDetailDto nFeeReceiptDetailDto : nOnceFeeReceiptDetailDtos) {
            // 检查费用名称是否相同
            if (!nFeeReceiptDetailDto.getFeeName().equals(feeReceiptDetailDto.getFeeName())) {
                continue;
            }

            // 检查单价是否相同（如果单价不为空）
            if (!StringUtil.isEmpty(nFeeReceiptDetailDto.getSquarePrice())
                    && !nFeeReceiptDetailDto.getSquarePrice().equals(feeReceiptDetailDto.getSquarePrice())) {
                continue;
            }

            // 检查单价状态是否一致（一个为空一个不为空）
            if(StringUtil.isEmpty(nFeeReceiptDetailDto.getSquarePrice()) && !StringUtil.isEmpty(feeReceiptDetailDto.getSquarePrice())){
                continue;
            }

            // 检查缴费对象是否相同
            if (!nFeeReceiptDetailDto.getPayerObjId().equals(feeReceiptDetailDto.getPayerObjId())) {
                continue;
            }

            // 检查时间连续性：计算两个费用明细之间的时间差
            Date endTime = DateUtil.getDateFromStringB(nFeeReceiptDetailDto.getEndTime());
            Date startTime = DateUtil.getDateFromStringB(feeReceiptDetailDto.getStartTime());
            long millisecondsBetween = startTime.getTime() - endTime.getTime();
            // 将毫秒差转换为天数
            long daysBetween = millisecondsBetween / (1000 * 60 * 60 * 24);
            // 判断天数差是否在-3到3天之间（允许一定的时间重叠或间隔）
            if (daysBetween < -3 || daysBetween > 3) {
                continue;
            }

            // 找到符合条件的合并目标
            nOnceFeeReceiptDetailDto = nFeeReceiptDetailDto;
        }

        // 如果没有找到符合条件的合并目标，将当前费用明细添加到列表中
        if (nOnceFeeReceiptDetailDto == null) {
            nOnceFeeReceiptDetailDtos.add(feeReceiptDetailDto);
            return;
        }

        // 执行合并操作：更新结束时间
        nOnceFeeReceiptDetailDto.setEndTime(feeReceiptDetailDto.getEndTime());
        
        // 合并金额：实际支付金额
        BigDecimal amount = new BigDecimal(nOnceFeeReceiptDetailDto.getAmount());
        amount = amount.add(new BigDecimal(feeReceiptDetailDto.getAmount()));
        nOnceFeeReceiptDetailDto.setAmount(amount.doubleValue() + "");
        
        // 更新当前度数
        nOnceFeeReceiptDetailDto.setCurDegrees(feeReceiptDetailDto.getCurDegrees());

        // 合并金额：应收金额
        BigDecimal receivableAmount = new BigDecimal(nOnceFeeReceiptDetailDto.getReceivableAmount());
        receivableAmount = receivableAmount.add(new BigDecimal(feeReceiptDetailDto.getReceivableAmount()));
        nOnceFeeReceiptDetailDto.setReceivableAmount(receivableAmount.doubleValue() + "");

        // 合并金额：记账金额
        BigDecimal acctAmount = new BigDecimal(nOnceFeeReceiptDetailDto.getAcctAmount());
        acctAmount = acctAmount.add(new BigDecimal(feeReceiptDetailDto.getAcctAmount()));
        nOnceFeeReceiptDetailDto.setAcctAmount(acctAmount.doubleValue() + "");

        // 合并面积（如果当前度数不为空）
        try {
            if (!StringUtil.isEmpty(feeReceiptDetailDto.getCurDegrees())) {
                BigDecimal areaAmount = new BigDecimal(nOnceFeeReceiptDetailDto.getArea());
                areaAmount = areaAmount.add(new BigDecimal(feeReceiptDetailDto.getArea()));
                nOnceFeeReceiptDetailDto.setArea(areaAmount.doubleValue() + "");
            }
        } catch (Exception e) {
            // 面积合并异常处理，忽略异常继续执行
        }
    }

}