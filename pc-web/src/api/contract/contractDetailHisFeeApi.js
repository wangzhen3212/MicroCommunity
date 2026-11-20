import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

export function queryFeeDetail(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/fee.queryFeeDetail',
      method: 'get',
      params: {
        ...params,
        communityId: getCommunityId()
      }
    }).then(response => {
      resolve(response.data)
    }).catch(error => {
      reject(error)
    })
  })
}