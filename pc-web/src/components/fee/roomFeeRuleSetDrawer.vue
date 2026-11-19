<template>
  <el-drawer :visible.sync="visible" size="520px" :with-header="false" @close="handleClose">
    <div class="drawer-wrap">
      <div class="drawer-head">
        <div>
          <div class="drawer-title">{{ $t('roomFeeRuleSet.title') }} · {{ room.roomName }}</div>
          <div class="drawer-sub">{{ $t('roomFeeRuleSet.subtitle') }}</div>
        </div>
        <el-button type="text" icon="el-icon-close" @click="handleClose"></el-button>
      </div>

      <div class="drawer-body" v-loading="loading">
        <div class="drawer-tip">{{ $t('roomFeeRuleSet.tip') }}</div>

        <div class="section">
          <div class="section-title">{{ $t('roomFeeRuleSet.step1') }}</div>
          <div v-if="feeConfigs.length === 0" class="section-empty">{{ $t('roomFeeRuleSet.noConfig') }}</div>
          <div v-for="config in feeConfigs" :key="config.configId" class="config-item"
            :class="{ on: isSelected(config), disabled: isExisting(config) }"
            @click="toggleConfig(config)">
            <el-checkbox :value="isSelected(config)" :disabled="isExisting(config)" @click.native.prevent>
            </el-checkbox>
            <div class="config-body">
              <div class="config-name">{{ config.feeName }}</div>
              <div class="config-desc">
                {{ config.feeTypeCdName || '-' }} · {{ config.computingFormulaName || '-' }}
                <span v-if="config.squarePrice"> · {{ config.squarePrice }} 元</span>
              </div>
            </div>
            <el-tag v-if="isExisting(config)" size="mini" type="info">{{ $t('roomFeeRuleSet.existsTag') }}</el-tag>
            <el-tag v-else size="mini">{{ $t('roomFeeRuleSet.addableTag') }}</el-tag>
          </div>
        </div>

        <el-row :gutter="12" class="section">
          <el-col :span="12">
            <div class="section-title">{{ $t('roomFeeRuleSet.step2') }}</div>
            <el-select v-model="period" size="small" style="width: 100%" @change="handlePeriodChange">
              <el-option :label="$t('roomFeeRuleSet.periodCurrent', { period: currentPeriod })" value="current">
              </el-option>
              <el-option :label="$t('roomFeeRuleSet.periodNext', { period: nextPeriod })" value="next"></el-option>
            </el-select>
          </el-col>
          <el-col :span="12">
            <div class="section-title">{{ $t('roomFeeRuleSet.step3') }}</div>
            <el-select v-model="billMode" size="small" style="width: 100%" @change="handleBillModeChange">
              <el-option :label="$t('roomFeeRuleSet.billImmediate')" value="immediate"></el-option>
              <el-option :label="$t('roomFeeRuleSet.billAuto')" value="auto"></el-option>
            </el-select>
          </el-col>
        </el-row>

        <div class="section">
          <div class="section-title">{{ $t('roomFeeRuleSet.maxTime') }}</div>
          <el-date-picker v-model="maxTime" type="date" size="small" value-format="yyyy-MM-dd" style="width: 100%">
          </el-date-picker>
        </div>

        <div class="section">
          <div class="section-title">{{ $t('roomFeeRuleSet.existing') }}</div>
          <div v-if="existingList.length === 0" class="section-empty">{{ $t('roomFeeRuleSet.noExisting') }}</div>
          <div v-for="item in existingList" :key="item.ruleId" class="existing-item">
            <span class="existing-name">{{ item.feeName || '-' }}</span>
            <span class="existing-time">{{ dateOnly(item.startTime) }} ~ {{ dateOnly(item.maxTime) }}</span>
            <el-tag size="mini" :type="item.state === '2008001' ? 'success' : 'info'">
              {{ item.state === '2008001' ? $t('roomFeeRuleBill.stateDoing') : $t('roomFeeRuleBill.stateStopped') }}
            </el-tag>
          </div>
        </div>
      </div>

      <div class="drawer-foot">
        <span class="foot-count">{{ $t('roomFeeRuleSet.selectedCount', { count: selectedConfigIds.length }) }}</span>
        <div>
          <el-button size="small" @click="handleClose">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" size="small" :loading="submitting" @click="handleSubmit">
            {{ $t('roomFeeRuleSet.save') }}
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import { saveRoomCreateFee, listFeeConfigs } from '@/api/fee/roomCreateFeeAddApi'
import { getCommunityId } from '@/api/community/communityApi'
import { dateFormat } from '@/utils/dateUtil'
import { filterFeeConfigsByFormula } from '@/utils/feeUtil'

const UNSUPPORTED_FORMULAS = ['1102', '4004']

