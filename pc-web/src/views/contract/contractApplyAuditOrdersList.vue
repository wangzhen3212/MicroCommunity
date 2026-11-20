<template>
  <div class="contract-pending-audit cc-page">
    <div class="cc-surface">
      <div class="cc-surface-head">
        <div class="cc-surface-title">{{ $t('contractPendingAudit.title') }}</div>
        <div class="cc-surface-actions">
          <span class="cc-surface-subtitle">{{ $t('contractPendingAudit.subtitle') }}</span>
        </div>
      </div>

      <div class="cc-toolbar">
        <el-input
          v-model="condition.searchText"
          style="width: 280px"
          clearable
          :placeholder="$t('contractPendingAudit.filter.keywordPlaceholder')"
          @keyup.enter.native="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">
          <i class="el-icon-search"></i>
          {{ $t('common.search') }}
        </el-button>
        <el-button @click="handleReset">
          <i class="el-icon-refresh"></i>
          {{ $t('common.reset') }}
        </el-button>
      </div>

      <el-table
        :data="contracts"
        v-loading="loading"
        :empty-text="$t('contractPendingAudit.record.none')"
        style="width: 100%"
      >
        <el-table-column :label="$t('contractPendingAudit.table.code')" min-width="240">
          <template slot-scope="scope">
            <div class="cc-cell-title">{{ scope.row.contractName }}</div>
            <div class="cc-cell-meta">{{ scope.row.contractCode }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('contractPendingAudit.table.partya')" min-width="140">
          <template slot-scope="scope">{{ scope.row.partyA || '-' }}</template>
        </el-table-column>
        <el-table-column :label="$t('contractPendingAudit.table.partyB')" min-width="160">
          <template slot-scope="scope">
            <div class="cc-cell-party">{{ scope.row.partyB || '-' }}</div>
            <div v-if="partyBMetaOf(scope.row)" class="cc-cell-party-meta">{{ partyBMetaOf(scope.row) }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('contractPendingAudit.table.amount')" align="right" width="130">
          <template slot-scope="scope">{{ formatAmount(scope.row.amount) }}</template>
        </el-table-column>
        <el-table-column :label="$t('contractPendingAudit.table.state')" align="center" width="130">
          <template slot-scope="scope">
            <contract-status-tag :state="scope.row.state" :state-name="scope.row.stateName" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('contractPendingAudit.table.lastAudit')" min-width="200">
          <template slot-scope="scope">
            <div v-if="lastRecordOf(scope.row)" class="cc-cell-record">
              <span>{{ lastRecordOf(scope.row).stepName || '-' }}</span>
              <span>{{ lastRecordOf(scope.row).auditorName || '-' }}</span>
              <span>{{ lastRecordOf(scope.row).resultName || '-' }}</span>
              <span class="cc-cell-record-time">{{ formatTime(lastRecordOf(scope.row).auditTime) }}</span>
            </div>
            <div v-else class="cc-cell-empty">{{ $t('contractPendingAudit.record.none') }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('contractPendingAudit.table.operation')" align="right" width="190">
          <template slot-scope="scope">
            <el-button
              v-if="scope.row.state === '11' && hasPrivilege('502026111600000009')"
              type="primary"
              size="mini"
              @click="openAuditDialog(scope.row)"
            >{{ $t('contractPendingAudit.op.legalAudit') }}</el-button>
            <el-button
              v-if="scope.row.state === '56' && hasPrivilege('502026111600000010')"
              type="primary"
              size="mini"
              @click="openAuditDialog(scope.row)"
            >{{ $t('contractPendingAudit.op.leaderAudit') }}</el-button>
            <span
              v-if="scope.row.state === '11' && !hasPrivilege('502026111600000009')"
              class="cc-cell-no-privilege"
            >{{ $t('contractPendingAudit.op.noPrivilege') }}</span>
            <span
              v-if="scope.row.state === '56' && !hasPrivilege('502026111600000010')"
              class="cc-cell-no-privilege"
            >{{ $t('contractPendingAudit.op.noPrivilege') }}</span>
            <el-button type="text" size="mini" @click="openDetail(scope.row)">
              {{ $t('contractPendingAudit.op.view') }}
            </el-button>
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

    <contract-audit-dialog ref="auditDialog" @success="getList" />
    <contract-detail-drawer ref="detailDrawer" />
  </div>
</template>

<script>
import ContractStatusTag from '@/components/contract/contractStatusTag'
import ContractAuditDialog from '@/components/contract/contractAuditDialog'
import ContractDetailDrawer from '@/components/contract/contractDetailDrawer'
import { listPendingAuditContracts } from '@/api/contract/contractAuditApi'
import { AUDIT_BIZ_TYPE, formatAmount } from '@/utils/contractUtil'

export default {
  name: 'ContractApplyAuditOrdersList',
  components: {
    ContractStatusTag,
    ContractAuditDialog,
    ContractDetailDrawer
  },
  data() {
    return {
      loading: false,
      contracts: [],
      condition: {
        searchText: ''
      },
      page: {
        current: 1,
        size: 10,
        total: 0
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    formatAmount,
    buildParams() {
      return {
        page: this.page.current,
        row: this.page.size,
        searchText: this.condition.searchText
      }
    },
    getList() {
      this.loading = true
      listPendingAuditContracts(this.buildParams()).then(res => {
        this.contracts = res.data || []
        this.page.total = res.total || 0
      }).catch(error => {
        this.contracts = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractPendingAudit.msg.fetchError'))
      }).finally(() => {
        this.loading = false
      })
    },
    partyBMetaOf(row) {
      const contacts = row.bContacts || ''
      const link = row.bLink || ''
      if (contacts && link) {
        return contacts + ' / ' + link
      }
      return contacts || link || ''
    },
    lastRecordOf(row) {
      const record = row.lastAuditLog || {}
      return record.auditTime ? record : null
    },
    formatTime(time) {
      if (!time) {
        return '-'
      }
      return String(time).replace('T', ' ').substring(0, 19)
    },
    handleSearch() {
      this.page.current = 1
      this.getList()
    },
    handleReset() {
      this.condition.searchText = ''
      this.handleSearch()
    },
    handleSizeChange(size) {
      this.page.size = size
      this.getList()
    },
    handleCurrentChange(current) {
      this.page.current = current
      this.getList()
    },
    openAuditDialog(row) {
      this.$refs.auditDialog.open({
        bizType: AUDIT_BIZ_TYPE.CONTRACT,
        bizId: row.contractId,
        state: row.state,
        contractCode: row.contractCode,
        contractName: row.contractName
      })
    },
    openDetail(row) {
      this.$refs.detailDrawer.open(row)
    }
  }
}
</script>

<style lang="scss" scoped>
.contract-pending-audit {
  .cc-surface-subtitle {
    font-size: 12px;
    color: #909399;
  }

  .cc-cell-title {
    font-weight: 600;
    color: #303133;
  }

  .cc-cell-meta {
    margin-top: 2px;
    font-size: 11px;
    color: #909399;
  }

  .cc-cell-party {
    color: #303133;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .cc-cell-party-meta {
    margin-top: 2px;
    font-size: 11px;
    color: #909399;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .cc-cell-record {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    font-size: 12px;
    color: #606266;
  }

  .cc-cell-record-time {
    color: #909399;
    font-variant-numeric: tabular-nums;
  }

  .cc-cell-empty,
  .cc-cell-no-privilege {
    font-size: 12px;
    color: #909399;
  }
}
</style>
