package com.java110.store.dao.impl;

import com.java110.core.base.dao.BaseServiceDao;
import com.java110.store.dao.IContractFeeServiceDao;
import com.java110.utils.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 合同费用构成 与数据库交互
 */
@Service("contractFeeServiceDaoImpl")
public class ContractFeeServiceDaoImpl extends BaseServiceDao implements IContractFeeServiceDao {

    private static Logger logger = LoggerFactory.getLogger(ContractFeeServiceDaoImpl.class);

    @Override
    public int saveContractFeeInfo(Map info) throws DAOException {
        logger.debug("保存合同费用构成 info : {}", info);
        return sqlSessionTemplate.insert("contractFeeServiceDaoImpl.saveContractFeeInfo", info);
    }

    @Override
    public int updateContractFeeInfo(Map info) throws DAOException {
        logger.debug("修改合同费用构成 info : {}", info);
        return sqlSessionTemplate.update("contractFeeServiceDaoImpl.updateContractFeeInfo", info);
    }

    @Override
    public List<Map> getContractFeeInfo(Map info) throws DAOException {
        logger.debug("查询合同费用构成 info : {}", info);
        return sqlSessionTemplate.selectList("contractFeeServiceDaoImpl.getContractFeeInfo", info);
    }

    @Override
    public int queryContractFeesCount(Map info) {
        List<Map> counts = sqlSessionTemplate.selectList("contractFeeServiceDaoImpl.queryContractFeesCount", info);
        if (counts.size() < 1) {
            return 0;
        }
        return Integer.parseInt(counts.get(0).get("count").toString());
    }
}
