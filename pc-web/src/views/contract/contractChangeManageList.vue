<template>
  <div class="contract-change-center cc-page">
    <div class="cc-page-head">
      <div>
        <div class="cc-page-title">{{ $t('contractChangeManage.title') }}</div>
        <div class="cc-page-subtitle">{{ $t('contractChangeManage.note') }}</div>
      </div>
      <div class="cc-page-actions">
        <el-input
          style="width: 220px"
          v-model="condition.contractName"
          clearable
          :placeholder="$t('contractChangeManage.search.contractName')"
          @keyup.enter.native="handleSearch"
        />
        <el-button @click="handleSearch">{{ $t('common.search') }}</el-button>
        <el-button @click="handleReset">{{ $t('common.reset') }}</el-button>
        <el-button type="primary" @click="handleApply">
          {{ $t('contractChangeManage.apply.title') }}
        </el-button>
      </div>
    </div>

    <div class="cc-change-card">
      <div class="cc-change-card-head">
        <div class="cc-tabs">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            class="cc-tab"
            :class="{ 'is-active': condition.planType === tab.key }"
            @click="switchTab(tab.key)"
          >
            {{ $t(tab.labelKey) }}
            <span class="cc-tab-count">{{ tab.count }}</span>
          </div>
        </div>
      </div>

      <div v-loading="loading" class="cc-change-list">
        <div v-for="plan in plans" :key="plan.planId" class="cc-change-row">
          <div class="cc-change-main">
            <div class="cc-change-tags">
              <span class="cc-change-kind" :class="kindClass(plan)">{{ kindText(plan) }}</span>
              <span class="cc-change-no">{{ plan.planId }}</span>
              <span class="cc-change-state" :class="stateClass(plan.state)">{{ plan.stateName || '-' }}</span>
            </div>
            <p class="cc-change-name">{{ plan.contractName }}</p>
            <p class="cc-change-sub">
              {{ plan.contractCode }}
              <span class="cc-change-dot">·</span>
              {{ $t('contractChangeManage.table.changePerson') }} {{ plan.changePersonName || '-' }}
              <span class="cc-change-dot">·</span>
              {{ formatTime(plan.createTime) }}
            </p>
          </div>
          <div class="cc-change-content">
            <p class="cc-change-label">{{ $t('contractChangeManage.content') }}</p>
            <p class="cc-change-value">{{ changeContent(plan) }}</p>
            <p v-if="plan.remark" class="cc-change-reason">{{ plan.remark }}</p>
          </div>
          <div class="cc-change-ops">
            <button type="button" class="cc-change-btn" @click="handleView(plan)">
              {{ $t('contractChangeManage.op.view') }}
            </button>
            <button
              v-if="String(plan.state) === '11'"
              type="button"
              class="cc-change-btn is-primary"
              @click="handleAudit(plan)"
            >{{ $t('contractChangeManage.op.audit') }}</button>
            <button
              v-if="canDelete(plan)"
              type="button"
              class="cc-change-btn is-danger"
              @click="handleDelete(plan)"
            >{{ $t('contractChangeManage.op.delete') }}</button>
          </div>
        </div>
        <div v-if="!loading && !plans.length" class="cc-empty">{{ $t('contractChangeManage.empty') }}</div>
      </div>

      <div class="cc-change-foot">
        <span>{{ $t('contractChangeManage.footNote') }}</span>
        <span>{{ $t('contractChangeManage.totalCount', { total: page.total }) }}</span>
      </div>
    </div>

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

    <contract-change-form ref="contractChangeForm" :contracts="contracts" @success="refresh" />
    <delete-contract-change-plan ref="deleteDialog" @success="refresh" />
  </div>
</template>

<script>
import ContractChangeForm from '@/components/contract/contractChangeForm'
import DeleteContractChangePlan from '@/components/contract/deleteContractChangePlan'
import { queryContract, queryContractType } from '@/api/contract/contractManageApi'
import { queryContractChangePlan } from '@/api/contract/contractChangeManageApi'
import { PLAN_TYPE } from '@/utils/contractPlanUtil'

