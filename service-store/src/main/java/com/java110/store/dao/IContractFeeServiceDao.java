package com.java110.store.dao;

import com.java110.utils.exception.DAOException;

import java.util.List;
import java.util.Map;

/**
 * 合同费用构成 DAO（费用项快照 + 试算结果）
 */
public interface IContractFeeServiceDao {

    int saveContractFeeInfo(Map info) throws DAOException;

    int updateContractFeeInfo(Map info) throws DAOException;

    List<Map> getContractFeeInfo(Map info) throws DAOException;

    int queryContractFeesCount(Map info);
}
