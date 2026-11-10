<template>
  <div class="community-manage">
    <section class="panel">
      <div class="panel-head">
        <div class="pt">
          <i></i><span>{{ $t('communityManage.list.title') }}</span>
        </div>
        <div class="ph-right">
          <span class="stat"><i class="dot"></i>{{ $t('communityManage.filterBar.page') }} <b>{{ list.length }}</b></span>
          <span class="stat"><i class="dot green"></i>{{ $t('communityManage.filterBar.total') }} <b>{{ total }}</b></span>
          <el-button size="small" class="btn-plain" @click="toDoc('/pages/hc/addCommunity_cn.html')">
            <i class="el-icon-document"></i>{{ $t('common.document') }}
          </el-button>
          <el-button type="primary" size="small" icon="el-icon-plus" class="btn-add" @click="handleAdd">
            {{ $t('communityManage.list.addCommunity') }}
          </el-button>
        </div>
      </div>

      <div class="filters">
        <el-input v-model="queryParams.name" class="f-name" size="small" :placeholder="$t('communityManage.filterBar.namePlaceholder')"
          clearable @keyup.enter.native="handleSearch" @clear="handleSearch" />
        <el-select v-model="queryParams.storeId" class="f-store" size="small" filterable clearable remote
          :remote-method="loadStores" :loading="storeLoading" :placeholder="$t('communityManage.filterBar.storePlaceholder')"
          @change="handleSearch">
          <el-option v-for="item in storeOptions" :key="item.storeId" :label="item.name" :value="item.storeId" />
        </el-select>
        <div class="f-area">
          <area-select ref="searchAreaRef" @selectArea="selectArea" />
        </div>
        <el-button type="primary" size="small" icon="el-icon-search" @click="handleSearch">
          {{ $t('common.search') }}
        </el-button>
        <el-button size="small" icon="el-icon-refresh-left" @click="handleReset">
          {{ $t('common.reset') }}
        </el-button>
        <span class="f-spacer"></span>
        <span class="f-hint">{{ $t('communityManage.filterBar.hint', { page: list.length, total: total }) }}</span>
      </div>

      <div v-loading="loading" class="cards-wrap">
        <div v-if="list.length" class="cards">
          <div v-for="row in list" :key="row.communityId" class="card" :class="{ muted: !isComplete(row) }" role="button"
            tabindex="0" @click="handleDetail(row)" @keyup.enter="handleDetail(row)">
            <div class="card-top">
              <span class="card-name" :title="row.name">{{ row.name || $t('communityManage.card.unnamed') }}</span>
              <span class="badge" :class="isComplete(row) ? 'ok' : 'warn'">
                <i class="bd"></i>{{ isComplete(row) ? $t('communityManage.card.normal') : $t('communityManage.card.incomplete') }}
              </span>
            </div>
            <div class="card-body">
              <div class="cb-left">
                <div class="card-loc" :title="location(row)">
                  <i class="el-icon-location-outline"></i>{{ location(row) }}
                </div>
                <div class="card-line">
                  <span class="lb">{{ $t('communityManage.card.store') }}</span>
                  <span class="vl" :title="row.storeName">{{ row.storeName || $t('communityManage.card.unlinked') }}</span>
                </div>
                <div class="card-line">
                  <span class="lb">{{ $t('communityManage.card.address') }}</span>
                  <span class="vl" :title="row.address">{{ row.address || $t('communityManage.card.unfilled') }}</span>
                </div>
              </div>
              <div class="cb-right">
                <div class="cb-item">
                  <span class="k">{{ $t('communityManage.card.tel') }}</span>
                  <span class="v" :title="row.tel">{{ row.tel || '-' }}</span>
                </div>
                <div class="cb-item">
                  <span class="k">{{ $t('communityManage.card.attr') }}</span>
                  <span class="v">{{ attrCount(row) }} {{ $t('communityManage.card.attrUnit') }}</span>
                </div>
              </div>
            </div>
            <div class="card-foot">
              <span class="cf-l">{{ $t('communityManage.card.code') }} {{ shortId(row.communityId) }}</span>
              <span class="cf-tag" :title="row.cityCode">{{ row.cityCode || '-' }}</span>
              <div class="card-ops" @click.stop>
                <el-dropdown trigger="click" @command="cmd => handleCardCommand(cmd, row)">
                  <el-button size="mini" plain>
                    {{ $t('common.operation') }}<i class="el-icon-arrow-down el-icon--right"></i>
                  </el-button>
                  <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item command="edit">{{ $t('common.edit') }}</el-dropdown-item>
                    <el-dropdown-item command="sync">{{ $t('communityManage.table.syncIot') }}</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>{{ $t('common.delete') }}</el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
              </div>
              <span class="cf-more">{{ $t('common.detail') }}<i class="el-icon-arrow-right"></i></span>
            </div>
          </div>
        </div>
        <div v-else-if="!loading" class="empty-state">
          <div class="es-icon"><i class="el-icon-office-building"></i></div>
          <p>{{ $t('communityManage.card.empty') }}</p>
          <el-button size="small" icon="el-icon-refresh-left" @click="handleReset">
            {{ $t('communityManage.card.clearFilter') }}
          </el-button>
        </div>
      </div>

      <div class="pager">
        <span class="pg-total">{{ $t('communityManage.filterBar.totalCount', { total: total }) }}</span>
        <el-pagination class="pg-right" background layout="sizes, prev, pager, next, jumper" :total="total"
          :page-size="queryParams.row" :current-page="queryParams.page" :page-sizes="[20, 40, 80]"
          @size-change="handleSizeChange" @current-change="handlePageChange" />
      </div>
    </section>

    <add-community ref="addCommunity" @listData="handleSuccess" />
    <edit-community ref="editCommunity" @listData="handleSuccess" />
    <delete-community ref="deleteCommunity" @listData="handleSuccess" />
    <community-data-to-iot ref="communityDataToIot" />

    <community-detail-drawer :visible.sync="detailVisible" :community-data="detailCommunity" @edit="handleEdit"
      @delete="handleDelete" @refresh="handleSuccess" />
  </div>
