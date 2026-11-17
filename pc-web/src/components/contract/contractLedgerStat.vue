<template>
  <div class="cc-stat-row">
    <div class="cc-stat-card">
      <div class="cc-stat-head">
        <div class="cc-stat-body">
          <div class="cc-stat-label">{{ $t('contractLedger.stat.total') }}</div>
          <div class="cc-stat-value">{{ total }}</div>
        </div>
        <div class="cc-stat-icon is-primary">
          <i class="el-icon-document"></i>
        </div>
      </div>
    </div>
    <div class="cc-stat-card">
      <div class="cc-stat-head">
        <div class="cc-stat-body">
          <div class="cc-stat-label">{{ $t('contractLedger.stat.effective') }}</div>
          <div class="cc-stat-value is-success">{{ effective }}</div>
        </div>
        <div class="cc-stat-icon is-success">
          <i class="el-icon-circle-check"></i>
        </div>
      </div>
      <div class="cc-stat-extra">{{ $t('contractLedger.stat.effectiveExtra', { roomCount: roomCount }) }}</div>
    </div>
    <div class="cc-stat-card" @click="$emit('filter-auditing')">
      <div class="cc-stat-head">
        <div class="cc-stat-body">
          <div class="cc-stat-label">{{ $t('contractLedger.stat.auditing') }}</div>
          <div class="cc-stat-value is-warning">{{ auditing }}</div>
        </div>
        <div class="cc-stat-icon is-warning">
          <i class="el-icon-warning-outline"></i>
        </div>
      </div>
      <div class="cc-stat-extra">{{ $t('contractLedger.stat.auditingExtra') }}</div>
    </div>
    <div class="cc-stat-card">
      <div class="cc-stat-head">
        <div class="cc-stat-body">
          <div class="cc-stat-label">{{ $t('contractLedger.stat.amount') }}</div>
          <div class="cc-stat-value">¥ {{ formatAmount(totalAmount) }}</div>
        </div>
        <div class="cc-stat-icon is-sky">
          <i class="el-icon-money"></i>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { formatAmount, toNumber } from '@/utils/contractUtil'

export default {
  name: 'ContractLedgerStat',
  props: {
    statistics: {
      type: Object,
      default: function () {
        return {}
      }
    }
  },
  computed: {
    total() {
      return toNumber(this.statistics.total)
    },
    effective() {
      return toNumber(this.statistics.effective)
    },
    auditing() {
      return toNumber(this.statistics.auditing)
    },
    draft() {
      return toNumber(this.statistics.draft)
    },
    roomCount() {
      return toNumber(this.statistics.roomCount)
    },
    totalAmount() {
      return toNumber(this.statistics.totalAmount)
    }
  },
  methods: {
    formatAmount
  }
}
</script>
