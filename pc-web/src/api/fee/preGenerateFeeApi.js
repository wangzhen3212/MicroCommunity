import request from '@/utils/request'

export function queryPreGenerateFeePeriods(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/fee.queryPreGenerateFeePeriods',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      if (res.code !== 0) {
        reject(new Error(res.msg))
        return
      }
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}

export function preGenerateFee(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/fee.preGenerateFee',
      method: 'post',
      data
    }).then(response => {
      const res = response.data
      if (res.code !== 0) {
        reject(new Error(res.msg))
        return
      }
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}
