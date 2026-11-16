package com.java110.store.dao;

import com.java110.utils.exception.DAOException;

import java.util.List;
import java.util.Map;

/**
 * 合同审批实例 DAO
 */
public interface IContractAuditServiceDao {

    int saveContractAuditInfo(Map info) throws DAOException;

    int updateContractAuditInfo(Map info) throws DAOException;

    List<Map> getContractAuditInfo(Map info) throws DAOException;

    int queryContractAuditsCount(Map info);

    /**
     * 审批汇总统计（待我审批 / 本月已通过 / 本月驳回）
     */
    List<Map> queryContractAuditStatistics(Map info);
}
