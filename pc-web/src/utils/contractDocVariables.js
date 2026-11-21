import { amountToChinese, termMonths } from '@/utils/contractUtil'

export const CONTRACT_DOC_VARIABLE_GROUPS = [
  { key: 'base', labelKey: 'contractWorkbench.doc.varGroupBase' },
  { key: 'party', labelKey: 'contractWorkbench.doc.varGroupParty' },
  { key: 'termAmount', labelKey: 'contractWorkbench.doc.varGroupTermAmount' },
  { key: 'target', labelKey: 'contractWorkbench.doc.varGroupTarget' },
  { key: 'other', labelKey: 'contractWorkbench.doc.varGroupOther' }
]

export const CONTRACT_DOC_VARIABLES = [
  { key: 'contractName', group: 'base', labelKey: 'contractWorkbench.doc.varContractName' },
  { key: 'contractCode', group: 'base', labelKey: 'contractWorkbench.doc.varContractCode' },
  { key: 'signingTime', group: 'base', labelKey: 'contractWorkbench.doc.varSigningTime' },
  { key: 'partyA', group: 'party', labelKey: 'contractWorkbench.doc.varPartyA' },
  { key: 'aContacts', group: 'party', labelKey: 'contractWorkbench.doc.varAContacts' },
  { key: 'aLink', group: 'party', labelKey: 'contractWorkbench.doc.varALink' },
  { key: 'partyB', group: 'party', labelKey: 'contractWorkbench.doc.varPartyB' },
  { key: 'bContacts', group: 'party', labelKey: 'contractWorkbench.doc.varBContacts' },
  { key: 'bLink', group: 'party', labelKey: 'contractWorkbench.doc.varBLink' },
  { key: 'creditCode', group: 'party', labelKey: 'contractWorkbench.doc.varCreditCode' },
  { key: 'amount', group: 'termAmount', labelKey: 'contractWorkbench.doc.varAmount' },
  { key: 'amountText', group: 'termAmount', labelKey: 'contractWorkbench.doc.varAmountText' },
  { key: 'startTime', group: 'termAmount', labelKey: 'contractWorkbench.doc.varStart' },
  { key: 'endTime', group: 'termAmount', labelKey: 'contractWorkbench.doc.varEnd' },
  { key: 'term', group: 'termAmount', labelKey: 'contractWorkbench.doc.varTerm' },
  { key: 'payMode', group: 'termAmount', labelKey: 'contractWorkbench.doc.varPayMode' },
  { key: 'houses', group: 'target', labelKey: 'contractWorkbench.doc.varHouses' },
  { key: 'targets', group: 'target', labelKey: 'contractWorkbench.doc.varTargets' },
  { key: 'plans', group: 'target', labelKey: 'contractWorkbench.doc.varPlans' },
  { key: 'place', group: 'other', labelKey: 'contractWorkbench.doc.varPlace' },
  { key: 'operator', group: 'other', labelKey: 'contractWorkbench.doc.varOperator' },
  { key: 'operatorLink', group: 'other', labelKey: 'contractWorkbench.doc.varOperatorLink' },
  { key: 'remark', group: 'other', labelKey: 'contractWorkbench.doc.varRemark' }
]

function textOf(value) {
  if (value === null || value === undefined) {
    return ''
  }
  return String(value).trim()
}

function joinList(list, mapper) {
  return (list || [])
    .map(item => textOf(mapper(item)))
    .filter(item => !!item)
    .join('、')
}

function defaultRoomNameOf(room) {
  const row = room || {}
  if (row.objName) {
    return row.objName
  }
  return row.roomName || row.shopNum || row.locationName || ''
}

function targetTextOf(target) {
  const row = target || {}
  const num = textOf(row.num)
  const parts = [textOf(row.name), textOf(row.spec)]
  if (num) {
    parts.push('×' + num)
  }
  return parts.filter(part => !!part).join(' ')
}

function planTextOf(plan) {
  const row = plan || {}
  const planTime = textOf(row.planTime).substring(0, 10)
  const planAmount = textOf(row.planAmount)
  if (!planTime && !planAmount) {
    return ''
  }
  return (planTime + ' ' + planAmount).trim()
}

export function buildVariableGroups(t) {
  const translate = typeof t === 'function' ? t : key => key
  return CONTRACT_DOC_VARIABLE_GROUPS.map(group => ({
    key: group.key,
    label: translate(group.labelKey),
    items: CONTRACT_DOC_VARIABLES
      .filter(item => item.group === group.key)
      .map(item => ({ key: item.key, label: translate(item.labelKey) }))
  }))
}

export function buildContractDocVariables(option) {
  const opt = option || {}
  const form = opt.form || {}
  const roomNameOf = typeof opt.roomNameOf === 'function' ? opt.roomNameOf : defaultRoomNameOf
  const months = termMonths(form.startTime, form.endTime)
  return {
    contractName: textOf(form.contractName),
    contractCode: textOf(form.contractCode),
    partyA: textOf(form.partyA),
    aContacts: textOf(form.aContacts),
    aLink: textOf(form.aLink),
    partyB: textOf(form.partyB),
    bContacts: textOf(form.bContacts),
    bLink: textOf(form.bLink),
    creditCode: textOf(form.creditCode),
    amount: textOf(form.amount),
    amountText: textOf(form.amount) ? amountToChinese(form.amount) : '',
    signingTime: textOf(form.signingTime),
    startTime: textOf(form.startTime),
    endTime: textOf(form.endTime),
    term: months > 0 ? months + '个月' : '',
    payMode: textOf(form.payMode),
    houses: joinList(opt.rooms, roomNameOf),
    targets: joinList(opt.targets, targetTextOf),
    plans: joinList(opt.plans, planTextOf),
    place: textOf(form.place),
    operator: textOf(form.operator),
    operatorLink: textOf(form.operatorLink),
    remark: textOf(form.remark)
  }
}
