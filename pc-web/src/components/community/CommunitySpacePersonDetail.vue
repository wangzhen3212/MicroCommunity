<template>
  <el-dialog
    :title="$t('communitySpacePersonManage.detailTitle')"
    :visible.sync="visible"
    width="560px"
    @close="handleClose"
  >
    <div v-if="person" class="detail-body">
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.orderId') }}</span>
        <span class="detail-value">{{ person.orderId || person.cspId }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.venue') }}</span>
        <span class="detail-value">{{ person.venueName || '-' }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.space') }}</span>
        <span class="detail-value">{{ person.spaceName || '-' }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.appointmentPerson') }}</span>
        <span class="detail-value">{{ person.personName || '-' }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.appointmentTel') }}</span>
        <span class="detail-value">{{ person.personTel || '-' }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.appointmentDate') }}</span>
        <span class="detail-value">{{ person.appointmentTime || '-' }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.appointmentTime') }}</span>
        <span class="detail-value">{{ timesText }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.receivableAmount') }}</span>
        <span class="detail-value">￥{{ person.receivableAmount || '0.00' }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.receivedAmount') }}</span>
        <span class="detail-value">￥{{ person.receivedAmount || '0.00' }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.payWay') }}</span>
        <span class="detail-value">{{ payWayText }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.payTime') }}</span>
        <span class="detail-value">{{ person.payTime || $t('communitySpacePersonManage.noTime') }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.state') }}</span>
        <span class="detail-value">
          <span class="state-pill" :class="'state-' + person.state">{{ stateText }}</span>
        </span>
      </div>
      <div v-if="person.state === 'CL'" class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.cancelTime') }}</span>
        <span class="detail-value">{{ person.cancelTime || $t('communitySpacePersonManage.noTime') }}</span>
      </div>
      <div v-if="person.state === 'CL'" class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.cancelReason') }}</span>
        <span class="detail-value">{{ person.cancelReason || '-' }}</span>
      </div>
      <div class="detail-row">
        <span class="detail-label">{{ $t('communitySpacePersonManage.remark') }}</span>
        <span class="detail-value">{{ person.remark || '-' }}</span>
      </div>
      <div class="detail-row times-block">
        <span class="detail-label">{{ $t('communitySpacePersonManage.confirmState') }}</span>
        <div class="times-list">
          <div v-for="item in timesList" :key="item.timeId" class="time-item">
            <span class="time-hours">{{ formatHour(item.hours) }}</span>
            <span class="time-state" :class="timeStateClass(item.state)">{{ timeStateText(item.state) }}</span>
          </div>
          <div v-if="timesList.length < 1" class="times-empty">{{ $t('communitySpacePersonManage.noTime') }}</div>
        </div>
      </div>
    </div>
    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.close') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
export default {
  name: 'CommunitySpacePersonDetail',
  data() {
    return {
      visible: false,
      person: null
    }
  },
  computed: {
    timesList() {
      if (!this.person || !this.person.times) {
        return []
      }
      return this.person.times.slice().sort((a, b) => Number(a.hours) - Number(b.hours))
    },
    timesText() {
      if (this.timesList.length < 1) {
        return '-'
      }
      const first = Number(this.timesList[0].hours)
      const last = Number(this.timesList[this.timesList.length - 1].hours)
      return this.formatHour(first) + ' - ' + this.formatHour(last + 1)
    },
    payWayText() {
      if (!this.person) {
        return '-'
      }
      return this.person.payWayName || this.$t('communitySpacePersonManage.payWayNone')
    },
    stateText() {
      if (!this.person) {
        return '-'
      }
      if (this.person.stateName) {
        return this.person.stateName
      }
      return this.$t('communitySpacePersonManage.stateOptions.' + this.person.state)
    }
  },
  methods: {
    open(row) {
      this.person = row
      this.visible = true
    },
    handleClose() {
      this.visible = false
      this.person = null
    },
    formatHour(value) {
      return (value < 10 ? '0' + value : '' + value) + ':00'
    },
    timeStateText(state) {
      if (state === 'C') {
        return this.$t('communitySpacePersonManage.timeStateDone')
      }
      if (state === 'CL') {
        return this.$t('communitySpacePersonManage.timeStateCanceled')
      }
      return this.$t('communitySpacePersonManage.timeStateWait')
    },
    timeStateClass(state) {
      if (state === 'C') {
        return 'time-done'
      }
      if (state === 'CL') {
        return 'time-canceled'
      }
      return 'time-wait'
    }
  }
}
</script>

<style lang="scss" scoped>
.detail-body {
  max-height: 520px;
  overflow-y: auto;
  font-size: 13px;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 1px solid #f1f5f9;
}

.detail-label {
  color: #64748b;
  flex: none;
  margin-right: 16px;
}

.detail-value {
  color: #334155;
  text-align: right;
  word-break: break-all;
}

.times-block {
  display: block;
}

.times-list {
  margin-top: 8px;
}

.time-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  border-radius: 6px;
  background: #f8fafc;
  margin-bottom: 6px;
}

.time-hours {
  color: #334155;
}

.time-state {
  font-size: 12px;
}

.time-done {
  color: #10b981;
}

.time-wait {
  color: #f59e0b;
}

.time-canceled {
  color: #94a3b8;
}

.times-empty {
  color: #94a3b8;
}

.state-pill {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: #f1f5f9;
  color: #64748b;
}

.state-WP {
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

.state-CL {
  background: #f1f5f9;
  color: #94a3b8;
}
</style>
