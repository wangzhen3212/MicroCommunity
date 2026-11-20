import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

export function queryContract(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract/queryContract',
      method: 'get',
      params: {
        ...params,
        communityId: getCommunityId()
      }
    }).then(response => {
      const res = response.data
      if (res.code == 0) {
        resolve(res)
      } else {
        reject(new Error(res.msg || '获取业主合同列表失败'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}