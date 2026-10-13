import request from '@/utils/request'

// 获取场馆列表
export function listCommunityVenue(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communityVenue.listCommunityVenue',
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

// 获取场地列表
export function listCommunitySpace(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.listCommunitySpace',
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

// 获取场地开放时段，传 appointmentTime 时仅返回该日期可预约（未开放/已占用已排除）的时段
export function listCommunitySpaceOpenTime(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.listCommunitySpaceOpenTime',
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

// 获取场地预约记录
export function listCommunitySpacePerson(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.listCommunitySpacePerson',
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

// 保存场地预约
export function saveCommunitySpacePerson(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.saveCommunitySpacePerson',
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

// 修改场地预约订单（收款）
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
