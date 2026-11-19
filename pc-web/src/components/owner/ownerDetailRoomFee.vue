<template>
  <div class="owner-detail-room-fee">
    <div class="filter-row">
      <el-select :value="roomFeeInfo.roomId" size="small" class="room-select" filterable
        :placeholder="$t('ownerDetailRoomFeeRule.roomPlaceholder')" @input="handleRoomChange">
        <el-option v-for="item in roomFeeInfo.rooms" :key="item.roomId" :label="_roomLabel(item)"
          :value="item.roomId"></el-option>
      </el-select>
      <el-select :value="roomFeeInfo.feeTypeCd" size="small" class="fee-type-select"
        :placeholder="$t('simplifyRoomFee.selectFeeType')" @input="handleFeeTypeChange">
        <el-option :label="$t('roomFeeRuleBill.allFeeType')" value=""></el-option>
        <template v-for="item in roomFeeInfo.feeTypeCds">
          <el-option v-if="item.statusCd != '888800010008'" :key="item.statusCd" :label="item.name"
            :value="item.statusCd"></el-option>
        </template>
      </el-select>
      <el-select :value="roomFeeInfo.configId" size="small" class="fee-item-select" filterable
        :placeholder="$t('simplifyRoomFee.selectFeeItem')" @input="handleConfigChange">
        <el-option :label="$t('roomFeeRuleBill.allFeeItem')" value=""></el-option>
        <el-option v-for="item in roomFeeInfo.feeConfigs" :key="item.configId" :label="item.feeName"
          :value="item.configId"></el-option>
      </el-select>
      <el-select :value="roomFeeInfo.stateFilter" size="small" class="state-select" @input="handleStateChange">
        <el-option :label="$t('roomFeeRuleBill.allState')" value=""></el-option>
        <el-option :label="$t('roomFeeRuleBill.stateDoing')" value="2008001"></el-option>
        <el-option :label="$t('roomFeeRuleBill.stateStopped')" value="2009001"></el-option>
      </el-select>
    </div>

    <div v-if="roomFeeInfo.roomsLoading" v-loading="true" class="room-fee-loading"></div>
    <div v-else-if="roomFeeInfo.rooms.length < 1" class="room-fee-empty">
      {{ $t('ownerDetailRoomFeeRule.noRoom') }}
    </div>
    <div v-else>
      <el-table ref="ruleTable" v-loading="roomFeeInfo.loading" :data="roomFeeInfo.pageRows" row-key="ruleId"
        class="rule-table" size="small" :empty-text="_emptyText()" @row-click="handleRowClick"
        @expand-change="handleExpandChange">
        <el-table-column type="expand">
          <template slot-scope="scope">
            <div class="bill-panel">
              <div class="bill-panel-head">
                <span class="bill-panel-title">
                  {{ $t('roomFeeRuleBill.subTitle', { feeName: _billDisplayName(scope.row) }) }}
                </span>
                <span class="bill-panel-count">{{ _billCountText(scope.row) }}</span>
                <el-select :value="scope.row.billStateFilter" size="mini" class="bill-state-filter"
                  @input="handleBillStateFilterChange(scope.row, $event)">
                  <el-option :label="$t('roomFeeRuleBill.billFilterAll')" value="all"></el-option>
                  <el-option :label="$t('roomFeeRuleBill.billFilterPaid')" value="paid"></el-option>
                  <el-option :label="$t('roomFeeRuleBill.billFilterUnpaid')" value="unpaid"></el-option>
                </el-select>
              </div>
              <div v-loading="scope.row.billsLoading" class="bill-panel-body">
                <div v-if="!scope.row.filteredBills || scope.row.filteredBills.length === 0" class="bill-panel-empty">
                  {{ _billEmptyText(scope.row) }}
                </div>
                <el-table v-else :data="scope.row.filteredBills" size="mini" class="bill-table">
                  <el-table-column prop="yearMonth" :label="$t('roomFeeRuleBill.billYearMonth')">
                  </el-table-column>
                  <el-table-column :label="$t('roomFeeRuleBill.billPeriod')">
                    <template slot-scope="billScope">
                      {{ _dateOnly(billScope.row.startTime) }} ~ {{ _dateOnly(billScope.row.endTime) }}
                    </template>
                  </el-table-column>
                  <el-table-column :label="$t('roomFeeRuleBill.billUnpaidAmount')" align="right">
                    <template slot-scope="billScope">
                      <span :class="Number(billScope.row.oweAmount) > 0 ? 'stat-owe' : 'text-muted'">
                        ¥{{ _money(billScope.row.oweAmount) }}
                      </span>
                    </template>
                  </el-table-column>
                  <el-table-column :label="$t('roomFeeRuleBill.billState')" align="center">
                    <template slot-scope="billScope">
                      <el-tag v-if="billScope.row.billState === 'paid'" size="mini" type="success">
                        {{ $t('roomFeeRuleBill.billPaid') }}
                      </el-tag>
                      <el-tag v-else-if="billScope.row.billState === 'cancelled'" size="mini" type="info">
                        {{ $t('roomFeeRuleBill.billCancelled') }}
                      </el-tag>
                      <el-tag v-else size="mini" type="danger">{{ $t('roomFeeRuleBill.billUnpaid') }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column :label="$t('roomFeeRuleBill.subColRemark')">
                    <template slot-scope="billScope">
                      <div class="bill-remark">{{ billScope.row.remark || '—' }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column :label="$t('roomFeeRuleBill.subColFeeRemark')">
                    <template slot-scope="billScope">
                      <div class="bill-remark">{{ billScope.row.feeRemark || '—' }}</div>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('roomFeeRuleBill.colFeeItem')" width="300">
          <template slot-scope="scope">
            <div class="cell-main cell-name">
              {{ _ruleDisplayName(scope.row) }}
              <el-tag v-if="scope.row.risk" size="mini" type="warning" class="cell-badge">
                {{ $t('roomFeeRuleBill.cancelledBadge') }}
              </el-tag>
            </div>
            <div class="cell-sub">
              {{ scope.row.feeTypeCdName || '-' }} · {{ scope.row.computingFormulaName || '-' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('roomFeeRuleBill.colRule')" min-width="300">
          <template slot-scope="scope">
            <div class="cell-main">{{ _ruleCycleText(scope.row) }}</div>
            <div class="cell-sub">{{ _dateOnly(scope.row.startTime) }} ~ {{ _dateOnly(scope.row.maxTime) }}</div>
            <div v-if="_rulePriceText(scope.row)" class="cell-sub">{{ _rulePriceText(scope.row) }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('roomFeeRuleBill.colNextBill')" width="150">
          <template slot-scope="scope">
            <template v-if="scope.row.state === '2008001'">
              <template v-if="_isMeterRule(scope.row)">
                <div class="cell-main text-meter">{{ $t('roomFeeRuleBill.nextBillMeter') }}</div>
                <div class="cell-sub">{{ $t('roomFeeRuleBill.meterAfterReading') }}</div>
              </template>
              <template v-else>
                <div class="cell-main">{{ _dateOnly(scope.row.curYearMonth) }}</div>
                <div class="cell-sub">{{ $t('roomFeeRuleBill.nextBillAuto') }}</div>
              </template>
            </template>
            <template v-else>
              <div class="cell-main text-muted">—</div>
              <div class="cell-sub">{{ $t('roomFeeRuleBill.noMoreBill') }}</div>
            </template>
          </template>
        </el-table-column>
        <el-table-column :label="$t('roomFeeRuleBill.colReceivable')" align="right" width="130">
          <template slot-scope="scope">
            <div class="cell-main" :class="Number(scope.row.oweAmount) > 0 ? 'stat-owe' : 'text-muted'">
              ¥{{ _money(scope.row.oweAmount) }}
            </div>
            <div class="cell-sub">{{ $t('roomFeeRuleBill.periods', { count: scope.row.unpaidCount }) }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('roomFeeRuleBill.colState')" align="center" width="120">
          <template slot-scope="scope">
            <el-tag size="mini" :type="_stateTagType(scope.row)"
              :class="['tag-state', { 'tag-meter': _isMeterRule(scope.row) }]">
              {{ _stateText(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="footer-bar">
        <span class="footer-stat footer-stat-owe">
          <span class="footer-stat-label">{{ $t('ownerDetailRoomFeeRule.summaryOwe') }}</span>
          <span class="footer-stat-value">¥{{ _statOwe() }}</span>
        </span>
        <span class="footer-total">
          {{ $t('ownerDetailRoomFeeRule.totalCount', { count: roomFeeInfo.filteredCount }) }}
        </span>
        <el-pagination small layout="prev, pager, next" :current-page="roomFeeInfo.currentPage"
          :page-size="roomFeeInfo.pageSize" :total="roomFeeInfo.filteredCount"
          @current-change="handlePageChange"></el-pagination>
      </div>
    </div>
  </div>
</template>

<script>
import { listRoomsByOwner } from '@/api/owner/ownerDetailRoomFeeApi'
import { listFeeConfigs } from '@/api/fee/simplifyRoomFeeApi'
import { getDict, getCommunityId } from '@/api/community/communityApi'
import { listRoomFeeRuleWithBill, listRuleBills } from '@/api/fee/roomFeeRuleBillApi'
import { EXCLUDE_COMPUTING_FORMULAS } from '@/utils/feeUtil'

export default {
  name: 'OwnerDetailRoomFee',
  data() {
    return {
      roomFeeInfo: {
        ownerId: '',
        ownerName: '',
        rooms: [],
        roomsLoading: false,
        roomId: '',
        roomName: '',
        rules: [],
        feeTypeCds: [],
        feeTypeCd: '',
        feeConfigs: [],
        configId: '',
        stateFilter: '',
        loading: false,
        currentPage: 1,
        pageSize: 20,
        filteredCount: 0,
        pageRows: [],
        ruleSeq: 0
      }
    }
  },
  methods: {
    open(params) {
      const data = params || {}
      const info = this.roomFeeInfo
      info.ownerId = data.ownerId || ''
      info.ownerName = data.name || data.ownerName || ''
      info.rooms = []
      info.roomsLoading = false
      info.roomId = ''
      info.roomName = ''
      info.rules = []
      info.loading = false
      info.ruleSeq = 0
      this._resetFilters()
      this.loadFeeTypeCds()
      this.loadFeeConfigs('')
      this.loadRooms()
    },
    loadRooms() {
      const info = this.roomFeeInfo
      if (!info.ownerId) {
        info.rooms = []
        info.roomsLoading = false
        info.rules = []
        this._refreshPageData()
        return
      }
      info.roomsLoading = true
      this._clearBillCache()
      listRoomsByOwner({
        communityId: getCommunityId(),
        ownerId: info.ownerId,
        page: 1,
        row: 100
      }).then(res => {
        const rooms = res.rooms || []
        info.rooms = rooms
        info.roomsLoading = false
        if (rooms.length < 1) {
          info.roomId = ''
          info.roomName = ''
          info.rules = []
          this._refreshPageData()
          return
        }
        const room = rooms[0]
        info.roomId = room.roomId
        info.roomName = this._roomLabel(room)
        this.loadRules()
      }).catch(error => {
        info.rooms = []
        info.roomsLoading = false
        info.rules = []
        this._refreshPageData()
        console.error('Failed to load owner rooms:', error)
      })
    },
    loadRules() {
      const info = this.roomFeeInfo
      if (!info.roomId) {
        info.rules = []
        this._refreshPageData()
        return
      }
      info.ruleSeq = info.ruleSeq + 1
      const seq = info.ruleSeq
      info.loading = true
      this._clearBillCache()
      listRoomFeeRuleWithBill({
        communityId: getCommunityId(),
        payerObjId: info.roomId,
        payerObjType: '3333'
      }).then(res => {
        if (seq !== info.ruleSeq) {
          return
        }
        const rules = res.data || []
        rules.forEach(rule => {
          this._initRuleBills(rule)
        })
        info.rules = rules
        info.loading = false
        this._refreshPageData()
      }).catch(error => {
        if (seq !== info.ruleSeq) {
          return
        }
        info.rules = []
        info.loading = false
        this._refreshPageData()
        this.$message.error(error.message || this.$t('roomFeeRuleBill.loadFailed'))
      })
    },
    loadFeeTypeCds() {
      getDict('pay_fee_config', 'fee_type_cd').then(data => {
        this.roomFeeInfo.feeTypeCds = data || []
      }).catch(error => {
        console.error('Failed to fetch fee type cds:', error)
      })
    },
    loadFeeConfigs(feeTypeCd) {
      const params = {
        page: 1,
        row: 500,
        communityId: getCommunityId(),
        feeTypeCd: feeTypeCd,
        isDefault: '',
        valid: '1'
      }
      listFeeConfigs(params).then(res => {
        this.roomFeeInfo.feeConfigs = res.feeConfigs || []
      }).catch(error => {
        console.error('Failed to fetch fee configs:', error)
      })
    },
    handleRoomChange(roomId) {
      const info = this.roomFeeInfo
      const rooms = info.rooms || []
      let roomName = ''
      rooms.forEach(room => {
        if (room.roomId === roomId) {
          roomName = this._roomLabel(room)
        }
      })
      info.roomId = roomId
      info.roomName = roomName
      this._resetFilters()
      this.loadRules()
    },
    handleFeeTypeChange(feeTypeCd) {
      this.roomFeeInfo.feeTypeCd = feeTypeCd
      this.roomFeeInfo.configId = ''
      this.loadFeeConfigs(feeTypeCd)
      this.handleQuery()
    },
    handleConfigChange(configId) {
      this.roomFeeInfo.configId = configId
      this.handleQuery()
    },
    handleStateChange(state) {
      this.roomFeeInfo.stateFilter = state
      this.handleQuery()
    },
    _resetFilters() {
      const info = this.roomFeeInfo
      info.feeTypeCd = ''
      info.configId = ''
      info.stateFilter = ''
      info.currentPage = 1
    },
    _buildFilteredRules() {
      const info = this.roomFeeInfo
      return info.rules.filter(item => {
        if (info.stateFilter && item.state !== info.stateFilter) {
          return false
        }
        if (info.feeTypeCd && item.feeTypeCd !== info.feeTypeCd) {
          return false
        }
        if (info.configId && item.configId !== info.configId) {
          return false
        }
        return true
      })
    },
    _buildPageRows(filteredRules) {
      const info = this.roomFeeInfo
      const start = (info.currentPage - 1) * info.pageSize
      return filteredRules.slice(start, start + info.pageSize)
    },
    _refreshPageData() {
      const info = this.roomFeeInfo
      const filteredRules = this._buildFilteredRules()
      info.filteredCount = filteredRules.length
      const maxPage = Math.max(1, Math.ceil(info.filteredCount / info.pageSize))
      if (info.currentPage > maxPage) {
        info.currentPage = maxPage
      }
      info.pageRows = this._buildPageRows(filteredRules)
    },
    _initRuleBills(rule) {
      this.$set(rule, 'billStateFilter', 'unpaid')
      this.$set(rule, 'filteredBills', [])
      this.$set(rule, 'billCount', 0)
      this.$set(rule, 'billsLoading', false)
      this.$set(rule, 'billCache', {})
    },
    _buildRuleBills(rule) {
      const bills = rule.bills || []
      if (rule.billStateFilter === 'paid') {
        return bills.filter(bill => bill.billState === 'paid')
      }
      if (rule.billStateFilter === 'unpaid') {
        return bills.filter(bill => bill.billState === 'unpaid' || bill.billState === 'cancelled')
      }
      return bills.slice(0)
    },
    _clearBillCache() {
      const rules = this.roomFeeInfo.rules || []
      rules.forEach(rule => {
        this.$set(rule, 'billCache', {})
        this.$set(rule, 'filteredBills', [])
        this.$set(rule, 'billCount', 0)
        this.$set(rule, 'billsLoading', false)
      })
    },
    _loadRuleBills(rule, state) {
      if (!rule) {
        return
      }
      const billState = state || 'unpaid'
      this.$set(rule, 'billStateFilter', billState)
      if (rule.syntheticOther || !rule.ruleId || rule.ruleId === '-1') {
        const localBills = this._buildRuleBills(rule)
        this.$set(rule, 'filteredBills', localBills)
        this.$set(rule, 'billCount', localBills.length)
        return
      }
      const cache = rule.billCache || {}
      if (cache[billState]) {
        this.$set(rule, 'filteredBills', cache[billState])
        this.$set(rule, 'billCount', cache[billState].length)
        return
      }
      if (rule.billsLoading) {
        return
      }
      const communityId = getCommunityId()
      if (!communityId) {
        return
      }
      this.$set(rule, 'billsLoading', true)
      listRuleBills({
        communityId: communityId,
        ruleId: rule.ruleId,
        billState: billState,
        page: -1
      }).then(res => {
        const bills = res.data || []
        const nextCache = Object.assign({}, rule.billCache || {})
        nextCache[billState] = bills
        this.$set(rule, 'billCache', nextCache)
        this.$set(rule, 'filteredBills', bills)
        this.$set(rule, 'billCount', bills.length)
        this.$set(rule, 'billsLoading', false)
      }).catch(error => {
        this.$set(rule, 'billsLoading', false)
        this.$message.error(error.message || this.$t('roomFeeRuleBill.loadFailed'))
      })
    },
    handleBillStateFilterChange(rule, state) {
      this._loadRuleBills(rule, state)
    },
    handleExpandChange(row, expandRows) {
      const rows = expandRows || []
      if (rows.indexOf(row) < 0) {
        return
      }
      this._loadRuleBills(row, row.billStateFilter || 'unpaid')
    },
    handleRowClick(row) {
      if (!this.$refs.ruleTable) {
        return
      }
      this.$refs.ruleTable.toggleRowExpansion(row)
    },
    handleQuery() {
      this.roomFeeInfo.currentPage = 1
      this._refreshPageData()
    },
    handlePageChange(page) {
      this.roomFeeInfo.currentPage = page
      this._refreshPageData()
    },
    _emptyText() {
      if (this.roomFeeInfo.roomId) {
        return this.$t('roomFeeRuleBill.empty')
      }
      return this.$t('roomFeeRuleBill.selectRoomFirst')
    },
    _statOwe() {
      let total = 0
      this.roomFeeInfo.rules.forEach(item => {
        const amount = parseFloat(item.oweAmount)
        if (!isNaN(amount)) {
          total += amount
        }
      })
      return total.toFixed(2)
    },
    _roomLabel(room) {
      const parts = []
      if (room.floorNum || room.floorNum === 0) {
        parts.push(room.floorNum)
      }
      if (room.unitNum) {
        parts.push(room.unitNum)
      }
      if (room.roomNum) {
        parts.push(room.roomNum)
      }
      return parts.join('-')
    },
    _money(value) {
      const amount = parseFloat(value)
      return isNaN(amount) ? '0.00' : amount.toFixed(2)
    },
    _dateOnly(value) {
      if (!value) {
        return '—'
      }
      return String(value).substring(0, 10)
    },
    _ruleDisplayName(row) {
      if (row.syntheticOther) {
        return this.$t('roomFeeRuleBill.syntheticName')
      }
      return row.feeName || '-'
    },
    _billDisplayName(row) {
      return this._ruleDisplayName(row)
    },
    _rulePriceText(row) {
      if (row.syntheticOther) {
        return ''
      }
      const parts = []
      if (!isNaN(parseFloat(row.squarePrice))) {
        parts.push(this.$t('roomFeeRuleBill.unitPrice', { price: this._money(row.squarePrice) }))
      }
      if (!isNaN(parseFloat(row.additionalAmount))) {
        parts.push(this.$t('roomFeeRuleBill.extraFee', { amount: this._money(row.additionalAmount) }))
      }
      return parts.join(' · ')
    },
    _ruleCycleText(row) {
      if (row.syntheticOther) {
        return '-'
      }
      const cycle = row.feeFlagName || '-'
      return this._isMeterRule(row) ? this.$t('roomFeeRuleBill.cycleMeter', { cycle: cycle }) : cycle
    },
    _isMeterRule(row) {
      return EXCLUDE_COMPUTING_FORMULAS.indexOf(row.computingFormula) >= 0
    },
    _stateText(row) {
      if (row.syntheticOther) {
        return this.$t('roomFeeRuleBill.stateNoRule')
      }
      if (row.state === '2008001') {
        return this._isMeterRule(row) ? this.$t('roomFeeRuleBill.nextBillMeter') : this.$t('roomFeeRuleBill.stateDoing')
      }
      if (row.feeFlag !== '2006012') {
        return this.$t('roomFeeRuleBill.stateStopped')
      }
      if (row.unpaidCount > 0) {
        return this.$t('roomFeeRuleBill.stateOnceUnpaid')
      }
      return this.$t('roomFeeRuleBill.stateOnceSettled')
    },
    _stateTagType(row) {
      if (row.syntheticOther) {
        return 'info'
      }
      if (row.state === '2008001') {
        return this._isMeterRule(row) ? 'info' : 'success'
      }
      return 'info'
    },
    _billCountText(row) {
      const count = Number(row.billCount) > 0 ? Number(row.billCount) : 0
      return this.$t('roomFeeRuleBill.billViewCount', { count: count })
    },
    _billTotalCount(row) {
      if (row.syntheticOther) {
        return (row.bills || []).length
      }
      const paidCount = Number(row.paidCount) > 0 ? Number(row.paidCount) : 0
      const unpaidCount = Number(row.unpaidCount) > 0 ? Number(row.unpaidCount) : 0
      const cancelledCount = Number(row.cancelledCount) > 0 ? Number(row.cancelledCount) : 0
      return paidCount + unpaidCount + cancelledCount
    },
    _billEmptyText(row) {
      if (this._billTotalCount(row) < 1) {
        return this._isMeterRule(row)
          ? this.$t('roomFeeRuleBill.subEmptyNoBillMeter')
          : this.$t('roomFeeRuleBill.subEmptyNoBill')
      }
      if (row.billStateFilter === 'unpaid' && Number(row.paidCount) > 0) {
        return this.$t('roomFeeRuleBill.subEmptyPaid')
      }
      return this.$t('roomFeeRuleBill.billFilterEmpty')
    }
  }
}
</script>

<style scoped>
.owner-detail-room-fee {
  margin-top: 10px;
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.room-select {
  width: 200px;
  margin-right: 8px;
}

.fee-type-select {
  width: 130px;
  margin-right: 8px;
}

.fee-item-select {
  width: 150px;
  margin-right: 8px;
}

.state-select {
  width: 110px;
  margin-right: 8px;
}

.room-fee-loading {
  min-height: 120px;
}

.room-fee-empty {
  padding: 40px 0;
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.stat-owe {
  color: #f56c6c;
}

.text-meter {
  color: #8b5cf6;
}

.tag-meter {
  color: #8b5cf6;
  background: #f5f3ff;
  border-color: #ede9fe;
}

.cell-main {
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.cell-name {
  font-weight: 600;
  color: #1f2d3d;
}

.cell-sub {
  margin-top: 2px;
  font-size: 11px;
  color: #909399;
}

.cell-badge {
  margin-left: 6px;
}

.text-muted {
  color: #c0c4cc;
}

.bill-remark {
  font-size: 12px;
  line-height: 1.5;
  color: #606266;
  white-space: pre-line;
}

.tag-state {
  border-radius: 10px;
}

.tag-state::before {
  content: '';
  display: inline-block;
  width: 5px;
  height: 5px;
  margin-right: 4px;
  vertical-align: middle;
  background: currentColor;
  border-radius: 50%;
}

.rule-table ::v-deep tbody tr {
  cursor: pointer;
}

.rule-table ::v-deep th.el-table__cell {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f8fafc;
}

.rule-table ::v-deep td.el-table__cell {
  font-size: 13px;
  border-bottom: 1px solid #f1f5f9;
}

.rule-table ::v-deep .el-table__expanded-cell {
  padding: 10px 12px 12px;
  background: #f8fafc;
}

.bill-table ::v-deep th.el-table__cell {
  font-size: 11px;
  font-weight: 400;
  color: #909399;
  background: #fff;
}

.bill-table ::v-deep td.el-table__cell {
  font-size: 12px;
  border-bottom: 1px solid #f7f8fa;
}

.bill-panel {
  overflow: hidden;
  margin-left: 40px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}

.bill-panel-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  padding: 8px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
}

.bill-panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.bill-panel-count {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.bill-state-filter {
  width: 110px;
  margin-left: auto;
}

.bill-panel-empty {
  padding: 16px 0;
  text-align: center;
  font-size: 12px;
  color: #909399;
}

.bill-panel-body {
  min-height: 60px;
}

.footer-bar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 12px;
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  border-top: 1px solid #ebeef5;
}

.footer-stat {
  display: flex;
  align-items: baseline;
  margin-right: 18px;
}

.footer-stat-label {
  margin-right: 6px;
  color: #909399;
}

.footer-stat-value {
  font-weight: 600;
  color: #303133;
}

.footer-stat-owe .footer-stat-value {
  color: #f56c6c;
}

.footer-total {
  margin-right: 8px;
}
</style>
