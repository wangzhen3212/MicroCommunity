/**
 * 合同收款计划工具：付款方式模板、按期次生成计划行、比例/金额互算、按期限重排
 */
import { addMonths, formatAmount, termMonths, toNumber } from '@/utils/contractUtil'

export const PAY_MODES = [
  { code: 'once', labelKey: 'contractWorkbench.modeOnce', ratios: [1] },
  { code: 'monthly', labelKey: 'contractWorkbench.modeMonthly', months: 1 },
  { code: 'quarterly', labelKey: 'contractWorkbench.modeQuarterly', months: 3 },
  { code: 'halfYear', labelKey: 'contractWorkbench.modeHalfYear', months: 6 },
  { code: 'yearly', labelKey: 'contractWorkbench.modeYearly', months: 12 },
  { code: 'r3040255', labelKey: 'contractWorkbench.mode3040255', ratios: [0.3, 0.4, 0.25, 0.05] },
  { code: 'r304030', labelKey: 'contractWorkbench.mode304030', ratios: [0.3, 0.4, 0.3] },
  { code: 'r5050', labelKey: 'contractWorkbench.mode5050', ratios: [0.5, 0.5] }
]

/**
 * 按付款方式模板生成期次
 *
 * @param {String} modeCode 付款方式编码
 * @param {Object} options { startTime, endTime, amount, planName }
 * @returns {Array} 期次列表
 */
export function buildPlansByMode(modeCode, options) {
  const mode = PAY_MODES.filter(item => item.code === modeCode)[0]
  const startTime = options.startTime
  const endTime = options.endTime
  const amount = toNumber(options.amount)
  const planName = options.planName || ''
  const plans = []
  if (!mode) {
    return plans
  }

  if (mode.ratios) {
    const totalRatio = mode.ratios.reduce((sum, item) => sum + item, 0)
    mode.ratios.forEach((ratio, index) => {
      plans.push({
        planSeq: index + 1,
        planName: planName ? `${planName}-${index + 1}期` : `${index + 1}期`,
        planTime: index === mode.ratios.length - 1 ? endTime : addMonths(startTime, index),
        ratio: ((ratio / totalRatio) * 100).toFixed(2),
        planAmount: formatAmount((amount * ratio) / totalRatio),
        payState: '0',
        triggerDesc: '',
        feeType: 'other'
      })
    })
    return normalizeAmount(plans, amount)
  }

  const months = termMonths(startTime, endTime)
  if (months < 1) {
    return plans
  }
  const count = Math.ceil(months / mode.months)
  for (let index = 0; index < count; index++) {
    plans.push({
      planSeq: index + 1,
      planName: planName ? `${planName}-${index + 1}期` : `${index + 1}期`,
      planTime: index === count - 1 ? endTime : addMonths(startTime, index * mode.months),
      ratio: (100 / count).toFixed(2),
      planAmount: formatAmount(amount / count),
      payState: '0',
      triggerDesc: '',
      feeType: 'other'
    })
  }
  return normalizeAmount(plans, amount)
}

/**
 * 金额合计对齐合同金额（差额补到最后一期）
 */
export function normalizeAmount(plans, amount) {
  if (!plans.length) {
    return plans
  }
  let total = 0
  plans.forEach(plan => {
    plan.planAmount = formatAmount(plan.planAmount)
    total += toNumber(plan.planAmount)
  })
  const diff = toNumber(amount) - total
  if (Math.abs(diff) > 0.001) {
    const last = plans[plans.length - 1]
    last.planAmount = formatAmount(toNumber(last.planAmount) + diff)
  }
  return plans
}

/**
 * 按金额均分
 */
export function divideAmount(plans, amount) {
  if (!plans.length) {
    return plans
  }
  const average = toNumber(amount) / plans.length
  plans.forEach(plan => {
    plan.planAmount = formatAmount(average)
  })
  return normalizeAmount(plans, amount)
}

/**
 * 按期次重排日期（首期取开始日期，末期取结束日期）
 */
export function reorderByTerm(plans, startTime, endTime) {
  if (!plans.length) {
    return plans
  }
  const months = termMonths(startTime, endTime)
  const step = months > 0 ? Math.max(1, Math.floor(months / plans.length)) : 1
  plans.forEach((plan, index) => {
    if (index === plans.length - 1) {
      plan.planTime = endTime
      return
    }
    plan.planTime = addMonths(startTime, index * step)
  })
  return plans
}

export function sumAmount(plans) {
  return plans.reduce((sum, plan) => sum + toNumber(plan.planAmount), 0)
}

export function sumRatio(plans) {
  return plans.reduce((sum, plan) => sum + toNumber(plan.ratio), 0)
}

/**
 * 比例合计校验（允许 0.01 误差）
 */
export function ratioValid(plans) {
  if (!plans.length) {
    return true
  }
  return Math.abs(sumRatio(plans) - 100) < 0.01
}

/**
 * 金额合计校验（允许 0.01 误差）
 */
export function amountValid(plans, amount) {
  if (!plans.length) {
    return true
  }
  return Math.abs(sumAmount(plans) - toNumber(amount)) < 0.01
}

export function createEmptyPlan(seq) {
  return {
    planSeq: seq,
    planName: `${seq}期`,
    planTime: '',
    ratio: '0.00',
    planAmount: '0.00',
    payState: '0',
    triggerDesc: '',
    feeType: 'other'
  }
}

/**
 * 合同变更类型：1001 主体变更 / 2002 租期调整 / 3003 资产调整
 */
export const PLAN_TYPE = {
  MAIN_BODY: '1001',
  LEASE: '2002',
  ASSETS: '3003'
}

/**
 * 组装变更申请报文（含合同原值，按变更类型覆盖）
 *
 * @param {Object} contract 原合同
 * @param {Object} form 变更表单
 * @returns {Object} 提交报文
 */
export function buildChangePayload(contract, form) {
  const source = contract || {}
  const payload = {
    contractId: source.contractId,
    contractName: source.contractName,
    contractCode: source.contractCode,
    templateId: source.templateId,
    templateName: source.templateName,
    partyA: source.partyA,
    partyB: source.partyB,
    aContacts: source.aContacts,
    bContacts: source.bContacts,
    aLink: source.aLink,
    bLink: source.bLink,
    operator: source.operator,
    operatorLink: source.operatorLink,
    amount: source.amount,
    startTime: source.startTime,
    endTime: source.endTime,
    signingTime: source.signingTime,
    planType: form.planType,
    changeRemark: form.changeRemark,
    effectiveDate: form.effectiveDate
  }

  if (form.planType === PLAN_TYPE.MAIN_BODY) {
    payload.partyA = form.partyA
    payload.aContacts = form.aContacts
    payload.aLink = form.aLink
  }

  if (form.planType === PLAN_TYPE.LEASE) {
    payload.startTime = form.startTime
    payload.endTime = form.endTime
  }

  if (form.planType === PLAN_TYPE.ASSETS) {
    payload.rooms = form.rooms || []
  }

  return payload
}
