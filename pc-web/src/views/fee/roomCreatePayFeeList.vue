<template>
  <div class="room-create-pay-fee-container">
    <div class="page-head">
      <div class="page-head-main">
        <h1 class="page-title">{{ $t('roomCreatePayFee.title') }}</h1>
      </div>
      <div class="page-head-actions">
        <el-button type="warning" size="small" @click="goBack">{{ $t('roomCreatePayFee.back') }}</el-button>
        <el-button type="primary" size="small" :disabled="selectedCount < 1" :loading="submitting" @click="submitCreate">
          {{ $t('roomCreatePayFee.submitCreate') }}
        </el-button>
      </div>
    </div>

    <div class="card fee-card">
      <div class="fee-grid">
        <div class="field">
          <label class="field-label">{{ $t('roomCreatePayFee.feeType') }}<em>*</em></label>
          <el-select v-model="form.feeTypeCd" clearable :placeholder="$t('roomCreatePayFee.selectFeeType')" @change="changeFeeTypeCd">
            <el-option v-for="item in feeTypes" :key="item.statusCd" :label="item.name" :value="item.statusCd"></el-option>
          </el-select>
        </div>
        <div class="field">
          <label class="field-label">{{ $t('roomCreatePayFee.feeItem') }}<em>*</em></label>
          <el-select v-model="form.configId" :placeholder="$t('roomCreatePayFee.selectFeeItem')" :no-data-text="$t('roomCreatePayFee.feeItemEmptyTip')" @change="changeConfigId">
            <el-option v-for="item in feeConfigs" :key="item.configId" :label="item.feeName" :value="item.configId"></el-option>
          </el-select>
        </div>
        <div class="field">
          <label class="field-label">{{ $t('roomCreatePayFee.startTime') }}<em>*</em></label>
          <el-date-picker v-model="form.startTime" type="date" value-format="yyyy-MM-dd" :placeholder="$t('roomCreatePayFee.selectStartTime')" @change="onTimeChange">
          </el-date-picker>
        </div>
        <div class="field">
          <label class="field-label">{{ $t('roomCreatePayFee.endTime') }}</label>
          <el-date-picker v-model="form.endTime" type="date" value-format="yyyy-MM-dd" :placeholder="$t('roomCreatePayFee.selectEndTime')" @change="onTimeChange">
          </el-date-picker>
        </div>
      </div>

      <div v-if="needAmount || needRate" class="fee-grid extra-grid">
        <div v-if="needAmount" class="field">
          <label class="field-label">{{ $t('roomCreatePayFee.amount') }}<em>*</em></label>
          <el-input v-model="form.amount" :placeholder="$t('roomCreatePayFee.inputAmount')"></el-input>
        </div>
        <template v-if="needRate">
          <div class="field">
            <label class="field-label">{{ $t('roomCreatePayFee.rateCycle') }}<em>*</em></label>
            <el-input v-model="form.rateCycle" :placeholder="$t('roomCreatePayFee.inputRateCycle')"></el-input>
          </div>
          <div class="field">
            <label class="field-label">{{ $t('roomCreatePayFee.rate') }}<em>*</em></label>
            <el-input v-model="form.rate" :placeholder="$t('roomCreatePayFee.inputRate')"></el-input>
          </div>
          <div class="field">
            <label class="field-label">{{ $t('roomCreatePayFee.rateStartTime') }}<em>*</em></label>
            <el-date-picker v-model="form.rateStartTime" type="date" value-format="yyyy-MM-dd" :placeholder="$t('roomCreatePayFee.selectRateStartTime')">
            </el-date-picker>
          </div>
        </template>
      </div>

      <p class="fee-meta">
        {{ $t('roomCreatePayFee.feeItemSummary') }}：
        <span v-if="feeMeta" class="fee-meta-text">{{ feeMeta }}</span>
        <span v-else class="fee-meta-empty">{{ $t('roomCreatePayFee.feeItemSummaryEmpty') }}</span>
      </p>
    </div>

    <div class="card room-card">
      <div class="card-head">
        <h2 class="card-title">
          <span class="title-dot"></span>{{ $t('roomCreatePayFee.selectRooms') }}
          <span class="title-tip">{{ $t('roomCreatePayFee.selectRoomsTip') }}</span>
        </h2>
        <div class="card-actions">
          <el-button size="mini" @click="clearAll">{{ $t('roomCreatePayFee.clearAll') }}</el-button>
        </div>
      </div>

      <div class="room-body">
        <div class="tree-pane">
          <div class="pane-head">
            <span>{{ $t('roomCreatePayFee.buildingLabel') }}</span>
            <span>{{ $t('roomCreatePayFee.selectedOfTotal') }}</span>
          </div>
          <room-batch-building-list :buildings="buildingNodes" :active-floor-id="activeFloorId" @toggle-building="toggleBuilding" @select-building="selectBuilding" />
        </div>
        <div class="grid-pane">
          <room-batch-grid
            :groups="groups"
            :selected-map="selectedMap"
            :keyword.sync="keyword"
            :loading="roomsLoading"
            :building-name="activeBuildingName"
            :building-meta="activeBuildingMeta"
            :other-matches="otherMatches"
            @toggle-room="toggleRoom"
            @toggle-group="toggleGroup"
            @jump-building="jumpBuilding" />
        </div>
      </div>

      <div class="room-bar">
        <div class="room-bar-left">
          <span class="bar-count">{{ $t('roomCreatePayFee.selectedRooms', { selected: selectedCount }) }}</span>
          <span class="bar-divider"></span>
          <el-button type="text" class="bar-toggle" @click="showPicked = !showPicked">
            {{ showPicked ? $t('roomCreatePayFee.hidePickedList') : $t('roomCreatePayFee.checkPickedList') }}
            <i :class="showPicked ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
          </el-button>
        </div>
        <div class="room-bar-right">
          <span class="bar-tip">{{ $t('roomCreatePayFee.skipBilledTip') }}</span>
          <el-button type="primary" size="small" :disabled="selectedCount < 1" :loading="submitting" @click="submitCreate">
            {{ $t('roomCreatePayFee.submitCreate') }}
          </el-button>
        </div>
      </div>

      <div v-show="showPicked" class="picked-panel">
        <room-picked-list :rooms="selectedRooms" @remove="removeRoom" @clear-all="clearAll" />
      </div>
    </div>
  </div>
