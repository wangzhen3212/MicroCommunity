import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

// 查询合同列表
export function getContractList(params) {
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
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}

// 更新合同信息

export function updateContract(data) {
  return new Promise((resolve, reject) => {
request({
      url: '/contract.updateContract',
      method: 'post',
      data: {
        ...data,
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

// 删除合同
export function deleteContract(data) {
  return new Promise((resolve, reject) => {
request({
      url: '/contract.deleteContract',
      method: 'post',
      data: {
        ...data,
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

// 上传合同文件
export function uploadContractFile(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contractFile/uploadContactFile',
      method: 'post',
      data,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }).then(response => {
      const res = response.data
      resolve(res)
    }).catch(error => {
      reject(error)
    })
  })
}