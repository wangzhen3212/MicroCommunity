package com.java110.store.dao.impl;

import com.java110.core.base.dao.BaseServiceDao;
import com.java110.store.dao.IContractAuditServiceDao;
import com.java110.utils.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 合同审批实例 与数据库交互
 */
@Service("contractAuditServiceDaoImpl")
public class ContractAuditServiceDaoImpl extends BaseServiceDao implements IContractAuditServiceDao {

    private static Logger logger = LoggerFactory.getLogger(ContractAuditServiceDaoImpl.class);

    @Override
    public int saveContractAuditInfo(Map info) throws DAOException {
        logger.debug("保存合同审批实例 info : {}", info);
        return sqlSessionTemplate.insert("contractAuditServiceDaoImpl.saveContractAuditInfo", info);
    }

    @Override
    public int updateContractAuditInfo(Map info) throws DAOException {
        logger.debug("修改合同审批实例 info : {}", info);
        return sqlSessionTemplate.update("contractAuditServiceDaoImpl.updateContractAuditInfo", info);
    }

    @Override
    public List<Map> getContractAuditInfo(Map info) throws DAOException {
        logger.debug("查询合同审批实例 info : {}", info);
        return sqlSessionTemplate.selectList("contractAuditServiceDaoImpl.getContractAuditInfo", info);
    }

    @Override
    public int queryContractAuditsCount(Map info) {
        List<Map> counts = sqlSessionTemplate.selectList("contractAuditServiceDaoImpl.queryContractAuditsCount", info);
        if (counts.size() < 1) {
            return 0;
        }
        return Integer.parseInt(counts.get(0).get("count").toString());
    }

    @Override
    public List<Map> queryContractAuditStatistics(Map info) {
        return sqlSessionTemplate.selectList("contractAuditServiceDaoImpl.queryContractAuditStatistics", info);
    }
}
