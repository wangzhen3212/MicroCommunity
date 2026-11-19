<template>
  <div class="contract-audit-center cc-page">
    <contract-audit-stat :statistics="statistics" @change="switchTab" />

    <div class="cc-surface">
      <div v-if="hasPrivilege('502026111600000003')" class="cc-surface-head">
        <div class="cc-surface-actions">
          <el-button @click="openConfig">
            <i class="el-icon-setting"></i>
            <span>{{ $t('contractAuditCenter.ruleSetting') }}</span>
          </el-button>
        </div>
      </div>

      <div class="cc-tabs">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          class="cc-tab"
          :class="{ 'is-active': activeTab === tab.key }"
          @click="switchTab(tab.key)"
        >
          {{ $t(tab.labelKey) }}
          <span class="cc-tab-count">{{ tab.count }}</span>
        </div>
        <p class="cc-tabs-tip">{{ $t('contractAuditCenter.nodeTip') }}</p>
      </div>

      <div class="cc-toolbar">
        <el-input
          v-model="condition.searchText"
          style="width: 280px"
          clearable
          :placeholder="$t('contractAuditCenter.filter.keywordPlaceholder')"
          @keyup.enter.native="handleSearch"
        />
        <el-select
          v-model="condition.auditType"
          style="width: 150px"
          clearable
          :placeholder="$t('contractAuditCenter.filter.auditType')"
        >
          <el-option :label="$t('contractAuditCenter.filter.auditTypeAll')" value="" />
          <el-option :label="$t('contractAuditCenter.typeContract')" value="1" />
          <el-option :label="$t('contractAuditCenter.typeChange')" value="2" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          {{ $t('common.search') }}
        </el-button>
        <el-button @click="handleReset">
          {{ $t('common.reset') }}
        </el-button>
      </div>

      <div v-loading="loading" class="cc-audit-list">
        <div v-for="(item, index) in items" :key="itemKey(item, index)" class="cc-audit-row">
          <contract-audit-item :item="item" />
          <div class="cc-audit-ops">
            <el-button
              v-if="canAudit(item)"
              type="primary"
              size="mini"
              @click="openAuditDialog(item)"
            >{{ $t('contractAuditCenter.op.audit') }}</el-button>
            <el-button
              v-if="canCancel(item)"
              size="mini"
              @click="handleCancel(item)"
            >{{ $t('contractAuditCenter.op.cancel') }}</el-button>
            <el-button size="mini" @click="handleView(item)">
              {{ $t('contractAuditCenter.op.view') }}
            </el-button>
          </div>
        </div>
        <div v-if="!loading && !items.length" class="cc-empty">
          {{ $t('contractAuditCenter.empty') }}
        </div>
      </div>

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

    <contract-audit-dialog ref="auditDialog" @success="refresh" />
    <contract-auditor-config ref="auditorConfig" @success="refresh" />
    <contract-detail-drawer ref="detailDrawer" />
  </div>
</template>

<script>
import ContractAuditStat from '@/components/contract/contractAuditStat'
import ContractAuditItem from '@/components/contract/contractAuditItem'
import ContractAuditDialog from '@/components/contract/contractAuditDialog'
import ContractAuditorConfig from '@/components/contract/contractAuditorConfig'
import ContractDetailDrawer from '@/components/contract/contractDetailDrawer'
import {
  cancelContractAudit,
  listContractAuditHistory,
  listContractAuditStatistics,
  listContractAuditTasks
} from '@/api/contract/contractAuditApi'
import { getUserId } from '@/api/user/userApi'

