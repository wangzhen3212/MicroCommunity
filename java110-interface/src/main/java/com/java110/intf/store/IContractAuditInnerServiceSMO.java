package com.java110.intf.store;

import com.java110.config.feign.FeignConfiguration;
import com.java110.dto.contract.ContractAuditConfigDto;
import com.java110.dto.contract.ContractAuditDto;
import com.java110.dto.contract.ContractAuditRecordDto;
import com.java110.po.contract.ContractAuditConfigPo;
import com.java110.po.contract.ContractAuditPo;
import com.java110.po.contract.ContractAuditRecordPo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;

/**
 * 合同两级审批（审批实例 / 环节记录 / 审核人配置）内部服务
 */
@FeignClient(name = "store-service", configuration = {FeignConfiguration.class})
@RequestMapping("/contractAuditApi")
public interface IContractAuditInnerServiceSMO {

    @RequestMapping(value = "/saveContractAudit", method = RequestMethod.POST)
    int saveContractAudit(@RequestBody ContractAuditPo contractAuditPo);

    @RequestMapping(value = "/updateContractAudit", method = RequestMethod.POST)
    int updateContractAudit(@RequestBody ContractAuditPo contractAuditPo);

    @RequestMapping(value = "/queryContractAudits", method = RequestMethod.POST)
    List<ContractAuditDto> queryContractAudits(@RequestBody ContractAuditDto contractAuditDto);

    @RequestMapping(value = "/queryContractAuditsCount", method = RequestMethod.POST)
    int queryContractAuditsCount(@RequestBody ContractAuditDto contractAuditDto);

    /**
     * 审批汇总卡：待我审批、本月已通过、本月驳回
     */
    @RequestMapping(value = "/queryContractAuditStatistics", method = RequestMethod.POST)
    List<Map> queryContractAuditStatistics(@RequestBody Map info);

    @RequestMapping(value = "/saveContractAuditRecord", method = RequestMethod.POST)
    int saveContractAuditRecord(@RequestBody ContractAuditRecordPo contractAuditRecordPo);

    @RequestMapping(value = "/updateContractAuditRecord", method = RequestMethod.POST)
    int updateContractAuditRecord(@RequestBody ContractAuditRecordPo contractAuditRecordPo);

    @RequestMapping(value = "/queryContractAuditRecords", method = RequestMethod.POST)
    List<ContractAuditRecordDto> queryContractAuditRecords(@RequestBody ContractAuditRecordDto contractAuditRecordDto);

    @RequestMapping(value = "/queryContractAuditRecordsCount", method = RequestMethod.POST)
    int queryContractAuditRecordsCount(@RequestBody ContractAuditRecordDto contractAuditRecordDto);

    @RequestMapping(value = "/saveContractAuditConfig", method = RequestMethod.POST)
    int saveContractAuditConfig(@RequestBody ContractAuditConfigPo contractAuditConfigPo);

    @RequestMapping(value = "/deleteContractAuditConfig", method = RequestMethod.POST)
    int deleteContractAuditConfig(@RequestBody ContractAuditConfigPo contractAuditConfigPo);

    @RequestMapping(value = "/queryContractAuditConfigs", method = RequestMethod.POST)
    List<ContractAuditConfigDto> queryContractAuditConfigs(@RequestBody ContractAuditConfigDto contractAuditConfigDto);

    @RequestMapping(value = "/queryContractAuditConfigsCount", method = RequestMethod.POST)
    int queryContractAuditConfigsCount(@RequestBody ContractAuditConfigDto contractAuditConfigDto);
}
