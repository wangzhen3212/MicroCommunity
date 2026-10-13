<template>
  <div class="space-confirm">
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpaceConfirm.statTodayCount') }}</div>
        <div class="stat-value stat-emerald">{{ summary.todayCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpaceConfirm.statTodayAmount') }}</div>
        <div class="stat-value stat-primary">￥{{ summary.todayAmount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpaceConfirm.statWaitCount') }}</div>
        <div class="stat-value stat-amber">{{ summary.waitCount }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpaceConfirm.statTotalCount') }}</div>
        <div class="stat-value stat-slate">{{ summary.totalCount }}</div>
      </div>
    </div>

    <el-row :gutter="16" class="main-row">
      <el-col :md="24" :lg="10">
        <space-scan-box-panel
          ref="scanBox"
          :auto-verify.sync="verify.auto"
          :scan-count="verify.count"
          @scan="handleScan"
        />
      </el-col>
      <el-col :md="24" :lg="14">
        <space-confirm-info-panel
          ref="infoPanel"
          :current="verify.current"
          :done="verify.done"
          :not-found="verify.notFound"
          :auto-verify="verify.auto"
          :verifying="verifying"
          :wait-count="summary.waitCount"
          @confirm="handleConfirm(false)"
          @reset="handleReset"
          @view-order="handleViewOrder"
        />
      </el-col>
    </el-row>

    <div class="panel record-panel">
      <div class="panel-header">
        <span class="panel-title">{{ $t('communitySpaceConfirm.recordTitle') }}</span>
        <el-input
          v-model="condition.keyword"
          size="small"
          class="keyword-input"
          :placeholder="$t('communitySpaceConfirm.recordKeywordPlaceholder')"
          clearable
          @input="handleConditionChange"
        />
        <el-date-picker
          v-model="condition.verifyDate"
          size="small"
          type="date"
          value-format="yyyy-MM-dd"
          class="date-picker"
          :placeholder="$t('communitySpaceConfirm.recordDate')"
          @change="handleSearch"
        />
        <el-button type="text" class="reset-link" @click="handleConditionReset">
          {{ $t('common.reset') }}
        </el-button>
        <span class="record-count">
          {{ $t('communitySpaceConfirm.recordCount', { count: pagination.total }) }}
        </span>
      </div>

      <el-table v-loading="loading" :data="records" class="record-table">
        <el-table-column prop="createTime" :label="$t('communitySpaceConfirm.recordVerifyTime')" width="150" />
        <el-table-column prop="orderId" :label="$t('communitySpaceConfirm.orderId')" width="180">
          <template slot-scope="scope">{{ scope.row.cspOrderId || scope.row.orderId }}</template>
        </el-table-column>
        <el-table-column prop="venueName" :label="$t('communitySpaceConfirm.venue')" width="120" />
        <el-table-column prop="spaceName" :label="$t('communitySpaceConfirm.space')" width="120" />
        <el-table-column prop="personName" :label="$t('communitySpaceConfirm.personName')" width="100" />
        <el-table-column prop="personTel" :label="$t('communitySpaceConfirm.personTel')" width="120" />
        <el-table-column prop="appointmentTime" :label="$t('communitySpaceConfirm.appointDate')" width="110" />
        <el-table-column :label="$t('communitySpaceConfirm.appointHours')" width="130">
          <template slot-scope="scope">{{ formatHours(scope.row) }}</template>
        </el-table-column>
        <el-table-column :label="$t('communitySpaceConfirm.receivableAmount')" width="110" align="right">
          <template slot-scope="scope">￥{{ scope.row.receivableAmount || '0.00' }}</template>
        </el-table-column>
        <el-table-column :label="$t('communitySpaceConfirm.recordVerifyMethod')" width="130">
          <template slot-scope="scope">
            <span class="method-tag" :class="scope.row.verifyMethod === '2' ? 'method-manual' : 'method-scan'">
              {{ formatVerifyMethod(scope.row.verifyMethod) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('communitySpaceConfirm.recordVerifier')" width="110">
          <template slot-scope="scope">{{ scope.row.verifierName || '-' }}</template>
        </el-table-column>
        <el-table-column :label="$t('communitySpaceConfirm.recordOperation')" width="100" align="right">
          <template slot-scope="scope">
            <el-button type="text" class="revoke-button" @click="handleRevoke(scope.row)">
              {{ $t('communitySpaceConfirm.revokeVerify') }}
            </el-button>
          </template>
        </el-table-column>
        <template slot="empty">
          <span>{{ $t('communitySpaceConfirm.recordEmpty') }}</span>
        </template>
      </el-table>

      <el-pagination
        class="record-pagination"
        :current-page="pagination.current"
        :page-sizes="[10, 20, 30, 50]"
        :page-size="pagination.size"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>
  </div>
</template>

<script>
import SpaceScanBoxPanel from '@/components/community/SpaceScanBoxPanel.vue'
import SpaceConfirmInfoPanel from '@/components/community/SpaceConfirmInfoPanel.vue'
import {
  listCommunitySpaceConfirmOrder,
  saveCommunitySpaceConfirmOrder,
  getCommunitySpaceConfirmOrder,
  deleteCommunitySpaceConfirmOrder,
  listCommunitySpaceConfirmOrderSummary
} from '@/api/community/communitySpaceConfirmApi'
import { getCommunityId } from '@/api/community/communityApi'

const AUTO_VERIFY_DELAY = 600
const SEARCH_DELAY = 400
const VERIFY_METHOD_SCAN = '1'
const VERIFY_METHOD_HAND = '2'

export default {
  name: 'CommunitySpaceConfirmList',
  components: {
    SpaceScanBoxPanel,
    SpaceConfirmInfoPanel
  },
  data() {
    return {
      communityId: getCommunityId(),
      summary: {
        todayCount: 0,
        todayAmount: '0.00',
        waitCount: 0,
        totalCount: 0
      },
      verify: {
        current: null,
        done: null,
        notFound: '',
        auto: true,
        count: 0,
        method: VERIFY_METHOD_SCAN
      },
      condition: {
        keyword: '',
        verifyDate: ''
      },
      records: [],
      pagination: {
        current: 1,
        size: 10,
        total: 0
      },
      loading: false,
      verifying: false,
      autoVerifyTimer: null,
      searchTimer: null
    }
  },
  watch: {
    'verify.auto': function (value) {
      this.$message.info(value ? this.$t('communitySpaceConfirm.autoVerifyOn') : this.$t('communitySpaceConfirm.autoVerifyOff'))
    }
  },
  created() {
    this.loadSummary()
    this.loadRecords()
  },
  beforeDestroy() {
    this.clearAutoVerifyTimer()
    if (this.searchTimer) {
      clearTimeout(this.searchTimer)
      this.searchTimer = null
    }
  },
  methods: {
    async loadSummary() {
      try {
        const data = await listCommunitySpaceConfirmOrderSummary({ communityId: this.communityId })
        if (data) {
          this.summary = {
            todayCount: data.todayCount || 0,
            todayAmount: data.todayAmount || '0.00',
            waitCount: data.waitCount || 0,
            totalCount: data.totalCount || 0
          }
        }
      } catch (error) {
        this.$message.error(error.message || this.$t('communitySpaceConfirm.loadFailed'))
      }
    },
    async loadRecords() {
      this.loading = true
      try {
        const params = {
          communityId: this.communityId,
          statusCd: '0',
          page: this.pagination.current,
          row: this.pagination.size
        }
        if (this.condition.keyword) {
          params.keyword = this.condition.keyword
        }
        if (this.condition.verifyDate) {
          params.beginTime = this.condition.verifyDate
          params.endTime = this.condition.verifyDate
        }
        const res = await listCommunitySpaceConfirmOrder(params)
        this.records = res.data || []
        this.pagination.total = res.total || 0
      } catch (error) {
        this.$message.error(error.message || this.$t('communitySpaceConfirm.loadFailed'))
      } finally {
        this.loading = false
      }
    },
    handleConditionChange() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
      this.searchTimer = setTimeout(this.handleSearch, SEARCH_DELAY)
    },
    handleSearch() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
      this.pagination.current = 1
      this.loadRecords()
    },
    handleConditionReset() {
      this.condition.keyword = ''
      this.condition.verifyDate = ''
      this.handleSearch()
    },
    handleSizeChange(size) {
      this.pagination.size = size
      this.pagination.current = 1
      this.loadRecords()
    },
    handleCurrentChange(current) {
      this.pagination.current = current
      this.loadRecords()
    },
    async handleScan(code, isManual) {
      if (!code) {
        return
      }
      this.clearAutoVerifyTimer()
      this.verify.count += 1
      this.verify.done = null
      this.verify.method = isManual ? VERIFY_METHOD_HAND : VERIFY_METHOD_SCAN
      try {
        const data = await getCommunitySpaceConfirmOrder({
          communityId: this.communityId,
          code: code
        })
        if (!data || !data.cspId) {
          this.verify.current = null
          this.verify.notFound = code
          this.$message.warning(this.$t('communitySpaceConfirm.notFoundTip', { code: code }))
          this.focusScanBox()
          return
        }
        this.verify.current = data
        this.verify.notFound = ''
        if (data.multiHit) {
          this.$message.warning(this.$t('communitySpaceConfirm.multiHitTip'))
        }
        const level = this.getVerifyLevel(data)
        if (level !== 'ok') {
          this.$message.warning(this.getVerifyTitle(data))
          this.focusScanBox()
          return
        }
        if (this.verify.auto) {
          const snapshot = data
          this.autoVerifyTimer = setTimeout(() => {
            if (this.verify.current === snapshot) {
              this.handleConfirm(true)
            }
          }, AUTO_VERIFY_DELAY)
        } else {
          this.$message.info(this.$t('communitySpaceConfirm.confirmVerifyTip'))
          this.focusScanBox()
        }
      } catch (error) {
        this.verify.current = null
        this.verify.notFound = code
        this.$message.error(error.message || this.$t('communitySpaceConfirm.loadFailed'))
        this.focusScanBox()
      }
    },
    async handleConfirm(isAuto) {
      this.clearAutoVerifyTimer()
      const current = this.verify.current
      if (!current) {
        this.$message.warning(this.$t('communitySpaceConfirm.confirmTip'))
        return
      }
      if (this.getVerifyLevel(current) !== 'ok') {
        this.$message.warning(this.getVerifyTitle(current))
        return
      }
      this.verifying = true
      try {
        const res = await saveCommunitySpaceConfirmOrder({
          communityId: this.communityId,
          timeId: current.timeId,
          verifyMethod: this.verify.method || VERIFY_METHOD_SCAN
        })
        const data = res.data && res.data.length > 0 ? res.data[0] : null
        this.verify.done = data ? Object.assign({}, data, { times: current.times }) : current
        this.verify.current = null
        this.verify.notFound = ''
        this.$message.success(isAuto ? this.$t('communitySpaceConfirm.verifySuccessTip') : this.$t('communitySpaceConfirm.verifySuccess'))
        this.focusScanBox()
        this.pagination.current = 1
        this.loadRecords()
        this.loadSummary()
      } catch (error) {
        this.$message.error(error.message || this.$t('common.operateFailed'))
      } finally {
        this.verifying = false
      }
    },
    handleReset() {
      this.clearAutoVerifyTimer()
      this.verify.current = null
      this.verify.done = null
      this.verify.notFound = ''
      const scanBox = this.$refs.scanBox
      if (scanBox) {
        scanBox.clear()
      }
    },
    handleViewOrder() {
      this.$router.push('/pages/property/communitySpacePersonManage')
    },
    handleRevoke(row) {
      this.$confirm(
        this.$t('communitySpaceConfirm.revokeConfirmTip', {
          venue: row.venueName || '-',
          space: row.spaceName || '-',
          date: row.appointmentTime || '-',
          hours: this.formatHours(row)
        }),
        this.$t('communitySpaceConfirm.revokeConfirmTitle'),
        {
          confirmButtonText: this.$t('common.confirm'),
          cancelButtonText: this.$t('common.cancel'),
          type: 'warning'
        }
      ).then(() => {
        this.doRevoke(row)
      }).catch(() => {})
    },
    async doRevoke(row) {
      try {
        await deleteCommunitySpaceConfirmOrder({
          communityId: this.communityId,
          orderId: row.orderId
        })
        this.$message.success(this.$t('communitySpaceConfirm.revokeSuccess'))
        this.loadRecords()
        this.loadSummary()
      } catch (error) {
        this.$message.error(error.message || this.$t('communitySpaceConfirm.revokeFailed'))
      }
    },
    focusScanBox() {
      const scanBox = this.$refs.scanBox
      if (scanBox) {
        scanBox.focus()
      }
    },
    clearAutoVerifyTimer() {
      if (this.autoVerifyTimer) {
        clearTimeout(this.autoVerifyTimer)
        this.autoVerifyTimer = null
      }
    },
    getVerifyLevel(order) {
      const infoPanel = this.$refs.infoPanel
      if (infoPanel) {
        return infoPanel.getLevel(order)
      }
      return 'ok'
    },
    getVerifyTitle(order) {
      const infoPanel = this.$refs.infoPanel
      if (infoPanel) {
        return infoPanel.getCheckTitle(order)
      }
      return order.title || order.desc || ''
    },
    formatHours(row) {
      if (row.hours === null || row.hours === undefined || row.hours === '') {
        return '-'
      }
      const hours = Number(row.hours)
      if (isNaN(hours)) {
        return '-'
      }
      return this.pad(hours) + ':00~' + this.pad(hours + 1) + ':00'
    },
    pad(value) {
      return value < 10 ? '0' + value : '' + value
    },
    formatVerifyMethod(method) {
      if (method === VERIFY_METHOD_HAND) {
        return this.$t('communitySpaceConfirm.verifyMethodManual')
      }
      return this.$t('communitySpaceConfirm.verifyMethodScan')
    }
  }
}
</script>

<style lang="scss" scoped>
.space-confirm {
  padding: 16px;
  background: #f1f5f9;
  min-height: calc(100vh - 90px);
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.stat-card {
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.12);
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

.stat-value {
  margin-top: 4px;
  font-size: 24px;
  font-weight: 700;
}

.stat-slate {
  color: #1e293b;
}

.stat-emerald {
  color: #059669;
}

.stat-primary {
  color: #2563eb;
}

.stat-amber {
  color: #d97706;
}

.main-row {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.main-row ::v-deep .el-col {
  display: flex;
}

.main-row ::v-deep .el-col > * {
  flex: 1;
  min-width: 0;
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.12);
}

.panel-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.keyword-input {
  width: 200px;
}

.date-picker {
  width: 150px;
}

.reset-link {
  padding: 0;
  color: #64748b;
}

.record-count {
  margin-left: auto;
  font-size: 12px;
  color: #64748b;
}

.record-table {
  width: 100%;
}

.method-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.method-scan {
  background: #eff6ff;
  color: #2563eb;
}

.method-manual {
  background: #f1f5f9;
  color: #64748b;
}

.revoke-button {
  color: #f43f5e;
}

.record-pagination {
  padding: 12px 16px;
  text-align: right;
}

@media (max-width: 1199px) {
  .main-row ::v-deep .el-col:first-child {
    margin-bottom: 16px;
  }

  .stat-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
