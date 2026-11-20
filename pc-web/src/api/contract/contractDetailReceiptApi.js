import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

export function queryFeeReceipt(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/feeReceipt/queryFeeReceipt',
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

export function listFeePrintPage(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/feePrintPage.listFeePrintPage',
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