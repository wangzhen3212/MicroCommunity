<template>
  <div class="contract-ledger cc-page">
    <contract-ledger-stat :statistics="statistics" @filter-auditing="filterAuditing" />

    <div class="cc-surface">
      <div class="cc-surface-head">
        <div class="cc-surface-title">{{ $t('contractLedger.list') }}</div>
        <div class="cc-surface-actions">
          <el-button type="primary" @click="createContract">
            <i class="el-icon-plus"></i>
            <span>{{ $t('contractWorkbench.titleNew') }}</span>
          </el-button>
          <el-button @click="handleExport">
            <i class="el-icon-download"></i>
            <span>{{ $t('contractLedger.filter.export') }}</span>
          </el-button>
        </div>
      </div>

      <contract-filter-bar
        :value="conditions"
        :partyas="partyas"
        @search="handleSearch"
        @reset="handleReset"
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
        v-loading="loading"
        :empty-text="$t('contractLedger.empty')"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column :label="$t('contractLedger.table.code')" min-width="240">
          <template slot-scope="scope">
            <div class="cc-cell-title-row">
              <span class="cc-cell-dot" :class="'is-' + categoryKey(scope.row)"></span>
              <span class="cc-cell-title">{{ scope.row.contractName }}</span>
              <span v-if="scope.row.changed === '1'" class="cc-cell-flag">{{ $t('contractLedger.filter.changedYes') }}</span>
            </div>
            <div class="cc-cell-meta-row">
              <span class="cc-cell-code">{{ scope.row.contractCode }}</span>
              <span v-if="scope.row.contractParentId" class="cc-cell-cat is-info">{{ $t('contractLedger.op.renew') }}</span>
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
            <div class="cc-cell-term">{{ formatTerm(scope.row.startTime, scope.row.endTime) }}</div>
            <div class="cc-cell-progress">
              <div class="cc-cell-progress-bar">
                <div
                  class="cc-cell-progress-inner"
                  :class="progressClass(scope.row)"
                  :style="{ width: progressPercent(scope.row) + '%' }"
                ></div>
              </div>
              <span
                v-if="daysOf(scope.row) !== null"
                class="cc-cell-days"
                :class="'is-' + daysLeftType(daysOf(scope.row))"
              >{{ daysText(daysOf(scope.row)) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="$t('contractLedger.table.state')" align="center" width="150">
          <template slot-scope="scope">
            <contract-status-tag
              :state="scope.row.state"
              :state-name="scope.row.stateName"
              :category="scope.row.category"
            />
          </template>
        </el-table-column>
        <el-table-column :label="$t('contractLedger.table.operation')" align="right" width="220" fixed="right">
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

      <div class="cc-surface-foot">
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
      </div>
    </div>


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
import { queryContract } from '@/api/contract/contractManageApi'
import { queryContractPartya } from '@/api/contract/contractPartyaApi'
import {
  batchStopContract,
  batchSubmitContract,
  exportContracts,
  queryContractStatistics
} from '@/api/contract/contractLedgerApi'
import {
  CONTRACT_CATEGORY,
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
      contracts: [],
      selection: [],
      conditions: {
        searchText: '',
        state: '',
        partyaId: '',
        changed: '',
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
  created() {
    this.loadStatistics()
    this.loadPartyas()
    this.listContracts()
  },
  methods: {
    formatAmount,
    formatTerm,
    daysLeftType,
    categoryKey(row) {
      const category = String(row.category || '')
      if (category === CONTRACT_CATEGORY.RENT) {
        return 'rent'
      }
      if (category === CONTRACT_CATEGORY.MAINTAIN) {
        return 'maintain'
      }
      return 'purchase'
    },
    progressPercent(row) {
      const start = new Date(String(row.startTime || '').replace(/-/g, '/')).getTime()
      const end = new Date(String(row.endTime || '').replace(/-/g, '/')).getTime()
      if (!start || !end || end <= start) {
        return 0
      }
      const percent = ((Date.now() - start) / (end - start)) * 100
      if (percent < 0) {
        return 0
      }
      return percent > 100 ? 100 : Math.round(percent)
    },
    progressClass(row) {
      const days = this.daysOf(row)
      if (days === null) {
        return 'is-normal'
      }
      if (days < 0) {
        return 'is-expired'
      }
      return days <= 30 ? 'is-warning' : 'is-normal'
    },
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
    buildParams() {
      const params = {
        page: this.page.current,
        row: this.page.size,
        searchText: this.conditions.searchText,
        partyaId: this.conditions.partyaId,
        changed: this.conditions.changed
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
        endTimeBegin: '',
        endTimeEnd: ''
      }
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
      window.open(`/#/views/contract/printContract?contractId=${contract.contractId}`)
    },
    deleteContractMethod(contract) {
      this.$refs.deleteContract.open(contract)
    }
  }
}
</script>

<style lang="scss" scoped>
.contract-ledger {
  .cc-cell-title-row {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }

  .cc-cell-dot {
    width: 6px;
    height: 6px;
    flex: none;
    border-radius: 50%;

    &.is-rent {
      background: #4f46e5;
    }

    &.is-purchase {
      background: #0ea5e9;
    }

    &.is-maintain {
      background: #e6a23c;
    }
  }

  .cc-cell-title {
    font-weight: 600;
    color: #303133;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .cc-cell-flag {
    flex: none;
    height: 18px;
    line-height: 18px;
    padding: 0 6px;
    border-radius: 4px;
    font-size: 10px;
    color: #0284c7;
    background: #e0f2fe;
  }

  .cc-cell-meta-row {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 4px;
  }

  .cc-cell-code {
    font-size: 11px;
    color: #909399;
    font-variant-numeric: tabular-nums;
  }

  .cc-cell-cat {
    height: 18px;
    line-height: 18px;
    padding: 0 6px;
    border-radius: 4px;
    font-size: 10px;

    &.is-rent {
      color: #4f46e5;
      background: #eef2ff;
    }

    &.is-purchase {
      color: #0ea5e9;
      background: #e0f2fe;
    }

    &.is-maintain {
      color: #e6a23c;
      background: #fdf6ec;
    }

    &.is-info {
      color: #909399;
      background: #f4f4f5;
    }
  }

  .cc-cell-term {
    font-size: 12px;
    color: #606266;
    font-variant-numeric: tabular-nums;
  }

  .cc-cell-progress {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 6px;
  }

  .cc-cell-progress-bar {
    flex: 1;
    min-width: 40px;
    height: 4px;
    border-radius: 2px;
    background: #f2f3f5;
    overflow: hidden;
  }

  .cc-cell-progress-inner {
    height: 100%;
    border-radius: 2px;

    &.is-normal {
      background: #67c23a;
    }

    &.is-warning {
      background: #e6a23c;
    }

    &.is-expired {
      background: #c0c4cc;
    }
  }

  .cc-cell-days {
    flex: none;
    font-size: 10px;
    white-space: nowrap;

    &.is-danger {
      color: #f56c6c;
    }

    &.is-warning {
      color: #e6a23c;
    }

    &.is-info {
      color: #909399;
    }
  }
}

</style>
