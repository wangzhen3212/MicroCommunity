package com.java110.store.dao.impl;

import com.java110.core.base.dao.BaseServiceDao;
import com.java110.store.dao.IContractAuditRecordServiceDao;
import com.java110.utils.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 合同审批环节记录 与数据库交互
 */
@Service("contractAuditRecordServiceDaoImpl")
public class ContractAuditRecordServiceDaoImpl extends BaseServiceDao implements IContractAuditRecordServiceDao {

    private static Logger logger = LoggerFactory.getLogger(ContractAuditRecordServiceDaoImpl.class);

    @Override
    public int saveContractAuditRecordInfo(Map info) throws DAOException {
        logger.debug("保存合同审批环节记录 info : {}", info);
        return sqlSessionTemplate.insert("contractAuditRecordServiceDaoImpl.saveContractAuditRecordInfo", info);
    }

    @Override
    public int updateContractAuditRecordInfo(Map info) throws DAOException {
        logger.debug("修改合同审批环节记录 info : {}", info);
        return sqlSessionTemplate.update("contractAuditRecordServiceDaoImpl.updateContractAuditRecordInfo", info);
    }

    @Override
    public List<Map> getContractAuditRecordInfo(Map info) throws DAOException {
        logger.debug("查询合同审批环节记录 info : {}", info);
        return sqlSessionTemplate.selectList("contractAuditRecordServiceDaoImpl.getContractAuditRecordInfo", info);
    }

    @Override
    public int queryContractAuditRecordsCount(Map info) {
        List<Map> counts = sqlSessionTemplate.selectList("contractAuditRecordServiceDaoImpl.queryContractAuditRecordsCount", info);
        if (counts.size() < 1) {
            return 0;
        }
        return Integer.parseInt(counts.get(0).get("count").toString());
    }
}
