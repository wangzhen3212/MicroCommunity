import request from '@/utils/request'
import { getCommunityId } from '@/api/community/communityApi'

// 合同模板卡片列表
export function listContractTemplates(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.listContractTemplates',
      method: 'get',
      params: {
        ...params,
        communityId: getCommunityId()
      }
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve({
          data: res.data || [],
          total: res.total || 0
        })
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 更新合同模板（名称/分类/版本/描述/正文）
export function updateContractTemplate(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.updateContractTemplate',
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

// 查询合同模板正文（编辑/预览）
export function queryContractTypeTemplate(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract/queryContractTypeTemplate',
      method: 'get',
      params: {
        ...params,
        communityId: getCommunityId()
      }
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res.data || [])
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 保存合同模板
export function saveContractTypeTemplate(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract/saveContractTypeTemplate',
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

// 删除合同模板
export function deleteContractTypeTemplate(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract/deleteContractTypeTemplate',
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

// 打印合同模板
export function printContractTemplate(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract/printContractTemplate',
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
