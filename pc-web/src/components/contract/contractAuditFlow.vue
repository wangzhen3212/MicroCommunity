<template>
  <div class="cc-flow">
    <div class="cc-flow-step" :class="stepClass('1')">
      <span class="cc-flow-dot"></span>
      <div class="cc-flow-name">
        {{ $t('contractAuditFlow.legal') }}
        <span v-if="isCurrent('1')" class="cc-badge is-primary">{{ $t('contractAuditFlow.current') }}</span>
      </div>
      <div class="cc-flow-meta">{{ stepMeta('1') }}</div>
      <div v-if="stepOpinion('1')" class="cc-flow-meta">{{ $t('contractAuditFlow.opinion') }}：{{ stepOpinion('1') }}</div>
    </div>

    <div class="cc-flow-step" :class="stepClass('2')">
      <span class="cc-flow-dot"></span>
      <div class="cc-flow-name">
        {{ $t('contractAuditFlow.leader') }}
        <span v-if="isCurrent('2')" class="cc-badge is-primary">{{ $t('contractAuditFlow.current') }}</span>
      </div>
      <div class="cc-flow-meta">{{ stepMeta('2') }}</div>
      <div v-if="stepOpinion('2')" class="cc-flow-meta">{{ $t('contractAuditFlow.opinion') }}：{{ stepOpinion('2') }}</div>
    </div>

    <div class="cc-flow-step" :class="finalClass">
      <span class="cc-flow-dot"></span>
      <div class="cc-flow-name">{{ $t('contractAuditFlow.effective') }}</div>
      <div class="cc-flow-meta">{{ finalMeta }}</div>
    </div>

    <div class="cc-flow-records">
      <div class="cc-flow-records-title">{{ $t('contractAuditFlow.history') }}</div>
      <div v-if="recordList.length">
        <div v-for="(record, index) in recordList" :key="'record' + index" class="cc-flow-record">
          <span class="cc-flow-record-step">{{ record.stepName || stepText(record.step) }}</span>
          <span class="cc-flow-record-auditor">{{ record.auditorName || '-' }}</span>
          <span class="cc-flow-record-result" :class="resultClass(record.result)">
            {{ record.resultName || resultText(record.result) }}
          </span>
          <span class="cc-flow-record-time">{{ formatTime(record.auditTime) }}</span>
          <span v-if="record.opinion" class="cc-flow-record-opinion">{{ record.opinion }}</span>
        </div>
      </div>
      <div v-else class="cc-flow-meta">{{ $t('contractAuditFlow.historyEmpty') }}</div>
    </div>
  </div>
</template>

<script>
import { AUDIT_RESULT, CONTRACT_STATE } from '@/utils/contractUtil'

export default {
  name: 'ContractAuditFlow',
  props: {
    state: {
      type: [String, Number],
      default: ''
    },
    records: {
      type: Array,
      default: function () {
        return []
      }
    }
  },
  computed: {
    stateValue() {
      return String(this.state === null || this.state === undefined ? '' : this.state)
    },
    recordList() {
      return this.records || []
    },
    finalClass() {
      if (this.stateValue === CONTRACT_STATE.EFFECTIVE) {
        return 'is-pass'
      }
      if (this.stateValue === CONTRACT_STATE.FAIL) {
        return 'is-reject'
      }
      return ''
    },
    finalMeta() {
      if (this.stateValue === CONTRACT_STATE.EFFECTIVE) {
        return this.$t('contractAuditFlow.resultPass')
      }
      if (this.stateValue === CONTRACT_STATE.FAIL) {
        return this.$t('contractAuditFlow.resultReject')
      }
      return this.$t('contractAuditFlow.waiting')
    }
  },
  methods: {
    recordOf(step) {
      let matched = null
      for (let index = 0; index < this.recordList.length; index++) {
        if (String(this.recordList[index].step) === String(step)) {
          matched = this.recordList[index]
        }
      }
      return matched
    },
    isCurrent(step) {
      if (this.stateValue === CONTRACT_STATE.AUDIT) {
        return String(step) === '1'
      }
      if (this.stateValue === CONTRACT_STATE.AUDIT_LEADER) {
        return String(step) === '2'
      }
      return false
    },
    stepClass(step) {
      if (this.isCurrent(step)) {
        return 'is-current'
      }
      const record = this.recordOf(step)
      if (!record) {
        return ''
      }
      if (String(record.result) === AUDIT_RESULT.PASS) {
        return 'is-pass'
      }
      if (String(record.result) === AUDIT_RESULT.REJECT) {
        return 'is-reject'
      }
      return ''
    },
    stepMeta(step) {
      const record = this.recordOf(step)
      if (!record) {
        return this.isCurrent(step) ? this.$t('contractAuditFlow.waiting') : '-'
      }
      const name = record.auditorName || '-'
      const result = record.resultName || this.resultText(record.result)
      const time = record.auditTime ? ' · ' + this.formatTime(record.auditTime) : ''
      return name + ' · ' + result + time
    },
    stepOpinion(step) {
      const record = this.recordOf(step)
      if (!record || !record.opinion) {
        return ''
      }
      return record.opinion
    },
    stepText(step) {
      if (String(step) === '1') {
        return this.$t('contractAuditFlow.legal')
      }
      if (String(step) === '2') {
        return this.$t('contractAuditFlow.leader')
      }
      return '-'
    },
    resultText(result) {
      if (String(result) === AUDIT_RESULT.PASS) {
        return this.$t('contractAuditFlow.resultPass')
      }
      if (String(result) === AUDIT_RESULT.REJECT) {
        return this.$t('contractAuditFlow.resultReject')
      }
      return '-'
    },
    resultClass(result) {
      if (String(result) === AUDIT_RESULT.PASS) {
        return 'is-pass'
      }
      if (String(result) === AUDIT_RESULT.REJECT) {
        return 'is-reject'
      }
      return ''
    },
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
.cc-flow-records {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #dcdfe6;
}

.cc-flow-records-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.cc-flow-record {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  line-height: 20px;
  color: #606266;
}

.cc-flow-record-step {
  font-weight: 600;
}

.cc-flow-record-result {
  &.is-pass {
    color: #67c23a;
  }

  &.is-reject {
    color: #f56c6c;
  }
}

.cc-flow-record-time {
  color: #909399;
  font-variant-numeric: tabular-nums;
}

.cc-flow-record-opinion {
  flex: 0 0 100%;
  color: #909399;
}
</style>
