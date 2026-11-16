package com.java110.intf.store;

import com.java110.config.feign.FeignConfiguration;
import com.java110.dto.contract.ContractContentDto;
import com.java110.po.contract.ContractContentPo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * 合同正文内部服务（正文独立表，按 contract_id 单条查询/upsert）
 */
@FeignClient(name = "store-service", configuration = {FeignConfiguration.class})
@RequestMapping("/contractContentApi")
public interface IContractContentInnerServiceSMO {

    @RequestMapping(value = "/saveContractContent", method = RequestMethod.POST)
    int saveContractContent(@RequestBody ContractContentPo contractContentPo);

    @RequestMapping(value = "/updateContractContent", method = RequestMethod.POST)
    int updateContractContent(@RequestBody ContractContentPo contractContentPo);

    @RequestMapping(value = "/queryContractContents", method = RequestMethod.POST)
    List<ContractContentDto> queryContractContents(@RequestBody ContractContentDto contractContentDto);
}
