package com.java110.report.smo.impl;

import com.java110.dto.fee.FeeDto;
import com.java110.intf.report.IGeneratorOweFeeInnerServiceSMO;
import com.java110.po.reportFee.ReportFeeMonthStatisticsPo;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName GeneratorFeeMonthStatisticsInnerServiceSMOImpl
 * @Description TODO
 * @Author wuxw
 * @Date 2020/10/15 21:53
 * @Version 1.0
 * add by wuxw 2020/10/15
 **/
@RestController
public class GeneratorOweFeeInnerServiceSMOImpl implements IGeneratorOweFeeInnerServiceSMO {

    @Override
    public int generatorOweData(@RequestBody ReportFeeMonthStatisticsPo reportFeeMonthStatisticsPo) {
        return 0;
    }

    @Override
    public int computeOweFee(@RequestBody FeeDto feeDto) {
        return 1;
    }

}
