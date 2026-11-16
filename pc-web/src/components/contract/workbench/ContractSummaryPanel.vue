<template>
  <div class="cc-summary-panel">
    <div class="cc-summary-block">
      <div class="cc-section-title">{{ $t('contractWorkbench.summary.title') }}</div>
      <div class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractWorkbench.summary.amount') }}</span>
        <span class="cc-kv-value">{{ formatAmount(form.amount) }}</span>
      </div>
      <div class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractWorkbench.summary.term') }}</span>
        <span class="cc-kv-value">{{ formatTerm(form.startTime, form.endTime) }}</span>
      </div>
      <div class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractWorkbench.summary.targetCount') }}</span>
        <span class="cc-kv-value">{{ targetCount }}</span>
      </div>
      <div class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractWorkbench.summary.planCount') }}</span>
        <span class="cc-kv-value">{{ planCount }}</span>
      </div>
      <div class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractWorkbench.summary.operator') }}</span>
        <span class="cc-kv-value">{{ form.operator || '-' }}</span>
      </div>
    </div>

    <div class="cc-summary-block">
      <div class="cc-section-title">
        {{ $t('contractWorkbench.summary.integrity') }}
        <span class="cc-badge" :class="integrityClass">{{ integrityPassed }}/7</span>
      </div>
      <div v-for="item in integrityItems" :key="item.key" class="cc-integrity-row">
        <i :class="item.pass ? 'el-icon-circle-check is-pass' : 'el-icon-warning-outline is-fail'"></i>
        <span>{{ $t(item.labelKey) }}</span>
      </div>
      <div class="cc-summary-tip">{{ $t('contractWorkbench.summary.integrityTip') }}</div>
    </div>

    <div class="cc-summary-block">
      <div class="cc-section-title">{{ $t('contractWorkbench.summary.flow') }}</div>
      <contract-audit-flow :audit="audit" :records="records" />
    </div>

    <div class="cc-summary-block">
      <div class="cc-section-title">{{ $t('contractWorkbench.summary.tipTitle') }}</div>
      <div class="cc-summary-tip">{{ $t('contractWorkbench.summary.tipText') }}</div>
    </div>
  </div>
</template>

<script>
import ContractAuditFlow from '@/components/contract/contractAuditFlow'
import { formatAmount, formatTerm, toNumber } from '@/utils/contractUtil'

export default {
  name: 'ContractSummaryPanel',
  components: {
    ContractAuditFlow
  },
  props: {
    form: {
      type: Object,
      default: function () {
        return {}
      }
    },
    targetCount: {
      type: Number,
      default: 0
    },
    planCount: {
      type: Number,
      default: 0
    },
    hasContent: {
      type: Boolean,
      default: false
    },
    audit: {
      type: Object,
      default: null
    },
    records: {
      type: Array,
      default: function () {
        return []
      }
    }
  },
  computed: {
    integrityItems() {
      const form = this.form || {}
      return [
        {
          key: 'base',
          labelKey: 'contractWorkbench.integrity.base',
          pass: !!(form.contractName && form.contractCode && form.contractType)
        },
        {
          key: 'partyA',
          labelKey: 'contractWorkbench.integrity.partyA',
          pass: !!(form.partyaId || form.partyA)
        },
        {
          key: 'partyB',
          labelKey: 'contractWorkbench.integrity.partyB',
          pass: !!(form.partyB && form.bLink)
        },
        {
          key: 'amount',
          labelKey: 'contractWorkbench.integrity.amount',
          pass: toNumber(form.amount) > 0
        },
        {
          key: 'term',
          labelKey: 'contractWorkbench.integrity.term',
          pass: !!(form.startTime && form.endTime)
        },
        {
          key: 'target',
          labelKey: 'contractWorkbench.integrity.target',
          pass: this.targetCount > 0
        },
        {
          key: 'plan',
          labelKey: 'contractWorkbench.integrity.plan',
          pass: this.planCount > 0 || this.hasContent
        }
      ]
    },
    integrityPassed() {
      return this.integrityItems.filter(item => item.pass).length
    },
    integrityClass() {
      return this.integrityPassed === 7 ? 'is-success' : 'is-warning'
    }
  },
  methods: {
    formatAmount,
    formatTerm,
    failedLabels() {
      return this.integrityItems.filter(item => !item.pass).map(item => this.$t(item.labelKey))
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-summary-panel {
  .cc-summary-block {
    margin-bottom: 20px;
  }

  .cc-integrity-row {
    display: flex;
    align-items: center;
    font-size: 13px;
    line-height: 24px;
    color: #606266;

    i {
      margin-right: 6px;

      &.is-pass {
        color: #67c23a;
      }

      &.is-fail {
        color: #e6a23c;
      }
    }
  }

  .cc-summary-tip {
    margin-top: 8px;
    font-size: 12px;
    line-height: 1.7;
    color: #909399;
  }
}
</style>