</template>

<script>
import { getCommunityId, getDict } from '@/api/community/communityApi'
import {
  listFeeConfigs,
  queryFloors,
  queryUnits,
  queryRooms,
  saveRoomCreatePayFee
} from '@/api/fee/roomCreatePayFeeApi'
import { filterFeeConfigsByFormula } from '@/utils/feeUtil'
import { dateFormat } from '@/utils/dateUtil'
import {
  buildUnitTree,
  buildFloorTree,
  calcNodeState,
  filterRooms,
  filterSellableRooms,
  formatFeeMeta
} from '@/utils/roomBatchUtil'
import RoomBatchBuildingList from '@/components/room/roomBatchBuildingList.vue'
import RoomBatchGrid from '@/components/room/roomBatchGrid.vue'
import RoomPickedList from '@/components/room/roomPickedList.vue'

const COMPUTING_FORMULA_DYNAMIC = '4004'
const COMPUTING_FORMULA_RANT_RATE = '1102'
const DEFAULT_END_TIME = '2050-01-01'
const ROOM_LOAD_BATCH = 6

export default {
  name: 'RoomCreatePayFeeList',
  components: { RoomBatchBuildingList, RoomBatchGrid, RoomPickedList },
  data() {
    return {
      communityId: '',
      feeTypes: [],
      feeConfigs: [],
      form: {
        feeTypeCd: '',
        configId: '',
        startTime: '',
        endTime: DEFAULT_END_TIME,
        amount: '',
        rateCycle: '',
        rate: '',
        rateStartTime: ''
      },
      dictNames: {
        computingFormula: {},
        feeFlag: {},
        billType: {}
      },
      floors: [],
      units: [],
      unitTree: [],
      roomMap: {},
      unitLoaded: {},
      unitLoading: {},
      selectedMap: {},
      activeFloorId: '',
      keyword: '',
      showPicked: false,
      submitting: false
    }
  },
  computed: {
    selectedConfig() {
      const configList = this.feeConfigs || []
      for (let i = 0; i < configList.length; i++) {
        if (configList[i].configId === this.form.configId) {
          return configList[i]
        }
      }
      return null
    },
    needAmount() {
      return !!this.selectedConfig && this.selectedConfig.computingFormula === COMPUTING_FORMULA_DYNAMIC
    },
    needRate() {
      return !!this.selectedConfig && this.selectedConfig.computingFormula === COMPUTING_FORMULA_RANT_RATE
    },
    feeMeta() {
      return formatFeeMeta(this.selectedConfig, this.dictNames)
    },
    allRooms() {
      const list = []
      this.unitTree.forEach(floor => {
        floor.units.forEach(unit => {
          const rooms = this.roomMap[unit.key] || []
          rooms.forEach(room => {
            list.push({
              roomId: room.roomId,
              roomNum: room.roomNum,
              roomName: room.roomName,
              state: room.state,
              stateName: room.stateName,
              ownerName: room.ownerName,
              builtUpArea: room.builtUpArea,
              floorId: floor.floorId,
              floorNum: room.floorNum || floor.floorNum,
              unitId: unit.unitId,
              unitNum: room.unitNum || unit.unitNum,
              unitKey: unit.key
            })
          })
        })
      })
      return list
    },
    selectedRooms() {
      const map = this.selectedMap
      return this.allRooms.filter(room => !!map[room.roomId])
    },
    selectedCount() {
      return this.selectedRooms.length
    },
    roomsLoading() {
      const loadingMap = this.unitLoading
      const keys = Object.keys(loadingMap)
      for (let i = 0; i < keys.length; i++) {
        if (loadingMap[keys[i]]) {
          return true
        }
      }
      return false
    },
    buildingNodes() {
      return this.unitTree.map(floor => {
        const roomIds = []
        let loaded = true
        floor.units.forEach(unit => {
          if (!this.unitLoaded[unit.key]) {
            loaded = false
          }
          (this.roomMap[unit.key] || []).forEach(room => roomIds.push(room.roomId))
        })
        const state = calcNodeState(roomIds, this.selectedMap)
        return {
          floorId: floor.floorId,
          floorNum: floor.floorNum,
          loaded: loaded,
          selCount: state.selCount,
          total: state.total,
          checked: state.checked,
          indeterminate: state.indeterminate
        }
      })
    },
    activeFloor() {
      const tree = this.unitTree
      for (let i = 0; i < tree.length; i++) {
        if (tree[i].floorId === this.activeFloorId) {
          return tree[i]
        }
      }
      return tree.length > 0 ? tree[0] : null
    },
    activeBuildingName() {
      if (!this.activeFloor) {
        return ''
      }
      return this.activeFloor.floorNum + this.$t('roomCreatePayFee.buildingSuffix')
    },
    activeBuildingMeta() {
      const floor = this.activeFloor
      if (!floor) {
        return ''
      }
      let total = 0
      floor.units.forEach(unit => {
        total += (this.roomMap[unit.key] || []).length
      })
      return this.$t('roomCreatePayFee.buildingMeta', { units: floor.units.length, total: total })
    },
    groups() {
      const floor = this.activeFloor
      if (!floor) {
        return []
      }
      const list = []
      floor.units.forEach(unit => {
        const rooms = filterRooms(this.roomMap[unit.key] || [], this.keyword)
        if (rooms.length < 1) {
          return
        }
        const state = calcNodeState(rooms.map(room => room.roomId), this.selectedMap)
        list.push({
          key: unit.key,
          unitId: unit.unitId,
          unitNum: unit.unitNum,
          rooms: rooms,
          selCount: state.selCount,
          total: state.total,
          checked: state.checked,
          indeterminate: state.indeterminate
        })
      })
      return list
    },
    otherMatches() {
      const keyword = (this.keyword || '').trim()
      if (!keyword) {
        return []
      }
      const list = []
      this.unitTree.forEach(floor => {
        if (floor.floorId === this.activeFloorId) {
          return
        }
        let count = 0
        floor.units.forEach(unit => {
          count += filterRooms(this.roomMap[unit.key] || [], keyword).length
        })
        if (count > 0) {
          list.push({ floorId: floor.floorId, floorNum: floor.floorNum, count: count })
        }
      })
      return list
    }
  },
  created() {
    this.initData()
  },
  methods: {
    async initData() {
      this.communityId = getCommunityId()
      this.form.startTime = dateFormat(new Date())
      await this.loadDictNames()
      await this.loadFeeTypeCds()
      await this.loadTree()
    },
    async loadDictNames() {
      const loadDict = async (columns) => {
        try {
          const list = await getDict('pay_fee_config', columns)
          const map = {}
          ;(list || []).forEach(item => {
            map[item.statusCd] = item.name
          })
          return map
        } catch (error) {
          return {}
        }
      }
      const computingFormula = await loadDict('computing_formula')
      const feeFlag = await loadDict('fee_flag')
      const billType = await loadDict('bill_type')
      this.dictNames = {
        computingFormula: computingFormula,
        feeFlag: feeFlag,
        billType: billType
      }
    },
    async loadFeeTypeCds() {
      try {
        const data = await getDict('pay_fee_config', 'fee_type_cd')
        this.feeTypes = data || []
      } catch (error) {
        this.feeTypes = []
      }
    },
    async loadFeeConfigs() {
      if (!this.form.feeTypeCd) {
        this.feeConfigs = []
        return
      }
      try {
        const params = {
          page: 1,
          row: 500,
          communityId: this.communityId,
          feeTypeCd: this.form.feeTypeCd,
          isDefault: 'F',
          valid: '1'
        }
        const { feeConfigs } = await listFeeConfigs(params)
        this.feeConfigs = filterFeeConfigsByFormula(feeConfigs)
      } catch (error) {
        this.feeConfigs = []
      }
    },
    async loadTree() {
      let tree = []
      try {
        const [units, floorData] = await Promise.all([
          queryUnits({ communityId: this.communityId, page: 1, row: 1000 }),
          queryFloors({ communityId: this.communityId, page: 1, row: 1000 })
        ])
        this.units = units || []
        this.floors = (floorData && floorData.apiFloorDataVoList) || []
        tree = buildUnitTree(this.units)
        if (tree.length < 1) {
          tree = buildFloorTree(this.floors)
        }
        this.unitTree = tree
        this.activeFloorId = tree.length > 0 ? tree[0].floorId : ''
      } catch (error) {
        this.$message.error(this.$t('roomCreatePayFee.loadRoomsFailed'))
        return
      }
      const activeFloor = this.activeFloor
      if (activeFloor) {
        await this.loadUnitsRooms(activeFloor.units.map(unit => unit.key))
      }
    },
    findUnit(unitKey) {
      const tree = this.unitTree
      for (let i = 0; i < tree.length; i++) {
        const units = tree[i].units
        for (let j = 0; j < units.length; j++) {
          if (units[j].key === unitKey) {
            return units[j]
          }
        }
      }
      return null
    },
    async loadUnitsRooms(unitKeys) {
      const pending = (unitKeys || []).filter(key => !this.unitLoaded[key] && !this.unitLoading[key])
      if (pending.length < 1) {
        return
      }
      pending.forEach(key => this.$set(this.unitLoading, key, true))
      for (let i = 0; i < pending.length; i += ROOM_LOAD_BATCH) {
        const batch = pending.slice(i, i + ROOM_LOAD_BATCH)
        await Promise.all(batch.map(key => this.loadUnitRooms(key)))
      }
    },
    async loadUnitRooms(unitKey) {
      const unit = this.findUnit(unitKey)
      if (!unit) {
        this.$set(this.unitLoading, unitKey, false)
        return
      }
      try {
        const params = { communityId: this.communityId, page: 1, row: 1000 }
        if (unit.unitId) {
          params.unitId = unit.unitId
        } else {
          params.floorId = unit.floorId
        }
        const res = await queryRooms(params)
        const rooms = filterSellableRooms((res && res.rooms) || [])
        rooms.forEach(room => {
          if (!room.floorNum) {
            room.floorNum = unit.floorNum
          }
          if (!room.unitNum) {
            room.unitNum = unit.unitNum
          }
        })
        this.$set(this.roomMap, unitKey, rooms)
        this.$set(this.unitLoaded, unitKey, true)
      } catch (error) {
        this.$set(this.roomMap, unitKey, [])
        this.$set(this.unitLoaded, unitKey, true)
      } finally {
        this.$set(this.unitLoading, unitKey, false)
      }
    },
    changeFeeTypeCd() {
      this.form.configId = ''
      this.feeConfigs = []
      this.resetExtraFields()
      this.loadFeeConfigs()
    },
    changeConfigId() {
      this.resetExtraFields()
      if (!this.form.endTime) {
        this.form.endTime = DEFAULT_END_TIME
      }
    },
    resetExtraFields() {
      this.form.amount = ''
      this.form.rateCycle = ''
      this.form.rate = ''
      this.form.rateStartTime = ''
    },
    onTimeChange() {
      if (!this.form.startTime || !this.form.endTime) {
        return
      }
      if (this.form.endTime < this.form.startTime) {
        this.$message.warning(this.$t('roomCreatePayFee.startAfterEndTip'))
      }
    },
    findFloor(floorId) {
      return this.unitTree.filter(item => item.floorId === floorId)[0] || null
    },
    selectBuilding(floorId) {
      if (this.activeFloorId === floorId) {
        return
      }
      this.activeFloorId = floorId
      this.keyword = ''
      const floor = this.findFloor(floorId)
      if (!floor) {
        return
      }
      this.loadUnitsRooms(floor.units.map(unit => unit.key))
    },
    jumpBuilding(floorId) {
      this.activeFloorId = floorId
      const floor = this.findFloor(floorId)
      if (!floor) {
        return
      }
      this.loadUnitsRooms(floor.units.map(unit => unit.key))
    },
    async toggleBuilding(floorId) {
      const floor = this.findFloor(floorId)
      if (!floor) {
        return
      }
      if (this.activeFloorId !== floorId) {
        this.activeFloorId = floorId
        this.keyword = ''
      }
      await this.loadUnitsRooms(floor.units.map(unit => unit.key))
      const roomIds = []
      floor.units.forEach(unit => {
        (this.roomMap[unit.key] || []).forEach(room => roomIds.push(room.roomId))
      })
      const state = calcNodeState(roomIds, this.selectedMap)
      this.setRoomsSelected(roomIds, !state.checked)
    },
    toggleGroup(unitKey) {
      const group = this.groups.filter(item => item.key === unitKey)[0]
      if (!group) {
        return
      }
      this.setRoomsSelected(group.rooms.map(room => room.roomId), !group.checked)
    },
    toggleRoom(roomId) {
      this.setRoomSelected(roomId, !this.selectedMap[roomId])
    },
    setRoomSelected(roomId, selected) {
      if (!roomId) {
        return
      }
      if (selected) {
        this.$set(this.selectedMap, roomId, true)
      } else {
        this.$delete(this.selectedMap, roomId)
      }
    },
    setRoomsSelected(roomIds, selected) {
      (roomIds || []).forEach(roomId => this.setRoomSelected(roomId, selected))
    },
    removeRoom(roomId) {
      this.$delete(this.selectedMap, roomId)
    },
    clearAll() {
      this.selectedMap = {}
    },
    validateForm() {
      if (!this.form.feeTypeCd) {
        this.$message.error(this.$t('roomCreatePayFee.selectFeeType'))
        return false
      }
      if (!this.form.configId) {
        this.$message.error(this.$t('roomCreatePayFee.selectFeeItem'))
        return false
      }
      if (this.form.startTime && typeof this.form.startTime !== 'string') {
        this.form.startTime = dateFormat(this.form.startTime)
      }
      if (this.form.endTime && typeof this.form.endTime !== 'string') {
        this.form.endTime = dateFormat(this.form.endTime)
      }
      if (this.form.rateStartTime && typeof this.form.rateStartTime !== 'string') {
        this.form.rateStartTime = dateFormat(this.form.rateStartTime)
      }
      if (!this.form.startTime) {
        this.$message.error(this.$t('roomCreatePayFee.selectStartTime'))
        return false
      }
      if (!this.form.endTime) {
        this.$message.error(this.$t('roomCreatePayFee.selectEndTime'))
        return false
      }
      if (this.form.endTime < this.form.startTime) {
        this.$message.error(this.$t('roomCreatePayFee.startAfterEndTip'))
        return false
      }
      if (this.selectedCount < 1) {
        this.$message.error(this.$t('roomCreatePayFee.noRoomSelected'))
        return false
      }
      if (this.needAmount && !this.form.amount) {
        this.$message.error(this.$t('roomCreatePayFee.inputAmount'))
        return false
      }
      if (this.needRate && !this.form.rateCycle) {
        this.$message.error(this.$t('roomCreatePayFee.inputRateCycle'))
        return false
      }
      if (this.needRate && !this.form.rate) {
        this.$message.error(this.$t('roomCreatePayFee.inputRate'))
        return false
      }
      if (this.needRate && !this.form.rateStartTime) {
        this.$message.error(this.$t('roomCreatePayFee.selectRateStartTime'))
        return false
      }
      return true
    },
    async submitCreate() {
      if (this.submitting) {
        return
      }
      if (!this.validateForm()) {
        return
      }
      const roomIds = this.selectedRooms.map(room => room.roomId)
      const count = roomIds.length
      const confirmMsg = count > 200
        ? this.$t('roomCreatePayFee.submitConfirmBig', { count: count })
        : this.$t('roomCreatePayFee.submitConfirm', { count: count })
      try {
        await this.$confirm(confirmMsg, this.$t('common.tip'), {
          confirmButtonText: this.$t('common.confirm'),
          cancelButtonText: this.$t('common.cancel'),
          type: 'warning'
        })
      } catch (error) {
        return
      }
      const config = this.selectedConfig
      const data = {
        communityId: this.communityId,
        feeTypeCd: this.form.feeTypeCd,
        configId: this.form.configId,
        startTime: this.form.startTime,
        endTime: this.form.endTime,
        feeFlag: config ? config.feeFlag : '',
        computingFormula: config ? config.computingFormula : '',
        roomIds: roomIds
      }
      if (this.needAmount) {
        data.amount = this.form.amount
      }
      if (this.needRate) {
        data.rateCycle = this.form.rateCycle
        data.rate = this.form.rate
        data.rateStartTime = this.form.rateStartTime
      }
      this.submitting = true
      try {
        await saveRoomCreatePayFee(data)
        this.$message.success(this.$t('roomCreatePayFee.submitSuccess', { count: count }))
        this.goBack()
      } catch (error) {
        this.$message.error(error.message || this.$t('common.saveFailed'))
      } finally {
        this.submitting = false
      }
    },
    goBack() {
      this.$router.go(-1)
    }
  }
}
</script>

