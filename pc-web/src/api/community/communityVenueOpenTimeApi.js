import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

// 获取场馆列表
export function listCommunityVenue(params) {
  return new Promise((resolve, reject) => {
    params.communityId = getCommunityId()
    request({
      url: '/communityVenue.listCommunityVenue',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      resolve(res.data)
    }).catch(error => {
      reject(error)
    })
  })
}

// 获取场馆开放时段配置
export function listCommunitySpaceOpenTime(params) {
  return new Promise((resolve, reject) => {
    params.communityId = getCommunityId()
    request({
      url: '/communitySpace.listCommunitySpaceOpenTime',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      resolve(res.data)
    }).catch(error => {
      reject(error)
    })
  })
}

// 批量保存场馆开放时段（按场馆 + 星期重建）
export function saveCommunitySpaceOpenTimeBatch(data) {
  return new Promise((resolve, reject) => {
    data.communityId = getCommunityId()
    request({
      url: '/communitySpaceOpenTime.saveCommunitySpaceOpenTimeBatch',
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
