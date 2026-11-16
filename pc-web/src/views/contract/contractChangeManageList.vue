<template>
  <div class="contract-change-center cc-page">
    <div class="cc-page-head">
      <div>
        <div class="cc-page-title">{{ $t('contractChangeManage.title') }}</div>
        <div class="cc-page-subtitle">{{ $t('contractChangeManage.note') }}</div>
      </div>
      <div class="cc-page-actions">
        <el-button type="primary" @click="handleApply">
          {{ $t('contractChangeManage.apply.title') }}
        </el-button>
      </div>
    </div>

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

    <div class="cc-toolbar">
      <el-input
        v-model="condition.contractName"
        style="width: 240px"
        clearable
        :placeholder="$t('contractChangeManage.search.contractName')"
        @keyup.enter.native="handleSearch"
      />
      <el-input
        v-model="condition.contractCode"
        style="width: 220px"
        clearable
        :placeholder="$t('contractChangeManage.search.contractCode')"
        @keyup.enter.native="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">
        {{ $t('common.search') }}
      </el-button>
      <el-button @click="handleReset">
        {{ $t('common.reset') }}
      </el-button>
    </div>

    <el-table v-loading="loading" :data="plans" border style="width: 100%">
      <el-table-column :label="$t('contractChangeManage.table.contractName')" min-width="200">
        <template slot-scope="scope">
          <div class="cc-cell-code">{{ scope.row.contractCode }}</div>
          <div class="cc-cell-name">{{ scope.row.contractName }}</div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractChangeManage.table.changeType')" align="center" width="120">
        <template slot-scope="scope">
          <span class="cc-badge is-info">{{ scope.row.planTypeName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractChangeManage.after')" min-width="260">
        <template slot-scope="scope">
          <span class="cc-change-after">{{ changeContent(scope.row) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractChangeManage.table.changePerson')" align="center" width="120">
        <template slot-scope="scope">{{ scope.row.changePersonName || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractChangeManage.table.applyTime')" align="center" width="170">
        <template slot-scope="scope">{{ formatTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractChangeManage.table.state')" align="center" width="110">
        <template slot-scope="scope">
          <span class="cc-badge" :class="stateClass(scope.row.state)">{{ scope.row.stateName || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.operation')" align="center" width="200" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="handleView(scope.row)">
            {{ $t('contractChangeManage.op.view') }}
          </el-button>
          <el-button
            v-if="String(scope.row.state) === '11'"
            type="text"
            size="mini"
            @click="handleAudit(scope.row)"
          >{{ $t('contractChangeManage.op.audit') }}</el-button>
          <el-button
            v-if="canDelete(scope.row)"
            type="text"
            size="mini"
            @click="handleDelete(scope.row)"
          >{{ $t('contractChangeManage.op.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!loading && !plans.length" class="cc-empty">{{ $t('contractChangeManage.empty') }}</div>

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
  .cc-cell-code {
    font-weight: 600;
    color: #303133;
  }

  .cc-cell-name {
    font-size: 12px;
    color: #606266;
  }

  .cc-change-after {
    color: #4f46e5;
    font-weight: 500;
  }
}
</style>