</template>

<script>
import { listCommunitys } from '@/api/community/communityManageApi'
import { getPropertyList } from '@/api/store/propertyCompanyManageApi'
import AreaSelect from '@/components/community/areaSelect'
import AddCommunity from '@/components/community/addCommunity'
import EditCommunity from '@/components/community/editCommunity'
import DeleteCommunity from '@/components/community/deleteCommunity'
import CommunityDataToIot from '@/components/community/communityDataToIot'
import CommunityDetailDrawer from '@/components/community/CommunityDetailDrawer'

export default {
  name: 'CommunityManageList',
  components: {
    AreaSelect,
    AddCommunity,
    EditCommunity,
    DeleteCommunity,
    CommunityDataToIot,
    CommunityDetailDrawer
  },
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      currentRow: null,
      detailVisible: false,
      detailCommunity: {},
      storeOptions: [],
      storeLoading: false,
      queryParams: {
        page: 1,
        row: 20,
        name: '',
        cityCode: '',
        storeId: ''
      }
    }
  },
  created() {
    this.loadStores()
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const params = {
          page: this.queryParams.page,
          row: this.queryParams.row
        }
        if (this.queryParams.name) {
          params.name = this.queryParams.name
        }
        if (this.queryParams.cityCode) {
          params.cityCode = this.queryParams.cityCode
        }
        if (this.queryParams.storeId) {
          params.memberId = this.queryParams.storeId
        }
        const res = await listCommunitys(params)
        this.list = res.communitys || []
        this.total = res.total || 0
      } catch (error) {
        this.list = []
        this.total = 0
        this.$message.error(error.message || this.$t('communityManage.fetchError'))
      } finally {
        this.loading = false
      }
    },
    async loadStores(nameLike) {
      this.storeLoading = true
      try {
        const params = { page: 1, row: 100 }
        if (nameLike) {
          params.name = nameLike
        }
        const res = await getPropertyList(params)
        this.storeOptions = res.data || []
      } catch (error) {
        this.storeOptions = []
      } finally {
        this.storeLoading = false
      }
    },
    isComplete(row) {
      return !!(row.tel && row.cityName)
    },
    location(row) {
      return row.cityName || row.address || this.$t('communityManage.card.locationEmpty')
    },
    attrCount(row) {
      return (row.communityAttrDtos || []).length
    },
    shortId(communityId) {
      const id = String(communityId || '')
      return id ? id.slice(-8) : '-'
    },
    selectArea(area) {
      this.queryParams.cityCode = area && area.selectArea ? area.selectArea : ''
    },
    handleSearch() {
      this.queryParams.page = 1
      this.fetchList()
    },
    handleReset() {
      this.queryParams = { page: 1, row: this.queryParams.row, name: '', cityCode: '', storeId: '' }
      if (this.$refs.searchAreaRef) {
        this.$refs.searchAreaRef.clearArea()
      }
      this.loadStores()
      this.fetchList()
    },
    handleAdd() {
      this.$refs.addCommunity.openModal()
    },
    handleEdit(row) {
      this.currentRow = { ...row }
      this.$refs.editCommunity.openModal(this.currentRow)
    },
    handleDelete(row) {
      this.currentRow = { ...row }
      this.$refs.deleteCommunity.openModal(this.currentRow)
    },
    handleCardCommand(command, row) {
      if (command === 'edit') {
        this.handleEdit(row)
      } else if (command === 'delete') {
        this.handleDelete(row)
      } else if (command === 'sync') {
        this.$refs.communityDataToIot.openModal(row)
      }
    },
    handleDetail(row) {
      this.detailCommunity = { ...row }
      this.detailVisible = true
    },
    handleSuccess() {
      this.detailVisible = false
      this.fetchList()
    },
    handleSizeChange(val) {
      this.queryParams.row = Number(val) || 20
      this.queryParams.page = 1
      this.fetchList()
    },
    handlePageChange(val) {
      this.queryParams.page = val
      this.fetchList()
    }
  }
}
</script>

