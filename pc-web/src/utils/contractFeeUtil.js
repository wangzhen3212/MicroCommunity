/**
 * 合同费用构成工具：公式类型判定、后端试算结果回填、合计汇总
 * 金额一律由后端 fee.computeContractFeeAmount（统一公式模块）计算，前端不再实现任何金额公式
 */
import { formatAmount, toNumber } from '@/utils/contractUtil'

// 水费 / 电费：按实结算，默认不计入合同金额
export const FEE_TYPE_WATER = '888800010015'
export const FEE_TYPE_ELECTRIC = '888800010016'

export const EXCLUDE_FEE_TYPE_CDS = [FEE_TYPE_WATER, FEE_TYPE_ELECTRIC]

// 标的类型：房屋 building_room / 商铺 building_shop，两套档案表 ID 互不通用
export const PAYER_OBJ_TYPE = {
  ROOM: '3333',
  SHOP: '4444'
}

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

/**
 * 标的类型判定：缺省按房屋处理
 */
export function payerObjTypeOf(row) {
  const value = String((row && row.payerObjType) || '')
  return value === PAYER_OBJ_TYPE.SHOP ? PAYER_OBJ_TYPE.SHOP : PAYER_OBJ_TYPE.ROOM
}

/**
 * 把后端试算结果写回费用行（单价/附加费/期数/结算基数/金额/计算说明）
 */
export function applyServerAmount(row, data) {
  if (!row || !data) {
    return row
  }
  const result = data.data ? data.data : data
  row.unitPrice = result.unitPrice || row.unitPrice || '0.00'
  row.additionalAmount = result.additionalAmount || row.additionalAmount || '0.00'
  row.cycle = result.cycle || row.cycle
  row.months = result.months
  row.baseAmount = formatAmount(result.baseAmount)
  row.amount = formatAmount(result.amount)
  row.computingFormulaName = result.computingFormulaName || row.computingFormulaName
  row.computeRemark = result.remark || ''
  return row
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
 * 手工金额类费用行：金额直接取手工录入值（不属于公式计算）
 */
export function applyManualAmount(row) {
  if (!row) {
    return row
  }
  row.baseAmount = '0.00'
  row.amount = formatAmount(row.manualAmount)
  return row
}

/**
 * 试算入参：标的集合统一取 payerObjType + objId
 */
export function buildComputeParams(objs, months, startTime, endTime) {
  const list = objs || []
  if (!list.length) {
    return null
  }
  return {
    payerObjType: payerObjTypeOf(list[0]),
    payerObjIds: list.map(item => item.objId || item.roomId || item.shopId).join(','),
    months: months,
    startTime: startTime,
    endTime: endTime
  }
}

export function sumFeeTotal(fees) {
  return (fees || []).reduce((sum, row) => sum + toNumber(row.amount), 0)
}

export function sumIncludedAmount(fees) {
  return (fees || [])
    .filter(row => !!row.includeInAmount)
    .reduce((sum, row) => sum + toNumber(row.amount), 0)
}
