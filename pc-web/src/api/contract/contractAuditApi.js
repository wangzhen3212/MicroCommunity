import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

// 审核通过/驳回（bizType 1 合同 / 2 变更单，result 1 通过 / 2 驳回）
export function auditContract(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.auditContract',
      method: 'post',
      data: {
        ...data,
        communityId: getCommunityId()
      }
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res)
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 查询合同待审核列表（state 11 法务校对 / 56 领导审批，含最近一次审批记录）
export function listPendingAuditContracts(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.listPendingAuditContracts',
      method: 'get',
      params: {
        ...params,
        communityId: getCommunityId()
      }
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve({
          data: res.data || [],
          total: res.total || 0
        })
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 查询单据审批情况（合同状态 + 审批记录时间线）
export function queryContractAuditFlow(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.queryContractAuditFlow',
      method: 'get',
      params: {
        ...params,
        communityId: getCommunityId()
      }
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res.data || {})
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}
