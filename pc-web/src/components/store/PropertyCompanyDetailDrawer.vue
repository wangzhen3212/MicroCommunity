<template>
  <transition name="company-drawer-fade">
    <div v-if="visible" class="company-detail-mask" @click.self="handleClose">
      <aside class="dw" role="dialog" aria-modal="true">
        <header class="dw-head">
          <div class="dw-title-row">
            <h3>{{ $t('propertyCompanyManage.drawer.title') }}</h3>
            <span class="dw-no">{{ company.name || $t('propertyCompanyManage.card.unnamed') }}</span>
            <span class="badge" :class="limited ? 'off' : 'ok'">
              <i class="bd"></i>{{ limited ? $t('propertyCompanyManage.restrictLogin') : $t('propertyCompanyManage.card.normal') }}
            </span>
            <div class="dw-actions">
              <el-button size="mini" @click="$emit('edit', company)">{{ $t('common.edit') }}</el-button>
              <el-button size="mini" type="danger" plain @click="$emit('delete', company)">{{ $t('common.delete') }}</el-button>
              <el-button size="mini" @click="$emit('login', company)">{{ $t('propertyCompanyManage.login') }}</el-button>
              <el-button v-if="!limited" size="mini" @click="$emit('limit', company)">
                {{ $t('propertyCompanyManage.restrictLogin') }}
              </el-button>
              <el-button v-else size="mini" @click="$emit('recover', company)">
                {{ $t('propertyCompanyManage.restoreLogin') }}
              </el-button>
              <el-button size="mini" @click="$emit('resetPwd', company)">{{ $t('propertyCompanyManage.resetPwd') }}</el-button>
              <el-button type="text" size="mini" icon="el-icon-close" class="dw-close" :title="$t('common.close')"
                @click="handleClose" />
            </div>
          </div>
          <div class="dw-info">
            <div v-for="item in infoList" :key="item.key" class="di">
              <span class="k">{{ item.label }}</span>
              <span class="v" :class="{ muted: !item.value || item.value === '-' }" :title="item.value || '-'">{{ item.value || '-' }}</span>
            </div>
          </div>
        </header>

        <nav class="dw-tabs">
          <button v-for="tab in tabs" :key="tab.name" type="button" class="dw-tab"
            :class="{ active: activeTab === tab.name }" @click="activeTab = tab.name">
            {{ tab.label }}<span v-if="tab.count !== undefined" class="tb">{{ tab.count }}</span>
          </button>
        </nav>

        <div v-loading="loading" class="dw-body">
          <div class="tbar">
            <span class="tbar-tip">{{ $t('propertyCompanyManage.drawer.communityTotal', { total: communities.length }) }}</span>
            <el-button type="primary" size="mini" icon="el-icon-plus" @click="handleAdd">
              {{ $t('propertyCompanyManage.drawer.joinCommunity') }}
            </el-button>
          </div>
          <div class="sec">
            <div class="table-card">
              <table class="mini-table">
                <thead>
                  <tr>
                    <th>{{ $t('propertyCompanyManage.drawer.communityName') }}</th>
                    <th>{{ $t('propertyCompanyManage.drawer.city') }}</th>
                    <th>{{ $t('propertyCompanyManage.drawer.communityId') }}</th>
                    <th>{{ $t('common.operation') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in communities" :key="item.communityId">
                    <td>{{ item.name || '-' }}</td>
                    <td>{{ item.cityName || '-' }}</td>
                    <td>{{ item.communityId || '-' }}</td>
                    <td class="tops">
                      <span class="tlink" @click="handleEdit(item)">{{ $t('common.edit') }}</span>
                      <span class="tlink danger" @click="handleQuit(item)">{{ $t('propertyCompanyManage.drawer.quit') }}</span>
                    </td>
                  </tr>
                  <tr v-if="!communities.length">
                    <td colspan="4" class="tempty">{{ communityError || $t('propertyCompanyManage.drawer.communityEmpty') }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <footer class="dw-foot">
          <span>{{ $t('propertyCompanyManage.drawer.code') }}：<b>{{ company.storeId || '-' }}</b></span>
          <span class="df-right">
            <span>{{ $t('propertyCompanyManage.drawer.updatedAt') }} {{ updatedTime }}</span>
            <el-button size="mini" @click="handleClose">{{ $t('common.close') }}</el-button>
          </span>
        </footer>
      </aside>

      <add-property-community ref="addRef" @success="handleCommunityChanged" />
      <edit-property-community ref="editRef" @success="handleCommunityChanged" />
      <delete-property-community ref="delRef" @success="handleCommunityChanged" />
    </div>
  </transition>
</template>

<script>
import { listCommunitys } from '@/api/community/propertyCommunityApi'
import AddPropertyCommunity from '@/components/community/AddPropertyCommunity'
import EditPropertyCommunity from '@/components/community/EditPropertyCommunity'
import DeletePropertyCommunity from '@/components/community/DeletePropertyCommunity'

const STATE_LIMIT_LOGIN = '48002'

function formatNow() {
  const d = new Date()
  const pad = n => (n < 10 ? '0' + n : '' + n)
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

export default {
  name: 'PropertyCompanyDetailDrawer',
  components: { AddPropertyCommunity, EditPropertyCommunity, DeletePropertyCommunity },
  props: {
    visible: { type: Boolean, default: false },
    companyData: { type: Object, default: null }
  },
  data() {
    return {
      activeTab: 'community',
      loading: false,
      communities: [],
      communityError: '',
      currentCommunity: null,
      updatedTime: ''
    }
  },
  computed: {
    company() {
      return this.companyData || {}
    },
    limited() {
      return this.company.state === STATE_LIMIT_LOGIN
    },
    tabs() {
      return [
        {
          name: 'community',
          label: this.$t('propertyCompanyManage.manageCommunity'),
          count: this.communities.length
        }
      ]
    },
    infoList() {
      const company = this.company
      return [
        { key: 'storeId', label: this.$t('propertyCompanyManage.drawer.companyId'), value: company.storeId },
        { key: 'tel', label: this.$t('propertyCompanyManage.drawer.admin'), value: company.tel },
        { key: 'corporation', label: this.$t('propertyCompanyManage.drawer.corporation'), value: company.corporation },
        { key: 'foundingTime', label: this.$t('propertyCompanyManage.drawer.foundingTime'), value: company.foundingTime },
        { key: 'createTime', label: this.$t('propertyCompanyManage.drawer.createTime'), value: company.createTime },
        {
          key: 'state',
          label: this.$t('propertyCompanyManage.drawer.loginState'),
          value: this.limited ? this.$t('propertyCompanyManage.drawer.limitLogin') : this.$t('propertyCompanyManage.drawer.allowLogin')
        },
        { key: 'address', label: this.$t('propertyCompanyManage.drawer.address'), value: company.address }
      ]
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.activeTab = 'community'
        this.communities = []
        this.communityError = ''
        this.currentCommunity = null
        this.updatedTime = formatNow()
        this.loadCommunities()
        document.addEventListener('keydown', this.handleKeydown)
      } else {
        document.removeEventListener('keydown', this.handleKeydown)
      }
    }
  },
  beforeDestroy() {
    document.removeEventListener('keydown', this.handleKeydown)
  },
  methods: {
    handleClose() {
      this.$emit('update:visible', false)
    },
    handleKeydown(e) {
      if (e.key === 'Escape' && !this.childDialogVisible()) {
        this.handleClose()
      }
    },
    childDialogVisible() {
      const opened = ['addRef', 'editRef', 'delRef'].some(ref => {
        const comp = this.$refs[ref]
        return comp && comp.visible
      })
      if (opened) {
        return true
      }
      const wrappers = document.querySelectorAll('.el-dialog__wrapper')
      for (let i = 0; i < wrappers.length; i++) {
        if (wrappers[i].style.display !== 'none') {
          return true
        }
      }
      return false
    },
    handleAdd() {
      this.$refs.addRef.open({ storeId: this.company.storeId || '' })
    },
    handleEdit(row) {
      this.currentCommunity = { ...row }
      this.$refs.editRef.open({
        ...row,
        memberId: this.company.storeId || ''
      })
    },
    handleQuit(row) {
      this.currentCommunity = { ...row }
      this.$refs.delRef.open({
        ...row,
        memberId: this.company.storeId || ''
      })
    },
    handleCommunityChanged() {
      this.loadCommunities()
      this.$emit('refresh')
    },
    async loadCommunities() {
      const storeId = this.company.storeId
      if (!storeId) {
        return
      }
      this.loading = true
      try {
        const res = await listCommunitys({ memberId: storeId, page: 1, row: 50 })
        this.communities = res.data || []
      } catch (error) {
        this.communities = []
        this.communityError = error.message || this.$t('propertyCompanyManage.fetchCommunityError')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.company-detail-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(16, 24, 40, 0.45);
  z-index: 1500;
  display: flex;
  justify-content: flex-end;
}
.dw {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  width: 900px;
  max-width: 94vw;
  background: #f7f9fc;
  display: flex;
  flex-direction: column;
  box-shadow: -30px 0 70px -24px rgba(9, 20, 42, 0.5);
  font-size: 13px;
  color: #1f2937;
}
.dw-head {
  flex: 0 0 auto;
  background: #fff;
  border-bottom: 1px solid #e6eaf0;
  padding: 15px 20px 14px;
}
.dw-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.dw-title-row h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #17263f;
}
.dw-no {
  font-size: 14px;
  font-weight: 600;
  color: #3b4a61;
}
.badge {
  height: 20px;
  padding: 0 7px;
  border-radius: 5px;
  font-size: 11px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}
.badge.ok {
  background: #e6f7ee;
  color: #0f8b51;
}
.badge.off {
  background: #f0f3f7;
  color: #7d8a9c;
}
.badge .bd {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  display: block;
}
.dw-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.dw-actions >>> .el-button {
  border-radius: 6px;
  font-weight: 400;
}
.dw-close {
  width: 24px;
  height: 24px;
  padding: 0;
  margin-left: 0;
  color: #8b98a9;
  font-size: 15px;
}
.dw-close:hover {
  color: #33415c;
}
.dw-info {
  margin-top: 13px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 13px 22px;
  background: #fafcff;
  border: 1px solid #eef2f8;
  border-radius: 10px;
  padding: 14px 16px;
}
.dw-info .di {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}
.dw-info .di .k {
  color: #8b98a9;
  font-size: 12.5px;
  white-space: nowrap;
  flex: 0 0 auto;
}
.dw-info .di .v {
  color: #2c3a50;
  font-size: 13px;
  font-weight: 500;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
.dw-info .di .v.muted {
  color: #a3afbf;
  font-weight: 400;
}
.dw-tabs {
  flex: 0 0 auto;
  background: #fff;
  border-bottom: 1px solid #e6eaf0;
  display: flex;
  padding: 0 12px;
  overflow-x: auto;
}
.dw-tab {
  position: relative;
  height: 44px;
  padding: 0 14px;
  font-size: 13px;
  color: #5b6879;
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
  background: transparent;
  border: 0;
  cursor: pointer;
  transition: color 0.16s;
}
.dw-tab:hover {
  color: #2b6cf6;
}
.dw-tab.active {
  color: #2b6cf6;
  font-weight: 600;
}
.dw-tab.active::after {
  content: "";
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 0;
  height: 2px;
  border-radius: 2px;
  background: #2b6cf6;
}
.dw-tab .tb {
  margin-left: 5px;
  font-size: 10.5px;
  font-weight: 500;
  color: #93a0b2;
  background: #f2f5fa;
  border-radius: 8px;
  padding: 1px 6px;
}
.dw-tab.active .tb {
  background: #e4edff;
  color: #2b6cf6;
}
.dw-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 20px 20px;
}
.tbar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 0 12px;
}
.tbar .tbar-tip {
  font-size: 12.5px;
  color: #8b98a9;
}
.tbar >>> .el-button {
  margin-left: auto;
  border-radius: 8px;
}
.sec {
  background: #fff;
  border: 1px solid #e9eef6;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
}
.sec:last-child {
  margin-bottom: 0;
}
.table-card {
  background: #fff;
  border: 1px solid #e8edf5;
  border-radius: 10px;
  overflow: auto;
}
.mini-table {
  width: 100%;
  border-collapse: collapse;
}
.mini-table th {
  background: #f7f9fc;
  font-size: 11.5px;
  color: #8b98a9;
  font-weight: 500;
  text-align: left;
  padding: 9px 12px;
  border-bottom: 1px solid #eef2f8;
  white-space: nowrap;
}
.mini-table td {
  padding: 10px 12px;
  font-size: 12.5px;
  color: #33415c;
  border-bottom: 1px solid #f4f7fb;
  white-space: nowrap;
}
.mini-table tr:last-child td {
  border-bottom: 0;
}
.mini-table .tempty {
  padding: 26px 12px;
  text-align: center;
  color: #a3afbf;
  white-space: normal;
}
.mini-table .tlink {
  color: #2b6cf6;
  cursor: pointer;
  font-size: 12px;
}
.mini-table .tlink:hover {
  text-decoration: underline;
}
.mini-table .tops .tlink + .tlink {
  margin-left: 12px;
}
.mini-table .tlink.danger {
  color: #f56c6c;
}
.dw-foot {
  flex: 0 0 auto;
  height: 52px;
  background: #fff;
  border-top: 1px solid #e6eaf0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  font-size: 12px;
  color: #8b98a9;
}
.dw-foot b {
  color: #5b6879;
  font-weight: 500;
}
.dw-foot .df-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}
.company-drawer-fade-enter-active,
.company-drawer-fade-leave-active {
  transition: opacity 0.22s;
}
.company-drawer-fade-enter-active .dw,
.company-drawer-fade-leave-active .dw {
  transition: transform 0.3s cubic-bezier(0.22, 0.8, 0.3, 1);
}
.company-drawer-fade-enter,
.company-drawer-fade-leave-to {
  opacity: 0;
}
.company-drawer-fade-enter .dw,
.company-drawer-fade-leave-to .dw {
  transform: translateX(100%);
}
@media (max-width: 1400px) {
  .dw-info {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
