import request from '@/utils/request'

// 查询房屋计费设置与账单（计费设置 + 未缴清/已取消账单 + 汇总）
export function listRoomFeeRuleWithBill(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/payFeeRule.listRoomFeeRuleWithBill',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      if (res.code !== 0) {
        reject(new Error(res.msg || 'error'))
        return
      }
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}