<style scoped>
.community-manage {
  --brand: #2b6cf6;
  --line: #e6eaf0;
  --text-2: #5b6879;
  --text-3: #8b98a9;
  --green: #12a45f;
  --green-soft: #e6f7ee;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
  padding: 20px;
  font-size: 13px;
  color: #1f2937;
}

.panel {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
  overflow: hidden;
}
.panel-head {
  height: 54px;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid #f1f4f9;
}
.panel-head .pt {
  font-weight: 600;
  color: #33415c;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
}
.panel-head .pt i {
  width: 3px;
  height: 13px;
  border-radius: 2px;
  background: var(--brand);
  display: block;
}
.panel-head .ph-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--text-3);
}
.stat {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 7px;
  background: #f6f9ff;
  color: var(--text-2);
  font-size: 12.5px;
  gap: 5px;
}
.stat b {
  color: #1c2b45;
  font-variant-numeric: tabular-nums;
}
.stat .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand);
  display: block;
}
.stat .dot.green {
  background: var(--green);
}
.btn-add {
  height: 32px;
  padding: 0 16px;
  font-size: 13.5px;
  border-radius: 8px;
}
.btn-plain {
  height: 32px;
  padding: 0 14px;
  font-size: 13px;
  border-radius: 8px;
  border-color: var(--line);
  color: var(--text-2);
  background: #fff;
}
.btn-plain >>> i {
  margin-right: 4px;
}

