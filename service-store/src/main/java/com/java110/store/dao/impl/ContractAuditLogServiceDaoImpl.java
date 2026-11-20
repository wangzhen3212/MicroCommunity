package com.java110.store.dao.impl;

import com.java110.core.base.dao.BaseServiceDao;
import com.java110.store.dao.IContractAuditLogServiceDao;
import com.java110.utils.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 合同审批记录 与数据库交互（contract_audit_log）
 */
@Service("contractAuditLogServiceDaoImpl")
public class ContractAuditLogServiceDaoImpl extends BaseServiceDao implements IContractAuditLogServiceDao {

    private static Logger logger = LoggerFactory.getLogger(ContractAuditLogServiceDaoImpl.class);

    @Override
    public int saveContractAuditLogInfo(Map info) throws DAOException {
        logger.debug("保存合同审批记录 info : {}", info);
        return sqlSessionTemplate.insert("contractAuditLogServiceDaoImpl.saveContractAuditLogInfo", info);
    }

    @Override
    public List<Map> queryContractAuditLogs(Map info) throws DAOException {
        logger.debug("查询合同审批记录 info : {}", info);
        return sqlSessionTemplate.selectList("contractAuditLogServiceDaoImpl.queryContractAuditLogs", info);
    }

    @Override
    public int queryContractAuditLogsCount(Map info) {
        List<Map> counts = sqlSessionTemplate.selectList("contractAuditLogServiceDaoImpl.queryContractAuditLogsCount", info);
        if (counts.size() < 1) {
            return 0;
        }
        return Integer.parseInt(counts.get(0).get("count").toString());
    }
}
