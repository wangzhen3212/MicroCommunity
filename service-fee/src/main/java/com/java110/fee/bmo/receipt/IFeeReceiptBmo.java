package com.java110.fee.bmo.receipt;

import com.java110.po.fee.PayFeeDetailPo;
import com.java110.po.prePayFee.PrePayFeePo;

public interface IFeeReceiptBmo {

    // 预付款生成收据
    void savePrePayFeeReceipt(PrePayFeePo prePayFeePo,String receiptCode);

    // 预付款生成收据（显式指定收费对象类型）
    void savePrePayFeeReceipt(PrePayFeePo prePayFeePo, String receiptCode, String payerObjType);

    // 缴费生成收据
    void savePayFeeReceipt(PayFeeDetailPo payFeeDetailPo, String receiptCode);
}
