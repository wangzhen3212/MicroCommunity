<template>
  <div class="cc-stat-row">
    <div class="cc-stat-card has-icon" @click="$emit('change', 'pending')">
      <div class="cc-stat-icon is-warning">
        <i class="el-icon-time"></i>
      </div>
      <div class="cc-stat-body">
        <div class="cc-stat-label">{{ $t('contractAuditCenter.stat.pending') }}</div>
        <div class="cc-stat-value">{{ pending }}</div>
      </div>
    </div>
    <div class="cc-stat-card has-icon" @click="$emit('change', 'pass')">
      <div class="cc-stat-icon is-success">
        <i class="el-icon-check"></i>
      </div>
      <div class="cc-stat-body">
        <div class="cc-stat-label">{{ $t('contractAuditCenter.stat.monthPass') }}</div>
        <div class="cc-stat-value">{{ monthPass }}</div>
      </div>
    </div>
    <div class="cc-stat-card has-icon" @click="$emit('change', 'reject')">
      <div class="cc-stat-icon is-danger">
        <i class="el-icon-close"></i>
      </div>
      <div class="cc-stat-body">
        <div class="cc-stat-label">{{ $t('contractAuditCenter.stat.monthReject') }}</div>
        <div class="cc-stat-value">{{ monthReject }}</div>
      </div>
    </div>
  </div>
</template>

<script>
import { toNumber } from '@/utils/contractUtil'

export default {
  name: 'ContractAuditStat',
  props: {
    statistics: {
      type: Object,
      default: function () {
        return {}
      }
    }
  },
  computed: {
    pending() {
      return toNumber(this.statistics.pending)
    },
    monthPass() {
      return toNumber(this.statistics.monthPass)
    },
    monthReject() {
      return toNumber(this.statistics.monthReject)
    }
  }
}
</script>
