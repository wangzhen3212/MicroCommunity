package com.java110.intf.store;

import com.java110.config.feign.FeignConfiguration;
import com.java110.dto.contract.ContractAuditLogDto;
import com.java110.po.contract.ContractAuditLogPo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * 合同审批记录（contract_audit_log）内部服务
 */
@FeignClient(name = "store-service", configuration = {FeignConfiguration.class})
@RequestMapping("/contractAuditApi")
public interface IContractAuditInnerServiceSMO {

    @RequestMapping(value = "/saveContractAuditLog", method = RequestMethod.POST)
    int saveContractAuditLog(@RequestBody ContractAuditLogPo contractAuditLogPo);

    @RequestMapping(value = "/queryContractAuditLogs", method = RequestMethod.POST)
    List<ContractAuditLogDto> queryContractAuditLogs(@RequestBody ContractAuditLogDto contractAuditLogDto);
}
