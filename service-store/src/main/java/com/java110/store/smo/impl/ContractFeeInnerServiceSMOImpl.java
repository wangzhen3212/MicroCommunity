package com.java110.store.smo.impl;

import com.java110.core.base.smo.BaseServiceSMO;
import com.java110.dto.PageDto;
import com.java110.dto.contract.ContractFeeDto;
import com.java110.intf.store.IContractFeeInnerServiceSMO;
import com.java110.po.contract.ContractFeePo;
import com.java110.store.dao.IContractFeeServiceDao;
import com.java110.store.util.ContractFeeNormalizeUtil;
import com.java110.utils.util.BeanConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 合同费用构成内部服务实现类
 */
@RestController
public class ContractFeeInnerServiceSMOImpl extends BaseServiceSMO implements IContractFeeInnerServiceSMO {

    @Autowired
    private IContractFeeServiceDao contractFeeServiceDaoImpl;

    @Override
    public int saveContractFee(@RequestBody ContractFeePo contractFeePo) {
        ContractFeeNormalizeUtil.normalizeForSave(contractFeePo);
        return contractFeeServiceDaoImpl.saveContractFeeInfo(BeanConvertUtil.beanCovertMap(contractFeePo));
    }

    @Override
    public int updateContractFee(@RequestBody ContractFeePo contractFeePo) {
        ContractFeeNormalizeUtil.normalizeForUpdate(contractFeePo);
        return contractFeeServiceDaoImpl.updateContractFeeInfo(BeanConvertUtil.beanCovertMap(contractFeePo));
    }

    @Override
    public List<ContractFeeDto> queryContractFees(@RequestBody ContractFeeDto contractFeeDto) {
        int page = contractFeeDto.getPage();
        if (page != PageDto.DEFAULT_PAGE) {
            contractFeeDto.setPage((page - 1) * contractFeeDto.getRow());
        }
        return BeanConvertUtil.covertBeanList(
                contractFeeServiceDaoImpl.getContractFeeInfo(BeanConvertUtil.beanCovertMap(contractFeeDto)),
                ContractFeeDto.class);
    }

    @Override
    public int queryContractFeesCount(@RequestBody ContractFeeDto contractFeeDto) {
        return contractFeeServiceDaoImpl.queryContractFeesCount(BeanConvertUtil.beanCovertMap(contractFeeDto));
    }
}
