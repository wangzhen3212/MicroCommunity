<template>
  <div class="info-panel">
    <div class="panel-header">
      <span class="panel-title">{{ $t('communitySpaceConfirm.infoTitle') }}</span>
      <span class="state-tag" :class="stateTagClass">{{ stateTagText }}</span>
    </div>

    <div class="panel-body">
      <div v-if="done" class="done-block">
        <div class="done-icon">
          <i class="el-icon-circle-check"></i>
        </div>
        <div class="done-title">{{ $t('communitySpaceConfirm.verifySuccess') }}</div>
        <div class="done-desc">{{ doneSummary }}</div>
        <div class="info-grid done-grid">
          <div v-for="item in doneRows" :key="item.label" class="info-row">
            <span class="info-label">{{ item.label }}</span>
            <span class="info-value">{{ item.value }}</span>
          </div>
        </div>
        <p class="done-tip">
          <i class="status-dot dot-on"></i>
          {{ $t('communitySpaceConfirm.verifySuccessTip') }}
        </p>
        <div class="action-row">
          <el-button type="primary" size="small" @click="$emit('reset')">
            {{ $t('communitySpaceConfirm.continueVerify') }}
          </el-button>
          <el-button size="small" @click="$emit('view-order')">
            {{ $t('communitySpaceConfirm.viewOrder') }}
          </el-button>
        </div>
      </div>

      <div v-else-if="notFound && !current" class="empty-block not-found-block">
        <i class="el-icon-circle-close empty-icon"></i>
        <p class="empty-title">{{ $t('communitySpaceConfirm.notFoundTip', { code: notFound }) }}</p>
        <p class="empty-desc">{{ $t('communitySpaceConfirm.notFoundDesc') }}</p>
      </div>

      <div v-else-if="!current" class="empty-block wait-block">
        <i class="el-icon-full-screen empty-icon"></i>
        <p class="empty-title">{{ $t('communitySpaceConfirm.waitTip') }}</p>
        <p class="empty-desc">{{ $t('communitySpaceConfirm.waitCountTip', { count: waitCount }) }}</p>
      </div>

      <div v-else class="current-block">
        <div class="check-bar" :class="checkBarClass">
          <i class="check-icon" :class="checkIcon"></i>
          <div>
            <div class="check-title">{{ checkTitle }}</div>
            <div class="check-desc">{{ checkDesc }}</div>
          </div>
        </div>

        <div class="info-grid">
          <div v-for="item in infoRows" :key="item.label" class="info-row">
            <span class="info-label">{{ item.label }}</span>
            <span class="info-value">{{ item.value }}</span>
          </div>
        </div>

        <div class="action-row">
          <span v-if="level === 'ok' && autoVerify" class="auto-verifying">
            <i class="el-icon-loading"></i>
            {{ $t('communitySpaceConfirm.autoVerifying') }}
          </span>
          <el-button
            v-else
            type="primary"
            size="small"
            class="confirm-button"
            :class="{ 'confirm-button-disabled': level !== 'ok' }"
            :disabled="level !== 'ok'"
            :loading="verifying"
            @click="$emit('confirm')"
          >
            {{ $t('communitySpaceConfirm.confirmVerify') }}
          </el-button>
          <el-button size="small" @click="$emit('reset')">
            {{ $t('communitySpaceConfirm.resetScan') }}
          </el-button>
          <el-button size="small" @click="$emit('view-order')">
            {{ $t('communitySpaceConfirm.viewOrder') }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const LEVEL_OK = 'ok'
const LEVEL_WARN = 'warn'
const LEVEL_ERROR = 'error'

export default {
  name: 'SpaceConfirmInfoPanel',
  props: {
    current: {
      type: Object,
      default: null
    },
    done: {
      type: Object,
      default: null
    },
    notFound: {
      type: String,
      default: ''
    },
    autoVerify: {
      type: Boolean,
      default: true
    },
    verifying: {
      type: Boolean,
      default: false
    },
    waitCount: {
      type: [Number, String],
      default: 0
    }
  },
  computed: {
    level() {
      return this.getLevel(this.current)
    },
    stateTagText() {
      if (this.done) {
        return this.$t('communitySpaceConfirm.stateDone')
      }
      if (this.notFound && !this.current) {
        return this.$t('communitySpaceConfirm.stateNotFound')
      }
      if (!this.current) {
        return this.$t('communitySpaceConfirm.stateWait')
      }
      if (this.level === LEVEL_OK) {
        return this.$t('communitySpaceConfirm.stateOk')
      }
      if (this.level === LEVEL_WARN) {
        return this.$t('communitySpaceConfirm.stateWarn')
      }
      return this.$t('communitySpaceConfirm.stateError')
    },
    stateTagClass() {
      if (this.done || this.level === LEVEL_OK) {
        return 'tag-ok'
      }
      if (this.notFound && !this.current) {
        return 'tag-error'
      }
      if (!this.current) {
        return 'tag-wait'
      }
      if (this.level === LEVEL_WARN) {
        return 'tag-warn'
      }
      return 'tag-error'
    },
    checkBarClass() {
      return this.level === LEVEL_OK ? 'bar-ok' : (this.level === LEVEL_WARN ? 'bar-warn' : 'bar-error')
    },
    checkIcon() {
      if (this.level === LEVEL_OK) {
        return 'el-icon-circle-check'
      }
      if (this.level === LEVEL_WARN) {
        return 'el-icon-warning'
      }
      return 'el-icon-circle-close'
    },
    checkTitle() {
      return this.getCheckTitle(this.current)
    },
    checkDesc() {
      return this.getCheckDesc(this.current)
    },
    infoRows() {
      const order = this.current
      if (!order) {
        return []
      }
      return [
        { label: this.$t('communitySpaceConfirm.orderId'), value: order.orderId || '-' },
        { label: this.$t('communitySpaceConfirm.venueSpace'), value: this.venueSpaceText(order) },
        { label: this.$t('communitySpaceConfirm.personName'), value: order.personName || '-' },
        { label: this.$t('communitySpaceConfirm.personTel'), value: order.personTel || '-' },
        { label: this.$t('communitySpaceConfirm.appointDate'), value: order.appointmentTime || '-' },
        { label: this.$t('communitySpaceConfirm.appointHours'), value: this.hoursText(order) },
        { label: this.$t('communitySpaceConfirm.receivableAmount'), value: this.amountText(order.receivableAmount) },
        { label: this.$t('communitySpaceConfirm.receivedAmount'), value: this.amountText(order.receivedAmount) },
        { label: this.$t('communitySpaceConfirm.payWay'), value: order.payWayName || this.$t('communitySpaceConfirm.payWayNone') },
        { label: this.$t('communitySpaceConfirm.stateLabel'), value: order.stateName || '-' },
        { label: this.$t('communitySpaceConfirm.verifyStateLabel'), value: this.isVerified(order) ? this.$t('communitySpaceConfirm.verified') : this.$t('communitySpaceConfirm.unverified') },
        { label: this.$t('communitySpaceConfirm.remark'), value: order.remark || '-' }
      ]
    },
    doneSummary() {
      const order = this.done
      if (!order) {
        return ''
      }
      return this.venueSpaceText(order) + '　' + (order.appointmentTime || '-') + '　' + this.hoursText(order)
    },
    doneRows() {
      const order = this.done
      if (!order) {
        return []
      }
      return [
        { label: this.$t('communitySpaceConfirm.personName'), value: order.personName || '-' },
        { label: this.$t('communitySpaceConfirm.personTel'), value: order.personTel || '-' },
        { label: this.$t('communitySpaceConfirm.receivableAmount'), value: this.amountText(order.receivableAmount) },
        { label: this.$t('communitySpaceConfirm.receivedAmount'), value: this.amountText(order.receivedAmountPerson) },
        { label: this.$t('communitySpaceConfirm.recordVerifyTime'), value: order.createTime || '-' },
        { label: this.$t('communitySpaceConfirm.recordVerifyMethod'), value: this.methodText(order.verifyMethod) },
        { label: this.$t('communitySpaceConfirm.recordVerifier'), value: order.verifierName || '-' }
      ]
    }
  },
  methods: {
    getLevel(order) {
      if (!order) {
        return ''
      }
      if (order.level === LEVEL_WARN || order.level === LEVEL_ERROR) {
        return order.level
      }
      return LEVEL_OK
    },
    isCanceled(order) {
      return !!order && order.state === 'CL'
    },
    isVerified(order) {
      if (!order) {
        return false
      }
      if (order.timeState === 'C') {
        return true
      }
      return !(order.waitTimeIds && order.waitTimeIds.length > 0)
    },
    getCheckTitle(order) {
      if (!order) {
        return ''
      }
      const level = this.getLevel(order)
      if (level === LEVEL_OK) {
        return this.$t('communitySpaceConfirm.checkOkTitle')
      }
      if (level === LEVEL_WARN) {
        return this.$t('communitySpaceConfirm.checkWarnTitle')
      }
      if (this.isCanceled(order)) {
        return this.$t('communitySpaceConfirm.checkErrorCancelTitle')
      }
      if (this.isVerified(order)) {
        return this.$t('communitySpaceConfirm.checkErrorVerifiedTitle')
      }
      return order.title || order.desc || this.$t('communitySpaceConfirm.stateError')
    },
    getCheckDesc(order) {
      if (!order) {
        return ''
      }
      const level = this.getLevel(order)
      if (level === LEVEL_OK) {
        return this.$t('communitySpaceConfirm.checkOkDesc', { hours: this.hoursText(order) })
      }
      if (level === LEVEL_WARN) {
        return this.$t('communitySpaceConfirm.checkWarnDesc', { amount: this.amountText(order.receivableAmount) })
      }
      if (this.isCanceled(order)) {
        return this.$t('communitySpaceConfirm.checkErrorCancelDesc')
      }
      if (this.isVerified(order)) {
        return this.$t('communitySpaceConfirm.checkErrorVerifiedDesc', {
          time: order.verifyTime || '-',
          verifier: order.verifierName || '-',
          method: this.methodText(order.verifyMethod)
        })
      }
      return order.desc || order.title || ''
    },
    venueSpaceText(order) {
      const venueName = order.venueName || '-'
      const spaceName = order.spaceName || order.field || '-'
      return venueName + ' · ' + spaceName
    },
    hoursList(order) {
      if (!order) {
        return []
      }
      if (order.times && order.times.length > 0) {
        return order.times.map(item => Number(item.hours)).filter(item => !isNaN(item))
      }
      if (order.hours !== null && order.hours !== undefined && order.hours !== '') {
        const hour = Number(order.hours)
        if (!isNaN(hour)) {
          return [hour]
        }
      }
      return []
    },
    hoursText(order) {
      const hours = this.hoursList(order)
      if (hours.length < 1) {
        return '-'
      }
      const sorted = hours.slice().sort((a, b) => a - b)
      const range = this.pad(sorted[0]) + ':00~' + this.pad(sorted[sorted.length - 1] + 1) + ':00'
      return range + '  ' + this.$t('communitySpaceConfirm.hoursUnit', { count: sorted.length })
    },
    pad(value) {
      return value < 10 ? '0' + value : '' + value
    },
    amountText(value) {
      if (value === null || value === undefined || value === '') {
        return '￥0.00'
      }
      return '￥' + Number(value).toFixed(2)
    },
    methodText(method) {
      if (method === '2') {
        return this.$t('communitySpaceConfirm.verifyMethodManual')
      }
      if (method === '1') {
        return this.$t('communitySpaceConfirm.verifyMethodScan')
      }
      return '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.info-panel {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.12);
}

.panel-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.state-tag {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
}

.tag-wait {
  background: #f1f5f9;
  color: #64748b;
}

.tag-ok {
  background: #ecfdf5;
  color: #059669;
}

.tag-warn {
  background: #fffbeb;
  color: #d97706;
}

.tag-error {
  background: #fff1f2;
  color: #e11d48;
}

.panel-body {
  flex: 1;
  padding: 16px;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot-on {
  background: #10b981;
}

.empty-block {
  padding: 48px 16px;
  text-align: center;
}

.empty-icon {
  font-size: 52px;
}

.wait-block .empty-icon {
  color: #cbd5e1;
}

.not-found-block .empty-icon {
  color: #fca5a5;
}

.empty-title {
  margin: 12px 0 0;
  font-size: 13px;
  color: #475569;
}

.empty-desc {
  margin: 6px 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.check-bar {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 16px;
  border: 1px solid transparent;
  border-radius: 8px;
}

.bar-ok {
  background: #ecfdf5;
  border-color: #d1fae5;
  color: #047857;
}

.bar-warn {
  background: #fffbeb;
  border-color: #fef3c7;
  color: #b45309;
}

.bar-error {
  background: #fff1f2;
  border-color: #ffe4e6;
  color: #be123c;
}

.check-icon {
  font-size: 18px;
  line-height: 22px;
}

.check-title {
  font-size: 13px;
  font-weight: 500;
}

.check-desc {
  margin-top: 3px;
  font-size: 12px;
  opacity: 0.85;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 32px;
  margin-top: 14px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
}

.info-label {
  flex: none;
  color: #64748b;
}

.info-value {
  color: #334155;
  text-align: right;
  word-break: break-all;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}

.auto-verifying {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 18px;
  border-radius: 8px;
  background: #ecfdf5;
  color: #059669;
  font-size: 13px;
}

.confirm-button {
  padding: 9px 22px;
}

.confirm-button-disabled {
  background-color: #f1f5f9 !important;
  border-color: #e2e8f0 !important;
  color: #94a3b8 !important;
}

.done-block {
  padding: 12px 8px;
  text-align: center;
}

.done-icon {
  font-size: 56px;
  color: #10b981;
}

.done-title {
  margin-top: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.done-desc {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.done-grid {
  max-width: 420px;
  margin: 16px auto 0;
  text-align: left;
}

.done-grid .info-row:last-child,
.done-grid .info-row:nth-last-child(2) {
  border-bottom: none;
}

.done-tip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: 14px 0 0;
  font-size: 12px;
  color: #059669;
}

.done-block .action-row {
  justify-content: center;
}
</style>
