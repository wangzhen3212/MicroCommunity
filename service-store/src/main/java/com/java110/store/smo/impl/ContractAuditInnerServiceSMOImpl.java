package com.java110.store.smo.impl;

import com.java110.core.base.smo.BaseServiceSMO;
import com.java110.dto.PageDto;
import com.java110.dto.contract.ContractAuditLogDto;
import com.java110.intf.store.IContractAuditInnerServiceSMO;
import com.java110.po.contract.ContractAuditLogPo;
import com.java110.store.dao.IContractAuditLogServiceDao;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 合同审批记录（contract_audit_log）内部服务实现
 */
@RestController
public class ContractAuditInnerServiceSMOImpl extends BaseServiceSMO implements IContractAuditInnerServiceSMO {

    @Autowired
    private IContractAuditLogServiceDao contractAuditLogServiceDaoImpl;

    @Override
    public int saveContractAuditLog(@RequestBody ContractAuditLogPo contractAuditLogPo) {
        return contractAuditLogServiceDaoImpl.saveContractAuditLogInfo(BeanConvertUtil.beanCovertMap(contractAuditLogPo));
    }

    @Override
    public List<ContractAuditLogDto> queryContractAuditLogs(@RequestBody ContractAuditLogDto contractAuditLogDto) {
        int page = contractAuditLogDto.getPage();
        if (page != PageDto.DEFAULT_PAGE) {
            contractAuditLogDto.setPage((page - 1) * contractAuditLogDto.getRow());
        }
        return BeanConvertUtil.covertBeanList(
                contractAuditLogServiceDaoImpl.queryContractAuditLogs(BeanConvertUtil.beanCovertMap(contractAuditLogDto)),
                ContractAuditLogDto.class);
    }
}