export default {
  name: 'ContractApplyAuditOrdersList',
  components: {
    ContractAuditStat,
    ContractAuditItem,
    ContractAuditDialog,
    ContractAuditorConfig,
    ContractDetailDrawer
  },
  data() {
    return {
      loading: false,
      statistics: {},
      items: [],
      activeTab: 'pending',
      currentUserId: '',
      directHandled: false,
      directParams: {
        auditId: '',
        bizId: ''
      },
      condition: {
        searchText: '',
        auditType: ''
      },
      page: {
        current: 1,
        size: 10,
        total: 0
      }
    }
  },
  computed: {
    tabs() {
      return [
        {
          key: 'pending',
          labelKey: 'contractAuditCenter.tab.pending',
          count: this.statistics.pending || 0
        },
        {
          key: 'pass',
          labelKey: 'contractAuditCenter.tab.pass',
          count: this.statistics.monthPass || 0
        },
        {
          key: 'reject',
          labelKey: 'contractAuditCenter.tab.reject',
          count: this.statistics.monthReject || 0
        }
      ]
    }
  },
  created() {
    this.currentUserId = getUserId()
    const query = this.$route.query || {}
    if (query.auditType) {
      this.condition.auditType = String(query.auditType)
    }
    if (query.auditId) {
      this.directParams.auditId = String(query.auditId)
    }
    if (query.bizId) {
      this.directParams.bizId = String(query.bizId)
    }
    this.loadStatistics()
    this.getList()
  },
  methods: {
    itemKey(item, index) {
      if (item && item.audit && item.audit.auditId) {
        return item.audit.auditId
      }
      return index
    },
    buildParams() {
      const params = {
        page: this.page.current,
        row: this.page.size,
        searchText: this.condition.searchText
      }
      if (this.condition.auditType) {
        params.auditType = this.condition.auditType
      }
      if (this.directParams.auditId) {
        params.auditId = this.directParams.auditId
      }
      if (this.directParams.bizId) {
        params.bizId = this.directParams.bizId
      }
      if (this.activeTab === 'pass') {
        params.state = '1'
      } else if (this.activeTab === 'reject') {
        params.state = '2'
      }
      return params
    },
    getList() {
      this.loading = true
      const request = this.activeTab === 'pending' ? listContractAuditTasks : listContractAuditHistory
      request(this.buildParams()).then(res => {
        this.items = res.data || []
        this.page.total = res.total || 0
        this.handleDirectTarget()
      }).catch(error => {
        this.items = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractAuditCenter.msg.fetchError'))
      }).finally(() => {
        this.loading = false
      })
    },
    handleDirectTarget() {
      if (this.directHandled) {
        return
      }
      const auditId = this.directParams.auditId
      const bizId = this.directParams.bizId
      if (!auditId && !bizId) {
        return
      }
      this.directHandled = true
      const target = this.items.find(item => {
        const audit = (item && item.audit) || {}
        if (auditId) {
          return String(audit.auditId) === String(auditId)
        }
        return String(audit.bizId) === String(bizId)
      })
      if (!target) {
        return
      }
      if (this.canAudit(target)) {
        this.openAuditDialog(target)
      } else {
        this.$message.warning(this.$t('contractAuditCenter.msg.noPrivilege'))
      }
    },
    loadStatistics() {
      listContractAuditStatistics({}).then(data => {
        this.statistics = data || {}
      }).catch(() => {
        this.statistics = {}
      })
    },
    refresh() {
      this.loadStatistics()
      this.getList()
    },
    handleSearch() {
      this.page.current = 1
      this.getList()
    },
    handleReset() {
      this.condition.searchText = ''
      this.condition.auditType = ''
      this.directParams = {
        auditId: '',
        bizId: ''
      }
      this.handleSearch()
    },
    switchTab(tab) {
      this.activeTab = tab
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
    currentRecord(item) {
      const audit = (item && item.audit) || {}
      const records = (item && item.records) || []
      for (let index = 0; index < records.length; index++) {
        const record = records[index]
        if (String(record.step) === String(audit.currentStep)) {
          return record
        }
      }
      return null
    },
    canAudit(item) {
      const audit = (item && item.audit) || {}
      if (String(audit.state) !== '0') {
        return false
      }
      const record = this.currentRecord(item)
      return !!record && String(record.result) === '0' &&
        String(record.auditorId) === String(this.currentUserId)
    },
    canCancel(item) {
      const audit = (item && item.audit) || {}
      if (String(audit.state) !== '0' || String(audit.currentStep) !== '1') {
        return false
      }
      if (String(audit.submitStaffId) !== String(this.currentUserId)) {
        return false
      }
      const record = this.currentRecord(item)
      return !record || String(record.result) === '0'
    },
    openAuditDialog(item) {
      this.$refs.auditDialog.open(item)
    },
    openConfig() {
      this.$refs.auditorConfig.open()
    },
    handleCancel(item) {
      const audit = (item && item.audit) || {}
      this.$confirm(this.$t('contractAuditCenter.dialog.cancelConfirm'), '提示', { type: 'warning' }).then(() => {
        cancelContractAudit({ auditId: audit.auditId }).then(() => {
          this.$message.success(this.$t('contractAuditCenter.dialog.cancelSuccess'))
          this.refresh()
        }).catch(error => {
          this.$message.error(error.message || this.$t('contractAuditCenter.dialog.error'))
        })
      }).catch(() => {
      })
    },
    handleView(item) {
      const audit = (item && item.audit) || {}
      this.$refs.detailDrawer.open({
        contractId: audit.bizId,
        contractCode: audit.bizCode,
        contractName: audit.bizName,
        amount: audit.bizAmount,
        partyB: audit.bizPartyB,
        state: audit.bizState,
        stateName: audit.bizStateName
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.contract-audit-center {
  .cc-surface-head {
    justify-content: flex-end;
    margin-bottom: 0;
  }

  .cc-tabs {
    padding-top: 8px;
  }

  .cc-tabs-tip {
    margin-left: auto;
    padding-bottom: 10px;
    font-size: 12px;
    color: #909399;
  }

  .cc-audit-list {
    min-height: 200px;
  }

  .cc-audit-row {
    display: flex;
    align-items: flex-start;
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 12px;
    padding: 12px 8px;
    margin-bottom: 12px;

    .cc-audit-ops {
      margin-left: auto;
      white-space: nowrap;
      padding: 4px 8px;
    }
  }
}
</style>
