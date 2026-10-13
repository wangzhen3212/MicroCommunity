import request from '@/utils/request'

// 查询预约订单列表
export function listCommunitySpacePerson(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.listCommunitySpacePerson',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}

// 删除预约订单
export function deleteCommunitySpacePerson(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.deleteCommunitySpacePerson',
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

// 查询场馆列表
export function listCommunityVenue(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communityVenue.listCommunityVenue',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}

// 查询场地列表
export function listCommunitySpace(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.listCommunitySpace',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}

// 修改预约订单（收款）
export function updateCommunitySpacePerson(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.updateCommunitySpacePerson',
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

// 查询预约订单汇总（总数/待支付/进行中/已完成/已取消/应收合计/实收合计）
export function listCommunitySpacePersonSummary(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.listCommunitySpacePersonSummary',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      resolve(res.data)
    }).catch(error => {
      reject(error)
    })
  })
}