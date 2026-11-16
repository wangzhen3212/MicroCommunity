package com.java110.store.dao.impl;

import com.java110.core.base.dao.BaseServiceDao;
import com.java110.store.dao.IContractAuditConfigServiceDao;
import com.java110.utils.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 合同审核人配置 与数据库交互
 */
@Service("contractAuditConfigServiceDaoImpl")
public class ContractAuditConfigServiceDaoImpl extends BaseServiceDao implements IContractAuditConfigServiceDao {

    private static Logger logger = LoggerFactory.getLogger(ContractAuditConfigServiceDaoImpl.class);

    @Override
    public int saveContractAuditConfigInfo(Map info) throws DAOException {
        logger.debug("保存合同审核人配置 info : {}", info);
        return sqlSessionTemplate.insert("contractAuditConfigServiceDaoImpl.saveContractAuditConfigInfo", info);
    }

    @Override
    public int updateContractAuditConfigInfo(Map info) throws DAOException {
        logger.debug("修改合同审核人配置 info : {}", info);
        return sqlSessionTemplate.update("contractAuditConfigServiceDaoImpl.updateContractAuditConfigInfo", info);
    }

    @Override
    public List<Map> getContractAuditConfigInfo(Map info) throws DAOException {
        logger.debug("查询合同审核人配置 info : {}", info);
        return sqlSessionTemplate.selectList("contractAuditConfigServiceDaoImpl.getContractAuditConfigInfo", info);
    }

    @Override
    public int queryContractAuditConfigsCount(Map info) {
        List<Map> counts = sqlSessionTemplate.selectList("contractAuditConfigServiceDaoImpl.queryContractAuditConfigsCount", info);
        if (counts.size() < 1) {
            return 0;
        }
        return Integer.parseInt(counts.get(0).get("count").toString());
    }
}
