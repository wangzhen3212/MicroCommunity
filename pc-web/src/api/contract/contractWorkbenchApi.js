import request from '@/utils/request'

// 生成合同编号（HC-{yyyy}-{ZL|CG|WB}-{4位序号}）
export function generateContractCode(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.generateContractCode',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res.data || {})
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 保存合同草稿（state=10）
export function saveContractDraft(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.saveContractDraft',
      method: 'post',
      data
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res.data || {})
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 正文变量渲染（模板 + 表单变量 + 房屋清单）
export function previewContractContent(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.previewContractContent',
      method: 'post',
      data
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res.data || {})
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 按合同查询正文
export function queryContractContent(params) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.queryContractContent',
      method: 'get',
      params
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res.data || {})
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}

// 保存合同（正式提交，可带 state=10 保存草稿）
export function saveContract(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.saveContract',
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

// 修改合同
export function updateContract(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/contract.updateContract',
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

// 上传合同附件
export function uploadContactFile(data) {
  return new Promise((resolve, reject) => {
    request({
      url: '/uploadContactFile',
      method: 'post',
      data,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }).then(response => {
      const res = response.data
      if (res.code === 0) {
        resolve(res.data || {})
      } else {
        reject(new Error(res.msg || 'error'))
      }
    }).catch(error => {
      reject(error)
    })
  })
}
