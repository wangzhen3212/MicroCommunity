import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

// 合同金额试算：金额统一由后端公式模块计算，只读不落库（仅用于标的租金合计）
export function computeContractFeeAmount(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/fee.computeContractFeeAmount',
      method: 'get',
      params: {
        ...params,
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

// 标的租金合计试算（mode=rent，走租金公式汇总）
export function computeContractRentAmount(params) {
  return computeContractFeeAmount({
    ...params,
    mode: 'rent'
  })
}
