<template>
  <div class="simplify-contract-fee">
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

    <el-table ref="ruleTable" v-loading="ruleBillInfo.loading" :data="ruleBillInfo.pageRows" row-key="ruleId"
      class="rule-table" size="small" :empty-text="_emptyText()" @row-click="handleRowClick"
      @expand-change="handleExpandChange">
      <el-table-column type="expand">
        <template slot-scope="scope">
          <div class="bill-panel">
            <div class="bill-panel-head">
              <span class="bill-panel-title">{{ $t('roomFeeRuleBill.subTitle', { feeName: _billDisplayName(scope.row) }) }}</span>
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
                <el-table-column prop="yearMonth" :label="$t('roomFeeRuleBill.billYearMonth')" width="120">
                </el-table-column>
                <el-table-column :label="$t('roomFeeRuleBill.billPeriod')" width="220">
                  <template slot-scope="billScope">
                    {{ _dateOnly(billScope.row.startTime) }} ~ {{ _dateOnly(billScope.row.endTime) }}
                  </template>
                </el-table-column>
                <el-table-column :label="$t('roomFeeRuleBill.billUnpaidAmount')" align="right" width="130">
                  <template slot-scope="billScope">
                    <span :class="Number(billScope.row.oweAmount) > 0 ? 'stat-owe' : 'text-muted'">
                      ¥{{ _money(billScope.row.oweAmount) }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column :label="$t('roomFeeRuleBill.billState')" align="center" width="200">
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
                <el-table-column :label="$t('roomFeeRuleBill.subColRemark')" width="300">
                  <template slot-scope="billScope">
                    <div class="bill-remark">{{ billScope.row.remark || '—' }}</div>
                  </template>
                </el-table-column>
                <el-table-column :label="$t('roomFeeRuleBill.subColFeeRemark')" width="220">
                  <template slot-scope="billScope">
                    <div class="bill-remark">{{ billScope.row.feeRemark || '—' }}</div>
                  </template>
                </el-table-column>
                <el-table-column :label="$t('common.operation')" align="center" min-width="260">
                  <template slot-scope="billScope">
                    <div class="op-cell">
                      <el-button type="text" size="mini" @click="_feeHis(billScope.row)">
                        {{ $t('roomFeeRuleBill.payHistory') }}
                      </el-button>
                      <el-button type="text" size="mini" @click="_feeHis(billScope.row)">
                        {{ $t('roomFeeRuleBill.menuDetail') }}
                      </el-button>
                      <el-dropdown v-if="billScope.row.billState === 'unpaid'">
                        <span class="more-link">
                          {{ $t('roomFeeRuleBill.moreOps') }}<i class="el-icon-arrow-down"></i>
                        </span>
                        <el-dropdown-menu slot="dropdown">
                          <el-dropdown-item v-if="hasPrivilege('502020090604200029')"
                            @click.native="_cancelBill(billScope.row)">
                            {{ $t('roomFeeRuleBill.cancelThisBill') }}
                          </el-dropdown-item>
                          <el-dropdown-item v-if="hasPrivilege('502020090427190001')"
                            @click.native="_editFee(billScope.row)">
                            {{ $t('roomFeeRuleBill.menuFeeChange') }}
                          </el-dropdown-item>
                          <el-dropdown-item v-if="hasPrivilege('502020090427190001')"
                            @click.native="_splitFee(billScope.row)">
                            {{ $t('roomFeeRuleBill.menuSplitFee') }}
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </el-dropdown>
                    </div>
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
      <el-table-column :label="$t('common.operation')" align="center" width="280" column-key="operation">
        <template slot-scope="scope">
          <div class="op-cell">
            <el-button v-if="scope.row.state === '2008001' && hasPrivilege('502020082314267912')" type="text"
              size="mini" @click="_preGenerateFee(scope.row)">
              {{ $t('roomFeeRuleBill.btnPreGenerate') }}
            </el-button>
            <template v-if="!scope.row.syntheticOther">
              <el-button v-if="hasPrivilege('502020090427190001')" type="text" size="mini" @click="_editRule(scope.row)">
                {{ $t('roomFeeRuleBill.menuEditRule') }}
              </el-button>
              <el-button v-if="scope.row.state === '2008001' && hasPrivilege('502021070488970005')" type="text"
                size="mini" @click="_finishRule(scope.row)">
                {{ $t('roomFeeRuleBill.menuFinishRule') }}
              </el-button>
              <el-button v-if="hasPrivilege('502020090604200029')" type="text" size="mini" class="text-danger"
                @click="_deleteRule(scope.row)">
                {{ $t('roomFeeRuleBill.menuCancel') }}
              </el-button>
            </template>
          </div>
        </template>
      </el-table-column>
      <template slot="append">
        <div v-if="ruleBillInfo.hiddenSettledCount > 0" class="settled-row">
          <span class="settled-text">
            {{ ruleBillInfo.showSettled
              ? $t('roomFeeRuleBill.shownTip', { count: ruleBillInfo.hiddenSettledCount })
              : $t('roomFeeRuleBill.hiddenTip', { count: ruleBillInfo.hiddenSettledCount }) }}
          </span>
          <el-button type="text" size="mini" @click="handleToggleSettled">
            {{ ruleBillInfo.showSettled ? $t('roomFeeRuleBill.hide') : $t('roomFeeRuleBill.show') }}
          </el-button>
        </div>
      </template>
    </el-table>

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

    <simplify-room-fee-cancel-dialog ref="cancelFee" @success="handleBillChanged"></simplify-room-fee-cancel-dialog>
    <finish-fee-rule ref="finishFeeRule" @success="handleBillChanged"></finish-fee-rule>
    <delete-fee-rule ref="deleteFeeRule" @success="handleBillChanged"></delete-fee-rule>
    <contract-create-fee-add ref="contractCreateFeeAdd" @success="handleBillChanged"></contract-create-fee-add>
    <temp-import-room-fee ref="tempImportRoomFee" @success="handleBillChanged"></temp-import-room-fee>
    <edit-fee-rule ref="editFeeRule" @success="handleBillChanged"></edit-fee-rule>
    <edit-fee ref="editFee" @success="handleBillChanged"></edit-fee>
    <split-fee ref="splitFee" @success="handleBillChanged"></split-fee>
    <pre-generate-fee ref="preGenerateFee" @success="handleBillChanged"></pre-generate-fee>
  </div>
</template>

<script>
import { listFeeConfigs } from '@/api/fee/simplifyRoomFeeApi'
import { getDict, getCommunityId } from '@/api/community/communityApi'
import { listRoomFeeRuleWithBill, listRuleBills } from '@/api/fee/roomFeeRuleBillApi'
import { queryPreGenerateFeePeriods, preGenerateFee as preGenerateFeeApi } from '@/api/fee/preGenerateFeeApi'
import { EXCLUDE_COMPUTING_FORMULAS } from '@/utils/feeUtil'
import SimplifyRoomFeeCancelDialog from '@/components/fee/simplifyRoomFeeCancelDialog'
import ContractCreateFeeAdd from '@/components/fee/contractCreateFeeAdd'
import TempImportRoomFee from '@/components/fee/tempImportRoomFee'
import EditFeeRule from '@/components/fee/editFeeRule'
import FinishFeeRule from '@/components/fee/finishFeeRule'
import DeleteFeeRule from '@/components/fee/deleteFeeRule'
import EditFee from '@/components/fee/editFee'
import SplitFee from '@/components/fee/splitFee'
import PreGenerateFee from '@/components/fee/preGenerateFee'

export default {
  name: 'SimplifyContractFee',
  components: {
    SimplifyRoomFeeCancelDialog,
    ContractCreateFeeAdd,
    TempImportRoomFee,
    EditFeeRule,
    FinishFeeRule,
    DeleteFeeRule,
    EditFee,
    SplitFee,
    PreGenerateFee
  },
  data() {
    return {
      ruleBillInfo: {
        roomInfo: {
          roomId: '',
          roomName: '',
          ownerId: '',
          ownerName: '',
          roomSubTypeName: '',
          stateName: '',
          link: '',
          builtUpArea: ''
        },
        payerObjType: '7777',
        loading: false,
        rules: [],
        feeTypeCds: [],
        feeTypeCd: '',
        configId: '',
        feeConfigs: [],
        stateFilter: '',
        showSettled: false,
        riskIgnored: false,
        riskOnly: false,
        currentPage: 1,
        pageSize: 20,
        filteredCount: 0,
        pageRows: [],
        riskRules: [],
        riskCount: 0,
        hiddenSettledRules: [],
        hiddenSettledCount: 0,
        pendingAutoExpand: false
      }
    }
  },
  methods: {
    open(params) {
      const data = params || {}
      this.ruleBillInfo.payerObjType = '7777'
      this.ruleBillInfo.roomInfo = {
        roomId: data.payerObjId || data.contractId || '',
        roomName: data.payerObjName || data.contractCode || data.contractName || data.roomName || '',
        ownerId: data.ownerId || '',
        ownerName: data.ownerName || '',
        roomSubTypeName: data.roomSubTypeName || '',
        stateName: data.stateName || '',
        link: data.link || '',
        builtUpArea: data.builtUpArea || data.roomArea || ''
      }
      this.ruleBillInfo.stateFilter = ''
      this.ruleBillInfo.feeTypeCd = ''
      this.ruleBillInfo.configId = ''
      this.ruleBillInfo.showSettled = false
      this.ruleBillInfo.riskIgnored = false
      this.ruleBillInfo.riskOnly = false
      this.ruleBillInfo.currentPage = 1
      this.ruleBillInfo.rules = []
      this.ruleBillInfo.pendingAutoExpand = true
      this._refreshPageData()
      this.loadData()
      this.loadFeeTypeCds()
      this.loadFeeConfigs('')
    },
    loadData() {
      const info = this.ruleBillInfo
      if (!info.roomInfo.roomId) {
        info.rules = []
        info.pendingAutoExpand = false
        this._refreshPageData()
        return
      }
      info.loading = true
      this._clearBillCache()
      listRoomFeeRuleWithBill({
        communityId: getCommunityId(),
        payerObjId: info.roomInfo.roomId,
        payerObjType: info.payerObjType
      }).then(res => {
        const rules = res.data || []
        rules.forEach(rule => {
          this._initRuleBills(rule)
        })
        info.rules = rules
        info.loading = false
        this._refreshPageData()
        this.$nextTick(() => {
          this._expandFirstRule()
        })
      }).catch(error => {
        info.rules = []
        info.loading = false
        info.pendingAutoExpand = false
        this._refreshPageData()
        this.$message.error(error.message || this.$t('roomFeeRuleBill.loadFailed'))
      })
    },
    loadFeeTypeCds() {
      getDict('pay_fee_config', 'fee_type_cd').then(data => {
        this.ruleBillInfo.feeTypeCds = data || []
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
        this.ruleBillInfo.feeConfigs = res.feeConfigs || []
      }).catch(error => {
        console.error('Failed to fetch fee configs:', error)
      })
    },
    handleFeeTypeChange(feeTypeCd) {
      this.ruleBillInfo.feeTypeCd = feeTypeCd
      this.ruleBillInfo.configId = ''
      this.loadFeeConfigs(feeTypeCd)
      this.handleQuery()
    },
    handleBillChanged() {
      this.ruleBillInfo.pendingAutoExpand = true
      this.loadData()
    },
    handleConfigChange(configId) {
      this.ruleBillInfo.configId = configId
      this.handleQuery()
    },
    handleStateChange(state) {
      this.ruleBillInfo.stateFilter = state
      this.handleQuery()
    },
    _buildFilteredRules() {
      const info = this.ruleBillInfo
      return info.rules.filter(item => {
        if (info.riskOnly && !item.risk) {
          return false
        }
        if (item.settled && !info.showSettled) {
          return false
        }
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
    _buildRiskRules() {
      return this.ruleBillInfo.rules.filter(item => item.risk)
    },
    _buildHiddenSettledList() {
      return this.ruleBillInfo.rules.filter(item => item.settled)
    },
    _buildPageRows(filteredRules) {
      const info = this.ruleBillInfo
      const start = (info.currentPage - 1) * info.pageSize
      return filteredRules.slice(start, start + info.pageSize)
    },
    _refreshPageData() {
      const info = this.ruleBillInfo
      const filteredRules = this._buildFilteredRules()
      info.filteredCount = filteredRules.length
      const maxPage = Math.max(1, Math.ceil(info.filteredCount / info.pageSize))
      if (info.currentPage > maxPage) {
        info.currentPage = maxPage
      }
      info.pageRows = this._buildPageRows(filteredRules)
      info.riskRules = this._buildRiskRules()
      info.riskCount = info.riskRules.length
      info.hiddenSettledRules = this._buildHiddenSettledList()
      info.hiddenSettledCount = info.hiddenSettledRules.length
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
      const rules = this.ruleBillInfo.rules || []
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
      // 无计费设置ID的账单行（rule_id 为空）无法按 ruleId 查询，退回本地过滤
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
    _expandFirstRule() {
      const info = this.ruleBillInfo
      if (!info.pendingAutoExpand) {
        return
      }
      info.pendingAutoExpand = false
      if (!this.$refs.ruleTable) {
        return
      }
      const rows = info.pageRows || []
      if (rows.length < 1) {
        return
      }
      this.$refs.ruleTable.toggleRowExpansion(rows[0], true)
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
    _isShowRisk() {
      return this.ruleBillInfo.riskCount > 0 && !this.ruleBillInfo.riskIgnored
    },
    _emptyText() {
      if (this.ruleBillInfo.roomInfo.roomId) {
        return this.$t('roomFeeRuleBill.emptyPayerObj')
      }
      return this.$t('roomFeeRuleBill.selectPayerObjFirst')
    },
    _statOwe() {
      let total = 0
      this.ruleBillInfo.rules.forEach(item => {
        const amount = parseFloat(item.oweAmount)
        if (!isNaN(amount)) {
          total += amount
        }
      })
      return total.toFixed(2)
    },
    handleQuery() {
      this.ruleBillInfo.currentPage = 1
      this._refreshPageData()
    },
    handlePageChange(page) {
      this.ruleBillInfo.currentPage = page
      this._refreshPageData()
    },
    handleToggleSettled() {
      const info = this.ruleBillInfo
      info.showSettled = !info.showSettled
      info.currentPage = 1
      this._refreshPageData()
    },
    handleRiskView() {
      const info = this.ruleBillInfo
      info.stateFilter = ''
      info.showSettled = false
      info.riskOnly = true
      info.currentPage = 1
      this._refreshPageData()
      this.$nextTick(() => {
        if (!this.$refs.ruleTable) {
          return
        }
        info.pageRows.forEach(row => {
          if (row.risk) {
            this.$refs.ruleTable.toggleRowExpansion(row, true)
          }
        })
      })
    },
    handleRiskIgnore() {
      this.ruleBillInfo.riskIgnored = true
    },
    handleRiskClear() {
      this.ruleBillInfo.riskOnly = false
      this.ruleBillInfo.currentPage = 1
      this._refreshPageData()
    },
    handleRowClick(row, column) {
      if (column && column.columnKey === 'operation') {
        return
      }
      if (!this.$refs.ruleTable) {
        return
      }
      this.$refs.ruleTable.toggleRowExpansion(row)
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
    },
    _findRuleByBill(bill) {
      const rules = this.ruleBillInfo.rules
      for (let i = 0; i < rules.length; i++) {
        const bills = rules[i].bills || []
        for (let j = 0; j < bills.length; j++) {
          if (bills[j].feeId === bill.feeId) {
            return rules[i]
          }
        }
      }
      return null
    },
    _checkRoom() {
      if (!this.ruleBillInfo.roomInfo.roomId) {
        this.$message.warning(this.$t('roomFeeRuleBill.selectPayerObjFirst'))
        return false
      }
      return true
    },
    _toBatchPayFee() {
      if (!this.ruleBillInfo.roomInfo.ownerId) {
        this.$message.warning(this.$t('roomFeeRuleBill.selectPayerObjFirst'))
        return
      }
      this.$router.push({
        path: '/views/fee/batchPayFeeOrder',
        query: {
          ownerId: this.ruleBillInfo.roomInfo.ownerId,
          payerObjType: this.ruleBillInfo.payerObjType
        }
      })
    },
    _toPrePayFee() {
      if (!this._checkRoom()) {
        return
      }
      this.$router.push({
        path: '/views/fee/prePayFeeOrder',
        query: {
          payerObjId: this.ruleBillInfo.roomInfo.roomId,
          payerObjType: this.ruleBillInfo.payerObjType
        }
      })
    },
    _openCreateFee() {
      if (!this._checkRoom()) {
        return
      }
      if (this.$refs.contractCreateFeeAdd) {
        this.$refs.contractCreateFeeAdd.open({
          contractId: this.ruleBillInfo.roomInfo.roomId,
          contractCode: this.ruleBillInfo.roomInfo.roomName
        })
      }
    },
    _openTempRoomFee() {
      if (!this._checkRoom()) {
        return
      }
      if (this.$refs.tempImportRoomFee) {
        this.$refs.tempImportRoomFee.open({
          roomId: this.ruleBillInfo.roomInfo.roomId,
          roomName: this.ruleBillInfo.roomInfo.roomName,
          ownerName: this.ruleBillInfo.roomInfo.ownerName,
          objType: this.ruleBillInfo.payerObjType
        })
      }
    },
    _feeHis(bill) {
      if (!bill) {
        return
      }
      this.$router.push({
        path: '/views/fee/feeDetail',
        query: { feeId: bill.feeId }
      })
    },
    _cancelBill(bill) {
      if (!bill || !this.$refs.cancelFee) {
        return
      }
      this.$refs.cancelFee.open({
        rule: this._findRuleByBill(bill) || {},
        bill: bill
      })
    },
    _finishRule(rule) {
      if (this.$refs.finishFeeRule) {
        this.$refs.finishFeeRule.open({
          ruleId: rule.ruleId,
          communityId: getCommunityId()
        })
      }
    },
    _deleteRule(rule) {
      if (this.$refs.deleteFeeRule) {
        this.$refs.deleteFeeRule.open({
          ruleId: rule.ruleId,
          communityId: getCommunityId()
        })
      }
    },
    _editRule(rule) {
      if (this.$refs.editFeeRule) {
        this.$refs.editFeeRule.open(rule)
      }
    },
    _editFee(bill) {
      if (!bill) {
        return
      }
      if (this.$refs.editFee) {
        this.$refs.editFee.open(bill)
      }
    },
    _splitFee(bill) {
      if (!bill) {
        return
      }
      if (this.$refs.splitFee) {
        this.$refs.splitFee.open(bill)
      }
    },
    _preGenerateFee(rule) {
      const communityId = getCommunityId()
      queryPreGenerateFeePeriods({
        communityId: communityId,
        ruleId: rule.ruleId
      }).then(res => {
        const data = res.data
        if (!data) {
          this.$message.warning(this.$t('simplifyRoomFee.noAvailablePeriod'))
          return
        }
        if (data.feeFlag === '2006012') {
          this.$confirm(this.$t('simplifyRoomFee.preGenerateConfirmTip'), this.$t('simplifyRoomFee.preGenerate'), {
            confirmButtonText: this.$t('common.confirm'),
            cancelButtonText: this.$t('common.cancel'),
            type: 'warning'
          }).then(() => {
            preGenerateFeeApi({
              communityId: communityId,
              ruleId: rule.ruleId,
              periods: []
            }).then(response => {
              this.$message.success(this.$t('simplifyRoomFee.preGenerateSuccess', { count: response.data }))
              this.loadData()
            }).catch(error => {
              this.$message.error(error.message || this.$t('simplifyRoomFee.preGenerateFailed'))
            })
          }).catch(() => {})
          return
        }
        if (!data.periods || data.periods.length < 1) {
          this.$message.warning(this.$t('simplifyRoomFee.noAvailablePeriod'))
          return
        }
        if (this.$refs.preGenerateFee) {
          this.$refs.preGenerateFee.open({
            communityId: communityId,
            ruleId: rule.ruleId,
            feeName: rule.feeName,
            endTime: data.endTime,
            periods: data.periods
          })
        }
      }).catch(error => {
        this.$message.error(error.message || this.$t('simplifyRoomFee.preGenerateFailed'))
      })
    }
  }
}
</script>

<style scoped>
.simplify-contract-fee {
  margin-top: 10px;
}

.filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.risk-filter-tag {
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

.tool-buttons {
  margin-left: auto;
}

.risk-alert {
  display: flex;
  align-items: flex-start;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 4px;
}

.risk-icon {
  margin-right: 8px;
  font-size: 16px;
  line-height: 20px;
  color: #e6a23c;
}

.risk-body {
  flex: 1;
}

.risk-title {
  font-size: 13px;
  font-weight: 600;
  color: #b88230;
}

.risk-desc {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}

.risk-actions {
  margin-top: 8px;
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

.text-danger {
  color: #f56c6c;
}

.op-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  gap: 8px;
}

.op-cell .el-button + .el-button {
  margin-left: 6px;
}

.more-link {
  margin-left: 8px;
  font-size: 12px;
  color: #409eff;
  cursor: pointer;
}

.bill-remark {
  font-size: 12px;
  line-height: 1.5;
  color: #606266;
  white-space: pre-line;
}

.settled-row {
  padding: 12px 0;
  text-align: center;
  border-top: 1px solid #f1f5f9;
}

.settled-text {
  font-size: 12px;
  color: #909399;
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
