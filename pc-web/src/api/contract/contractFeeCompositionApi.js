import request from '@/utils/request'
import { getCommunityId, getDict } from '@/api/community/communityApi'
import { filterFeeConfigsByFormula } from '@/utils/feeUtil'

// 费用类型字典（pay_fee_config.fee_type_cd）
export function listFeeTypes() {
  return new Promise((resolve, reject) => {
    getDict('pay_fee_config', 'fee_type_cd').then(data => {
      resolve(data || [])
    }).catch(error => {
      reject(error)
    })
  })
}

// 按费用类型查询可用费用项（剔除抄表类公式）
export function listFeeConfigsByType(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/feeConfig.listFeeConfigs',
      method: 'get',
      params: {
        ...params,
        communityId: getCommunityId(),
        isDefault: 'F',
        valid: '1',
        page: 1,
        row: 500
      }
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(filterFeeConfigsByFormula(res.feeConfigs || []))
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}