export default {
  name: 'ContractChangeManageList',
  components: {
    ContractChangeForm,
    DeleteContractChangePlan
  },
  data() {
    return {
      loading: false,
      plans: [],
      contracts: [],
      contractTypes: [],
      counts: {
        all: 0,
        [PLAN_TYPE.MAIN_BODY]: 0,
        [PLAN_TYPE.LEASE]: 0,
        [PLAN_TYPE.ASSETS]: 0
      },
      condition: {
        planType: '',
        contractName: '',
        contractCode: ''
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
        { key: '', labelKey: 'contractChangeManage.tabs.all', count: this.counts.all },
        { key: PLAN_TYPE.MAIN_BODY, labelKey: 'contractChangeManage.tabs.mainBody', count: this.counts[PLAN_TYPE.MAIN_BODY] },
        { key: PLAN_TYPE.LEASE, labelKey: 'contractChangeManage.tabs.lease', count: this.counts[PLAN_TYPE.LEASE] },
        { key: PLAN_TYPE.ASSETS, labelKey: 'contractChangeManage.tabs.assets', count: this.counts[PLAN_TYPE.ASSETS] }
      ]
    }
  },
  created() {
    this.getList()
    this.getCounts()
    this.getContracts()
    this.getContractTypes()
  },
  methods: {
    formatTime(time) {
      if (!time) {
        return '-'
      }
      return String(time).replace('T', ' ').substring(0, 19)
    },
    changeContent(row) {
      const planType = String(row.planType)
      if (planType === PLAN_TYPE.MAIN_BODY) {
        let content = row.partyA || '-'
        if (row.aContacts) {
          content += ' / ' + row.aContacts
        }
        if (row.aLink) {
          content += ' / ' + row.aLink
        }
        return content
      }
      if (planType === PLAN_TYPE.LEASE) {
        return (row.startTime ? String(row.startTime).substring(0, 10) : '-') +
          ' ~ ' + (row.endTime ? String(row.endTime).substring(0, 10) : '-')
      }
      return row.remark || '-'
    },
    stateClass(state) {
      const value = String(state)
      if (value === '22') {
        return 'is-success'
      }
      if (value === '33') {
        return 'is-danger'
      }
      if (value === '11') {
        return 'is-warning'
      }
      return 'is-info'
    },
    kindText(plan) {
      const planType = String(plan.planType)
      if (planType === PLAN_TYPE.MAIN_BODY) {
        return this.$t('contractChangeManage.tabs.mainBody')
      }
      if (planType === PLAN_TYPE.LEASE) {
        return this.$t('contractChangeManage.tabs.lease')
      }
      if (planType === PLAN_TYPE.ASSETS) {
        return this.$t('contractChangeManage.tabs.assets')
      }
      return plan.planTypeName || '-'
    },
    kindClass(plan) {
      const planType = String(plan.planType)
      if (planType === PLAN_TYPE.MAIN_BODY) {
        return 'is-primary'
      }
      if (planType === PLAN_TYPE.LEASE) {
        return 'is-warning'
      }
      return 'is-success'
    },
    canDelete(row) {
      const state = String(row.state)
      return state === '10' || state === '11'
    },
    buildParams() {
      const params = {
        page: this.page.current,
        row: this.page.size,
        contractName: this.condition.contractName,
        contractCode: this.condition.contractCode
      }
      if (this.condition.planType) {
        params.planType = this.condition.planType
      }
      return params
    },
    getList() {
      this.loading = true
      queryContractChangePlan(this.buildParams()).then(res => {
        this.plans = res.data || []
        this.page.total = res.total || 0
      }).catch(error => {
        this.plans = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractChangeManage.fetchError'))
      }).finally(() => {
        this.loading = false
      })
    },
    getCounts() {
      const types = ['', PLAN_TYPE.MAIN_BODY, PLAN_TYPE.LEASE, PLAN_TYPE.ASSETS]
      types.forEach(type => {
        const params = { page: 1, row: 1 }
        if (type) {
          params.planType = type
        }
        queryContractChangePlan(params).then(res => {
          this.$set(this.counts, type === '' ? 'all' : type, res.total || 0)
        }).catch(() => {
        })
      })
    },
    getContracts() {
      queryContract({ page: 1, row: 200 }).then(res => {
        this.contracts = res.data || []
      }).catch(() => {
        this.contracts = []
      })
    },
    getContractTypes() {
      queryContractType({ page: 1, row: 100 }).then(res => {
        this.contractTypes = res.data || []
      }).catch(() => {
        this.contractTypes = []
      })
    },
    handleSearch() {
      this.page.current = 1
      this.getList()
    },
    handleReset() {
      this.condition.contractName = ''
      this.condition.contractCode = ''
      this.handleSearch()
    },
    switchTab(planType) {
      this.condition.planType = planType
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
    refresh() {
      this.getList()
      this.getCounts()
    },
    handleApply() {
      this.$refs.contractChangeForm.open()
    },
    handleView(row) {
      this.$router.push(`/views/contract/contractChangeDetails?planId=${row.planId}`)
    },
    handleAudit(row) {
      this.$router.push({
        path: '/pages/admin/contractApplyAuditOrders',
        query: {
          bizId: row.planId,
          auditType: '2'
        }
      })
    },
    handleDelete(row) {
      this.$refs.deleteDialog.open(row)
    }
  }
}
</script>

<style lang="scss" scoped>
.contract-change-center {
  .cc-change-card {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 12px;
    overflow: hidden;
  }

  .cc-change-card-head {
    padding: 8px 20px 0;
    border-bottom: 1px solid #f2f3f5;

    .cc-tabs {
      margin-bottom: 0;
      border-bottom: none;
    }
  }

  .cc-change-list {
    min-height: 120px;
  }

  .cc-change-row {
    display: flex;
    align-items: flex-start;
    gap: 16px;
    padding: 16px 20px;
    border-bottom: 1px solid #f5f6f8;
    transition: background-color .2s;

    &:hover {
      background: #fafbfc;
    }
  }

  .cc-change-main {
    flex: 1;
    min-width: 220px;
  }

  .cc-change-tags {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
  }

  .cc-change-kind {
    height: 18px;
    line-height: 18px;
    padding: 0 6px;
    border-radius: 4px;
    font-size: 10px;
    font-weight: 500;

    &.is-primary {
      color: #4f46e5;
      background: #eef2ff;
    }

    &.is-warning {
      color: #e6a23c;
      background: #fdf6ec;
    }

    &.is-success {
      color: #67c23a;
      background: #f0f9eb;
    }
  }

  .cc-change-no {
    font-size: 11px;
    color: #909399;
    font-variant-numeric: tabular-nums;
  }

  .cc-change-state {
    height: 18px;
    line-height: 18px;
    padding: 0 6px;
    border-radius: 9px;
    font-size: 10px;

    &.is-warning {
      color: #e6a23c;
      background: #fdf6ec;
    }

    &.is-success {
      color: #67c23a;
      background: #f0f9eb;
    }

    &.is-danger {
      color: #f56c6c;
      background: #fef0f0;
    }

    &.is-info {
      color: #909399;
      background: #f4f4f5;
    }
  }

  .cc-change-name {
    margin: 6px 0 0;
    font-size: 13.5px;
    font-weight: 500;
    line-height: 20px;
    color: #303133;
  }

  .cc-change-sub {
    margin: 2px 0 0;
    font-size: 11px;
    line-height: 16px;
    color: #909399;

    .cc-change-dot {
      margin: 0 6px;
      color: #dcdfe6;
    }
  }

  .cc-change-content {
    flex: 1;
    min-width: 240px;
  }

  .cc-change-label {
    margin: 0 0 4px;
    font-size: 11px;
    color: #909399;
  }

  .cc-change-value {
    display: inline-block;
    margin: 0;
    padding: 4px 8px;
    border-radius: 8px;
    background: #f0f9eb;
    font-size: 12.5px;
    line-height: 18px;
    color: #303133;
  }

  .cc-change-reason {
    margin: 6px 0 0;
    font-size: 11px;
    line-height: 16px;
    color: #909399;
  }

  .cc-change-ops {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: none;
    margin-left: auto;
  }

  .cc-change-btn {
    height: 32px;
    padding: 0 12px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    background: #fff;
    font-size: 12px;
    color: #606266;
    cursor: pointer;
    transition: color .2s, border-color .2s, background-color .2s;

    &:hover {
      border-color: #c7d2fe;
      color: #4f46e5;
    }

    &.is-primary {
      border-color: #4f46e5;
      background: #4f46e5;
      color: #fff;

      &:hover {
        border-color: #6366f1;
        background: #6366f1;
        color: #fff;
      }
    }

    &.is-danger:hover {
      border-color: #fbc4c4;
      color: #f56c6c;
    }
  }

  .cc-change-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 20px;
    border-top: 1px solid #f2f3f5;
    font-size: 12px;
    color: #909399;
  }
}

</style>
