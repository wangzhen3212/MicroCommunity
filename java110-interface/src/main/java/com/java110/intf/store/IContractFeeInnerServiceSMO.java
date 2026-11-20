package com.java110.intf.store;

import com.java110.config.feign.FeignConfiguration;
import com.java110.dto.contract.ContractFeeDto;
import com.java110.po.contract.ContractFeePo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * 合同费用构成内部服务（contract_fee）
 */
@FeignClient(name = "store-service", configuration = {FeignConfiguration.class})
@RequestMapping("/contractFeeApi")
public interface IContractFeeInnerServiceSMO {

    @RequestMapping(value = "/saveContractFee", method = RequestMethod.POST)
    int saveContractFee(@RequestBody ContractFeePo contractFeePo);

    @RequestMapping(value = "/updateContractFee", method = RequestMethod.POST)
    int updateContractFee(@RequestBody ContractFeePo contractFeePo);

    @RequestMapping(value = "/queryContractFees", method = RequestMethod.POST)
    List<ContractFeeDto> queryContractFees(@RequestBody ContractFeeDto contractFeeDto);

    @RequestMapping(value = "/queryContractFeesCount", method = RequestMethod.POST)
    int queryContractFeesCount(@RequestBody ContractFeeDto contractFeeDto);
}
