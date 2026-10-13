<template>
  <div class="order-manage">
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpacePersonManage.statTotal') }}</div>
        <div class="stat-value stat-slate">{{ summary.total }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpacePersonManage.statPending') }}</div>
        <div class="stat-value stat-amber">{{ summary.pending }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpacePersonManage.statActive') }}</div>
        <div class="stat-value stat-primary">{{ summary.active }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpacePersonManage.statReceived') }}</div>
        <div class="stat-value stat-emerald">￥{{ summary.receivedTotal }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpacePersonManage.statCanceled') }}</div>
        <div class="stat-value stat-rose">{{ summary.canceled }}</div>
      </div>
    </div>

    <div class="panel manage-panel">
      <div class="filter-row">
        <el-input v-model="conditions.keyword" size="small" class="filter-input" clearable
          :placeholder="$t('communitySpacePersonManage.keywordPlaceholder')" @keyup.enter.native="handleQuery" />
        <el-select v-model="conditions.venueId" size="small" class="filter-select" clearable
          :placeholder="$t('communitySpacePersonManage.venuePlaceholder')" @change="handleVenueChange">
          <el-option v-for="item in venues" :key="item.venueId" :label="item.name" :value="item.venueId" />
        </el-select>
        <el-select v-model="conditions.spaceId" size="small" class="filter-select" clearable
          :placeholder="$t('communitySpacePersonManage.spacePlaceholder')">
          <el-option v-for="item in filterSpaces" :key="item.spaceId" :label="item.name" :value="item.spaceId" />
        </el-select>
        <el-select v-model="conditions.state" size="small" class="filter-select" clearable
          :placeholder="$t('communitySpacePersonManage.statePlaceholder')">
          <el-option v-for="(label, key) in stateOptions" :key="key" :label="label" :value="key" />
        </el-select>
        <el-date-picker v-model="dateRange" size="small" class="filter-date" type="daterange" value-format="yyyy-MM-dd"
          range-separator="~" :start-placeholder="$t('communitySpacePersonManage.appointmentDate')"
          :end-placeholder="$t('communitySpacePersonManage.appointmentDate')" @change="handleDateChange" />
        <el-button type="primary" size="small" @click="handleQuery">
          {{ $t('communitySpacePersonManage.search') }}
        </el-button>
        <el-button size="small" @click="handleReset">
          {{ $t('communitySpacePersonManage.reset') }}
        </el-button>
        <el-button size="small" class="export-button" @click="handleExport">
          <i class="el-icon-download"></i>
          {{ $t('communitySpacePersonManage.export') }}
        </el-button>
      </div>

      <el-table v-loading="loading" :data="orders" style="width: 100%">
        <el-table-column prop="orderId" :label="$t('communitySpacePersonManage.orderId')" min-width="170" />
        <el-table-column prop="venueName" :label="$t('communitySpacePersonManage.venue')" min-width="120" />
        <el-table-column prop="spaceName" :label="$t('communitySpacePersonManage.space')" min-width="110" />
        <el-table-column prop="personName" :label="$t('communitySpacePersonManage.appointmentPerson')" min-width="110" />
        <el-table-column prop="personTel" :label="$t('communitySpacePersonManage.appointmentTel')" min-width="130" />
        <el-table-column prop="appointmentTime" :label="$t('communitySpacePersonManage.appointmentDate')" min-width="120" />
        <el-table-column :label="$t('communitySpacePersonManage.appointmentTime')" min-width="130">
          <template slot-scope="scope">{{ formatTimes(scope.row.times) }}</template>
        </el-table-column>
        <el-table-column :label="$t('communitySpacePersonManage.receivableAmount')" min-width="100" align="right">
          <template slot-scope="scope">￥{{ scope.row.receivableAmount || '0.00' }}</template>
        </el-table-column>
        <el-table-column :label="$t('communitySpacePersonManage.receivedAmount')" min-width="100" align="right">
          <template slot-scope="scope">￥{{ scope.row.receivedAmount || '0.00' }}</template>
        </el-table-column>
        <el-table-column :label="$t('communitySpacePersonManage.payWay')" min-width="110">
          <template slot-scope="scope">{{ scope.row.payWayName || $t('communitySpacePersonManage.payWayNone') }}</template>
        </el-table-column>
        <el-table-column :label="$t('communitySpacePersonManage.state')" min-width="110" align="center">
          <template slot-scope="scope">
            <span class="state-pill" :class="'state-' + scope.row.state">{{ stateText(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="$t('communitySpacePersonManage.remark')" min-width="140"
          show-overflow-tooltip>
          <template slot-scope="scope">{{ scope.row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column :label="$t('communitySpacePersonManage.operation')" align="right" width="260" fixed="right">
          <template slot-scope="scope">
            <el-button v-if="canShowQrcode(scope.row)" type="text" @click="handleQrcode(scope.row)">
              {{ $t('communitySpacePersonManage.verifyQrcode') }}
            </el-button>
            <el-button type="text" @click="handleDetail(scope.row)">
              {{ $t('communitySpacePersonManage.detail') }}
            </el-button>
            <el-button v-if="canCancel(scope.row)" type="text" class="danger-text" @click="handleCancel(scope.row)">
              {{ $t('communitySpacePersonManage.cancelAppointment') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination class="pagination" :current-page="page.current" :page-sizes="[10, 20, 30, 50]"
        :page-size="page.size" layout="total, sizes, prev, pager, next, jumper" :total="page.total"
        @size-change="handleSizeChange" @current-change="handlePageChange" />
    </div>

    <community-space-person-qrcode ref="qrcodeModal" />
    <community-space-person-detail ref="detailModal" />
    <delete-community-space-person ref="deleteModal" @success="handleReload" />
  </div>
</template>

<script>
import {
  listCommunitySpacePerson,
  listCommunitySpacePersonSummary,
  listCommunityVenue,
  listCommunitySpace
} from '@/api/community/communitySpacePersonManageApi'
import { getCommunityId } from '@/api/community/communityApi'
import CommunitySpacePersonDetail from '@/components/community/CommunitySpacePersonDetail'
import CommunitySpacePersonQrcode from '@/components/community/CommunitySpacePersonQrcode'
import DeleteCommunitySpacePerson from '@/components/community/deleteCommunitySpacePerson'

export default {
  name: 'CommunitySpacePersonManageList',
  components: {
    CommunitySpacePersonQrcode,
    CommunitySpacePersonDetail,
    DeleteCommunitySpacePerson
  },
  data() {
    return {
      loading: false,
      venues: [],
      allSpaces: [],
      orders: [],
      dateRange: [],
      summary: {
        total: 0,
        pending: '0',
        active: '0',
        finished: '0',
        canceled: '0',
        receivableTotal: '0.00',
        receivedTotal: '0.00'
      },
      conditions: {
        keyword: '',
        venueId: '',
        spaceId: '',
        state: '',
        beginDate: '',
        endDate: '',
        communityId: ''
      },
      page: {
        current: 1,
        size: 10,
        total: 0
      }
    }
  },
  computed: {
    stateOptions() {
      return {
        WP: this.$t('communitySpacePersonManage.stateOptions.WP'),
        S: this.$t('communitySpacePersonManage.stateOptions.S'),
        C: this.$t('communitySpacePersonManage.stateOptions.C'),
        CL: this.$t('communitySpacePersonManage.stateOptions.CL'),
        W: this.$t('communitySpacePersonManage.stateOptions.W'),
        F: this.$t('communitySpacePersonManage.stateOptions.F')
      }
    },
    filterSpaces() {
      if (!this.conditions.venueId) {
        return this.allSpaces
      }
      return this.allSpaces.filter(item => item.venueId === this.conditions.venueId)
    }
  },
  created() {
    this.conditions.communityId = getCommunityId()
    this.loadVenues()
    this.loadSpaces()
    this.handleReload()
  },
  methods: {
    async loadVenues() {
      try {
        const res = await listCommunityVenue({ page: 1, row: 100, communityId: this.conditions.communityId })
        this.venues = res.data || []
      } catch (error) {
        this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
      }
    },
    async loadSpaces() {
      try {
        const res = await listCommunitySpace({ page: 1, row: 1000, communityId: this.conditions.communityId })
        this.allSpaces = res.data || []
      } catch (error) {
        this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
      }
    },
    async loadOrders() {
      this.loading = true
      try {
        const res = await listCommunitySpacePerson({ ...this.conditions, page: this.page.current, row: this.page.size })
        this.orders = res.data || []
        this.page.total = res.total || 0
      } catch (error) {
        this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
      } finally {
        this.loading = false
      }
    },
    async loadSummary() {
      try {
        const res = await listCommunitySpacePersonSummary({ ...this.conditions })
        if (res) {
          this.summary = res
        }
      } catch (error) {
        this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
      }
    },
    handleReload() {
      this.loadOrders()
      this.loadSummary()
    },
    handleQuery() {
      this.page.current = 1
      this.handleReload()
    },
    handleReset() {
      this.conditions = {
        keyword: '',
        venueId: '',
        spaceId: '',
        state: '',
        beginDate: '',
        endDate: '',
        communityId: this.conditions.communityId
      }
      this.dateRange = []
      this.handleQuery()
    },
    handleVenueChange() {
      this.conditions.spaceId = ''
    },
    handleDateChange(value) {
      if (value && value.length === 2) {
        this.conditions.beginDate = value[0]
        this.conditions.endDate = value[1]
        return
      }
      this.conditions.beginDate = ''
      this.conditions.endDate = ''
    },
    handlePageChange(page) {
      this.page.current = page
      this.loadOrders()
    },
    handleSizeChange(size) {
      this.page.size = size
      this.page.current = 1
      this.loadOrders()
    },
    stateText(row) {
      if (row.stateName) {
        return row.stateName
      }
      return this.stateOptions[row.state] || row.state
    },
    formatTimes(times) {
      if (!times || times.length < 1) {
        return '-'
      }
      const hours = times.map(item => Number(item.hours)).sort((a, b) => a - b)
      return this.formatHour(hours[0]) + '-' + this.formatHour(hours[hours.length - 1] + 1)
    },
    formatHour(value) {
      return (value < 10 ? '0' + value : '' + value) + ':00'
    },
    canShowQrcode(row) {
      if (row.state !== 'S') {
        return false
      }
      const times = row.times || []
      return times.some(item => item.state === 'W')
    },
    canCancel(row) {
      return row.state === 'S' || row.state === 'WP' || row.state === 'W'
    },
    handleQrcode(row) {
      this.$refs.qrcodeModal.open(row)
    },
    handleDetail(row) {
      this.$refs.detailModal.open(row)
    },
    handleCancel(row) {
      this.$refs.deleteModal.open(row)
    },
    async handleExport() {
      try {
        const res = await listCommunitySpacePerson({ ...this.conditions, page: 1, row: 1000 })
        const rows = res.data || []
        if (rows.length < 1) {
          this.$message.warning(this.$t('communitySpacePersonManage.exportEmpty'))
          return
        }
        const header = [
          this.$t('communitySpacePersonManage.orderId'),
          this.$t('communitySpacePersonManage.venue'),
          this.$t('communitySpacePersonManage.space'),
          this.$t('communitySpacePersonManage.appointmentPerson'),
          this.$t('communitySpacePersonManage.appointmentTel'),
          this.$t('communitySpacePersonManage.appointmentDate'),
          this.$t('communitySpacePersonManage.appointmentTime'),
          this.$t('communitySpacePersonManage.receivableAmount'),
          this.$t('communitySpacePersonManage.receivedAmount'),
          this.$t('communitySpacePersonManage.payWay'),
          this.$t('communitySpacePersonManage.state'),
          this.$t('communitySpacePersonManage.remark')
        ]
        const lines = [header.join(',')]
        rows.forEach(row => {
          const cells = [
            row.orderId,
            row.venueName,
            row.spaceName,
            row.personName,
            row.personTel,
            row.appointmentTime,
            this.formatTimes(row.times),
            row.receivableAmount,
            row.receivedAmount,
            row.payWayName || this.$t('communitySpacePersonManage.payWayNone'),
            this.stateText(row),
            row.remark
          ]
          lines.push(cells.map(cell => this.csvCell(cell)).join(','))
        })
        const blob = new Blob(['\ufeff' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'community_space_person_' + this.formatDate(new Date()) + '.csv')
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        this.$message.success(this.$t('communitySpacePersonManage.exportSuccess'))
      } catch (error) {
        this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
      }
    },
    csvCell(value) {
      const text = value === null || value === undefined ? '' : '' + value
      return '"' + text.replace(/"/g, '""') + '"'
    },
    formatDate(date) {
      const pad = value => (value < 10 ? '0' + value : '' + value)
      return date.getFullYear() + pad(date.getMonth() + 1) + pad(date.getDate())
    }
  }
}
</script>

<style lang="scss" scoped>
.order-manage {
  padding: 16px;
  background: #f1f5f9;
  min-height: calc(100vh - 90px);
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
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

.stat-amber {
  color: #d97706;
}

.stat-primary {
  color: #2563eb;
}

.stat-emerald {
  color: #059669;
}

.stat-rose {
  color: #f43f5e;
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.12);
}

.manage-panel {
  overflow: hidden;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.filter-input {
  width: 220px;
}

.filter-select {
  width: 160px;
}

.filter-date {
  width: 260px;
}

.export-button {
  margin-left: auto;
}

.pagination {
  padding: 12px 16px;
}

.state-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: #f1f5f9;
  color: #64748b;
}

.state-WP,
.state-W {
  background: #fef3c7;
  color: #d97706;
}

.state-S {
  background: #dbeafe;
  color: #2563eb;
}

.state-C {
  background: #d1fae5;
  color: #059669;
}

.state-CL,
.state-F {
  background: #f1f5f9;
  color: #94a3b8;
}

.danger-text {
  color: #f56c6c;
}

::v-deep .el-table th {
  background: #f8fafc;
  color: #64748b;
  font-weight: 500;
}

::v-deep .el-table td {
  color: #334155;
}
</style>
