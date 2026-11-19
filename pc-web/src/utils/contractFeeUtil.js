/**
 * 合同费用构成计算：公式映射、逐行计算、合计汇总
 * 口径：月数 = termMonths(生效日期, 到期日期)；周期数 = ceil(月数 / 周期月数)
 */
import { formatAmount, termMonths, toNumber } from '@/utils/contractUtil'

// 水费 / 电费：按实结算，默认不计入合同金额
export const FEE_TYPE_WATER = '888800010015'
export const FEE_TYPE_ELECTRIC = '888800010016'

export const EXCLUDE_FEE_TYPE_CDS = [FEE_TYPE_WATER, FEE_TYPE_ELECTRIC]

export const FEE_FORMULA = {
  RENT: '1101',
  RENT_STEP: '1102',
  BUILT_UP: '1001',
  BUILT_UP_RATIO: '1002',
  ROOM_AREA: '3003',
  FIXED: '2002',
  DYNAMIC: '4004',
  CUSTOM: '7007',
  METER: '5005',
  METER_USE: '6006',
  METER_DYNAMIC: '9009'
}

// 结算基数取数口径
const BASE_TYPE = {
  '1101': 'rent',
  '1102': 'rent',
  '1001': 'builtUpArea',
  '1002': 'builtUpArea',
  '3003': 'roomArea'
}

// 金额需手工录入的计算方式（含抄表类，按实结算）
const MANUAL_FORMULAS = ['4004', '7007', '5005', '6006', '9009']

// 使用「单价」参与计算的方式：面积类与固定费用
const UNIT_PRICE_FORMULAS = ['1001', '1002', '3003', '2002']

// 使用「附加费」的方式
const ADDITIONAL_FORMULAS = ['1001', '3003']

export function isManualFormula(computingFormula) {
  return MANUAL_FORMULAS.indexOf(String(computingFormula)) >= 0
}

export function isUnitPriceFormula(computingFormula) {
  return UNIT_PRICE_FORMULAS.indexOf(String(computingFormula)) >= 0
}

export function isAdditionalFormula(computingFormula) {
  return ADDITIONAL_FORMULAS.indexOf(String(computingFormula)) >= 0
}

export function defaultIncludeInAmount(feeTypeCd) {
  return EXCLUDE_FEE_TYPE_CDS.indexOf(String(feeTypeCd)) === -1
}

export function termMonthsOf(context) {
  const ctx = context || {}
  return termMonths(ctx.startTime, ctx.endTime)
}

export function cycleCount(months, cycle) {
  const cycleMonths = Math.max(1, Math.round(toNumber(cycle)))
  const totalMonths = months > 0 ? months : 1
  return Math.max(1, Math.ceil(totalMonths / cycleMonths))
}

/**
 * 结算基数：房源租金合计 / 建筑面积合计 / 室内面积合计
 */
export function calcFeeBase(row, context) {
  const rooms = (context && context.rooms) || []
  const baseType = BASE_TYPE[String(row && row.computingFormula)]
  if (!baseType) {
    return 0
  }
  return rooms.reduce((sum, room) => {
    if (baseType === 'rent') {
      return sum + toNumber(room.rentUnitPrice || room.roomRent)
    }
    if (baseType === 'roomArea') {
      return sum + toNumber(room.roomArea)
    }
    return sum + toNumber(room.builtUpArea)
  }, 0)
}

/**
 * 单行金额：手工口径取 manualAmount，其余按公式自动计算
 */
export function calcFeeAmount(row, context) {
  if (!row) {
    return 0
  }
  const formula = String(row.computingFormula)
  if (isManualFormula(formula)) {
    return toNumber(row.manualAmount)
  }
  const months = termMonthsOf(context)
  const base = calcFeeBase(row, context)
  const unitPrice = toNumber(row.unitPrice)
  const additionalAmount = toNumber(row.additionalAmount)
  if (formula === FEE_FORMULA.RENT || formula === FEE_FORMULA.RENT_STEP) {
    return base * cycleCount(months, row.cycle)
  }
  if (formula === FEE_FORMULA.BUILT_UP) {
    return base * unitPrice * months + additionalAmount
  }
  if (formula === FEE_FORMULA.BUILT_UP_RATIO) {
    const coefficient = toNumber(row.coefficient) || 1
    return base * unitPrice * coefficient * months
  }
  if (formula === FEE_FORMULA.ROOM_AREA) {
    return base * unitPrice * months + additionalAmount
  }
  if (formula === FEE_FORMULA.FIXED) {
    return unitPrice
  }
  return toNumber(row.manualAmount)
}

/**
 * 重算费用行（原地刷新 baseAmount / amount），返回同一数组
 */
export function recalcFees(fees, context) {
  const list = fees || []
  list.forEach(row => {
    row.baseAmount = formatAmount(calcFeeBase(row, context))
    row.amount = formatAmount(calcFeeAmount(row, context))
  })
  return list
}

export function sumFeeTotal(fees) {
  return (fees || []).reduce((sum, row) => sum + toNumber(row.amount), 0)
}

export function sumIncludedAmount(fees) {
  return (fees || [])
    .filter(row => !!row.includeInAmount)
    .reduce((sum, row) => sum + toNumber(row.amount), 0)
}
