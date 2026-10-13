import request from '@/utils/request'

// 查询核销订单列表
export function listCommunitySpaceConfirmOrder(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.listCommunitySpaceConfirmOrder',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      resolve({
        data: res.data,
        total: res.total
      })
    }).catch(error => {
      reject(error)
    })
  })
}

// 核销场地订单
export function saveCommunitySpaceConfirmOrder(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.saveCommunitySpaceConfirmOrder',
      method: 'post',
      data
    }).then(response => {
      const res = response.data
      if (res.code == 0) {
        resolve(res)
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 扫码解析预约核销信息（扫码前预校验）
export function getCommunitySpaceConfirmOrder(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.getCommunitySpaceConfirmOrder',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res.data)
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 撤销核销
export function deleteCommunitySpaceConfirmOrder(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.deleteCommunitySpaceConfirmOrder',
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

// 查询核销统计
export function listCommunitySpaceConfirmOrderSummary(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.listCommunitySpaceConfirmOrderSummary',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res.data)
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}