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

    <div v-if="roomFeeInfo.roomsLoading" v-loading="true" class="rule-cards-loading"></div>
    <div v-else-if="roomFeeInfo.rooms.length < 1" class="rule-cards-empty">
      {{ $t('ownerDetailRoomFeeRule.noRoom') }}
    </div>
    <template >
      <div v-loading="roomFeeInfo.loading && roomFeeInfo.pageRows.length === 0" class="rule-cards">
        <div v-if="!roomFeeInfo.pageRows || roomFeeInfo.pageRows.length === 0" class="rule-cards-empty">
          {{ _emptyText() }}
        </div>

        <div v-for="(row, index) in roomFeeInfo.pageRows" :key="row.ruleId || ('rule-' + index)"
          :class="['rule-card', { 'is-expanded': row.expanded }]">
          <div class="rule-card-head" @click="_toggleRule(row)">
            <div class="head-left">
              <i :class="['chevron el-icon-arrow-down', { 'is-open': row.expanded }]"></i>
              <div class="title-area">
                <div class="card-title">
                  {{ _ruleDisplayName(row) }}
                  <el-tag v-if="row.risk" size="mini" type="warning" class="cell-badge">
                    {{ $t('roomFeeRuleBill.cancelledBadge') }}
                  </el-tag>
                </div>
                <div class="card-subtitle">
                  {{ row.feeTypeCdName || '-' }} · {{ row.computingFormulaName || '-' }}
                </div>
              </div>
              <div class="meta-info">
                <div class="meta-item">
                  <span>{{ $t('roomFeeRuleBill.colNextBill') }}</span>{{ _nextBillText(row) }}
                </div>
                <div v-if="_rulePriceText(row)" class="meta-item">
                  {{ _rulePriceText(row) }}
                </div>
                <div class="meta-item">
                  <span>{{ $t('roomFeeRuleBill.colRule') }}</span>
                  <span class="meta-rule-value">
                    <span>{{ _ruleCycleText(row) }}</span>
                    <span v-if="_rulePeriodText(row)" class="meta-sub">{{ _rulePeriodText(row) }}</span>
                  </span>
                </div>
              </div>
            </div>

            <div class="head-right">
              <div class="amount-block">
                <div :class="['amount-value', Number(row.oweAmount) > 0 ? 'is-owe' : 'is-zero']">
                  ¥{{ _money(row.oweAmount) }}
                </div>
                <div class="amount-desc">
                  {{ Number(row.unpaidCount) > 0
                    ? $t('roomFeeRuleBill.periods', { count: row.unpaidCount })
                    : $t('roomFeeRuleBill.noOwe') }}
                </div>
              </div>
              <div class="status-block">
                <span :class="['status-pill', 'tone-' + _stateTone(row)]">{{ _stateText(row) }}</span>
                <div class="link-group" @click.stop>
                  <el-select :value="row.billStateFilter" size="mini" class="bill-card-filter"
                    @input="_switchBillFilter(row, $event)">
                    <el-option :label="$t('roomFeeRuleBill.tabAllBills')" value="all"></el-option>
                    <el-option :label="$t('roomFeeRuleBill.tabUnpaidBills')" value="unpaid"></el-option>
                    <el-option :label="$t('roomFeeRuleBill.tabPaidBills')" value="paid"></el-option>
                  </el-select>
                </div>
              </div>
            </div>
          </div>

          <div v-show="row.expanded" v-loading="row.billsLoading" class="rule-card-body">
            <div v-if="!row.filteredBills || row.filteredBills.length === 0" class="bill-panel-empty">
              {{ _billEmptyText(row) }}
            </div>
            <div v-else class="bill-list">
              <div class="bill-row bill-row-head">
                <div>{{ $t('roomFeeRuleBill.billYearMonth') }}</div>
                <div>{{ $t('roomFeeRuleBill.billPeriod') }}</div>
                <div class="align-right">{{ $t('roomFeeRuleBill.billUnpaidAmount') }}</div>
                <div>{{ $t('roomFeeRuleBill.billState') }}</div>
                <div>{{ $t('roomFeeRuleBill.subColRemark') }}</div>
              </div>
              <div v-for="bill in row.filteredBills" :key="bill.feeId" class="bill-row">
                <div class="col-month">{{ bill.yearMonth || '—' }}</div>
                <div class="col-period">
                  {{ _dateOnly(bill.startTime) }} ~ {{ _dateOnly(bill.endTime) }}
                </div>
                <div :class="['col-amount', Number(bill.oweAmount) > 0 ? 'is-owe' : 'is-muted']">
                  ¥{{ _money(bill.oweAmount) }}
                </div>
                <div>
                  <span :class="['status-pill', 'tone-' + _billTone(bill)]">{{ _billStateText(bill) }}</span>
                </div>
                <div class="col-desc">
                  <span v-if="bill.remark">{{ bill.remark }}</span>
                  <span v-if="bill.feeRemark">{{ bill.feeRemark }}</span>
                  <span v-if="!bill.remark && !bill.feeRemark">—</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

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
    </template>
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
        roomsLoading: true,
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
      info.roomsLoading = true
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
      this.$set(rule, 'expanded', false)
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
        this.$set(rule, 'expanded', false)
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
    _toggleRule(rule) {
      if (!rule) {
        return
      }
      if (rule.expanded) {
        this.$set(rule, 'expanded', false)
        return
      }
      this.$set(rule, 'expanded', true)
      this._loadRuleBills(rule, rule.billStateFilter || 'unpaid')
    },
    _switchBillFilter(rule, state) {
      if (!rule) {
        return
      }
      this._loadRuleBills(rule, state)
      this.$set(rule, 'expanded', true)
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
    _rulePeriodText(row) {
      if (row.syntheticOther || !row.startTime || !row.maxTime) {
        return ''
      }
      return this._dateOnly(row.startTime) + ' ~ ' + this._dateOnly(row.maxTime)
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
    _stateTone(row) {
      if (row.syntheticOther) {
        return 'muted'
      }
      if (row.state === '2008001') {
        return this._isMeterRule(row) ? 'meter' : 'success'
      }
      if (Number(row.unpaidCount) > 0) {
        return 'danger'
      }
      return 'muted'
    },
    _nextBillText(row) {
      if (row.syntheticOther || row.state !== '2008001') {
        return '—'
      }
      if (this._isMeterRule(row)) {
        return this.$t('roomFeeRuleBill.nextBillMeter')
      }
      return this._dateOnly(row.curYearMonth)
    },
    _billTone(bill) {
      if (bill.billState === 'paid') {
        return 'success'
      }
      if (bill.billState === 'cancelled') {
        return 'muted'
      }
      return 'danger'
    },
    _billStateText(bill) {
      if (bill.billState === 'paid') {
        return this.$t('roomFeeRuleBill.billPaid')
      }
      if (bill.billState === 'cancelled') {
        return this.$t('roomFeeRuleBill.billCancelled')
      }
      return this.$t('roomFeeRuleBill.billUnpaid')
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

<style lang="scss" scoped src="@/styles/feeRuleBill.scss"></style>

<style scoped>
.owner-detail-room-fee {
  min-height: calc(100vh - 420px);
  margin-top: 10px;
}

.room-select {
  width: 200px;
  margin-right: 8px;
}

.rule-cards-loading {
  min-height: 280px;
}

.owner-detail-room-fee .bill-list .bill-row {
  grid-template-columns: 100px 200px 110px 90px minmax(160px, 1fr);
  min-width: 660px;
}
</style>
