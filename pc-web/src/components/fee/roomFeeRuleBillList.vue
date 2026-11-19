<template>
  <div class="room-fee-rule-bill">
    <el-row v-if="roomInfo.roomId" :gutter="10" class="summary-row">
      <el-col :span="10">
        <div class="room-title">
          <span class="room-name">{{ roomInfo.roomName }}</span>
          <el-tag v-if="roomInfo.roomSubTypeName" size="mini">{{ roomInfo.roomSubTypeName }}</el-tag>
          <el-tag v-if="roomInfo.stateName" size="mini" type="success">{{ roomInfo.stateName }}</el-tag>
        </div>
        <div class="room-sub">
          <span v-if="roomInfo.ownerName">{{ $t('roomFeeRuleBill.owner') }} {{ roomInfo.ownerName }}</span>
          <span v-if="roomInfo.link"> · {{ roomInfo.link }}</span>
          <span v-if="roomInfo.builtUpArea"> · {{ $t('roomFeeRuleBill.buildArea') }} {{ roomInfo.builtUpArea }} ㎡</span>
        </div>
      </el-col>
      <el-col :span="14">
        <div class="stat-row">
          <div class="stat-item">
            <div class="stat-label">{{ $t('roomFeeRuleBill.summaryRules') }}</div>
            <div class="stat-value">{{ statRuleCount }} <span class="stat-unit">{{ $t('roomFeeRuleBill.unitItem') }}</span></div>
          </div>
          <div class="stat-item">
            <div class="stat-label">{{ $t('roomFeeRuleBill.summaryReceivable') }}</div>
            <div class="stat-value">¥{{ statReceivable }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">{{ $t('roomFeeRuleBill.summaryReceived') }}</div>
            <div class="stat-value stat-received">¥{{ statReceived }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">{{ $t('roomFeeRuleBill.summaryOwe') }}</div>
            <div class="stat-value stat-owe">¥{{ statOwe }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="intro-line">{{ $t('roomFeeRuleBill.intro') }}</div>

    <el-alert v-if="showRisk" type="warning" :closable="false" show-icon class="risk-alert">
      <div slot="title" class="risk-title">{{ $t('roomFeeRuleBill.riskTitle', { count: riskRules.length }) }}</div>
      <div class="risk-body">
        <span class="risk-desc">{{ $t('roomFeeRuleBill.riskDesc') }}</span>
        <el-button size="mini" type="warning" @click="handleRiskView">{{ $t('roomFeeRuleBill.riskAction') }}</el-button>
        <el-button size="mini" type="text" @click="riskIgnored = true">{{ $t('roomFeeRuleBill.riskIgnore') }}</el-button>
      </div>
    </el-alert>

    <el-row :gutter="10" class="filter-row">
      <el-col :span="6">
        <el-input v-model="keyword" size="small" clearable
          :placeholder="$t('roomFeeRuleBill.keywordPlaceholder')" @input="handleQuery"></el-input>
      </el-col>
      <el-col :span="5">
        <el-select v-model="stateFilter" size="small" @change="handleQuery">
          <el-option :label="$t('roomFeeRuleBill.allState')" value=""></el-option>
          <el-option :label="$t('roomFeeRuleBill.stateDoing')" value="2008001"></el-option>
          <el-option :label="$t('roomFeeRuleBill.stateStopped')" value="2009001"></el-option>
        </el-select>
      </el-col>
      <el-col :span="13" class="text-right">
        <el-tag v-if="riskOnly" size="small" type="warning" closable class="risk-filter-tag" @close="handleRiskClear">
          {{ $t('roomFeeRuleBill.riskOnlyTag', { count: riskRules.length }) }}
        </el-tag>
        <el-button size="small" @click="handleReset">
          <i class="el-icon-refresh"></i>
          {{ $t('common.reset') }}
        </el-button>
        <el-button v-if="hasPrivilege('502020082314267912')" type="primary" size="small" @click="_toBatchPay">
          <i class="el-icon-money"></i>
          {{ $t('roomFeeRuleBill.btnBatchPay') }}
        </el-button>
        <el-button v-if="hasPrivilege('502022073096990007')" type="primary" size="small" @click="openRuleSet">
          <i class="el-icon-setting"></i>
          {{ $t('roomFeeRuleSet.entry') }}
        </el-button>
      </el-col>
    </el-row>

    <el-table ref="ruleTable" v-loading="loading" :data="pageRows" row-key="ruleId" border size="small"
      :empty-text="roomInfo.roomId ? $t('roomFeeRuleBill.empty') : $t('roomFeeRuleBill.selectRoomFirst')">
      <el-table-column type="expand">
        <template slot-scope="scope">
          <div class="bill-panel">
            <div class="bill-panel-head">
              <span class="bill-panel-title">
                {{ $t('roomFeeRuleBill.subTitle', { feeName: ruleDisplayName(scope.row) }) }}
              </span>
              <span class="bill-panel-count">{{ $t('roomFeeRuleBill.subUnpaidCount', { count: scope.row.unpaidCount }) }}</span>
              <span class="bill-panel-tip">
                {{ scope.row.paidCount > 0 ? $t('roomFeeRuleBill.subPaidTip', { count: scope.row.paidCount }) : $t('roomFeeRuleBill.subTip') }}
              </span>
            </div>
            <div v-if="!scope.row.bills || scope.row.bills.length === 0" class="bill-panel-empty">
              {{ scope.row.billCount > 0 ? $t('roomFeeRuleBill.subEmptyPaid') : $t('roomFeeRuleBill.subEmptyNoBill') }}
            </div>
            <el-table v-else :data="scope.row.bills" size="mini" border>
              <el-table-column prop="yearMonth" :label="$t('roomFeeRuleBill.billYearMonth')" align="center" width="100">
              </el-table-column>
              <el-table-column :label="$t('roomFeeRuleBill.billPeriod')" align="center">
                <template slot-scope="billScope">
                  {{ dateOnly(billScope.row.startTime) }} ~ {{ dateOnly(billScope.row.endTime) }}
                </template>
              </el-table-column>
              <el-table-column :label="$t('roomFeeRuleBill.billReceivable')" align="center" width="100">
                <template slot-scope="billScope">¥{{ money(billScope.row.receivableAmount) }}</template>
              </el-table-column>
              <el-table-column :label="$t('roomFeeRuleBill.billReceived')" align="center" width="100">
                <template slot-scope="billScope">¥{{ money(billScope.row.receivedAmount) }}</template>
              </el-table-column>
              <el-table-column :label="$t('roomFeeRuleBill.billState')" align="center" width="100">
                <template slot-scope="billScope">
                  <el-tag v-if="billScope.row.billState === 'cancelled'" size="mini" type="info">
                    {{ $t('roomFeeRuleBill.billCancelled') }}
                  </el-tag>
                  <el-tag v-else size="mini" type="danger">{{ $t('roomFeeRuleBill.billUnpaid') }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column :label="$t('common.operation')" align="center" width="200">
                <template slot-scope="billScope">
                  <el-button type="text" size="mini" @click="_feeHis(billScope.row)">
                    {{ $t('roomFeeRuleBill.payHistory') }}
                  </el-button>
                  <el-button v-if="billScope.row.billState === 'unpaid' && hasPrivilege('502020090604200029')"
                    type="text" size="mini" class="text-danger" @click="_openCancel(scope.row, billScope.row, 'bill')">
                    {{ $t('roomFeeRuleBill.cancelThisBill') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('roomFeeRuleBill.colFeeItem')" min-width="200">
        <template slot-scope="scope">
          <div class="cell-main">
            {{ ruleDisplayName(scope.row) }}
            <el-tag v-if="scope.row.risk" size="mini" type="warning" class="cell-badge">
              {{ $t('roomFeeRuleBill.cancelledBadge') }}
            </el-tag>
          </div>
          <div class="cell-sub">
            {{ scope.row.feeTypeCdName || '-' }} · {{ scope.row.computingFormulaName || '-' }}
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('roomFeeRuleBill.colRule')" min-width="170">
        <template slot-scope="scope">
          <div class="cell-main">{{ scope.row.feeFlagName || '-' }}</div>
          <div class="cell-sub">{{ dateOnly(scope.row.startTime) }} ~ {{ dateOnly(scope.row.maxTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('roomFeeRuleBill.colNextBill')" align="center" width="120">
        <template slot-scope="scope">
          <template v-if="scope.row.state === '2008001'">
            <div class="cell-main">{{ dateOnly(scope.row.curYearMonth) }}</div>
            <div class="cell-sub">{{ $t('roomFeeRuleBill.nextBillAuto') }}</div>
          </template>
          <template v-else>
            <div class="cell-main text-muted">—</div>
            <div class="cell-sub">{{ $t('roomFeeRuleBill.noMoreBill') }}</div>
          </template>
        </template>
      </el-table-column>
      <el-table-column :label="$t('roomFeeRuleBill.colReceivable')" align="right" width="110">
        <template slot-scope="scope">
          <div class="cell-main">¥{{ money(scope.row.receivableAmount) }}</div>
          <div class="cell-sub">{{ $t('roomFeeRuleBill.periods', { count: scope.row.billCount }) }}</div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('roomFeeRuleBill.colOwe')" align="right" width="110">
        <template slot-scope="scope">
          <div class="cell-main" :class="{ 'stat-owe': Number(scope.row.oweAmount) > 0 }">
            ¥{{ money(scope.row.oweAmount) }}
          </div>
          <div v-if="scope.row.unpaidCount > 0" class="cell-sub stat-owe">
            {{ $t('roomFeeRuleBill.unpaidPeriods', { count: scope.row.unpaidCount }) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('roomFeeRuleBill.colState')" align="center" width="130">
        <template slot-scope="scope">
          <el-tag size="mini" :type="stateTagType(scope.row)">{{ stateText(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.operation')" align="center" width="210">
        <template slot-scope="scope">
          <el-button v-if="scope.row.state === '2008001' && hasPrivilege('502020082314267912')"
            type="text" size="mini" @click="_preGenerateFee(scope.row)">
            {{ $t('roomFeeRuleBill.btnPreGenerate') }}
          </el-button>
          <el-button v-if="Number(scope.row.oweAmount) > 0 && hasPrivilege('502020082314267912')"
            type="text" size="mini" @click="_toPay(scope.row)">
            {{ $t('roomFeeRuleBill.btnPay') }}
          </el-button>
          <el-dropdown v-if="pendingBill(scope.row) || hasPrivilege('502020090604200029') || hasPrivilege('502021070488970005') || hasPrivilege('502020090427190001')">
            <span class="el-dropdown-link">{{ $t('common.more') }}<i class="el-icon-arrow-down el-icon--right"></i></span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item v-if="!scope.row.syntheticOther && (hasPrivilege('502020090604200029') || hasPrivilege('502021070488970005'))"
                @click.native="_openCancel(scope.row, null, 'both')">
                {{ $t('roomFeeRuleBill.menuCancel') }}
                <span class="menu-tag">{{ $t('roomFeeRuleBill.menuCancelTag') }}</span>
              </el-dropdown-item>
              <el-dropdown-item v-if="!scope.row.syntheticOther && hasPrivilege('502020090427190001')"
                @click.native="_editRule(scope.row)">
                {{ $t('roomFeeRuleBill.menuEditRule') }}
              </el-dropdown-item>
              <el-dropdown-item v-if="!scope.row.syntheticOther && hasPrivilege('502021070488970005')"
                @click.native="_openCancel(scope.row, null, 'rule')">
                {{ $t('roomFeeRuleBill.menuFinishRule') }}
              </el-dropdown-item>
              <el-dropdown-item v-if="scope.row.state === '2008001' && hasPrivilege('502020090427190001') && pendingBill(scope.row)"
                @click.native="_editFee(scope.row)">
                {{ $t('roomFeeRuleBill.menuFeeChange') }}
              </el-dropdown-item>
              <el-dropdown-item v-if="scope.row.state === '2008001' && hasPrivilege('502020090427190001') && pendingBill(scope.row)"
                @click.native="_splitFee(scope.row)">
                {{ $t('roomFeeRuleBill.menuSplitFee') }}
              </el-dropdown-item>
              <el-dropdown-item v-if="pendingBill(scope.row)" @click.native="_feeHis(pendingBill(scope.row))">
                {{ $t('roomFeeRuleBill.menuDetail') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <div class="footer-bar">
      <span class="footer-tip">{{ $t('roomFeeRuleBill.footerTip') }}</span>
      <div class="footer-right">
        <span v-if="hiddenSettledList.length > 0" class="footer-hidden">
          {{ showSettled
            ? $t('roomFeeRuleBill.shownTip', { count: hiddenSettledList.length })
            : $t('roomFeeRuleBill.hiddenTip', { count: hiddenSettledList.length }) }}
          <el-button type="text" size="mini" @click="toggleSettled">
            {{ showSettled ? $t('roomFeeRuleBill.hide') : $t('roomFeeRuleBill.show') }}
          </el-button>
        </span>
        <span class="footer-total">{{ $t('roomFeeRuleBill.totalCount', { count: filteredRules.length }) }}</span>
        <el-pagination small layout="prev, pager, next" :current-page="currentPage" :page-size="pageSize"
          :total="filteredRules.length" @current-change="handleCurrentChange"></el-pagination>
      </div>
    </div>

    <room-fee-cancel-dialog ref="roomFeeCancel" @success="loadData"></room-fee-cancel-dialog>
    <room-fee-rule-set-drawer ref="roomFeeRuleSet" @success="loadData"></room-fee-rule-set-drawer>
    <edit-fee-rule ref="editFeeRule" @success="loadData"></edit-fee-rule>
    <pre-generate-fee ref="preGenerateFee" @success="loadData"></pre-generate-fee>
    <edit-fee ref="editFee" @success="loadData"></edit-fee>
    <split-fee ref="splitFee" @success="loadData"></split-fee>
  </div>
</template>

<script>
import { listRoomFeeRuleWithBill } from '@/api/fee/roomFeeRuleBillApi'
import { queryPreGenerateFeePeriods, preGenerateFee as preGenerateFeeApi } from '@/api/fee/preGenerateFeeApi'
import { getCommunityId } from '@/api/community/communityApi'
import RoomFeeCancelDialog from './roomFeeCancelDialog'
import RoomFeeRuleSetDrawer from './roomFeeRuleSetDrawer'
import EditFeeRule from './editFeeRule'
import PreGenerateFee from './preGenerateFee'
import EditFee from './editFee'
import SplitFee from './splitFee'

export default {
  name: 'RoomFeeRuleBillList',
  components: {
    RoomFeeCancelDialog,
    RoomFeeRuleSetDrawer,
    EditFeeRule,
    PreGenerateFee,
    EditFee,
    SplitFee
  },
  data() {
    return {
      loading: false,
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
      rules: [],
      keyword: '',
      stateFilter: '',
      showSettled: false,
      riskIgnored: false,
      riskOnly: false,
      currentPage: 1,
      pageSize: 20
    }
  },
  computed: {
    riskRules() {
      return this.rules.filter(item => item.risk)
    },
    showRisk() {
      return this.riskRules.length > 0 && !this.riskIgnored
    },
    hiddenSettledList() {
      return this.rules.filter(item => item.settled)
    },
    filteredRules() {
      const keyword = (this.keyword || '').trim()
      return this.rules.filter(item => {
        if (this.riskOnly && !item.risk) {
          return false
        }
        if (item.settled && !this.showSettled) {
          return false
        }
        if (this.stateFilter && item.state !== this.stateFilter) {
          return false
        }
        if (keyword) {
          const feeName = item.feeName || ''
          const feeTypeCdName = item.feeTypeCdName || ''
          if (feeName.indexOf(keyword) < 0 && feeTypeCdName.indexOf(keyword) < 0) {
            return false
          }
        }
        return true
      })
    },
    pageRows() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.filteredRules.slice(start, start + this.pageSize)
    },
    statRuleCount() {
      return this.rules.filter(item => !item.syntheticOther).length
    },
    statReceivable() {
      return this.sumAmount('receivableAmount')
    },
    statReceived() {
      return this.sumAmount('receivedAmount')
    },
    statOwe() {
      return this.sumAmount('oweAmount')
    }
  },
  methods: {
    open(params) {
      this.roomInfo = {
        roomId: params.roomId || params.payerObjId || '',
        roomName: params.roomName || params.payerObjName || '',
        ownerId: params.ownerId || '',
        ownerName: params.ownerName || '',
        roomSubTypeName: params.roomSubTypeName || '',
        stateName: params.stateName || '',
        link: params.link || '',
        builtUpArea: params.builtUpArea || params.roomArea || ''
      }
      this.keyword = ''
      this.stateFilter = ''
      this.showSettled = false
      this.riskIgnored = false
      this.riskOnly = false
      this.currentPage = 1
      this.rules = []
      this.loadData()
    },
    async loadData() {
      if (!this.roomInfo.roomId) {
        this.rules = []
        return
      }
      this.loading = true
      try {
        const res = await listRoomFeeRuleWithBill({
          communityId: getCommunityId(),
          payerObjId: this.roomInfo.roomId,
          payerObjType: '3333'
        })
        this.rules = res.data || []
      } catch (error) {
        this.rules = []
        this.$message.error(error.message || this.$t('roomFeeRuleBill.loadFailed'))
      } finally {
        this.loading = false
      }
    },
    sumAmount(field) {
      let total = 0
      this.rules.forEach(item => {
        const amount = parseFloat(item[field])
        if (!isNaN(amount)) {
          total += amount
        }
      })
      return total.toFixed(2)
    },
    money(value) {
      const amount = parseFloat(value)
      return isNaN(amount) ? '0.00' : amount.toFixed(2)
    },
    dateOnly(value) {
      if (!value) {
        return '—'
      }
      return String(value).substring(0, 10)
    },
    ruleDisplayName(row) {
      if (row.syntheticOther) {
        return this.$t('roomFeeRuleBill.syntheticName')
      }
      return row.feeName || '-'
    },
    stateText(row) {
      if (row.syntheticOther) {
        return this.$t('roomFeeRuleBill.stateNoRule')
      }
      if (row.state === '2008001') {
        return this.$t('roomFeeRuleBill.stateDoing')
      }
      if (row.unpaidCount > 0) {
        return this.$t('roomFeeRuleBill.stateOnceUnpaid')
      }
      return this.$t('roomFeeRuleBill.stateOnceSettled')
    },
    stateTagType(row) {
      if (row.syntheticOther) {
        return 'info'
      }
      if (row.state === '2008001') {
        return 'success'
      }
      return 'info'
    },
    pendingBill(row) {
      if (!row.bills) {
        return null
      }
      return row.bills.find(bill => bill.billState === 'unpaid') || null
    },
    handleQuery() {
      this.currentPage = 1
    },
    handleReset() {
      this.keyword = ''
      this.stateFilter = ''
      this.showSettled = false
      this.riskOnly = false
      this.currentPage = 1
    },
    toggleSettled() {
      this.showSettled = !this.showSettled
      this.currentPage = 1
    },
    handleCurrentChange(page) {
      this.currentPage = page
    },
    handleRiskView() {
      this.keyword = ''
      this.stateFilter = ''
      this.showSettled = false
      this.riskOnly = true
      this.currentPage = 1
      this.$nextTick(() => {
        const table = this.$refs.ruleTable
        if (!table) {
          return
        }
        this.pageRows.forEach(row => {
          if (row.risk) {
            table.toggleRowExpansion(row, true)
          }
        })
      })
    },
    handleRiskClear() {
      this.riskOnly = false
      this.currentPage = 1
    },
    async openRuleSet() {
      if (!this.roomInfo.roomId) {
        this.$message.warning(this.$t('roomFeeRuleBill.selectRoomFirst'))
        return
      }
      if (this.rules.length === 0) {
        await this.loadData()
      }
      this.$refs.roomFeeRuleSet.open({
        roomId: this.roomInfo.roomId,
        roomName: this.roomInfo.roomName,
        ownerName: this.roomInfo.ownerName,
        existingRules: this.rules.filter(item => !item.syntheticOther),
        existingConfigIds: this.rules
          .filter(item => !item.syntheticOther)
          .map(item => item.configId)
      })
    },
    _toBatchPay() {
      this.$router.push({
        path: '/views/fee/batchPayFeeOrder',
        query: {
          ownerId: this.roomInfo.ownerId,
          payerObjType: '3333'
        }
      })
    },
    _toPay() {
      this.$router.push({
        path: '/views/fee/owePayFeeOrder',
        query: {
          payObjId: this.roomInfo.roomId,
          payObjType: '3333',
          roomName: this.roomInfo.roomName
        }
      })
    },
    _feeHis(bill) {
      this.$router.push({
        path: '/views/fee/feeDetail',
        query: { feeId: bill.feeId }
      })
    },
    _openCancel(rule, bill, mode) {
      this.$refs.roomFeeCancel.open({
        rule: rule,
        bill: bill || this.pendingBill(rule),
        mode: mode
      })
    },
    _editRule(rule) {
      this.$refs.editFeeRule.open(rule)
    },
    _editFee(rule) {
      const bill = this.pendingBill(rule)
      if (!bill) {
        return
      }
      this.$refs.editFee.open(bill)
    },
    _splitFee(rule) {
      const bill = this.pendingBill(rule)
      if (!bill) {
        return
      }
      this.$refs.splitFee.open(bill)
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
.room-fee-rule-bill {
  margin-top: 10px;
}

.summary-row {
  padding: 12px 14px;
  margin-bottom: 12px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.room-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.room-name {
  margin-right: 8px;
}

.room-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.stat-row {
  display: flex;
  justify-content: flex-end;
}

.stat-item {
  min-width: 110px;
  text-align: right;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.stat-unit {
  font-size: 11px;
  font-weight: 400;
  color: #909399;
}

.stat-received {
  color: #67c23a;
}

.stat-owe {
  color: #f56c6c;
}

.risk-alert {
  margin-bottom: 12px;
}

.intro-line {
  padding: 6px 0 10px;
  font-size: 12px;
  line-height: 1.6;
  color: #909399;
}

.risk-filter-tag {
  margin-right: 8px;
}

.risk-title {
  font-weight: 600;
}

.risk-body {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.risk-desc {
  margin-right: 12px;
  font-size: 12px;
}

.filter-row {
  margin-bottom: 10px;
}

.cell-main {
  color: #303133;
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

.menu-tag {
  margin-left: 6px;
  font-size: 11px;
  color: #f56c6c;
}

.bill-panel {
  padding: 8px 12px 12px;
  background: #fbfdff;
}

.bill-panel-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  padding-bottom: 8px;
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

.bill-panel-tip {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
}

.bill-panel-empty {
  padding: 16px 0;
  text-align: center;
  font-size: 12px;
  color: #909399;
}

.footer-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
}

.footer-tip {
  flex: 1;
}

.footer-right {
  display: flex;
  align-items: center;
}

.footer-hidden {
  margin-right: 12px;
}

.footer-total {
  margin-right: 8px;
}
</style>
