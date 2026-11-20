<template>
  <div class="contract-room-fee">
    <div class="filter-row">
      <el-select v-model="selectedRoomId" size="small" class="payer-obj-select" filterable clearable
        :placeholder="$t('contractRoomFee.selectRoom')" @change="handleRoomChange">
        <el-option v-for="item in rooms" :key="item.roomId" :label="_roomLabel(item)"
          :value="item.roomId"></el-option>
      </el-select>
      <el-select :value="ruleBillInfo.feeTypeCd" size="small" class="fee-type-select"
        :placeholder="$t('simplifyRoomFee.selectFeeType')" @input="handleFeeTypeChange">
        <el-option :label="$t('roomFeeRuleBill.allFeeType')" value=""></el-option>
        <template v-for="item in ruleBillInfo.feeTypeCds">
          <el-option v-if="item.statusCd != '888800010008'" :key="item.statusCd" :label="item.name"
            :value="item.statusCd"></el-option>
        </template>
      </el-select>
      <el-select :value="ruleBillInfo.configId" size="small" class="fee-item-select" filterable
        :placeholder="$t('simplifyRoomFee.selectFeeItem')" @input="handleConfigChange">
        <el-option :label="$t('roomFeeRuleBill.allFeeItem')" value=""></el-option>
        <el-option v-for="item in ruleBillInfo.feeConfigs" :key="item.configId" :label="item.feeName"
          :value="item.configId"></el-option>
      </el-select>
      <el-select :value="ruleBillInfo.stateFilter" size="small" class="state-select" @input="handleStateChange">
        <el-option :label="$t('roomFeeRuleBill.allState')" value=""></el-option>
        <el-option :label="$t('roomFeeRuleBill.stateDoing')" value="2008001"></el-option>
        <el-option :label="$t('roomFeeRuleBill.stateStopped')" value="2009001"></el-option>
      </el-select>
      <el-button type="primary" size="small" @click="handleQuery">{{ $t('common.query') }}</el-button>
      <span class="tool-buttons">
        <el-button v-if="hasPrivilege('502020082314267912')" type="primary" size="small" @click="_payFee">
          {{ $t('contractRoomFee.payment') }}
        </el-button>
      </span>
    </div>

    <div v-if="rooms.length === 0" class="rule-cards">
      <div class="rule-cards-empty">{{ $t('contractRoomFee.noRoom') }}</div>
    </div>

    <div v-else v-loading="ruleBillInfo.loading" class="rule-cards">
      <div v-if="!ruleBillInfo.pageRows || ruleBillInfo.pageRows.length === 0" class="rule-cards-empty">
        {{ _emptyText() }}
      </div>

      <div v-for="(row, index) in ruleBillInfo.pageRows" :key="row.ruleId || ('rule-' + index)"
        :class="['rule-card', { 'is-expanded': row.expanded }]">
        <div class="rule-card-head" @click="_toggleRule(row)">
          <div class="head-left">
            <i :class="['chevron el-icon-arrow-down', { 'is-open': row.expanded }]"></i>
            <div class="title-area">
              <div class="card-title">{{ _ruleDisplayName(row) }}</div>
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
            <div class="bill-row bill-row-head no-action">
              <div>{{ $t('roomFeeRuleBill.billYearMonth') }}</div>
              <div>{{ $t('roomFeeRuleBill.billPeriod') }}</div>
              <div class="align-right">{{ $t('roomFeeRuleBill.billUnpaidAmount') }}</div>
              <div>{{ $t('roomFeeRuleBill.billState') }}</div>
              <div>{{ $t('roomFeeRuleBill.subColRemark') }}</div>
            </div>
            <div v-for="bill in row.filteredBills" :key="bill.feeId" class="bill-row no-action">
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

      <div v-if="ruleBillInfo.hiddenSettledCount > 0" class="settled-row">
        <i class="el-icon-info settled-icon"></i>
        <span class="settled-text">
          {{ ruleBillInfo.showSettled
            ? $t('roomFeeRuleBill.shownTip', { count: ruleBillInfo.hiddenSettledCount })
            : $t('roomFeeRuleBill.hiddenTip', { count: ruleBillInfo.hiddenSettledCount }) }}
        </span>
        <a class="settled-link" @click="handleToggleSettled">
          {{ ruleBillInfo.showSettled ? $t('roomFeeRuleBill.hide') : $t('roomFeeRuleBill.show') }}
        </a>
      </div>
    </div>

    <div class="footer-bar">
      <span class="footer-stat footer-stat-owe">
        <span class="footer-stat-label">{{ $t('roomFeeRuleBill.summaryOwe') }}</span>
        <span class="footer-stat-value">¥{{ _statOwe() }}</span>
      </span>
      <span class="footer-total">{{ $t('roomFeeRuleBill.totalCount', { count: ruleBillInfo.filteredCount }) }}</span>
      <el-pagination small layout="prev, pager, next" :current-page="ruleBillInfo.currentPage"
        :page-size="ruleBillInfo.pageSize" :total="ruleBillInfo.filteredCount"
        @current-change="handlePageChange"></el-pagination>
    </div>
  </div>
</template>

<script>
import feeRuleBillMixin from '@/mixins/feeRuleBillMixin'
import { queryContractRoom } from '@/api/contract/contractDetailRoomApi'

export default {
  name: 'ContractRoomFee',
  mixins: [feeRuleBillMixin],
  data() {
    return {
      contractId: '',
      rooms: [],
      selectedRoomId: '',
      feeRuleBillConfig: {
        payerObjType: '3333',
        emptyTextKey: 'roomFeeRuleBill.emptyPayerObj',
        selectTipKey: 'contractRoomFee.selectRoom'
      }
    }
  },
  methods: {
    open(params) {
      const data = params || {}
      this.contractId = data.contractId || ''
      this.rooms = []
      this.selectedRoomId = ''
      this.initFeeRuleBill({})
      this._loadRooms()
    },
    _loadRooms() {
      if (!this.contractId) {
        return
      }
      queryContractRoom({
        contractId: this.contractId,
        payerObjType: '3333',
        page: 1,
        row: 200
      }).then(res => {
        this.rooms = res.data || []
      }).catch(error => {
        this.rooms = []
        this.$message.error(error.message)
      })
    },
    _roomLabel(row) {
      if (!row) {
        return ''
      }
      const roomName = row.roomName || ''
      return row.ownerName ? roomName + '（' + row.ownerName + '）' : roomName
    },
    handleRoomChange(roomId) {
      if (!roomId) {
        this.initFeeRuleBill({})
        return
      }
      const room = this.rooms.find(item => item.roomId === roomId)
      if (!room) {
        this.initFeeRuleBill({})
        return
      }
      this.initFeeRuleBill({
        payerObjId: room.roomId,
        payerObjName: room.roomName,
        ownerId: room.ownerId,
        ownerName: room.ownerName
      })
    },
    _payFee() {
      if (!this.ruleBillInfo.roomInfo.roomId) {
        this.$message.warning(this.$t('contractRoomFee.selectRoom'))
        return
      }
      this._toBatchPayFee()
    }
  }
}
</script>

<style lang="scss" scoped src="@/styles/feeRuleBill.scss"></style>
