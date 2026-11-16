package com.java110.store.smo.impl;

import com.java110.core.base.smo.BaseServiceSMO;
import com.java110.dto.PageDto;
import com.java110.dto.contract.ContractAuditConfigDto;
import com.java110.dto.contract.ContractAuditDto;
import com.java110.dto.contract.ContractAuditRecordDto;
import com.java110.intf.store.IContractAuditInnerServiceSMO;
import com.java110.po.contract.ContractAuditConfigPo;
import com.java110.po.contract.ContractAuditPo;
import com.java110.po.contract.ContractAuditRecordPo;
import com.java110.store.dao.IContractAuditConfigServiceDao;
import com.java110.store.dao.IContractAuditRecordServiceDao;
import com.java110.store.dao.IContractAuditServiceDao;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 合同两级审批（审批实例 / 环节记录 / 审核人配置）内部服务实现
 */
@RestController
public class ContractAuditInnerServiceSMOImpl extends BaseServiceSMO implements IContractAuditInnerServiceSMO {

    @Autowired
    private IContractAuditServiceDao contractAuditServiceDaoImpl;

    @Autowired
    private IContractAuditRecordServiceDao contractAuditRecordServiceDaoImpl;

    @Autowired
    private IContractAuditConfigServiceDao contractAuditConfigServiceDaoImpl;

    @Override
    public int saveContractAudit(@RequestBody ContractAuditPo contractAuditPo) {
        return contractAuditServiceDaoImpl.saveContractAuditInfo(BeanConvertUtil.beanCovertMap(contractAuditPo));
    }

    @Override
    public int updateContractAudit(@RequestBody ContractAuditPo contractAuditPo) {
        return contractAuditServiceDaoImpl.updateContractAuditInfo(BeanConvertUtil.beanCovertMap(contractAuditPo));
    }

    @Override
    public List<ContractAuditDto> queryContractAudits(@RequestBody ContractAuditDto contractAuditDto) {
        int page = contractAuditDto.getPage();
        if (page != PageDto.DEFAULT_PAGE) {
            contractAuditDto.setPage((page - 1) * contractAuditDto.getRow());
        }
        return BeanConvertUtil.covertBeanList(
                contractAuditServiceDaoImpl.getContractAuditInfo(BeanConvertUtil.beanCovertMap(contractAuditDto)),
                ContractAuditDto.class);
    }

    @Override
    public int queryContractAuditsCount(@RequestBody ContractAuditDto contractAuditDto) {
        return contractAuditServiceDaoImpl.queryContractAuditsCount(BeanConvertUtil.beanCovertMap(contractAuditDto));
    }

    @Override
    public List<Map> queryContractAuditStatistics(@RequestBody Map info) {
        return contractAuditServiceDaoImpl.queryContractAuditStatistics(info);
    }

    @Override
    public int saveContractAuditRecord(@RequestBody ContractAuditRecordPo contractAuditRecordPo) {
        return contractAuditRecordServiceDaoImpl.saveContractAuditRecordInfo(BeanConvertUtil.beanCovertMap(contractAuditRecordPo));
    }

    @Override
    public int updateContractAuditRecord(@RequestBody ContractAuditRecordPo contractAuditRecordPo) {
        return contractAuditRecordServiceDaoImpl.updateContractAuditRecordInfo(BeanConvertUtil.beanCovertMap(contractAuditRecordPo));
    }

    @Override
    public List<ContractAuditRecordDto> queryContractAuditRecords(@RequestBody ContractAuditRecordDto contractAuditRecordDto) {
        int page = contractAuditRecordDto.getPage();
        if (page != PageDto.DEFAULT_PAGE) {
            contractAuditRecordDto.setPage((page - 1) * contractAuditRecordDto.getRow());
        }
        return BeanConvertUtil.covertBeanList(
                contractAuditRecordServiceDaoImpl.getContractAuditRecordInfo(BeanConvertUtil.beanCovertMap(contractAuditRecordDto)),
                ContractAuditRecordDto.class);
    }

    @Override
    public int queryContractAuditRecordsCount(@RequestBody ContractAuditRecordDto contractAuditRecordDto) {
        return contractAuditRecordServiceDaoImpl.queryContractAuditRecordsCount(BeanConvertUtil.beanCovertMap(contractAuditRecordDto));
    }

    @Override
    public int saveContractAuditConfig(@RequestBody ContractAuditConfigPo contractAuditConfigPo) {
        return contractAuditConfigServiceDaoImpl.saveContractAuditConfigInfo(BeanConvertUtil.beanCovertMap(contractAuditConfigPo));
    }

    @Override
    public int deleteContractAuditConfig(@RequestBody ContractAuditConfigPo contractAuditConfigPo) {
        contractAuditConfigPo.setStatusCd("1");
        return contractAuditConfigServiceDaoImpl.updateContractAuditConfigInfo(BeanConvertUtil.beanCovertMap(contractAuditConfigPo));
    }

    @Override
    public List<ContractAuditConfigDto> queryContractAuditConfigs(@RequestBody ContractAuditConfigDto contractAuditConfigDto) {
        int page = contractAuditConfigDto.getPage();
        if (page != PageDto.DEFAULT_PAGE) {
            contractAuditConfigDto.setPage((page - 1) * contractAuditConfigDto.getRow());
        }
        return BeanConvertUtil.covertBeanList(
                contractAuditConfigServiceDaoImpl.getContractAuditConfigInfo(BeanConvertUtil.beanCovertMap(contractAuditConfigDto)),
                ContractAuditConfigDto.class);
    }

    @Override
    public int queryContractAuditConfigsCount(@RequestBody ContractAuditConfigDto contractAuditConfigDto) {
        return contractAuditConfigServiceDaoImpl.queryContractAuditConfigsCount(BeanConvertUtil.beanCovertMap(contractAuditConfigDto));
    }
}
