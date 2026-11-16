// 需要抄表读数（meter_water）才能计费的公式，创建费用弹窗中不展示对应费用项
// 5005 (本期度数-上期度数)*单价+附加费
// 6006 用量*单价+附加费
// 9009 (本期度数-上期度数)*动态单价+附加费
export const EXCLUDE_COMPUTING_FORMULAS = ['5005', '6006', '9009']

export function filterFeeConfigsByFormula(feeConfigs) {
  const list = feeConfigs || []
  return list.filter(item => EXCLUDE_COMPUTING_FORMULAS.indexOf(item.computingFormula) === -1)
}
