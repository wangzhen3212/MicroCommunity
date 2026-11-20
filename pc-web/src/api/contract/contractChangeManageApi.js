import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

/**
 * 查询合同变更计划列表
 * @param {Object} params 查询参数
 * @returns {Promise} 返回Promise对象
 */
export function queryContractChangePlan(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract/queryContractChangePlan',
      method: 'get',
      params: {
        ...params,
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

/**
 * 删除合同变更计划

 * @param {Object} data 删除数据
 * @returns {Promise} 返回Promise对象
 */
export function deleteContractChangePlan(data) {
  return new Promise((resolve, reject) => {
request({
      url: '/contract/deleteContractChangePlan',
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