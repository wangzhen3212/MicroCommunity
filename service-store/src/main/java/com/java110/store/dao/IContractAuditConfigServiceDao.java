package com.java110.store.dao;

import com.java110.utils.exception.DAOException;

import java.util.List;
import java.util.Map;

/**
 * 合同审核人配置 DAO
 */
public interface IContractAuditConfigServiceDao {

    int saveContractAuditConfigInfo(Map info) throws DAOException;

    int updateContractAuditConfigInfo(Map info) throws DAOException;

    List<Map> getContractAuditConfigInfo(Map info) throws DAOException;

    int queryContractAuditConfigsCount(Map info);
}
