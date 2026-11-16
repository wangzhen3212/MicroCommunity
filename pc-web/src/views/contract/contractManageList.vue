<template>
  <div class="contract-ledger cc-page">
    <div class="cc-page-head">
      <div>
        <div class="cc-page-title">{{ $t('contractLedger.title') }}</div>
        <div class="cc-page-subtitle">{{ $t('contractLedger.subtitle') }}</div>
      </div>
      <div class="cc-page-actions">
        <el-button type="primary" @click="createContract">
          <i class="el-icon-plus"></i>
          {{ $t('contractWorkbench.titleNew') }}
        </el-button>
      </div>
    </div>

    <contract-ledger-stat :statistics="statistics" @filter-auditing="filterAuditing" />

    <div class="cc-tabs">
      <div
        v-for="tab in categoryTabs"
        :key="tab.key"
        class="cc-tab"
        :class="{ 'is-active': conditions.category === tab.key }"
        @click="switchCategory(tab.key)"
      >
        {{ $t(tab.labelKey) }}
        <span class="cc-tab-count">{{ tab.count }}</span>
      </div>
    </div>

    <contract-filter-bar
      :value="conditions"
      :partyas="partyas"
      @search="handleSearch"
      @reset="handleReset"
      @export="handleExport"
    />

    <contract-bulk-bar
      :count="selection.length"
      @submit="handleBatchSubmit"
      @export="handleExport"
      @stop="handleBatchStop"
      @clear="clearSelection"
    />

    <el-table
      ref="table"
      :data="contracts"
      border
      v-loading="loading"
      style="width: 100%; margin-top: 12px"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" />
      <el-table-column :label="$t('contractLedger.table.code')" min-width="200">
        <template slot-scope="scope">
          <div class="cc-cell-code">
            <span>{{ scope.row.contractCode }}</span>
            <span v-if="scope.row.contractParentId" class="cc-badge is-info">{{ $t('contractLedger.op.renew') }}</span>
          </div>
          <div class="cc-cell-name">
            {{ scope.row.contractName }}
            <span v-if="scope.row.changed === '1'" class="cc-badge is-warning">{{ $t('contractLedger.filter.changedYes') }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractLedger.table.partya')" min-width="150">
        <template slot-scope="scope">{{ scope.row.partyA || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractLedger.table.partyB')" min-width="150">
        <template slot-scope="scope">{{ scope.row.partyB || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractLedger.table.amount')" align="right" width="130">
        <template slot-scope="scope">{{ formatAmount(scope.row.amount) }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractLedger.table.term')" min-width="210">
        <template slot-scope="scope">
          <div>{{ formatTerm(scope.row.startTime, scope.row.endTime) }}</div>
          <span
            v-if="daysOf(scope.row) !== null"
            class="cc-badge"
            :class="'is-' + daysLeftType(daysOf(scope.row))"
          >{{ daysText(daysOf(scope.row)) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractLedger.table.state')" align="center" width="150">
        <template slot-scope="scope">
          <contract-status-tag
            :state="scope.row.state"
            :state-name="scope.row.stateName"
            :category="scope.row.category"
            show-category
          />
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractLedger.table.operation')" align="center" width="220" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="openDetail(scope.row)">{{ $t('contractLedger.op.detail') }}</el-button>
          <el-button
            v-if="canEdit(scope.row)"
            type="text"
            size="mini"
            @click="editContract(scope.row)"
          >{{ $t('contractLedger.op.edit') }}</el-button>
          <el-button
            v-if="String(scope.row.category) !== '1'"
            type="text"
            size="mini"
            @click="openPerformance(scope.row)"
          >{{ $t('contractLedger.op.performance') }}</el-button>
          <el-button
            v-if="String(scope.row.state) === CONTRACT_STATE.AUDIT"
            type="text"
            size="mini"
            @click="goAudit(scope.row)"
          >{{ $t('contractLedger.op.audit') }}</el-button>
          <el-button type="text" size="mini" @click="printContract(scope.row)">{{ $t('contractLedger.op.print') }}</el-button>
          <el-button
            v-if="canStop(scope.row)"
            type="text"
            size="mini"
            @click="stopContract(scope.row)"
          >{{ $t('contractLedger.op.stop') }}</el-button>
          <el-button
            v-if="canEdit(scope.row)"
            type="text"
            size="mini"
            @click="deleteContractMethod(scope.row)"
          >{{ $t('contractLedger.op.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && !contracts.length" class="cc-empty">{{ $t('contractLedger.empty') }}</div>

    <el-pagination
      class="cc-pagination"
      :current-page="page.current"
      :page-sizes="[10, 20, 30, 50]"
      :page-size="page.size"
      :total="page.total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />

    <contract-detail-drawer
      ref="detailDrawer"
      @edit="editContract"
      @submit="submitContract"
      @stop="stopContract"
      @delete="deleteContractMethod"
      @print="printContract"
    />
    <delete-contract ref="deleteContract" @success="refreshAll" />
  </div>
</template>

<script>
import ContractLedgerStat from '@/components/contract/contractLedgerStat'
import ContractFilterBar from '@/components/contract/contractFilterBar'
import ContractBulkBar from '@/components/contract/contractBulkBar'
import ContractStatusTag from '@/components/contract/contractStatusTag'
import ContractDetailDrawer from '@/components/contract/contractDetailDrawer'
import DeleteContract from '@/components/contract/deleteContract'
import { queryContract, queryContractType } from '@/api/contract/contractManageApi'
import { queryContractPartya } from '@/api/contract/contractPartyaApi'
import {
  batchStopContract,
  batchSubmitContract,
  exportContracts,
  queryContractStatistics
} from '@/api/contract/contractLedgerApi'
import {
  CONTRACT_STATE,
  addMonths,
  daysLeft,
  daysLeftType,
  formatAmount,
  formatDate,
  formatTerm,
  saveBase64File
} from '@/utils/contractUtil'

export default {
  name: 'ContractManageList',
  components: {
    ContractLedgerStat,
    ContractFilterBar,
    ContractBulkBar,
    ContractStatusTag,
    ContractDetailDrawer,
    DeleteContract
  },
  data() {
    return {
      loading: false,
      statistics: {},
      partyas: [],
      contractTypes: [],
      contracts: [],
      selection: [],
      conditions: {
        searchText: '',
        state: '',
        partyaId: '',
        changed: '',
        category: '',
        endTimeBegin: '',
        endTimeEnd: ''
      },
      page: {
        current: 1,
        size: 10,
        total: 0
      }
    }
  },
  computed: {
    categoryTabs() {
      return [
        { key: '', labelKey: 'contractCategory.all', count: this.statistics.total || 0 },
        { key: '1', labelKey: 'contractCategory.rent', count: this.statistics.rentCount || 0 },
        { key: '2', labelKey: 'contractCategory.purchase', count: this.statistics.purchaseCount || 0 },
        { key: '3', labelKey: 'contractCategory.maintain', count: this.statistics.maintainCount || 0 }
      ]
    }
  },
  created() {
    this.loadStatistics()
    this.loadPartyas()
    this.loadContractTypes()
    this.listContracts()
  },
  methods: {
    formatAmount,
    formatTerm,
    daysLeftType,
    daysOf(row) {
      if (String(row.state) !== CONTRACT_STATE.EFFECTIVE) {
        return null
      }
      return daysLeft(row.endTime)
    },
    daysText(days) {
      if (days < 0) {
        return this.$t('contractLedger.daysLeft.expired', { days: Math.abs(days) })
      }
      return this.$t('contractLedger.daysLeft.remain', { days: days })
    },
    canEdit(row) {
      const state = String(row.state)
      return state === CONTRACT_STATE.DRAFT || state === CONTRACT_STATE.FAIL
    },
    canStop(row) {
      const state = String(row.state)
      return state === CONTRACT_STATE.DRAFT ||
        state === CONTRACT_STATE.AUDIT ||
        state === CONTRACT_STATE.EFFECTIVE
    },
    loadStatistics() {
      queryContractStatistics({}).then(data => {
        this.statistics = data || {}
      }).catch(() => {
        this.$message.error(this.$t('contractLedger.msg.statError'))
      })
    },
    loadPartyas() {
      queryContractPartya({ page: 1, row: 200 }).then(res => {
        this.partyas = res.data || []
      }).catch(() => {
        this.partyas = []
      })
    },
    loadContractTypes() {
      queryContractType({ page: 1, row: 100 }).then(res => {
        this.contractTypes = res.data || []
      }).catch(() => {
        this.contractTypes = []
      })
    },
    buildParams() {
      const params = {
        page: this.page.current,
        row: this.page.size,
        searchText: this.conditions.searchText,
        partyaId: this.conditions.partyaId,
        changed: this.conditions.changed,
        category: this.conditions.category
      }
      const state = this.conditions.state
      if (state === 'expiring') {
        params.state = CONTRACT_STATE.EFFECTIVE
        params.endTimeBegin = formatDate(new Date())
        params.endTimeEnd = addMonths(formatDate(new Date()), 1)
      } else if (state === 'expired') {
        params.state = CONTRACT_STATE.EFFECTIVE
        params.endTimeEnd = formatDate(new Date(Date.now() - 24 * 3600 * 1000))
      } else if (state) {
        params.state = state
      }
      return params
    },
    listContracts() {
      this.loading = true
      queryContract(this.buildParams()).then(res => {
        this.contracts = res.data || []
        this.page.total = res.total || 0
      }).catch(error => {
        this.contracts = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractLedger.msg.fetchError'))
      }).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
      this.page.current = 1
      this.listContracts()
    },
    handleReset() {
      this.conditions = {
        searchText: '',
        state: '',
        partyaId: '',
        changed: '',
        category: this.conditions.category,
        endTimeBegin: '',
        endTimeEnd: ''
      }
      this.handleSearch()
    },
    switchCategory(category) {
      this.conditions.category = category
      this.handleSearch()
    },
    filterAuditing() {
      this.conditions.state = CONTRACT_STATE.AUDIT
      this.handleSearch()
    },
    handleSelectionChange(rows) {
      this.selection = rows
    },
    clearSelection() {
      this.selection = []
      if (this.$refs.table) {
        this.$refs.table.clearSelection()
      }
    },
    handleSizeChange(size) {
      this.page.size = size
      this.listContracts()
    },
    handleCurrentChange(current) {
      this.page.current = current
      this.listContracts()
    },
    refreshAll() {
      this.loadStatistics()
      this.listContracts()
    },
    handleExport() {
      const params = this.buildParams()
      delete params.page
      delete params.row
      if (this.selection.length) {
        params.contractIds = this.selection.map(item => item.contractId)
      }
      exportContracts({ ...params, exportType: 'ledger' }).then(data => {
        saveBase64File(data.base64, data.fileName)
        this.$message.success(this.$t('contractLedger.msg.exportSuccess'))
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractLedger.msg.exportError'))
      })
    },
    handleBatchSubmit() {
      if (!this.selection.length) {
        this.$message.warning(this.$t('contractLedger.msg.noSelection'))
        return
      }
      this.$confirm(this.$t('contractLedger.msg.submitConfirm'), '提示', { type: 'warning' }).then(() => {
        batchSubmitContract({
          contractIds: this.selection.map(item => item.contractId)
        }).then(data => {
          this.$message.success(this.$t('contractLedger.msg.submitSuccess', {
            success: data.successCount || 0,
            fail: data.failCount || 0
          }))
          this.notifyFails(data.failList)
          this.clearSelection()
          this.refreshAll()
        }).catch(error => {
          this.$message.error(error.message)
        })
      }).catch(() => {
      })
    },
    handleBatchStop() {
      if (!this.selection.length) {
        this.$message.warning(this.$t('contractLedger.msg.noSelection'))
        return
      }
      this.$confirm(this.$t('contractLedger.msg.stopConfirm'), '提示', { type: 'warning' }).then(() => {
        batchStopContract({
          contractIds: this.selection.map(item => item.contractId)
        }).then(data => {
          this.$message.success(this.$t('contractLedger.msg.stopSuccess', {
            success: data.successCount || 0,
            fail: data.failCount || 0
          }))
          this.notifyFails(data.failList)
          this.clearSelection()
          this.refreshAll()
        }).catch(error => {
          this.$message.error(error.message)
        })
      }).catch(() => {
      })
    },
    notifyFails(failList) {
      if (!failList || !failList.length) {
        return
      }
      this.$alert(failList.map(item => item.msg).join('；'), '提示', { type: 'warning' })
    },
    submitContract(contract) {
      batchSubmitContract({ contractIds: [contract.contractId] }).then(data => {
        this.$message.success(this.$t('contractLedger.msg.submitSuccess', {
          success: data.successCount || 0,
          fail: data.failCount || 0
        }))
        this.notifyFails(data.failList)
        this.refreshAll()
      }).catch(error => {
        this.$message.error(error.message)
      })
    },
    stopContract(contract) {
      this.$confirm(this.$t('contractLedger.msg.stopConfirm'), '提示', { type: 'warning' }).then(() => {
        batchStopContract({ contractIds: [contract.contractId] }).then(data => {
          this.$message.success(this.$t('contractLedger.msg.stopSuccess', {
            success: data.successCount || 0,
            fail: data.failCount || 0
          }))
          this.notifyFails(data.failList)
          this.refreshAll()
        }).catch(error => {
          this.$message.error(error.message)
        })
      }).catch(() => {
      })
    },
    openDetail(contract) {
      this.$refs.detailDrawer.open(contract)
    },
    createContract() {
      this.$router.push({ path: '/views/contract/addContract' })
    },
    editContract(contract) {
      this.$router.push({
        path: '/views/contract/addContract',
        query: { contractId: contract.contractId }
      })
    },
    goAudit(contract) {
      this.$router.push({
        path: '/pages/admin/contractApplyAuditOrders',
        query: {
          bizId: contract.contractId,
          auditType: '1'
        }
      })
    },
    openPerformance(contract) {
      this.$router.push({
        path: '/pages/admin/contractPerformance',
        query: { contractId: contract.contractId }
      })
    },
    printContract(contract) {
      window.open(`/#/views/contract/printContract?contractTypeId=${contract.contractType}&contractId=${contract.contractId}`)
    },
    deleteContractMethod(contract) {
      this.$refs.deleteContract.open(contract)
    }
  }
}
</script>

<style lang="scss" scoped>
.contract-ledger {
  .cc-cell-code {
    font-weight: 600;
    color: #303133;
  }

  .cc-cell-name {
    font-size: 12px;
    color: #606266;
  }
}
</style>
