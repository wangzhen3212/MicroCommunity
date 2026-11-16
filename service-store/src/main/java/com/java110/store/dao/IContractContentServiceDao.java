package com.java110.store.dao;

import com.java110.utils.exception.DAOException;

import java.util.List;
import java.util.Map;

/**
 * 合同正文 DAO（正文独立表）
 */
public interface IContractContentServiceDao {

    int saveContractContentInfo(Map info) throws DAOException;

    int updateContractContentInfo(Map info) throws DAOException;

    List<Map> getContractContentInfo(Map info) throws DAOException;

    int getContractContentCount(Map info);
}
