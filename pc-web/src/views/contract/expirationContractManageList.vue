<template>
  <div class="contract-remind cc-page">
    <div class="cc-surface">
    <div class="cc-surface-head">
      <div class="cc-surface-title">{{ $t('contractRemind.filter.title') }}</div>
    </div>

    <div class="cc-toolbar">
      <el-input
        v-model="condition.searchText"
        style="width: 240px"
        clearable
        :placeholder="$t('contractRemind.filter.keywordPlaceholder')"
        @keyup.enter.native="handleSearch"
      />
      <el-select v-model="condition.days" style="width: 150px">
        <el-option :label="$t('contractRemind.filter.days') + ' 30'" value="30" />
        <el-option :label="$t('contractRemind.filter.days') + ' 60'" value="60" />
        <el-option :label="$t('contractRemind.filter.days') + ' 90'" value="90" />
      </el-select>
      <el-checkbox v-model="includeExpired">{{ $t('contractRemind.filter.includeExpired') }}</el-checkbox>
      <el-button type="primary" @click="handleSearch">
        <i class="el-icon-search"></i>
        {{ $t('common.search') }}
      </el-button>
      <el-button @click="handleReset">
        <i class="el-icon-refresh"></i>
        {{ $t('common.reset') }}
      </el-button>
    </div>
    </div>

    <div class="cc-surface">
    <div class="cc-surface-head">
      <div class="cc-surface-title">{{ $t('contractRemind.title') }}</div>
      <div class="cc-surface-actions">
        <span v-if="selection.length" class="cc-selected">
          {{ $t('contractRemind.bulk.selected', { count: selection.length }) }}
        </span>
        <el-button
          v-if="hasPrivilege('502026111600000006')"
          :disabled="!selection.length"
          @click="openPushDialog"
        >
          <i class="el-icon-bell"></i>
          <span>{{ $t('contractRemind.bulk.push') }}</span>
        </el-button>
        <el-button :disabled="!selection.length" @click="handleBatchRenew">
          <i class="el-icon-refresh-left"></i>
          <span>{{ $t('contractRemind.bulk.renew') }}</span>
        </el-button>
        <el-button v-if="hasPrivilege('502026111600000004')" @click="handleExport">
          <i class="el-icon-download"></i>
          <span>{{ $t('contractRemind.filter.export') }}</span>
        </el-button>
      </div>
    </div>

    <el-table
      ref="table"
      v-loading="loading"
      :data="reminds"
      :empty-text="$t('contractRemind.empty')"
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" />
      <el-table-column :label="$t('contractRemind.table.daysLeft')" align="center" width="130">
        <template slot-scope="scope">
          <span class="cc-badge" :class="'is-' + daysLeftType(scope.row.daysLeft)">
            {{ daysText(scope.row.daysLeft) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractRemind.table.code')" min-width="200">
        <template slot-scope="scope">
          <div class="cc-cell-code">{{ scope.row.contractCode }}</div>
          <div class="cc-cell-name">{{ scope.row.contractName }}</div>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractRemind.table.category')" align="center" width="120">
        <template slot-scope="scope">{{ scope.row.templateName || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractRemind.table.partyB')" min-width="150">
        <template slot-scope="scope">{{ scope.row.partyB || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractRemind.table.endTime')" align="center" width="120">
        <template slot-scope="scope">{{ formatEndTime(scope.row.endTime) }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractRemind.table.amount')" align="right" width="130">
        <template slot-scope="scope">{{ formatAmount(scope.row.amount) }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractRemind.table.operator')" align="center" width="120">
        <template slot-scope="scope">{{ scope.row.operator || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractRemind.table.logs')" min-width="180">
        <template slot-scope="scope">
          <div v-if="scope.row.logs && scope.row.logs.length" class="cc-remind-log">
            <span
              v-for="(log, index) in scope.row.logs.slice(0, 3)"
              :key="log.logId || index"
              class="cc-badge is-info"
            >{{ logText(log) }}</span>
          </div>
          <span v-else class="cc-remind-empty">{{ $t('contractRemind.log.empty') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractRemind.table.operation')" align="right" width="180" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="handleDetail(scope.row)">
            {{ $t('contractRemind.op.detail') }}
          </el-button>
          <el-button type="text" size="mini" @click="handleRenew(scope.row)">
            {{ $t('contractRemind.op.renew') }}
          </el-button>
          <el-button type="text" size="mini" @click="handlePush(scope.row)">
            {{ $t('contractRemind.op.push') }}
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

    <el-dialog :title="$t('contractRemind.push.title')" :visible.sync="pushDialog" width="460px">
      <el-form label-width="90px">
        <el-form-item :label="$t('contractRemind.push.channels')">
          <el-checkbox-group v-model="pushChannels">
            <el-checkbox label="inner">{{ $t('contractRemind.push.inner') }}</el-checkbox>
            <el-checkbox label="sms">{{ $t('contractRemind.push.sms') }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="pushDialog = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="pushing" @click="handlePushSubmit">
          {{ $t('contractRemind.push.confirm') }}
        </el-button>
      </span>
    </el-dialog>

    <contract-detail-drawer ref="detailDrawer" />
  </div>
</template>

<script>
import ContractDetailDrawer from '@/components/contract/contractDetailDrawer'
import { listContractReminds, pushContractRemind } from '@/api/contract/contractRemindApi'
import { exportContracts } from '@/api/contract/contractLedgerApi'
import { daysLeftType, formatAmount, saveBase64File } from '@/utils/contractUtil'

export default {
  name: 'ExpirationContractManageList',
  components: {
    ContractDetailDrawer
  },
  data() {
    return {
      loading: false,
      reminds: [],
      selection: [],
      pushDialog: false,
      pushing: false,
      pushChannels: ['inner'],
      pushTargets: [],
      includeExpired: false,
      condition: {
        searchText: '',
        days: '30'
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
    daysLeftType,
    daysText(days) {
      const value = Number(days || 0)
      if (value < 0) {
        return this.$t('contractRemind.daysLeft.expired', { days: Math.abs(value) })
      }
      return this.$t('contractRemind.daysLeft.remain', { days: value })
    },
    formatEndTime(time) {
      if (!time) {
        return '-'
      }
      return String(time).replace('T', ' ').substring(0, 10)
    },
    logText(log) {
      const type = log.remindType === 'manual'
        ? this.$t('contractRemind.log.manual')
        : log.remindType
      const channel = log.channel === 'sms'
        ? this.$t('contractRemind.log.channelSms')
        : this.$t('contractRemind.log.channelInner')
      return type + '/' + channel
    },
    buildParams() {
      return {
        page: this.page.current,
        row: this.page.size,
        searchText: this.condition.searchText,
        days: this.condition.days,
        includeExpired: this.includeExpired ? '1' : '0'
      }
    },
    getList() {
      this.loading = true
      listContractReminds(this.buildParams()).then(res => {
        this.reminds = res.data || []
        this.page.total = res.total || 0
      }).catch(error => {
        this.reminds = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractRemind.msg.fetchError'))
      }).finally(() => {
        this.loading = false
      })
    },
    handleSearch() {
      this.page.current = 1
      this.getList()
    },
    handleReset() {
      this.condition = {
        searchText: '',
        days: '30'
      }
      this.includeExpired = false
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
    handleSelectionChange(rows) {
      this.selection = rows
    },
    handleDetail(row) {
      this.$refs.detailDrawer.open(row)
    },
    handleRenew(row) {
      this.$router.push({
        path: '/views/contract/addContract',
        query: { parentContractId: row.contractId }
      })
    },
    handleBatchRenew() {
      if (!this.selection.length) {
        this.$message.warning(this.$t('contractRemind.push.noSelection'))
        return
      }
      this.$router.push({
        path: '/views/contract/addContract',
        query: { parentContractId: this.selection[0].contractId }
      })
    },
    handlePush(row) {
      this.pushTargets = [row.contractId]
      this.pushChannels = ['inner']
      this.pushDialog = true
    },
    openPushDialog() {
      if (!this.selection.length) {
        this.$message.warning(this.$t('contractRemind.push.noSelection'))
        return
      }
      this.pushTargets = this.selection.map(item => item.contractId)
      this.pushChannels = ['inner']
      this.pushDialog = true
    },
    handlePushSubmit() {
      if (!this.pushChannels.length) {
        this.$message.warning(this.$t('contractRemind.push.noSelection'))
        return
      }
      this.pushing = true
      pushContractRemind({
        contractIds: this.pushTargets,
        channels: this.pushChannels.join(','),
        remindType: 'manual'
      }).then(data => {
        this.$message.success(this.$t('contractRemind.push.success', {
          success: data.successCount || 0,
          fail: data.failCount || 0
        }))
        this.pushDialog = false
        this.getList()
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractRemind.push.error'))
      }).finally(() => {
        this.pushing = false
      })
    },
    handleExport() {
      const params = this.buildParams()
      delete params.page
      delete params.row
      if (this.selection.length) {
        params.contractIds = this.selection.map(item => item.contractId)
      }
      exportContracts({ ...params, exportType: 'remind' }).then(data => {
        saveBase64File(data.base64, data.fileName)
        this.$message.success(this.$t('contractRemind.msg.exportSuccess'))
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractRemind.msg.exportError'))
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.contract-remind {
  .cc-selected {
    margin-right: 4px;
    font-size: 12px;
    color: #4f46e5;
    white-space: nowrap;
  }

  .cc-cell-code {
    font-weight: 600;
    color: #303133;
  }

  .cc-cell-name {
    font-size: 12px;
    color: #606266;
  }

  .cc-remind-log {
    .cc-badge {
      margin-right: 6px;
    }
  }

  .cc-remind-empty {
    font-size: 12px;
    color: #c0c4cc;
  }
}
</style>
