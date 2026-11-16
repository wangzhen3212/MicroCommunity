<template>
  <div class="cc-flow">
    <div class="cc-flow-step" :class="submitterClass">
      <span class="cc-flow-dot"></span>
      <div class="cc-flow-name">{{ $t('contractAuditFlow.submitter') }}</div>
      <div v-if="audit" class="cc-flow-meta">
        {{ audit.submitStaffName || '-' }} · {{ formatTime(audit.submitTime) }}
      </div>
      <div v-else class="cc-flow-meta">{{ $t('contractAuditFlow.noStart') }}</div>
    </div>
    <div class="cc-flow-step" :class="stepClass('1')">
      <span class="cc-flow-dot"></span>
      <div class="cc-flow-name">
        {{ $t('contractAuditFlow.legal') }}
        <span v-if="isCurrent('1')" class="cc-badge is-primary">{{ $t('contractAuditFlow.current') }}</span>
      </div>
      <div class="cc-flow-meta">
        {{ stepMeta('1') }}
      </div>
      <div v-if="stepOpinion('1')" class="cc-flow-meta">{{ $t('contractAuditFlow.opinion') }}：{{ stepOpinion('1') }}</div>
    </div>
    <div class="cc-flow-step" :class="stepClass('2')">
      <span class="cc-flow-dot"></span>
      <div class="cc-flow-name">
        {{ $t('contractAuditFlow.leader') }}
        <span v-if="isCurrent('2')" class="cc-badge is-primary">{{ $t('contractAuditFlow.current') }}</span>
      </div>
      <div class="cc-flow-meta">
        {{ stepMeta('2') }}
      </div>
      <div v-if="stepOpinion('2')" class="cc-flow-meta">{{ $t('contractAuditFlow.opinion') }}：{{ stepOpinion('2') }}</div>
    </div>
    <div class="cc-flow-step" :class="effectiveClass">
      <span class="cc-flow-dot"></span>
      <div class="cc-flow-name">{{ $t('contractAuditFlow.effective') }}</div>
      <div class="cc-flow-meta">{{ effectiveMeta }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ContractAuditFlow',
  props: {
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
    submitterClass() {
      if (!this.audit) {
        return ''
      }
      return 'is-pass'
    },
    effectiveClass() {
      if (!this.audit) {
        return ''
      }
      if (String(this.audit.state) === '1') {
        return 'is-pass'
      }
      if (String(this.audit.state) === '2') {
        return 'is-reject'
      }
      return ''
    },
    effectiveMeta() {
      if (!this.audit) {
        return '-'
      }
      const state = String(this.audit.state)
      if (state === '1') {
        return this.$t('contractAuditCenter.statePass') + ' · ' + this.formatTime(this.audit.finishTime)
      }
      if (state === '2') {
        return this.$t('contractAuditCenter.stateReject') + ' · ' + this.formatTime(this.audit.finishTime)
      }
      if (state === '3') {
        return this.$t('contractAuditCenter.stateCancel')
      }
      return this.$t('contractAuditFlow.waiting')
    }
  },
  methods: {
    recordOf(step) {
      for (let index = 0; index < this.records.length; index++) {
        if (String(this.records[index].step) === step) {
          return this.records[index]
        }
      }
      return null
    },
    isCurrent(step) {
      if (!this.audit) {
        return false
      }
      return String(this.audit.state) === '0' && String(this.audit.currentStep) === step
    },
    stepClass(step) {
      const record = this.recordOf(step)
      if (this.isCurrent(step)) {
        return 'is-current'
      }
      if (record) {
        if (String(record.result) === '1') {
          return 'is-pass'
        }
        if (String(record.result) === '2') {
          return 'is-reject'
        }
      }
      return ''
    },
    stepMeta(step) {
      const record = this.recordOf(step)
      if (!record) {
        return this.$t('contractAuditFlow.waiting')
      }
      const resultKey = String(record.result) === '1'
        ? 'contractAuditCenter.resultPass'
        : (String(record.result) === '2' ? 'contractAuditCenter.resultReject' : 'contractAuditCenter.resultDoing')
      const name = record.auditorName || '-'
      const time = record.auditTime ? ' · ' + this.formatTime(record.auditTime) : ''
      return name + ' · ' + this.$t(resultKey) + time
    },
    stepOpinion(step) {
      const record = this.recordOf(step)
      if (!record || !record.opinion) {
        return ''
      }
      return record.opinion
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
