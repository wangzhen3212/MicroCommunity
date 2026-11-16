<template>
  <div class="contract-performance">
    <div class="cc-page-head">
      <div>
        <div class="cc-page-title">{{ $t('contractPerformance.title') }}</div>
        <div class="cc-page-subtitle">{{ $t('contractPerformance.subtitle') }}</div>
      </div>
    </div>

    <div class="cc-stat-row">
      <div class="cc-stat-card">
        <div class="cc-stat-label">{{ $t('contractPerformance.stat.performing') }}</div>
        <div class="cc-stat-value">{{ summary.performingCount || 0 }}</div>
        <div class="cc-stat-extra">{{ $t('contractPerformance.stat.performingExtra') }}</div>
      </div>
      <div class="cc-stat-card">
        <div class="cc-stat-label">{{ $t('contractPerformance.stat.paid') }}</div>
        <div class="cc-stat-value">{{ formatAmount(summary.paidAmount) }}</div>
        <div class="cc-stat-extra">{{ $t('contractPerformance.stat.paidExtra') }}</div>
      </div>
      <div class="cc-stat-card">
        <div class="cc-stat-label">{{ $t('contractPerformance.stat.unpaid') }}</div>
        <div class="cc-stat-value">{{ formatAmount(summary.unpaidAmount) }}</div>
        <div class="cc-stat-extra">{{ $t('contractPerformance.stat.unpaidExtra') }}</div>
      </div>
      <div class="cc-stat-card">
        <div class="cc-stat-label">{{ $t('contractPerformance.stat.overdue') }}</div>
        <div class="cc-stat-value">{{ summary.overdueCount || 0 }}</div>
        <div class="cc-stat-extra">{{ $t('contractPerformance.stat.overdueExtra') }}</div>
      </div>
    </div>

    <div class="cc-toolbar">
      <el-input
        style="width: 220px"
        v-model="conditions.searchText"
        :placeholder="$t('contractPerformance.filter.keywordPlaceholder')"
        clearable
        @keyup.enter.native="handleSearch"
      />
      <el-select
        style="width: 150px"
        v-model="conditions.category"
        :placeholder="$t('contractPerformance.filter.categoryPlaceholder')"
        clearable
      >
        <el-option :label="$t('contractCategory.purchase')" value="2" />
        <el-option :label="$t('contractCategory.maintain')" value="3" />
      </el-select>
      <el-select
        style="width: 150px"
        v-model="conditions.payState"
        :placeholder="$t('contractPerformance.filter.payStatePlaceholder')"
        clearable
      >
        <el-option :label="$t('contractPerformance.payState.doing')" value="3" />
        <el-option :label="$t('contractPerformance.payState.paid')" value="1" />
        <el-option :label="$t('contractPerformance.payState.overdue')" value="2" />
      </el-select>
      <el-button type="primary" @click="handleSearch">
        <i class="el-icon-search"></i>
        {{ $t('common.search') }}
      </el-button>
      <el-button @click="handleReset">
        <i class="el-icon-refresh"></i>
        {{ $t('common.reset') }}
      </el-button>
      <el-button @click="handleExport">
        <i class="el-icon-download"></i>
        {{ $t('contractPerformance.filter.export') }}
      </el-button>
    </div>

    <el-table :data="performances" border v-loading="loading" style="width: 100%">
      <el-table-column :label="$t('contractPerformance.table.code')" min-width="200">
        <template slot-scope="scope">
          <div class="cc-cell-code">{{ scope.row.contractCode }}</div>
          <div class="cc-cell-name">{{ scope.row.contractName }}</div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractPerformance.table.payMode')" align="center" width="130">
        <template slot-scope="scope">{{ scope.row.payMode || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractPerformance.table.progress')" width="180">
        <template slot-scope="scope">
          <contract-perf-progress
            :amount="scope.row.amount"
            :paid-amount="scope.row.paidAmount"
            :plan-count="scope.row.planCount"
            :paid-count="scope.row.paidCount"
            :overdue-count="scope.row.overdueCount"
          />
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractPerformance.table.plan')" align="center" width="120">
        <template slot-scope="scope">{{ scope.row.paidCount || 0 }}/{{ scope.row.planCount || 0 }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractPerformance.table.next')" min-width="180">
        <template slot-scope="scope">
          <div>{{ scope.row.nextPlanName || '-' }}</div>
          <div class="cc-cell-name">{{ formatTime(scope.row.nextPlanTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractPerformance.table.state')" align="center" width="120">
        <template slot-scope="scope">
          <span class="cc-badge" :class="stateClass(scope.row)">{{ stateText(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractPerformance.table.operation')" align="center" width="160">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="openDetail(scope.row)">{{ $t('contractPerformance.op.detail') }}</el-button>
          <el-button type="text" size="mini" @click="openPayment(scope.row)">{{ $t('contractPerformance.op.pay') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && !performances.length" class="cc-empty">{{ $t('contractPerformance.empty') }}</div>

    <el-pagination
      :current-page="page.current"
      :page-sizes="[10, 20, 30, 50]"
      :page-size="page.size"
      :total="page.total"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 16px; text-align: right"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />

    <contract-payment-dialog ref="paymentDialog" @success="handleSearch" />
    <contract-detail-drawer ref="detailDrawer" @edit="editContract" @print="printContract" />
  </div>
</template>

<script>
import ContractPerfProgress from '@/components/contract/contractPerfProgress'
import ContractPaymentDialog from '@/components/contract/contractPaymentDialog'
import ContractDetailDrawer from '@/components/contract/contractDetailDrawer'
import { listContractPerformances } from '@/api/contract/contractPerformanceApi'
import { exportContracts } from '@/api/contract/contractLedgerApi'
import { formatAmount, saveBase64File, toNumber } from '@/utils/contractUtil'

export default {
  name: 'ContractPerformanceList',
  components: {
    ContractPerfProgress,
    ContractPaymentDialog,
    ContractDetailDrawer
  },
  data() {
    return {
      loading: false,
      performances: [],
      summary: {},
      conditions: {
        searchText: '',
        category: '',
        payState: ''
      },
      page: {
        current: 1,
        size: 10,
        total: 0
      }
    }
  },
  created() {
    if (this.$route.query && this.$route.query.contractId) {
      this.page.size = 10
    }
    this.listPerformances()
  },
  methods: {
    formatAmount,
    listPerformances() {
      this.loading = true
      const params = {
        page: this.page.current,
        row: this.page.size,
        searchText: this.conditions.searchText,
        category: this.conditions.category,
        payState: this.conditions.payState
      }
      if (this.$route.query && this.$route.query.contractId) {
        params.contractId = this.$route.query.contractId
      }
      listContractPerformances(params).then(res => {
        this.performances = res.data || []
        this.page.total = res.total || 0
        this.summary = res.sumTotal || {}
      }).catch(error => {
        this.performances = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractPerformance.msg.fetchError'))
      }).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
      this.page.current = 1
      this.listPerformances()
    },
    handleReset() {
      this.conditions = {
        searchText: '',
        category: '',
        payState: ''
      }
      this.handleSearch()
    },
    handleSizeChange(size) {
      this.page.size = size
      this.listPerformances()
    },
    handleCurrentChange(current) {
      this.page.current = current
      this.listPerformances()
    },
    handleExport() {
      exportContracts({
        exportType: 'performance',
        searchText: this.conditions.searchText,
        category: this.conditions.category,
        payState: this.conditions.payState
      }).then(data => {
        saveBase64File(data.base64, data.fileName)
        this.$message.success(this.$t('contractPerformance.msg.exportSuccess'))
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractPerformance.msg.exportError'))
      })
    },
    stateText(row) {
      if (toNumber(row.overdueCount) > 0) {
        return this.$t('contractPerformance.payState.overdue')
      }
      if (toNumber(row.planCount) > 0 && toNumber(row.paidCount) >= toNumber(row.planCount)) {
        return this.$t('contractPerformance.payState.paid')
      }
      return this.$t('contractPerformance.payState.doing')
    },
    stateClass(row) {
      if (toNumber(row.overdueCount) > 0) {
        return 'is-danger'
      }
      if (toNumber(row.planCount) > 0 && toNumber(row.paidCount) >= toNumber(row.planCount)) {
        return 'is-success'
      }
      return 'is-warning'
    },
    formatTime(time) {
      if (!time) {
        return '-'
      }
      return String(time).replace('T', ' ').substring(0, 10)
    },
    openPayment(row) {
      this.$refs.paymentDialog.open(row)
    },
    openDetail(row) {
      this.$refs.detailDrawer.open({
        contractId: row.contractId,
        contractCode: row.contractCode,
        contractName: row.contractName,
        amount: row.amount,
        partyB: row.partyB,
        category: row.category,
        state: row.state,
        startTime: row.startTime,
        endTime: row.endTime,
        operator: row.operator,
        payMode: row.payMode
      })
    },
    editContract(contract) {
      this.$router.push({
        path: '/views/contract/addContract',
        query: { contractId: contract.contractId }
      })
    },
    printContract(contract) {
      window.open(`/#/views/contract/printContract?contractId=${contract.contractId}`)
    }
  }
}
</script>

<style lang="scss" scoped>
.contract-performance {
  padding: 16px;

  .cc-page-head {
    margin-bottom: 16px;

    .cc-page-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }

    .cc-page-subtitle {
      margin-top: 4px;
      font-size: 12px;
      color: #909399;
    }
  }

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
