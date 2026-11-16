import request from '@/utils/request'

// 提交两级审批（auditType 1 合同 / 2 变更）
export function submitContractAudit(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.submitContractAudit',
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

// 审批通过/驳回
export function auditContract(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.auditContract',
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

// 撤回审批
export function cancelContractAudit(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.cancelContractAudit',
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

// 待我审批
export function listContractAuditTasks(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.listContractAuditTasks',
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

// 已通过 / 已驳回 / 我发起
export function listContractAuditHistory(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.listContractAuditHistory',
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

// 审核汇总卡（待我审批 / 本月已通过 / 本月驳回）
export function listContractAuditStatistics(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.listContractAuditStatistics',
      method: 'get',
      params
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

// 保存审核人配置
export function saveContractAuditorConfig(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.saveContractAuditorConfig',
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

// 查询审核人配置
export function queryContractAuditorConfig(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.queryContractAuditorConfig',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res.data || { legalAuditors: [], leaderAuditors: [], configs: [] })
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 查询单据审批链（详情抽屉 / 工作台右栏 / 审核中心共用）
export function queryContractAuditFlow(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.queryContractAuditFlow',
      method: 'get',
      params
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