<style lang="scss" scoped>
.room-create-pay-fee-container {
  padding: 16px 20px 24px;
  text-align: left;

  .page-head {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-end;
    justify-content: space-between;
    margin-bottom: 16px;

    .page-title {
      margin: 0;
      font-size: 21px;
      font-weight: 700;
      color: #0f172a;
    }
  }

  .card {
    margin-bottom: 16px;
    background: #fff;
    border: 1px solid #e8edf4;
    border-radius: 12px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  }

  .fee-card {
    padding: 16px 20px;

    .fee-grid {
      display: grid;
      grid-template-columns: 3fr 4fr 2fr 3fr;
      gap: 14px 18px;
    }

    .extra-grid {
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      margin-top: 14px;
      padding-top: 14px;
      border-top: 1px dashed #eef2f7;
    }

    .field {
      min-width: 0;

      .field-label {
        display: block;
        margin-bottom: 6px;
        font-size: 12px;
        font-weight: 600;
        color: #64748b;

        em {
          margin-left: 2px;
          font-style: normal;
          color: #f43f5e;
        }
      }
    }

    ::v-deep .el-select,
    ::v-deep .el-date-editor,
    ::v-deep .el-input {
      width: 100%;
    }

    .fee-meta {
      margin: 12px 0 0;
      font-size: 11.5px;
      color: #94a3b8;

      .fee-meta-text {
        color: #64748b;
      }

      .fee-meta-empty {
        color: #cbd5e1;
      }
    }
  }

  .room-card {
    overflow: hidden;

    .card-head {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 8px;
      padding: 12px 20px;
      border-bottom: 1px solid #f1f5f9;

      .card-title {
        display: flex;
        align-items: center;
        margin: 0;
        font-size: 14px;
        font-weight: 600;
        color: #1e293b;

        .title-dot {
          width: 3px;
          height: 14px;
          margin-right: 8px;
          background: #2563eb;
          border-radius: 3px;
        }

        .title-tip {
          margin-left: 8px;
          font-size: 11.5px;
          font-weight: 400;
          color: #94a3b8;
        }
      }

      .card-actions {
        display: flex;
        align-items: center;
        gap: 8px;
      }
    }

    .room-body {
      display: flex;
      flex-wrap: wrap;
    }

    .tree-pane {
      flex: 0 0 236px;
      width: 236px;
      max-height: 560px;
      overflow-y: auto;
      border-right: 1px solid #f1f5f9;
      background: #f8fafc;
    }

    .pane-head {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 14px;
      font-size: 11.5px;
      color: #94a3b8;
      border-bottom: 1px solid #f1f5f9;
    }

    .grid-pane {
      flex: 1;
      min-width: 0;
    }

    .room-bar {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 10px;
      padding: 10px 20px;
      background: #f8fafc;
      border-top: 1px solid #f1f5f9;

      .room-bar-left {
        display: flex;
        align-items: center;
        font-size: 12.5px;
        color: #64748b;

        .bar-count {
          color: #2563eb;
        }

        .bar-divider {
          width: 1px;
          height: 12px;
          margin: 0 10px;
          background: #e2e8f0;
        }

        .bar-toggle {
          padding: 0;
          font-size: 12.5px;
          color: #2563eb;
        }
      }

      .room-bar-right {
        display: flex;
        align-items: center;

        .bar-tip {
          margin-right: 10px;
          font-size: 11.5px;
          color: #94a3b8;
        }
      }
    }

    .picked-panel {
      padding: 10px 20px 14px;
      border-top: 1px solid #f1f5f9;
    }
  }

  @media (max-width: 992px) {
    .fee-card {
      .fee-grid {
        grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      }
    }

    .room-card {
      .tree-pane {
        flex: 0 0 100%;
        width: 100%;
        border-right: none;
        border-bottom: 1px solid #f1f5f9;
      }
    }
  }
}
</style>
