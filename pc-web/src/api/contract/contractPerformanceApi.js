import request from '@/utils/request'

// 履约汇总列表（已付/待付/逾期/进度/下一节点，sumTotal 为汇总卡）
export function listContractPerformances(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.listContractPerformances',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve({
          data: res.data || [],
          total: res.total || 0,
          sumTotal: res.sumTotal || {}
        })
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 单合同收款计划期次明细
export function listContractCollectionPlans(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.listContractCollectionPlans',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res.data || { plans: [], statistics: {} })
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 登记 / 撤销付款
export function saveContractPayment(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.saveContractPayment',
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
