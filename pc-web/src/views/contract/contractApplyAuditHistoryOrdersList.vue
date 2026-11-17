<template>
  <div class="contract-audit-history">
    <div class="cc-page-head">
      <div>
        <div class="cc-page-title">{{ $t('contractApplyAuditHistoryOrders.title') }}</div>
        <div class="cc-page-subtitle">{{ $t('contractAuditCenter.subtitle') }}</div>
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
      </div>
    </div>

    <div class="cc-toolbar">
      <el-input
        v-model="condition.searchText"
        style="width: 280px"
        clearable
        :placeholder="$t('contractAuditCenter.filter.keywordPlaceholder')"
        @keyup.enter.native="handleSearch"
      />
      <el-button type="primary" size="small" @click="handleSearch">
        <i class="el-icon-search"></i>
        {{ $t('common.search') }}
      </el-button>
      <el-button size="small" @click="handleReset">
        <i class="el-icon-refresh"></i>
        {{ $t('common.reset') }}
      </el-button>
    </div>

    <div v-loading="loading" class="cc-audit-list">
      <div v-for="(item, index) in items" :key="itemKey(item, index)" class="cc-audit-row">
        <contract-audit-item :item="item" />
        <div class="cc-audit-ops">
          <el-button size="mini" @click="handleView(item)">
            {{ $t('contractAuditCenter.op.view') }}
          </el-button>
        </div>
      </div>
      <div v-if="!loading && !items.length" class="cc-empty">
        {{ $t('contractAuditCenter.empty') }}
      </div>
    </div>

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

    <contract-detail-drawer ref="detailDrawer" />
  </div>
</template>

<script>
import ContractAuditItem from '@/components/contract/contractAuditItem'
import ContractDetailDrawer from '@/components/contract/contractDetailDrawer'
import { listContractAuditHistory } from '@/api/contract/contractAuditApi'

export default {
  name: 'ContractApplyAuditHistoryOrdersList',
  components: {
    ContractAuditItem,
    ContractDetailDrawer
  },
  data() {
    return {
      loading: false,
      items: [],
      activeTab: 'pass',
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
  computed: {
    tabs() {
      return [
        { key: 'pass', labelKey: 'contractAuditCenter.tab.pass' },
        { key: 'reject', labelKey: 'contractAuditCenter.tab.reject' },
        { key: 'mine', labelKey: 'contractAuditCenter.tab.mine' }
      ]
    }
  },
  created() {
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
      if (this.activeTab === 'mine') {
        params.viewType = 'mine'
      } else {
        params.state = this.activeTab === 'pass' ? '1' : '2'
      }
      return params
    },
    getList() {
      this.loading = true
      listContractAuditHistory(this.buildParams()).then(res => {
        this.items = res.data || []
        this.page.total = res.total || 0
      }).catch(error => {
        this.items = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractApplyAuditHistoryOrders.fetchError'))
      }).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
      this.page.current = 1
      this.getList()
    },
    handleReset() {
      this.condition.searchText = ''
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
.contract-audit-history {
  padding: 16px;

  .cc-page-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
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

  .cc-tabs {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 12px;

    .cc-tab {
      padding: 10px 16px;
      font-size: 14px;
      color: #606266;
      cursor: pointer;
      border-bottom: 2px solid transparent;

      &.is-active {
        color: #409eff;
        border-bottom-color: #409eff;
      }
    }
  }

  .cc-toolbar {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
  }

  .cc-audit-list {
    min-height: 200px;
  }

  .cc-audit-row {
    display: flex;
    align-items: flex-start;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 8px;
    margin-bottom: 12px;

    .cc-audit-ops {
      margin-left: auto;
      white-space: nowrap;
      padding: 4px 8px;
    }
  }
}
</style>
