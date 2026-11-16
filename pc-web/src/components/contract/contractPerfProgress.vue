<template>
  <div class="cc-perf-progress">
    <el-progress
      :percentage="percentage"
      :status="status"
      :stroke-width="8"
      :show-text="false"
    />
    <div class="cc-perf-meta">
      <span>{{ formatAmount(paidAmount) }} / {{ formatAmount(amount) }}</span>
      <span v-if="overdueCount > 0" class="cc-badge is-danger">{{ overdueCount }}</span>
    </div>
  </div>
</template>

<script>
import { formatAmount, toNumber } from '@/utils/contractUtil'

export default {
  name: 'ContractPerfProgress',
  props: {
    amount: {
      type: [String, Number],
      default: 0
    },
    paidAmount: {
      type: [String, Number],
      default: 0
    },
    planCount: {
      type: [String, Number],
      default: 0
    },
    paidCount: {
      type: [String, Number],
      default: 0
    },
    overdueCount: {
      type: [String, Number],
      default: 0
    }
  },
  computed: {
    percentage() {
      const total = toNumber(this.amount)
      if (total <= 0) {
        return 0
      }
      const value = Math.round((toNumber(this.paidAmount) / total) * 100)
      return value > 100 ? 100 : value
    },
    status() {
      if (toNumber(this.overdueCount) > 0) {
        return 'exception'
      }
      if (this.percentage >= 100) {
        return 'success'
      }
      return null
    }
  },
  methods: {
    formatAmount
  }
}
</script>

<style lang="scss" scoped>
.cc-perf-progress {
  min-width: 140px;

  .cc-perf-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 4px;
    font-size: 12px;
    color: #909399;

    .cc-badge {
      margin-left: 8px;
    }
  }
}
</style>
