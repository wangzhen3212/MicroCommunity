import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

// 查询甲方主体列表
export function queryContractPartya(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contractPartya/queryContractPartya',
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

// 新增甲方主体
export function saveContractPartya(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contractPartya/saveContractPartya',
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

// 修改甲方主体
export function updateContractPartya(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contractPartya/updateContractPartya',
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

// 删除甲方主体
export function deleteContractPartya(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contractPartya/deleteContractPartya',
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

// 设置默认甲方主体（唯一性由后端保证）
export function setDefaultContractPartya(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.setDefaultContractPartya',
      method: 'post',
      data
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
