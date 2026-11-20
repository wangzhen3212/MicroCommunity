package com.java110.store.dao;

import com.java110.utils.exception.DAOException;

import java.util.List;
import java.util.Map;

/**
 * 合同审批记录 DAO（contract_audit_log）
 */
public interface IContractAuditLogServiceDao {

    int saveContractAuditLogInfo(Map info) throws DAOException;

    List<Map> queryContractAuditLogs(Map info) throws DAOException;

    int queryContractAuditLogsCount(Map info);
}