.filters {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
  padding: 10px 16px 11px;
  border-bottom: 1px solid #f1f4f9;
  background: linear-gradient(180deg, #fbfdff, #fff);
}
.filters .f-name {
  width: 240px;
  flex: 0 0 auto;
}
.filters .f-store {
  width: 190px;
  flex: 0 0 auto;
}
.filters .f-area {
  width: 460px;
  flex: 0 0 auto;
  display: flex;
}
.filters .f-area >>> .area-select {
  width: 100%;
  display: flex;
}
.filters >>> .el-input__inner {
  border-radius: 8px;
}
.filters >>> .el-button {
  border-radius: 8px;
}
.filters .f-spacer {
  flex: 1 1 auto;
}
.filters .f-hint {
  font-size: 12.5px;
  color: var(--text-3);
}

.cards-wrap {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 16px 16px 12px;
}
.cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}
.card {
  position: relative;
  text-align: left;
  background: #fff;
  border: 1px solid #e6ebf3;
  border-radius: 11px;
  padding: 16px 16px 14px;
  cursor: pointer;
  outline: none;
  transition: transform 0.18s cubic-bezier(0.2, 0.7, 0.3, 1), box-shadow 0.18s, border-color 0.18s;
}
.card::before {
  content: "";
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  border-radius: 11px 0 0 11px;
  background: linear-gradient(180deg, #5b93ff, #2b6cf6);
}
.card.muted::before {
  background: linear-gradient(180deg, #c3ccda, #9aa7ba);
}
.card:hover,
.card:focus {
  transform: translateY(-3px);
  border-color: #bcd2fb;
  box-shadow: 0 12px 24px -14px rgba(23, 48, 92, 0.4), 0 2px 6px rgba(23, 48, 92, 0.06);
}
.card-top {
  display: flex;
  align-items: center;
  gap: 16px;
}
.card-name {
  font-size: 16px;
  font-weight: 700;
  color: #1c2b45;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
  flex: 1 1 auto;
}
.badge {
  flex: 0 0 auto;
  height: 22px;
  padding: 0 8px;
  border-radius: 5px;
  font-size: 11.5px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}
.badge.ok {
  background: var(--green-soft);
  color: #0f8b51;
}
.badge.warn {
  background: #fff6e6;
  color: #c07c07;
}
.badge .bd {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  display: block;
}
.card-loc {
  font-size: 13px;
  color: #7f8c9e;
  display: flex;
  align-items: center;
  gap: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-loc i {
  font-size: 13px;
  color: #a9b4c4;
}
.card-body {
  margin-top: 12px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.cb-left {
  flex: 1 1 auto;
  min-width: 0;
}
.cb-right {
  flex: 0 0 auto;
  max-width: 46%;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 16px;
}
.cb-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 13px;
  max-width: 100%;
}
.cb-item .k {
  color: #9aa7b8;
  flex: 0 0 auto;
}
.cb-item .v {
  color: #3b4a61;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
.card-line {
  margin-top: 9px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-2);
  min-width: 0;
}
.card-line .lb {
  color: #9aa7b8;
  flex: 0 0 auto;
}
.card-line .vl {
  color: #3b4a61;
  flex: 1 1 auto;
  min-width: 0;
  font-variant-numeric: tabular-nums;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-foot {
  margin-top: 15px;
  padding-top: 11px;
  border-top: 1px dashed #edf1f7;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  color: #93a0b2;
}
.card-foot .cf-l {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
  flex: 1 1 auto;
}
.card-foot .cf-tag {
  flex: 0 0 auto;
  padding: 1px 6px;
  border-radius: 5px;
  background: #f2f6fd;
  color: #6a7789;
  font-variant-numeric: tabular-nums;
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-foot .cf-more {
  flex: 0 0 auto;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  color: var(--brand);
  font-weight: 600;
  opacity: 0.72;
  transition: opacity 0.16s;
}
.card:hover .cf-more {
  opacity: 1;
}
.card-ops >>> .el-button {
  border-radius: 6px;
  font-weight: 400;
}
.empty-state {
  padding: 56px 0 64px;
  text-align: center;
  color: #9aa7b8;
}
.empty-state .es-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 12px;
  border-radius: 14px;
  background: #f3f6fb;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c2cddc;
  font-size: 24px;
}
.empty-state p {
  font-size: 13px;
  margin: 0 0 12px;
}

.pager {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px 14px;
  flex-wrap: wrap;
  color: var(--text-2);
  font-size: 12.5px;
  border-top: 1px solid #f4f7fb;
}
.pager .pg-total b {
  color: #33415c;
  font-variant-numeric: tabular-nums;
}
.pager .pg-right {
  margin-left: auto;
}
.pager >>> .el-pagination {
  padding: 0;
  font-weight: 400;
}
.pager >>> .el-pagination__total,
.pager >>> .el-pagination__jump {
  font-size: 12.5px;
  color: var(--text-2);
}

@media (max-width: 1560px) {
  .cards {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 1200px) {
  .cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 900px) {
  .cards {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
}
</style>
