/**
 * 合同中心通用工具：金额大写、期限文案、剩余天数、状态与分类徽标、编号规则
 */

export const CONTRACT_STATE = {
  DRAFT: '10',
  AUDIT: '11',
  AUDIT_LEADER: '56',
  EFFECTIVE: '22',
  FAIL: '33',
  STOPPED: '44'
}

/**
 * 审批单据类型：1 合同 / 2 变更单
 */
export const AUDIT_BIZ_TYPE = {
  CONTRACT: '1',
  CHANGE: '2'
}

/**
 * 审批结果：1 通过 / 2 驳回
 */
export const AUDIT_RESULT = {
  PASS: '1',
  REJECT: '2'
}

/**
 * 审核按钮权限码：法务校对 / 领导审批
 */
export const AUDIT_PRIVILEGE = {
  LEGAL: '502026111600000009',
  LEADER: '502026111600000010'
}

export const CONTRACT_CATEGORY = {
  RENT: '1',
  PURCHASE: '2',
  MAINTAIN: '3'
}

/**
 * 标的类型：房屋 building_room / 商铺 building_shop，两套档案表 ID 互不通用
 */
export const PAYER_OBJ_TYPE = {
  ROOM: '3333',
  SHOP: '4444'
}

export const PAY_STATE = {
  DOING: '0',
  PAID: '1',
  OVERDUE: '2'
}

export const AUDIT_STATE = {
  DOING: '0',
  PASS: '1',
  REJECT: '2',
  CANCEL: '3'
}

export const AUDIT_STEP = {
  LEGAL: '1',
  LEADER: '2'
}

/**
 * 状态徽标：返回 { type, key }，key 为 i18n 键
 */
export function stateTagOf(state) {
  const value = String(state === null || state === undefined ? '' : state)
  if (value === CONTRACT_STATE.DRAFT) {
    return { type: 'info', key: 'contractState.draft' }
  }
  if (value === CONTRACT_STATE.AUDIT) {
    return { type: 'warning', key: 'contractState.auditing' }
  }
  if (value === CONTRACT_STATE.AUDIT_LEADER) {
    return { type: 'warning', key: 'contractState.leaderAudit' }
  }
  if (value === CONTRACT_STATE.EFFECTIVE) {
    return { type: 'success', key: 'contractState.effective' }
  }
  if (value === CONTRACT_STATE.FAIL) {
    return { type: 'danger', key: 'contractState.fail' }
  }
  if (value === CONTRACT_STATE.STOPPED) {
    return { type: 'info', key: 'contractState.stopped' }
  }
  return { type: 'info', key: '' }
}

/**
 * 单据当前所处审核环节对应文案（state=11 法务校对 / 56 领导审批 / 22 完成）
 */
export function auditStepNameOf(state) {
  const value = String(state === null || state === undefined ? '' : state)
  if (value === CONTRACT_STATE.AUDIT) {
    return 'contractState.auditing'
  }
  if (value === CONTRACT_STATE.AUDIT_LEADER) {
    return 'contractState.leaderAudit'
  }
  if (value === CONTRACT_STATE.EFFECTIVE) {
    return 'contractState.done'
  }
  if (value === CONTRACT_STATE.FAIL) {
    return 'contractState.fail'
  }
  return ''
}

/**
 * 合同分类徽标：返回 { type, key }
 */
export function categoryTagOf(category) {
  const value = String(category === null || category === undefined ? '2' : category)
  if (value === CONTRACT_CATEGORY.RENT) {
    return { type: 'primary', key: 'contractCategory.rent' }
  }
  if (value === CONTRACT_CATEGORY.MAINTAIN) {
    return { type: 'warning', key: 'contractCategory.maintain' }
  }
  return { type: 'success', key: 'contractCategory.purchase' }
}

/**
 * 合同分类取值：由模板 category 决定，缺省为采购
 */
export function guessCategory(contract) {
  if (!contract || !contract.category) {
    return CONTRACT_CATEGORY.PURCHASE
  }
  return String(contract.category)
}

export function toNumber(value) {
  if (value === null || value === undefined || value === '') {
    return 0
  }
  const number = parseFloat(value)
  return isNaN(number) ? 0 : number
}

export function formatAmount(value) {
  return toNumber(value).toFixed(2)
}

/**
 * 合同期限：起止日期 + 月数
 */
