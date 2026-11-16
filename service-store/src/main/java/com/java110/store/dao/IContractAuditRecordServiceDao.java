package com.java110.store.dao;

import com.java110.utils.exception.DAOException;

import java.util.List;
import java.util.Map;

/**
 * 合同审批环节记录 DAO
 */
public interface IContractAuditRecordServiceDao {

    int saveContractAuditRecordInfo(Map info) throws DAOException;

    int updateContractAuditRecordInfo(Map info) throws DAOException;

    List<Map> getContractAuditRecordInfo(Map info) throws DAOException;

    int queryContractAuditRecordsCount(Map info);
}
