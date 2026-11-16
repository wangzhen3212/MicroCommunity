import request from '@/utils/request'

// 到期提醒清单（剩余天数升序，带提醒记录）
export function listContractReminds(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.listContractReminds',
      method: 'get',
      params
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

// 批量推送到期提醒（站内信 + 短信）
export function pushContractRemind(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.pushContractRemind',
      method: 'post',
      data
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
