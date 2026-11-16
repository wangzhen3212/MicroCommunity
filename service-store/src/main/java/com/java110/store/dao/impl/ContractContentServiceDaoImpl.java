package com.java110.store.dao.impl;

import com.java110.core.base.dao.BaseServiceDao;
import com.java110.store.dao.IContractContentServiceDao;
import com.java110.utils.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 合同正文 与数据库交互
 */
@Service("contractContentServiceDaoImpl")
public class ContractContentServiceDaoImpl extends BaseServiceDao implements IContractContentServiceDao {

    private static Logger logger = LoggerFactory.getLogger(ContractContentServiceDaoImpl.class);

    @Override
    public int saveContractContentInfo(Map info) throws DAOException {
        logger.debug("保存合同正文 info : {}", info);
        return sqlSessionTemplate.insert("contractContentServiceDaoImpl.saveContractContentInfo", info);
    }

    @Override
    public int updateContractContentInfo(Map info) throws DAOException {
        logger.debug("修改合同正文 info : {}", info);
        return sqlSessionTemplate.update("contractContentServiceDaoImpl.updateContractContentInfo", info);
    }

    @Override
    public List<Map> getContractContentInfo(Map info) throws DAOException {
        logger.debug("查询合同正文 info : {}", info);
        return sqlSessionTemplate.selectList("contractContentServiceDaoImpl.getContractContentInfo", info);
    }

    @Override
    public int getContractContentCount(Map info) {
        List<Map> counts = sqlSessionTemplate.selectList("contractContentServiceDaoImpl.getContractContentCount", info);
        if (counts.size() < 1) {
            return 0;
        }
        return Integer.parseInt(counts.get(0).get("count").toString());
    }
}
