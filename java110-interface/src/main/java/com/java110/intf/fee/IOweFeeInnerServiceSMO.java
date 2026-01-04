package com.java110.intf.fee;

import com.alibaba.fastjson.JSONArray;
import com.java110.config.feign.FeignConfiguration;
import com.java110.dto.fee.BillDto;
import com.java110.dto.fee.BillOweFeeDto;
import com.java110.dto.fee.FeeAttrDto;
import com.java110.dto.fee.FeeDto;
import com.java110.po.fee.PayFeePo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * @ClassName IFeeInnerServiceSMO
 * @Description 费用接口类
 * @Author wuxw
 * @Date 2019/4/24 9:04
 * @Version 1.0
 * add by wuxw 2019/4/24
 **/
@FeignClient(name = "fee-service", configuration = {FeignConfiguration.class})
@RequestMapping("/oweFeeApi")
public interface IOweFeeInnerServiceSMO {

    /**
     * <p>查询小区楼信息</p>
     *
     * @param feeDto 数据对象分享
     * @return FeeDto 对象数据
     */
    @RequestMapping(value = "/queryOwnerOweFee", method = RequestMethod.POST)
    List<FeeDto> queryOwnerOweFee(@RequestBody FeeDto feeDto);
}
