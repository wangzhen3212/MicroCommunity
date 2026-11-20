import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

// 查询工作流审核信息
export function listWorkflowAuditInfo(params) {
  return new Promise((resolve, reject) => {
    request({
      url: 'workflow.listWorkflowAuditInfo',
      method: 'get',
      params: {
        ...params,
        communityId: getCommunityId()
      }
    }).then(response => {
const res = response.data
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}