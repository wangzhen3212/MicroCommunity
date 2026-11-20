import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

// 合同台账统计（总数/分类计数/生效数/待审数/总金额/覆盖房屋数）
export function queryContractStatistics(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.listContractStatistics',
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

// 合同导出（台账/履约/提醒），返回 { fileName, base64 }
export function exportContracts(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.exportContracts',
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

// 批量提交审核
export function batchSubmitContract(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.batchSubmitContract',
      method: 'post',
      data: {
        ...data,
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

// 批量终止
export function batchStopContract(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.batchStopContract',
      method: 'post',
      data: {
        ...data,
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