export function formatTerm(startTime, endTime) {
  if (!startTime && !endTime) {
    return '-'
  }
  const months = termMonths(startTime, endTime)
  const term = months > 0 ? `（${months}个月）` : ''
  return `${(startTime || '-').substring(0, 10)} ~ ${(endTime || '-').substring(0, 10)}${term}`
}

/**
 * 合同期限月数
 */
export function termMonths(startTime, endTime) {
  const start = parseDate(startTime)
  const end = parseDate(endTime)
  if (!start || !end || end.getTime() <= start.getTime()) {
    return 0
  }
  let months = (end.getFullYear() - start.getFullYear()) * 12 + (end.getMonth() - start.getMonth())
  if (end.getDate() >= start.getDate()) {
    months += 1
  }
  return months < 1 ? 1 : months
}

/**
 * 剩余天数（小于 0 表示已过期）
 */
export function daysLeft(endTime) {
  const end = parseDate(endTime)
  if (!end) {
    return null
  }
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const endDay = new Date(end.getFullYear(), end.getMonth(), end.getDate())
  return Math.round((endDay.getTime() - today.getTime()) / (24 * 3600 * 1000))
}

/**
 * 剩余天数徽标颜色
 */
export function daysLeftType(days) {
  if (days === null || days === undefined) {
    return 'info'
  }
  if (days < 0) {
    return 'danger'
  }
  if (days <= 7) {
    return 'danger'
  }
  if (days <= 30) {
    return 'warning'
  }
  return 'info'
}

/**
 * 编号规则 HC-{yyyy}-{ZL|CG|WB}-{4位序号}
 */
export function buildContractCode(category, seq, year) {
  const prefixMap = {
    [CONTRACT_CATEGORY.RENT]: 'ZL',
    [CONTRACT_CATEGORY.PURCHASE]: 'CG',
    [CONTRACT_CATEGORY.MAINTAIN]: 'WB'
  }
  const prefix = prefixMap[String(category)] || 'CG'
  const currentYear = year || new Date().getFullYear()
  const serial = String(toNumber(seq) + 1).padStart(4, '0')
  return `HC-${currentYear}-${prefix}-${serial}`
}

/**
 * 金额大写
 */
export function amountToChinese(value) {
  const amount = toNumber(value)
  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const intRadices = ['', '拾', '佰', '仟']
  const intUnits = ['', '万', '亿', '兆']
  const decUnits = ['角', '分']

  const integerNum = Math.floor(Math.abs(amount))
  const decimalNum = Math.round((Math.abs(amount) - integerNum) * 100)
  if (integerNum === 0 && decimalNum === 0) {
    return '零元整'
  }

  let chineseStr = ''
  const integerStr = String(integerNum)
  const intLength = integerStr.length
  let zeroCount = 0
  for (let i = 0; i < intLength; i++) {
    const number = integerStr.substr(i, 1)
    const position = intLength - i - 1
    const unitIndex = Math.floor(position / 4)
    const radixIndex = position % 4
    if (number === '0') {
      zeroCount++
    } else {
      if (zeroCount > 0) {
        chineseStr += digits[0]
      }
      zeroCount = 0
      chineseStr += digits[parseInt(number)] + intRadices[radixIndex]
    }
    if (radixIndex === 0 && zeroCount < 4) {
      chineseStr += intUnits[unitIndex]
    }
  }
  chineseStr += '元'

  const decimalStr = String(decimalNum).padStart(2, '0')
  for (let i = 0; i < decimalStr.length; i++) {
    const number = decimalStr.substr(i, 1)
    if (number !== '0') {
      chineseStr += digits[parseInt(number)] + decUnits[i]
    }
  }
  if (decimalNum === 0) {
    chineseStr += '整'
  }
  return (amount < 0 ? '负' : '') + chineseStr
}

/**
 * 电话格式校验
 */
export function isTel(tel) {
  return /^1[3-9]\d{9}$|^0\d{2,3}-?\d{7,8}$/.test(String(tel || ''))
}

/**
 * 统一社会信用代码校验（18 位字母数字）
 */
export function isCreditCode(code) {
  return /^[0-9A-HJ-NPQRTUWXY]{18}$/.test(String(code || ''))
}

export function parseDate(value) {
  if (!value) {
    return null
  }
  if (value instanceof Date) {
    return value
  }
  const text = String(value).replace(/-/g, '/')
  const date = new Date(text)
  return isNaN(date.getTime()) ? null : date
}

