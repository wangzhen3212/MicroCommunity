<template>
  <transition name="community-drawer-fade">
    <div v-if="visible" class="community-detail-mask" @click.self="handleClose">
      <aside class="dw" role="dialog" aria-modal="true">
        <header class="dw-head">
          <div class="dw-title-row">
            <h3>{{ $t('communityManage.drawer.title') }}</h3>
            <span class="dw-no">{{ community.name || $t('communityManage.card.unnamed') }}</span>
            <span class="badge" :class="complete ? 'ok' : 'warn'">
              <i class="bd"></i>{{ complete ? $t('communityManage.card.normal') : $t('communityManage.card.incomplete') }}
            </span>
            <div class="dw-actions">
              <el-button size="mini" @click="$emit('edit', community)">{{ $t('common.edit') }}</el-button>
              <el-button size="mini" type="danger" plain @click="$emit('delete', community)">{{ $t('common.delete') }}</el-button>
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
          <template v-if="activeTab === 'company'">
            <div class="sec">
              <div class="sec-head">
                <h4><i></i>{{ $t('communityManage.drawer.company') }}</h4>
                <span v-if="property" class="sh-right">
                  {{ propertyLimited ? $t('communityManage.drawer.limitLogin') : $t('communityManage.drawer.service') }}
                </span>
              </div>
              <div v-if="property" class="kv-grid">
                <div class="kv">
                  <div class="k">{{ $t('communityManage.drawer.companyName') }}</div>
                  <div class="v">{{ property.name || '-' }}</div>
                </div>
                <div class="kv">
                  <div class="k">{{ $t('communityManage.drawer.companyId') }}</div>
                  <div class="v mono">{{ property.storeId || '-' }}</div>
                </div>
                <div class="kv">
                  <div class="k">{{ $t('communityManage.drawer.admin') }}</div>
                  <div class="v">{{ property.tel || '-' }}</div>
                </div>
                <div class="kv">
                  <div class="k">{{ $t('communityManage.drawer.corporation') }}</div>
                  <div class="v">{{ property.corporation || '-' }}</div>
                </div>
                <div class="kv">
                  <div class="k">{{ $t('communityManage.drawer.foundingTime') }}</div>
                  <div class="v">{{ property.foundingTime || '-' }}</div>
                </div>
                <div class="kv">
                  <div class="k">{{ $t('communityManage.drawer.loginState') }}</div>
                  <div class="v">{{ propertyLimited ? $t('communityManage.drawer.limitLogin') : $t('communityManage.drawer.allowLogin') }}</div>
                </div>
                <div class="kv kv-full">
                  <div class="k">{{ $t('communityManage.drawer.companyAddress') }}</div>
                  <div class="v">{{ property.address || '-' }}</div>
                </div>
              </div>
              <div v-else class="empty-state">
                <div class="es-icon"><i class="el-icon-office-building"></i></div>
                <p>{{ propertyError || $t('communityManage.drawer.companyEmpty') }}</p>
              </div>
            </div>
            <div class="sec">
              <div class="sec-head">
                <h4><i></i>{{ $t('communityManage.drawer.contact') }}</h4>
              </div>
              <div class="kv-grid">
                <div class="kv">
                  <div class="k">{{ $t('communityManage.drawer.tel') }}</div>
                  <div class="v">{{ community.tel || '-' }}</div>
                </div>
                <div class="kv">
                  <div class="k">{{ $t('communityManage.drawer.city') }}</div>
                  <div class="v">{{ community.cityName || '-' }}</div>
                </div>
                <div class="kv">
                  <div class="k">{{ $t('communityManage.drawer.cityCode') }}</div>
                  <div class="v">{{ community.cityCode || '-' }}</div>
                </div>
                <div class="kv kv-full">
                  <div class="k">{{ $t('communityManage.drawer.address') }}</div>
                  <div class="v">{{ community.address || '-' }}</div>
                </div>
                <div class="kv kv-full">
                  <div class="k">{{ $t('communityManage.drawer.landmarks') }}</div>
                  <div class="v">{{ community.nearbyLandmarks || '-' }}</div>
                </div>
              </div>
            </div>
          </template>

          <template v-else>
            <div class="tbar">
              <span class="tbar-tip">{{ $t('communityManage.drawer.attrTotal', { total: attrList.length }) }}</span>
            </div>
            <div class="sec">
              <div class="table-card">
                <table class="mini-table">
                  <thead>
                    <tr>
                      <th>{{ $t('communityManage.drawer.specName') }}</th>
                      <th>{{ $t('communityManage.drawer.specCd') }}</th>
                      <th>{{ $t('communityManage.drawer.value') }}</th>
                      <th>{{ $t('communityManage.drawer.listShow') }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="attr in attrList" :key="attr.attrId || attr.specCd">
                      <td>{{ attr.specName || '-' }}</td>
                      <td>{{ attr.specCd || '-' }}</td>
                      <td>{{ attr.value || '-' }}</td>
                      <td>
                        <span class="pill" :class="attr.listShow === 'Y' ? 'green' : ''">
                          {{ attr.listShow === 'Y' ? $t('communityManage.drawer.show') : $t('communityManage.drawer.notShow') }}
                        </span>
                      </td>
                    </tr>
                    <tr v-if="!attrList.length">
                      <td colspan="4" class="tempty">{{ $t('communityManage.drawer.attrEmpty') }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </template>
        </div>

        <footer class="dw-foot">
          <span>{{ $t('communityManage.drawer.code') }}：<b>{{ community.communityId || '-' }}</b></span>
          <span class="df-right">
            <span>{{ $t('communityManage.drawer.updatedAt') }} {{ updatedTime }}</span>
            <el-button size="mini" @click="handleClose">{{ $t('common.close') }}</el-button>
          </span>
        </footer>
      </aside>
    </div>
  </transition>
</template>

<script>
import { getPropertyList } from '@/api/store/propertyCompanyManageApi'

const STATE_LIMIT_LOGIN = '48002'

function formatNow() {
  const d = new Date()
  const pad = n => (n < 10 ? '0' + n : '' + n)
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
}

export default {
  name: 'CommunityDetailDrawer',
  props: {
    visible: { type: Boolean, default: false },
    communityData: { type: Object, default: null }
  },
  data() {
    return {
      activeTab: 'company',
      loading: false,
      property: null,
      propertyError: '',
      updatedTime: ''
    }
  },
  computed: {
    community() {
      return this.communityData || {}
    },
    complete() {
      return !!(this.community.tel && this.community.cityName)
    },
    attrList() {
      return this.community.communityAttrDtos || []
    },
    propertyLimited() {
      return !!this.property && this.property.state === STATE_LIMIT_LOGIN
    },
    tabs() {
      return [
        { name: 'company', label: this.$t('communityManage.drawer.tabCompany') },
        { name: 'attr', label: this.$t('communityManage.drawer.tabAttr'), count: this.attrList.length }
      ]
    },
    infoList() {
      const community = this.community
      return [
        { key: 'communityId', label: this.$t('communityManage.drawer.communityId'), value: community.communityId },
        { key: 'tel', label: this.$t('communityManage.drawer.tel'), value: community.tel },
        { key: 'cityName', label: this.$t('communityManage.drawer.city'), value: community.cityName },
        { key: 'cityCode', label: this.$t('communityManage.drawer.cityCode'), value: community.cityCode },
        { key: 'storeName', label: this.$t('communityManage.drawer.store'), value: community.storeName },
        { key: 'stateName', label: this.$t('communityManage.drawer.state'), value: community.stateName },
        { key: 'landmarks', label: this.$t('communityManage.drawer.landmarks'), value: community.nearbyLandmarks },
        { key: 'address', label: this.$t('communityManage.drawer.address'), value: community.address }
      ]
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.activeTab = 'company'
        this.property = null
        this.propertyError = ''
        this.updatedTime = formatNow()
        this.loadProperty()
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
      const wrappers = document.querySelectorAll('.el-dialog__wrapper')
      for (let i = 0; i < wrappers.length; i++) {
        if (wrappers[i].style.display !== 'none') {
          return true
        }
      }
      return false
    },
    async loadProperty() {
      const storeId = this.community.storeId
      if (!storeId) {
        this.propertyError = this.$t('communityManage.drawer.companyEmpty')
        return
      }
      this.loading = true
      try {
        const res = await getPropertyList({ storeId, page: 1, row: 1 })
        const list = res.data || []
        this.property = list.length ? list[0] : null
        if (!this.property) {
          this.propertyError = this.$t('communityManage.drawer.companyNotFound')
        }
      } catch (error) {
        this.property = null
        this.propertyError = error.message || this.$t('communityManage.drawer.companyFetchError')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.community-detail-mask {
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
.dw-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
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
.sec-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sec-head h4 {
  margin: 0;
  font-size: 13.5px;
  font-weight: 600;
  color: #28374f;
  display: flex;
  align-items: center;
  gap: 8px;
}
.sec-head h4 i {
  width: 3px;
  height: 12px;
  border-radius: 2px;
  background: #2b6cf6;
  display: block;
}
.sec-head .sh-right {
  margin-left: auto;
  font-size: 12px;
  color: #8b98a9;
}
.kv-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 20px;
  margin-top: 12px;
}
.kv {
  min-width: 0;
}
.kv .k {
  font-size: 12px;
  color: #93a0b2;
  margin-bottom: 4px;
}
.kv .v {
  font-size: 13px;
  color: #2c3a50;
  font-weight: 500;
  word-break: break-all;
}
.kv .v.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12.5px;
  font-weight: 400;
  color: #41506a;
}
.kv-full {
  grid-column: 1 / -1;
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
.pill {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 5px;
  font-size: 11.5px;
  background: #f3f6fb;
  color: #5b6879;
}
.pill.green {
  background: #e6f7ee;
  color: #0f8b51;
}
.empty-state {
  padding: 26px 0 20px;
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
  margin: 0;
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
.community-drawer-fade-enter-active,
.community-drawer-fade-leave-active {
  transition: opacity 0.22s;
}
.community-drawer-fade-enter-active .dw,
.community-drawer-fade-leave-active .dw {
  transition: transform 0.3s cubic-bezier(0.22, 0.8, 0.3, 1);
}
.community-drawer-fade-enter,
.community-drawer-fade-leave-to {
  opacity: 0;
}
.community-drawer-fade-enter .dw,
.community-drawer-fade-leave-to .dw {
  transform: translateX(100%);
}
@media (max-width: 1400px) {
  .dw-info {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .kv-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
