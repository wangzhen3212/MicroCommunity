export function naturalCompare(a, b) {
  const sa = a === undefined || a === null ? '' : String(a)
  const sb = b === undefined || b === null ? '' : String(b)
  const na = parseFloat(sa)
  const nb = parseFloat(sb)
  if (!isNaN(na) && !isNaN(nb) && na !== nb) {
    return na - nb
  }
  return sa.localeCompare(sb)
}

export function buildUnitTree(units) {
  const list = units || []
  const floorMap = {}
  const tree = []
  list.forEach(unit => {
    const floorId = unit.floorId || ''
    const floorNum = unit.floorNum || ''
    if (!floorMap[floorId]) {
      floorMap[floorId] = { floorId: floorId, floorNum: floorNum, units: [] }
      tree.push(floorMap[floorId])
    }
    floorMap[floorId].units.push({
      key: unit.unitId || floorId,
      unitId: unit.unitId || '',
      unitNum: unit.unitNum || '0',
      floorId: floorId,
      floorNum: floorNum
    })
  })
  tree.forEach(floor => {
    floor.units.sort((a, b) => naturalCompare(a.unitNum, b.unitNum))
  })
  tree.sort((a, b) => naturalCompare(a.floorNum, b.floorNum))
  return tree
}

export function buildFloorTree(floors) {
  const tree = (floors || []).map(floor => {
    return {
      floorId: floor.floorId,
      floorNum: floor.floorNum,
      units: [{
        key: floor.floorId,
        unitId: '',
        unitNum: '0',
        floorId: floor.floorId,
        floorNum: floor.floorNum
      }]
    }
  })
  tree.sort((a, b) => naturalCompare(a.floorNum, b.floorNum))
  return tree
}

export function calcNodeState(roomIds, selectedMap) {
  const ids = roomIds || []
  const map = selectedMap || {}
  let selCount = 0
  ids.forEach(roomId => {
    if (map[roomId]) {
      selCount++
    }
  })
  const total = ids.length
  return {
    selCount: selCount,
    total: total,
    checked: total > 0 && selCount === total,
    indeterminate: selCount > 0 && selCount < total
  }
}

export const ROOM_STATE_FREE = '2002'

export function isSellableRoom(room) {
  if (!room) {
    return false
  }
  return room.state !== ROOM_STATE_FREE
}

export function filterSellableRooms(rooms) {
  return (rooms || []).filter(isSellableRoom)
}

export function filterRooms(rooms, keyword, states) {
  const kw = (keyword || '').trim().toLowerCase()
  const stateList = states || []
  return (rooms || []).filter(room => {
    if (stateList.length > 0 && stateList.indexOf(room.state) === -1) {
      return false
    }
    if (!kw) {
      return true
    }
    const text = [room.roomNum, room.roomName, room.stateName, room.ownerName].join(' ').toLowerCase()
    return text.indexOf(kw) > -1
  })
}

export function getRoomLabel(room) {
  if (!room) {
    return ''
  }
  if (room.roomName) {
    return room.roomName
  }
  const floorNum = room.floorNum || ''
  const unitNum = room.unitNum || ''
  const roomNum = room.roomNum || ''
  if (!unitNum || unitNum === '0') {
    return floorNum ? floorNum + '-' + roomNum : roomNum
  }
  return floorNum + '-' + unitNum + '-' + roomNum
}

export function formatFeeMeta(config, dictNames) {
  if (!config) {
    return ''
  }
  const names = dictNames || {}
  const parts = []
  if (config.feeName) {
    parts.push(config.feeName)
  }
  const formulaMap = names.computingFormula || {}
  const formulaName = config.computingFormulaName || formulaMap[config.computingFormula] || ''
  const squarePrice = Number(config.squarePrice)
  const price = squarePrice > 0 ? config.squarePrice : (config.additionalAmount || '')
  const units = config.units || '元'
  const unitLabel = squarePrice > 0 ? units + '/㎡' : units
  let feeInfo = formulaName
  if (price && Number(price) > 0) {
    feeInfo = feeInfo ? feeInfo + ' ' : ''
    feeInfo += price + ' ' + unitLabel
  }
  if (feeInfo) {
    parts.push(feeInfo)
  }
  const flagMap = names.feeFlag || {}
  const billTypeMap = names.billType || {}
  const flagName = config.feeFlagName || flagMap[config.feeFlag] || config.feeFlag || ''
  const billTypeName = config.billTypeName || billTypeMap[config.billType] || ''
  if (flagName) {
    parts.push(billTypeName ? flagName + '（' + billTypeName + '）' : flagName)
  }
  return parts.join(' · ')
}
