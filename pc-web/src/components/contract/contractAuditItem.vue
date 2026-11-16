<template>
  <div class="cc-audit-item">
    <div class="cc-audit-item-head">
      <span class="cc-audit-code">{{ audit.auditCode }}</span>
      <span class="cc-badge is-info">{{ auditTypeText }}</span>
      <span class="cc-badge" :class="stateClass">{{ stateText }}</span>
      <span class="cc-audit-contract">{{ audit.bizCode }} {{ audit.bizName }}</span>
      <span class="cc-audit-amount">{{ formatAmount(audit.bizAmount) }}</span>
    </div>
    <div class="cc-audit-item-meta">
      {{ $t('contractAuditCenter.table.submitter') }}：{{ audit.submitStaffName || '-' }}
      · {{ formatTime(audit.submitTime) }}
      · {{ $t('contractAuditCenter.table.currentStep') }}：{{ currentStepText }}
    </div>
    <contract-audit-flow :audit="audit" :records="records" />
  </div>
</template>

<script>
import ContractAuditFlow from '@/components/contract/contractAuditFlow'
import { formatAmount } from '@/utils/contractUtil'

export default {
  name: 'ContractAuditItem',
  components: {
    ContractAuditFlow
  },
  props: {
    item: {
      type: Object,
      default: function () {
        return {}
      }
    }
  },
  computed: {
    audit() {
      return this.item.audit || {}
    },
    records() {
      return this.item.records || []
    },
    auditTypeText() {
      return String(this.audit.auditType) === '2'
        ? this.$t('contractAuditCenter.typeChange')
        : this.$t('contractAuditCenter.typeContract')
    },
    stateText() {
      const state = String(this.audit.state)
      if (state === '0') {
        return this.$t('contractAuditCenter.stateDoing')
      }
      if (state === '1') {
        return this.$t('contractAuditCenter.statePass')
      }
      if (state === '2') {
        return this.$t('contractAuditCenter.stateReject')
      }
      if (state === '3') {
        return this.$t('contractAuditCenter.stateCancel')
      }
      return this.$t('contractState.unknown')
    },
    stateClass() {
      const state = String(this.audit.state)
      if (state === '1') {
        return 'is-success'
      }
      if (state === '2') {
        return 'is-danger'
      }
      if (state === '3') {
        return 'is-info'
      }
      return 'is-warning'
    },
    currentStepText() {
      if (String(this.audit.state) !== '0') {
        return this.stateText
      }
      return String(this.audit.currentStep) === '2'
        ? this.$t('contractAuditCenter.stepLeader')
        : this.$t('contractAuditCenter.stepLegal')
    }
  },
  methods: {
    formatAmount,
    formatTime(time) {
      if (!time) {
        return ''
      }
      return String(time).replace('T', ' ').substring(0, 19)
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-audit-item {
  padding: 4px 8px;

  .cc-audit-item-head {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    font-size: 13px;

    .cc-audit-code {
      font-weight: 600;
      margin-right: 8px;
    }

    .cc-audit-contract {
      margin-left: 12px;
      color: #303133;
    }

    .cc-audit-amount {
      margin-left: auto;
      font-variant-numeric: tabular-nums;
    }
  }

  .cc-audit-item-meta {
    font-size: 12px;
    color: #909399;
    margin: 6px 0 12px;
  }
}
</style>
