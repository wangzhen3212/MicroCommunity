<template>
  <div class="simplify-parking-space-fee">
    <div class="filter-row">
      <el-tag v-if="ruleBillInfo.riskOnly" size="mini" type="warning" closable class="risk-filter-tag"
        @close="handleRiskClear">
        {{ $t('roomFeeRuleBill.riskOnlyTag', { count: ruleBillInfo.riskCount }) }}
      </el-tag>
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
        <el-button v-if="hasPrivilege('502020082314267912')" type="primary" size="small" @click="_toBatchPayFee">
          <i class="el-icon-plus"></i>
          {{ $t('simplifyRoomFee.payment') }}
        </el-button>
        <el-button v-if="hasPrivilege('502020082314267912')" type="primary" size="small" @click="_toPrePayFee">
          <i class="el-icon-plus"></i>
          {{ $t('simplifyRoomFee.prePayFee') }}
        </el-button>
        <el-button v-if="hasPrivilege('502022073096990007')" type="primary" size="small" @click="_openCreateFee">
          <i class="el-icon-plus"></i>
          {{ $t('simplifyRoomFee.createFee') }}
        </el-button>
        <el-button v-if="hasPrivilege('502022073007100003')" type="primary" size="small" @click="_openTempRoomFee">
          <i class="el-icon-plus"></i>
          {{ $t('simplifyRoomFee.tempFee') }}
        </el-button>
      </span>
    </div>

    <div v-if="_isShowRisk()" class="risk-alert">
      <div class="risk-icon"><i class="el-icon-warning"></i></div>
      <div class="risk-body">
        <div class="risk-title">{{ $t('roomFeeRuleBill.riskTitle', { count: ruleBillInfo.riskCount }) }}</div>
        <div class="risk-desc">{{ $t('roomFeeRuleBill.riskDesc') }}</div>
        <div class="risk-actions">
          <el-button type="warning" size="mini" @click="handleRiskView">{{ $t('roomFeeRuleBill.riskAction') }}</el-button>
          <el-button size="mini" @click="handleRiskIgnore">{{ $t('roomFeeRuleBill.riskIgnore') }}</el-button>
        </div>
      </div>
    </div>

    <div v-loading="ruleBillInfo.loading" class="rule-cards">
      <div v-if="!ruleBillInfo.pageRows || ruleBillInfo.pageRows.length === 0" class="rule-cards-empty">
        {{ _emptyText() }}
      </div>

      <div v-for="(row, index) in ruleBillInfo.pageRows" :key="row.ruleId || ('rule-' + index)"
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
                <a v-if="row.state === '2008001' && hasPrivilege('502020082314267912')"
                  @click="_preGenerateFee(row)">
                  {{ $t('roomFeeRuleBill.btnPreGenerate') }}
                </a>
                <a v-if="!row.syntheticOther && hasPrivilege('502020090427190001')" @click="_editRule(row)">
                  {{ $t('roomFeeRuleBill.menuEditRule') }}
                </a>
                <el-dropdown v-if="!row.syntheticOther" trigger="click">
                  <span class="more-link">
                    {{ $t('roomFeeRuleBill.moreOps') }}<i class="el-icon-arrow-down"></i>
                  </span>
                  <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item v-if="row.state === '2008001' && hasPrivilege('502021070488970005')"
                      @click.native="_finishRule(row)">
                      {{ $t('roomFeeRuleBill.menuFinishRule') }}
                    </el-dropdown-item>
                    <el-dropdown-item v-if="hasPrivilege('502020090604200029')" @click.native="_deleteRule(row)">
                      {{ $t('roomFeeRuleBill.menuCancel') }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
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
              <div class="align-right">{{ $t('common.operation') }}</div>
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
              <div class="col-action">
                <el-button type="text" size="mini" @click="_feeHis(bill)">
                  {{ $t('roomFeeRuleBill.payHistory') }}
                </el-button>
                <el-button type="text" size="mini" @click="_feeHis(bill)">
                  {{ $t('roomFeeRuleBill.menuDetail') }}
                </el-button>
                <el-dropdown v-if="bill.billState === 'unpaid'" trigger="click">
                  <span class="more-link">
                    {{ $t('roomFeeRuleBill.moreOps') }}<i class="el-icon-arrow-down"></i>
                  </span>
                  <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item v-if="hasPrivilege('502020090604200029')" @click.native="_cancelBill(bill)">
                      {{ $t('roomFeeRuleBill.cancelThisBill') }}
                    </el-dropdown-item>
                    <el-dropdown-item v-if="hasPrivilege('502020090427190001')" @click.native="_editFee(bill)">
                      {{ $t('roomFeeRuleBill.menuFeeChange') }}
                    </el-dropdown-item>
                    <el-dropdown-item v-if="hasPrivilege('502020090427190001')" @click.native="_splitFee(bill)">
                      {{ $t('roomFeeRuleBill.menuSplitFee') }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
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

    <delete-fee ref="deleteFee" @success="handleBillChanged"></delete-fee>
    <finish-fee-rule ref="finishFeeRule" @success="handleBillChanged"></finish-fee-rule>
    <delete-fee-rule ref="deleteFeeRule" @success="handleBillChanged"></delete-fee-rule>
    <parking-space-create-fee-add ref="parkingSpaceCreateFeeAdd" @success="handleBillChanged"></parking-space-create-fee-add>
    <temp-import-parking-space-fee ref="tempImportParkingSpaceFee" @success="handleBillChanged"></temp-import-parking-space-fee>
    <edit-fee-rule ref="editFeeRule" @success="handleBillChanged"></edit-fee-rule>
    <edit-fee ref="editFee" @success="handleBillChanged"></edit-fee>
    <split-fee ref="splitFee" @success="handleBillChanged"></split-fee>
    <pre-generate-fee ref="preGenerateFee" @success="handleBillChanged"></pre-generate-fee>
  </div>
</template>

<script>
import feeRuleBillMixin from '@/mixins/feeRuleBillMixin'
import DeleteFee from '@/components/fee/deleteFee'
import ParkingSpaceCreateFeeAdd from '@/components/fee/parkingSpaceCreateFeeAdd'
import TempImportParkingSpaceFee from '@/components/fee/tempImportParkingSpaceFee'
import EditFeeRule from '@/components/fee/editFeeRule'
import FinishFeeRule from '@/components/fee/finishFeeRule'
import DeleteFeeRule from '@/components/fee/deleteFeeRule'
import EditFee from '@/components/fee/editFee'
import SplitFee from '@/components/fee/splitFee'
import PreGenerateFee from '@/components/fee/preGenerateFee'

export default {
  name: 'SimplifyParkingSpaceFee',
  components: {
    DeleteFee,
    ParkingSpaceCreateFeeAdd,
    TempImportParkingSpaceFee,
    EditFeeRule,
    FinishFeeRule,
    DeleteFeeRule,
    EditFee,
    SplitFee,
    PreGenerateFee
  },
  mixins: [feeRuleBillMixin],
  data() {
    return {
      feeRuleBillConfig: {
        payerObjType: '6666',
        emptyTextKey: 'roomFeeRuleBill.emptyPayerObj',
        selectTipKey: 'roomFeeRuleBill.selectPayerObjFirst'
      }
    }
  },
  methods: {
    open(params) {
      const data = params || {}
      this.initFeeRuleBill({
        payerObjId: data.payerObjId || data.psId || '',
        payerObjName: data.payerObjName || data.psName || data.roomName || '',
        ownerId: data.ownerId || '',
        ownerName: data.ownerName || '',
        roomSubTypeName: data.roomSubTypeName || '',
        stateName: data.stateName || '',
        link: data.link || '',
        builtUpArea: data.builtUpArea || data.roomArea || ''
      })
    },
    _openCreateFee() {
      if (!this._checkPayerObj()) {
        return
      }
      if (this.$refs.parkingSpaceCreateFeeAdd) {
        this.$refs.parkingSpaceCreateFeeAdd.open({
          psId: this.ruleBillInfo.roomInfo.roomId,
          psName: this.ruleBillInfo.roomInfo.roomName,
          ownerId: this.ruleBillInfo.roomInfo.ownerId,
          ownerName: this.ruleBillInfo.roomInfo.ownerName
        })
      }
    },
    _openTempRoomFee() {
      if (!this._checkPayerObj()) {
        return
      }
      if (this.$refs.tempImportParkingSpaceFee) {
        this.$refs.tempImportParkingSpaceFee.open({
          psId: this.ruleBillInfo.roomInfo.roomId,
          psName: this.ruleBillInfo.roomInfo.roomName,
          ownerName: this.ruleBillInfo.roomInfo.ownerName
        })
      }
    }
  }
}
</script>

<style lang="scss" scoped src="@/styles/feeRuleBill.scss"></style>
