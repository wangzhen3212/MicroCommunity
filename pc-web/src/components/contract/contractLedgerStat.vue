<template>
  <div class="cc-stat-row">
    <div class="cc-stat-card">
      <div class="cc-stat-label">{{ $t('contractLedger.stat.total') }}</div>
      <div class="cc-stat-value">{{ total }}</div>
      <div class="cc-stat-extra">
        {{ $t('contractLedger.stat.totalExtra', { rent: rentCount, purchase: purchaseCount, maintain: maintainCount }) }}
      </div>
    </div>
    <div class="cc-stat-card">
      <div class="cc-stat-label">{{ $t('contractLedger.stat.effective') }}</div>
      <div class="cc-stat-value">{{ effective }}</div>
      <div class="cc-stat-extra">{{ $t('contractLedger.stat.effectiveExtra', { roomCount: roomCount }) }}</div>
    </div>
    <div class="cc-stat-card" @click="$emit('filter-auditing')">
      <div class="cc-stat-label">{{ $t('contractLedger.stat.auditing') }}</div>
      <div class="cc-stat-value">{{ auditing }}</div>
      <div class="cc-stat-extra">{{ $t('contractLedger.stat.auditingExtra') }}</div>
    </div>
    <div class="cc-stat-card">
      <div class="cc-stat-label">{{ $t('contractLedger.stat.amount') }}</div>
      <div class="cc-stat-value">{{ formatAmount(totalAmount) }}</div>
      <div class="cc-stat-extra">
        {{ $t('contractLedger.stat.amountExtra', {
          rentAmount: formatAmount(rentAmount),
          purchaseAmount: formatAmount(purchaseAmount),
          maintainAmount: formatAmount(maintainAmount)
        }) }}
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
    rentCount() {
      return toNumber(this.statistics.rentCount)
    },
    purchaseCount() {
      return toNumber(this.statistics.purchaseCount)
    },
    maintainCount() {
      return toNumber(this.statistics.maintainCount)
    },
    totalAmount() {
      return toNumber(this.statistics.totalAmount)
    },
    rentAmount() {
      return toNumber(this.statistics.rentAmount)
    },
    purchaseAmount() {
      return toNumber(this.statistics.purchaseAmount)
    },
    maintainAmount() {
      return toNumber(this.statistics.maintainAmount)
    }
  },
  methods: {
    formatAmount
  }
}
</script>