/**
 * 日期加减月份，返回 yyyy-MM-dd
 */
export function addMonths(value, months) {
  const date = parseDate(value)
  if (!date) {
    return ''
  }
  const target = new Date(date.getFullYear(), date.getMonth() + months, date.getDate())
  return formatDate(target)
}

export function formatDate(date) {
  if (!date) {
    return ''
  }
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

/**
 * 保存后端返回的 base64 文件（导出 Excel 复用）
 */
export function saveBase64File(base64, fileName) {
  if (!base64) {
    return
  }
  const binary = window.atob(base64)
  const length = binary.length
  const bytes = new Uint8Array(length)
  for (let index = 0; index < length; index++) {
    bytes[index] = binary.charCodeAt(index)
  }
  const blob = new Blob([bytes], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName || 'export.xlsx'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

/**
 * 标的类型判定：缺省按房屋处理
 */
export function payerObjTypeOf(row) {
  const value = String((row && row.payerObjType) || '')
  return value === PAYER_OBJ_TYPE.SHOP ? PAYER_OBJ_TYPE.SHOP : PAYER_OBJ_TYPE.ROOM
}

/**
 * 明细金额映射：objId -> 金额（租金小计、逐标的核对）
 */
export function detailAmountMap(data) {
  const result = data && data.data ? data.data : data
  const map = {}
  const details = (result && result.details) || []
  details.forEach(detail => {
    map[detail.objId] = formatAmount(detail.amount)
  })
  return map
}

/**
 * 计划行提交归一：planTime 空串不传；planAmount / ratio 空串转默认值
 */
export function normalizePlanForSubmit(row) {
  const source = row || {}
  const plan = { ...source }
  plan.planAmount = formatAmount(source.planAmount)
  plan.ratio = isEmptyValue(source.ratio) ? '0.00' : String(source.ratio).trim()
  plan.planTime = isEmptyValue(source.planTime) ? '' : source.planTime
  return plan
}

function isEmptyValue(value) {
  return value === null || value === undefined || String(value).trim() === ''
}

/**
 * 正文变量渲染：支持 {{key}} 与 ${key}，只替换 variables 中存在的变量
 */
export function renderContractContent(content, variables) {
  if (!content || !variables) {
    return content || ''
  }
  return String(content).replace(/\{\{\s*([a-zA-Z0-9_]+)\s*\}\}|\$\{\s*([a-zA-Z0-9_]+)\s*\}/g, function (match, braceKey, dollarKey) {
    const key = braceKey || dollarKey
    if (!Object.prototype.hasOwnProperty.call(variables, key)) {
      return match
    }
    const value = variables[key]
    return value === null || value === undefined ? '' : String(value)
  })
}

/**
 * 正文 HTML 白名单清洗（仅保留安全标签与属性）
 */
export function sanitizeHtml(html) {
  if (!html) {
    return ''
  }
  const allowedTags = ['p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'h1', 'h2', 'h3', 'h4', 'h5',
    'ul', 'ol', 'li', 'table', 'thead', 'tbody', 'tr', 'td', 'th', 'span', 'div', 'a', 'sub', 'sup', 'img', 'blockquote']
  const container = document.createElement('div')
  container.innerHTML = String(html)
  cleanNode(container, allowedTags)
  return container.innerHTML
}

function cleanNode(node, allowedTags) {
  const children = Array.prototype.slice.call(node.children || [])
  children.forEach(child => {
    const tagName = (child.tagName || '').toLowerCase()
    if (allowedTags.indexOf(tagName) < 0) {
      const text = document.createTextNode(child.textContent || '')
      node.replaceChild(text, child)
      return
    }
    const attributes = Array.prototype.slice.call(child.attributes || [])
    attributes.forEach(attr => {
      const name = attr.name.toLowerCase()
      const value = String(attr.value || '')
      if (name.indexOf('on') === 0) {
        child.removeAttribute(attr.name)
        return
      }
      if (name === 'href' && value.toLowerCase().indexOf('javascript:') === 0) {
        child.removeAttribute(attr.name)
      }
      if (['style', 'class', 'src', 'href', 'colspan', 'rowspan', 'alt', 'title'].indexOf(name) < 0) {
        child.removeAttribute(attr.name)
      }
    })
    cleanNode(child, allowedTags)
  })
}
