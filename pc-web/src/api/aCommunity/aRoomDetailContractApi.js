import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

export function queryAdminOwnerContract(params) {
  return request({
    url: '/contract.queryAdminOwnerContract',
    method: 'get',
    params: {
      ...params,
      communityId: getCommunityId()
    }
  }).then(res => {
    return res.data
  })
}