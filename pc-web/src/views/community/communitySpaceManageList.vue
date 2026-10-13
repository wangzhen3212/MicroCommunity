<template>
  <div class="space-manage">
    <div class="stat-row">
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpaceManage.statVenueTotal') }}</div>
        <div class="stat-value stat-slate">{{ venues.length }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpaceManage.statSpaceTotal') }}</div>
        <div class="stat-value stat-primary">{{ statSpaceTotal }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpaceManage.statSpaceEnabled') }}</div>
        <div class="stat-value stat-emerald">{{ statSpaceEnabled }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">{{ $t('communitySpaceManage.statAvgFee') }}</div>
        <div class="stat-value stat-rose">￥{{ statAvgFee }}</div>
      </div>
    </div>

    <div class="manage-body">
      <div class="panel venue-panel">
        <div class="panel-header">
          <span class="panel-title">{{ $t('communitySpaceManage.venueList') }}</span>
          <el-button type="text" class="link-button" @click="handleAddVenue">
            {{ $t('communitySpaceManage.addVenue') }}
          </el-button>
        </div>
        <div class="venue-list">
          <div v-for="item in venues" :key="item.venueId" class="venue-item"
            :class="{ active: currentVenueId === item.venueId }" @click="handleSelectVenue(item)">
            <div class="venue-item-main">
              <div class="venue-name">{{ item.name }}</div>
              <div class="venue-desc">{{ item.remark || '-' }}</div>
              <div class="venue-meta">
                <span>{{ $t('communitySpaceManage.openTimePill', { start: item.startTime, end: item.endTime }) }}</span>
                <span>{{ $t('communitySpaceManage.spaceCountTip', { count: countSpace(item.venueId) }) }}</span>
              </div>
            </div>
            <div class="venue-item-actions">
              <el-button type="text" @click.stop="handleEditVenue(item)">
                {{ $t('communitySpaceManage.edit') }}
              </el-button>
              <el-button type="text" class="danger-text" @click.stop="handleDeleteVenue(item)">
                {{ $t('communitySpaceManage.delete') }}
              </el-button>
            </div>
          </div>
          <div v-if="venues.length < 1" class="empty-tip">{{ $t('communitySpaceManage.noVenue') }}</div>
        </div>
      </div>

      <div class="panel space-panel">
        <div class="panel-header space-header">
          <div class="crumb">
            <span class="crumb-root">{{ $t('communitySpaceManage.spaceAreaTitle') }}</span>
            <span class="crumb-sep">/</span>
            <span class="crumb-current">{{ currentVenue ? currentVenue.name : '-' }}</span>
          </div>
          <span class="crumb-count">{{ $t('communitySpaceManage.totalSpacesTip', { count: page.total }) }}</span>
          <span v-if="currentVenue" class="open-pill">
            {{ $t('communitySpaceManage.openTimePill', { start: currentVenue.startTime, end: currentVenue.endTime }) }}
          </span>
          <div class="header-actions">
            <el-input v-model="conditions.name" size="small" clearable class="search-input"
              :placeholder="$t('communitySpaceManage.searchSpacePlaceholder')" @keyup.enter.native="handleQuery" />
            <el-button size="small" @click="handleQuery">
              {{ $t('communitySpaceManage.query') }}
            </el-button>
            <el-button type="primary" size="small" @click="handleAddSpace">
              <i class="el-icon-plus"></i>
              {{ $t('communitySpaceManage.addSpace') }}
            </el-button>
          </div>
        </div>

        <div class="inherit-bar">
          <i class="el-icon-info"></i>
          {{ $t('communitySpaceManage.inheritTip') }}
        </div>

        <el-table v-loading="loading" :data="spaces" style="width: 100%">
          <el-table-column prop="name" :label="$t('communitySpaceManage.name')" min-width="140" />
          <el-table-column prop="feeMoney" :label="$t('communitySpaceManage.feeMoney')" width="140">
            <template slot-scope="scope">￥{{ scope.row.feeMoney }}</template>
          </el-table-column>
          <el-table-column prop="adminName" :label="$t('communitySpaceManage.adminName')" width="140">
            <template slot-scope="scope">{{ scope.row.adminName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="tel" :label="$t('communitySpaceManage.tel')" width="150">
            <template slot-scope="scope">{{ scope.row.tel || '-' }}</template>
          </el-table-column>
          <el-table-column :label="$t('communitySpaceManage.state')" width="110" align="center">
            <template slot-scope="scope">
              <span class="state-pill" :class="scope.row.state === '1001' ? 'state-enable' : 'state-disable'">
                {{ scope.row.state === '1001' ? $t('communitySpaceManage.statusEnable')
                  : $t('communitySpaceManage.statusDisable') }}
              </span>
            </template>
          </el-table-column>
          <el-table-column :label="$t('communitySpaceManage.operation')" align="right" width="260">
            <template slot-scope="scope">
              <el-button type="text" @click="handleEditSpace(scope.row)">
                {{ $t('communitySpaceManage.edit') }}
              </el-button>
              <el-button type="text" @click="handleOpenTime(scope.row)">
                {{ $t('communitySpaceManage.openTime') }}
              </el-button>
              <el-button type="text" @click="handleToggleState(scope.row)">
                {{ scope.row.state === '1001' ? $t('communitySpaceManage.disableAction')
                  : $t('communitySpaceManage.enableAction') }}
              </el-button>
              <el-button type="text" class="danger-text" @click="handleDeleteSpace(scope.row)">
                {{ $t('communitySpaceManage.delete') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="spaces.length < 1" class="empty-tip">{{ $t('communitySpaceManage.noSpaceInVenue') }}</div>

        <el-pagination class="pagination" :current-page="page.current" :page-sizes="[10, 20, 30, 50]"
          :page-size="page.size" layout="total, sizes, prev, pager, next, jumper" :total="page.total"
          @size-change="handleSizeChange" @current-change="handlePageChange" />
      </div>
    </div>

    <add-community-venue ref="addVenueModal" @success="refreshAll" />
    <edit-community-venue ref="editVenueModal" @success="refreshAll" />
    <delete-community-venue ref="deleteVenueModal" @success="refreshAll" />
    <add-community-space ref="addSpaceModal" @success="refreshAll" />
    <edit-community-space ref="editSpaceModal" @success="refreshAll" />
    <delete-community-space ref="deleteSpaceModal" @success="refreshAll" />
    <edit-community-space-open-time ref="openTimeModal" @success="refreshAll" />
  </div>
</template>

<script>
import {
  listCommunityVenue,
  listCommunitySpace,
  updateCommunitySpace
} from '@/api/community/communitySpaceManageApi'
import AddCommunityVenue from '@/components/community/AddCommunityVenue'
import EditCommunityVenue from '@/components/community/EditCommunityVenue'
import DeleteCommunityVenue from '@/components/community/DeleteCommunityVenue'
import AddCommunitySpace from '@/components/community/AddCommunitySpace'
import EditCommunitySpace from '@/components/community/EditCommunitySpace'
import DeleteCommunitySpace from '@/components/community/DeleteCommunitySpace'
import EditCommunitySpaceOpenTime from '@/components/community/EditCommunitySpaceOpenTime'

export default {
  name: 'CommunitySpaceManageList',
  components: {
    AddCommunityVenue,
    EditCommunityVenue,
    DeleteCommunityVenue,
    AddCommunitySpace,
    EditCommunitySpace,
    DeleteCommunitySpace,
    EditCommunitySpaceOpenTime
  },
  data() {
    return {
      loading: false,
      venues: [],
      allSpaces: [],
      spaces: [],
      currentVenueId: '',
      conditions: {
        venueId: '',
        name: '',
        page: 1,
        row: 10
      },
      page: {
        current: 1,
        size: 10,
        total: 0
      }
    }
  },
  computed: {
    currentVenue() {
      return this.venues.find(item => item.venueId === this.currentVenueId)
    },
    statSpaceTotal() {
      return this.allSpaces.length
    },
    statSpaceEnabled() {
      return this.allSpaces.filter(item => item.state === '1001').length
    },
    statAvgFee() {
      if (this.allSpaces.length < 1) {
        return '0.00'
      }
      let total = 0
      this.allSpaces.forEach(item => {
        const fee = Number(item.feeMoney)
        total += isNaN(fee) ? 0 : fee
      })
      return (total / this.allSpaces.length).toFixed(2)
    }
  },
  created() {
    this.listVenues()
    this.listAllSpaces()
  },
  methods: {
    async listVenues() {
      try {
        const res = await listCommunityVenue({ page: 1, row: 100 })
        this.venues = res.data || []
        if (!this.currentVenueId || !this.venues.some(item => item.venueId === this.currentVenueId)) {
          this.currentVenueId = this.venues.length > 0 ? this.venues[0].venueId : ''
          this.conditions.venueId = this.currentVenueId
          this.conditions.page = 1
          this.page.current = 1
        }
        this.listSpaces()
      } catch (error) {
        this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
      }
    },
    async listAllSpaces() {
      try {
        const res = await listCommunitySpace({ page: 1, row: 1000 })
        this.allSpaces = res.data || []
      } catch (error) {
        this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
      }
    },
    async listSpaces() {
      if (!this.conditions.venueId) {
        this.spaces = []
        this.page.total = 0
        return
      }
      this.loading = true
      try {
        const res = await listCommunitySpace({ ...this.conditions })
        this.spaces = res.data || []
        this.page.total = res.total || 0
      } catch (error) {
        this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
      } finally {
        this.loading = false
      }
    },
    refreshAll() {
      this.listVenues()
      this.listAllSpaces()
    },
    countSpace(venueId) {
      return this.allSpaces.filter(item => item.venueId === venueId).length
    },
    handleSelectVenue(venue) {
      if (this.currentVenueId === venue.venueId) {
        return
      }
      this.currentVenueId = venue.venueId
      this.conditions.venueId = venue.venueId
      this.conditions.page = 1
      this.page.current = 1
      this.listSpaces()
    },
    handleQuery() {
      this.conditions.page = 1
      this.page.current = 1
      this.listSpaces()
    },
    handlePageChange(page) {
      this.conditions.page = page
      this.page.current = page
      this.listSpaces()
    },
    handleSizeChange(size) {
      this.conditions.row = size
      this.conditions.page = 1
      this.page.size = size
      this.page.current = 1
      this.listSpaces()
    },
    handleAddVenue() {
      this.$refs.addVenueModal.open()
    },
    handleEditVenue(venue) {
      this.$refs.editVenueModal.open(venue)
    },
    handleDeleteVenue(venue) {
      this.$refs.deleteVenueModal.open(venue, this.countSpace(venue.venueId))
    },
    handleAddSpace() {
      if (!this.conditions.venueId) {
        this.$message.warning(this.$t('communitySpaceManage.noVenue'))
        return
      }
      this.$refs.addSpaceModal.open(this.conditions.venueId, this.venues)
    },
    handleEditSpace(row) {
      this.$refs.editSpaceModal.open(row, this.venues)
    },
    handleOpenTime(row) {
      this.$refs.openTimeModal.open(row)
    },
    handleDeleteSpace(row) {
      this.$refs.deleteSpaceModal.open(row)
    },
    handleToggleState(row) {
      const state = row.state === '1001' ? '2002' : '1001'
      updateCommunitySpace({
        spaceId: row.spaceId,
        state: state
      }).then(() => {
        this.$message.success(state === '1001' ? this.$t('communitySpaceManage.enableSuccess')
          : this.$t('communitySpaceManage.disableSuccess'))
        this.refreshAll()
      }).catch(error => {
        this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.space-manage {
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

.stat-primary {
  color: #2563eb;
}

.stat-emerald {
  color: #059669;
}

.stat-rose {
  color: #f43f5e;
}

.manage-body {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.12);
}

.venue-panel {
  width: 280px;
  flex: none;
}

.space-panel {
  flex: 1;
  min-width: 0;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.link-button {
  padding: 0;
  font-size: 12px;
  color: #2563eb;
}

.venue-list {
  padding: 10px;
  max-height: 560px;
  overflow-y: auto;
}

.venue-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.venue-item:hover {
  border-color: #93c5fd;
  background: #f8fafc;
}

.venue-item.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.venue-item-main {
  min-width: 0;
  flex: 1;
}

.venue-name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.venue-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #94a3b8;
}

.venue-meta {
  display: flex;
  flex-direction: column;
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.venue-item-actions {
  flex: none;
}

.venue-item-actions .el-button {
  padding: 0 0 0 8px;
  font-size: 12px;
}

.danger-text {
  color: #f56c6c;
}

.space-header {
  flex-wrap: wrap;
  gap: 10px;
}

.crumb {
  font-size: 13px;
}

.crumb-root {
  color: #94a3b8;
}

.crumb-sep {
  margin: 0 6px;
  color: #cbd5e1;
}

.crumb-current {
  font-weight: 600;
  color: #334155;
}

.crumb-count {
  font-size: 12px;
  color: #94a3b8;
}

.open-pill {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: #eff6ff;
  color: #2563eb;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.search-input {
  width: 200px;
}

.inherit-bar {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
  font-size: 12px;
  color: #64748b;
}

.inherit-bar i {
  margin-right: 6px;
  color: #94a3b8;
}

.state-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.state-enable {
  background: #d1fae5;
  color: #059669;
}

.state-disable {
  background: #f1f5f9;
  color: #94a3b8;
}

.empty-tip {
  padding: 20px 16px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}

.pagination {
  padding: 12px 16px;
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
