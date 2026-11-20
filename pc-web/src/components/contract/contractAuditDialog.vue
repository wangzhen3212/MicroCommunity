<template>
  <el-dialog
    :title="$t('contractPendingAudit.dialog.title')"
    :visible.sync="visible"
    width="560px"
    @close="handleClose"
  >
    <div class="cc-audit-head">
      <div class="cc-audit-name">{{ form.contractName || '-' }}</div>
      <div class="cc-audit-meta">
        <span>{{ form.contractCode || '-' }}</span>
        <span class="cc-audit-step">{{ $t('contractPendingAudit.dialog.step') }}：{{ stepText }}</span>
      </div>
    </div>

    <el-collapse v-model="activeHistory" class="cc-audit-history">
      <el-collapse-item :title="$t('contractPendingAudit.dialog.history')" name="history">
        <div v-if="records.length">
          <div v-for="(record, index) in records" :key="'record' + index" class="cc-audit-record">
            <span class="cc-audit-record-step">{{ record.stepName || '-' }}</span>
            <span>{{ record.auditorName || '-' }}</span>
            <span :class="resultClass(record.result)">{{ record.resultName || '-' }}</span>
            <span class="cc-audit-record-time">{{ formatTime(record.auditTime) }}</span>
            <span v-if="record.opinion" class="cc-audit-record-opinion">{{ record.opinion }}</span>
          </div>
        </div>
        <div v-else class="cc-audit-empty">{{ $t('contractPendingAudit.dialog.historyEmpty') }}</div>
      </el-collapse-item>
    </el-collapse>

    <el-form label-width="90px">
      <el-form-item :label="$t('contractPendingAudit.dialog.opinion')">
        <el-input
          type="textarea"
          :rows="4"
          maxlength="200"
          show-word-limit
          v-model="opinion"
          :placeholder="$t('contractPendingAudit.dialog.opinionPlaceholder')"
        />
      </el-form-item>
    </el-form>

    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="danger" :loading="loading" @click="handleSubmit(AUDIT_RESULT.REJECT)">
        {{ $t('contractPendingAudit.dialog.reject') }}
      </el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit(AUDIT_RESULT.PASS)">
        {{ $t('contractPendingAudit.dialog.pass') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { auditContract, queryContractAuditFlow } from '@/api/contract/contractAuditApi'
import { AUDIT_BIZ_TYPE, AUDIT_RESULT, CONTRACT_STATE } from '@/utils/contractUtil'

export default {
  name: 'ContractAuditDialog',
  data() {
    return {
      AUDIT_RESULT,
      visible: false,
      loading: false,
      activeHistory: [],
      opinion: '',
      records: [],
      form: {
        bizType: AUDIT_BIZ_TYPE.CONTRACT,
        bizId: '',
        state: '',
        contractCode: '',
        contractName: ''
      }
    }
  },
  computed: {
    stepText() {
      if (String(this.form.state) === CONTRACT_STATE.AUDIT) {
        return this.$t('contractPendingAudit.op.legalAudit')
      }
      if (String(this.form.state) === CONTRACT_STATE.AUDIT_LEADER) {
        return this.$t('contractPendingAudit.op.leaderAudit')
      }
      return '-'
    }
  },
  methods: {
    open(params) {
      const data = params || {}
      this.form = {
        bizType: String(data.bizType || AUDIT_BIZ_TYPE.CONTRACT),
        bizId: data.bizId || '',
        state: String(data.state || ''),
        contractCode: data.contractCode || '',
        contractName: data.contractName || ''
      }
      this.opinion = ''
      this.records = []
      this.activeHistory = []
      this.visible = true
      this.loadFlow()
    },
    loadFlow() {
      if (!this.form.bizId) {
        return
      }
      queryContractAuditFlow({
        bizType: this.form.bizType,
        bizId: this.form.bizId
      }).then(data => {
        this.records = data.records || []
        if (data.state) {
          this.form.state = String(data.state)
        }
      }).catch(() => {
        this.records = []
      })
    },
    handleClose() {
      this.visible = false
      this.opinion = ''
      this.loading = false
    },
    handleSubmit(result) {
      const opinion = (this.opinion || '').trim()
      if (result === AUDIT_RESULT.REJECT && !opinion) {
        this.$message.error(this.$t('contractPendingAudit.dialog.opinionRequired'))
        return
      }
      if (opinion.length > 200) {
        this.$message.error(this.$t('contractPendingAudit.dialog.opinionMax'))
        return
      }
      this.loading = true
      auditContract({
        bizType: this.form.bizType,
        bizId: this.form.bizId,
        result: result,
        opinion: opinion
      }).then(() => {
        this.$message.success(this.$t('contractPendingAudit.dialog.success'))
        this.loading = false
        this.$emit('success')
        this.handleClose()
      }).catch(error => {
        this.loading = false
        this.$message.error(error.message || this.$t('contractPendingAudit.dialog.error'))
      })
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
.cc-audit-head {
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.cc-audit-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.cc-audit-meta {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.cc-audit-history {
  margin: 12px 0;
}

.cc-audit-record {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  line-height: 20px;
  color: #606266;

  .is-pass {
    color: #67c23a;
  }

  .is-reject {
    color: #f56c6c;
  }
}

.cc-audit-record-step {
  font-weight: 600;
}

.cc-audit-record-time {
  color: #909399;
  font-variant-numeric: tabular-nums;
}

.cc-audit-record-opinion {
  flex: 0 0 100%;
  color: #909399;
}

.cc-audit-empty {
  font-size: 12px;
  color: #909399;
}
</style>