export default {
  name: 'RoomFeeRuleSetDrawer',
  data() {
    return {
      visible: false,
      loading: false,
      submitting: false,
      room: {
        roomId: '',
        roomName: '',
        ownerName: ''
      },
      existingList: [],
      existingConfigIds: [],
      feeConfigs: [],
      selectedConfigIds: [],
      period: 'current',
      billMode: 'immediate',
      maxTime: '2050-01-01'
    }
  },
  computed: {
    currentPeriod() {
      return this.monthStart(0).substring(0, 7)
    },
    nextPeriod() {
      return this.monthStart(1).substring(0, 7)
    },
    startTime() {
      if (this.billMode === 'auto' || this.period === 'next') {
        return this.monthStart(1)
      }
      return this.monthStart(0)
    },
    selectedConfigs() {
      return this.feeConfigs.filter(config => this.selectedConfigIds.indexOf(config.configId) >= 0)
    }
  },
  methods: {
    open(params) {
      this.room = {
        roomId: params.roomId || '',
        roomName: params.roomName || '',
        ownerName: params.ownerName || ''
      }
      this.existingList = params.existingRules || []
      this.existingConfigIds = params.existingConfigIds || []
      this.selectedConfigIds = []
      this.period = 'current'
      this.billMode = 'immediate'
      this.maxTime = '2050-01-01'
      this.visible = true
      this.loadFeeConfigs()
    },
    loadFeeConfigs() {
      this.loading = true
      listFeeConfigs({
        page: 1,
        row: 500,
        communityId: getCommunityId(),
        isDefault: 'F',
        state: 'Y'
      }).then(res => {
        const configs = filterFeeConfigsByFormula(res && res.feeConfigs)
        this.feeConfigs = (configs || []).filter(config => UNSUPPORTED_FORMULAS.indexOf(config.computingFormula) < 0)
      }).catch(error => {
        this.feeConfigs = []
        this.$message.error(error.message || this.$t('roomFeeRuleSet.loadFailed'))
      }).finally(() => {
        this.loading = false
      })
    },
    isExisting(config) {
      return this.existingConfigIds.indexOf(config.configId) >= 0
    },
    isSelected(config) {
      return this.selectedConfigIds.indexOf(config.configId) >= 0
    },
    toggleConfig(config) {
      if (this.isExisting(config)) {
        return
      }
      const index = this.selectedConfigIds.indexOf(config.configId)
      if (index >= 0) {
        this.selectedConfigIds.splice(index, 1)
        return
      }
      this.selectedConfigIds.push(config.configId)
    },
    handlePeriodChange(period) {
      if (period === 'current' && this.billMode === 'auto') {
        this.billMode = 'immediate'
      }
    },
    handleBillModeChange(billMode) {
      if (billMode === 'auto') {
        this.period = 'next'
      }
    },
    monthStart(offset) {
      const now = new Date()
      return dateFormat(new Date(now.getFullYear(), now.getMonth() + offset, 1))
    },
    dateOnly(value) {
      if (!value) {
        return '—'
      }
      return String(value).substring(0, 10)
    },
    handleClose() {
      this.visible = false
      this.submitting = false
    },
    async handleSubmit() {
      if (!this.room.roomId) {
        this.$message.warning(this.$t('roomFeeRuleSet.noRoom'))
        return
      }
      if (this.selectedConfigs.length === 0) {
        this.$message.warning(this.$t('roomFeeRuleSet.selectRequired'))
        return
      }
      this.submitting = true
      const communityId = getCommunityId()
      const startTime = this.startTime
      const failures = []
      for (let i = 0; i < this.selectedConfigs.length; i++) {
        const config = this.selectedConfigs[i]
        try {
          await saveRoomCreateFee({
            communityId: communityId,
            locationObjId: this.room.roomId,
            locationTypeCdName: this.room.roomName,
            configId: config.configId,
            feeTypeCd: config.feeTypeCd,
            startTime: startTime,
            endTime: this.maxTime
          })
        } catch (error) {
          failures.push((config.feeName || config.configId) + ': ' + (error.message || ''))
        }
      }
      this.submitting = false
      if (failures.length === 0) {
        this.$message.success(this.$t('roomFeeRuleSet.saveSuccess'))
        this.$emit('success')
        this.handleClose()
        return
      }
      this.$message.error(this.$t('roomFeeRuleSet.partialFail', {
        count: failures.length,
        detail: failures[0]
      }))
      this.$emit('success')
    }
  }
}
</script>

<style scoped>
.drawer-wrap {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.drawer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid #ebeef5;
}

.drawer-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.drawer-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.drawer-body {
  flex: 1;
  padding: 14px 18px;
  overflow-y: auto;
}

.drawer-tip {
  padding: 8px 12px;
  font-size: 12px;
  line-height: 1.6;
  color: #606266;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.section {
  margin-top: 14px;
}

.section-title {
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #606266;
}

.section-empty {
  font-size: 12px;
  color: #909399;
}

.config-item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  margin-bottom: 6px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  cursor: pointer;
}

.config-item.on {
  background: #ecf5ff;
  border-color: #409eff;
}

.config-item.disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.config-body {
  flex: 1;
  margin-left: 8px;
}

.config-name {
  font-size: 13px;
  color: #303133;
}

.config-desc {
  margin-top: 2px;
  font-size: 11px;
  color: #909399;
}

.existing-item {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  margin-bottom: 6px;
  font-size: 12px;
  background: #f8fafc;
  border: 1px solid #f2f6fc;
  border-radius: 4px;
}

.existing-name {
  color: #303133;
}

.existing-time {
  flex: 1;
  margin-left: 10px;
  color: #909399;
}

.drawer-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  border-top: 1px solid #ebeef5;
}

.foot-count {
  font-size: 12px;
  color: #909399;
}
</style>
