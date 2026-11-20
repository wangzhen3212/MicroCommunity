import request from '@/utils/request'

// 已办列表改读自建两级审批接口，旧工作流历史任务接口仅作兜底
export {
  auditContract,
  queryContractAuditFlow
} from '@/api/contract/contractAuditApi'


/**
 * 查询合同历史任务列表
 * @param {Object} params 查询参数
 * @param {number} params.page 当前页码
 * @param {number} params.row 每页显示条数
 * @param {string} [params.AuditOrdersId] 审核订单ID
 * @param {string} [params.userName] 用户名
 * @param {string} [params.auditLink] 审核环节
 * @returns {Promise} 包含合同历史任务数据的Promise对象
 */
export function queryContractHistoryTask(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract/queryContractHistoryTask',
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

/**
 * 获取合同详情
 * @param {string} contractId 合同ID
 * @returns {Promise} 包含合同详情的Promise对象
 */
export function getContractDetail(contractId) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract/getContractDetail',
      method: 'get',
      params: { contractId }
    }).then(response => {
      const res = response.data
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}