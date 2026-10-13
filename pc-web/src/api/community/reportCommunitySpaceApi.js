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
// 传 spaceId 时返回小时维度的开放明细，供扫码前只读复核；返回完整响应，便于判断 code
export function listCommunitySpaceOpenTime(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/communitySpace.listCommunitySpaceOpenTime',
      method: 'post',
      data
    }).then(response => {
      const res = response.data
      resolve(res)
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

// 保存场地预约（建单即收款，返回 cspId/orderId/receivableAmount/receivedAmount/state）
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

// 扫码枪支付（被扫），返回完整响应，按 code 分支处理
export function qrCodePayment(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/payment.qrCodePayment',
      method: 'post',
      data
    }).then(response => {
      const res = response.data
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}

// 查询扫码支付结果，返回完整响应，按 code 分支处理
export function checkPayFinish(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/payment.checkPayFinish',
      method: 'post',
      data
    }).then(response => {
      const res = response.data
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}
