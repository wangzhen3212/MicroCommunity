<template>
  <el-dialog :visible.sync="visible" width="620px" :close-on-click-modal="false" @close="handleClose">
    <div slot="title" class="dlg-head">
      <div class="dlg-title">{{ $t('roomFeeCancel.title') }} · {{ feeName }}</div>
      <div class="dlg-sub">{{ $t('roomFeeCancel.subtitle') }}</div>
    </div>

    <div v-if="hasRule" class="rule-info">
      {{ $t('roomFeeCancel.ruleInfo', { feeName: feeName }) }}
      <span v-if="ruleDetail">（{{ ruleDetail }}）</span>
    </div>

    <div class="mode-list">
      <div class="mode-item" :class="{ on: mode === 'bill', disabled: !canCancelBill }" @click="selectMode('bill')">
        <el-radio v-model="mode" label="bill" :disabled="!canCancelBill"></el-radio>
        <div class="mode-body">
          <div class="mode-head">
            <span class="mode-name">{{ $t('roomFeeCancel.modeBill') }}</span>
            <el-tag size="mini" type="danger">{{ $t('roomFeeCancel.modeBillTag') }}</el-tag>
          </div>
          <div class="mode-desc">{{ $t('roomFeeCancel.modeBillDesc', { bill: billText }) }}</div>
        </div>
      </div>

      <div class="mode-item" :class="{ on: mode === 'rule', disabled: !canFinishRule }" @click="selectMode('rule')">
        <el-radio v-model="mode" label="rule" :disabled="!canFinishRule"></el-radio>
        <div class="mode-body">
          <div class="mode-head">
            <span class="mode-name">{{ $t('roomFeeCancel.modeRule') }}</span>
          </div>
          <div class="mode-desc">{{ $t('roomFeeCancel.modeRuleDesc', { feeName: feeName }) }}</div>
        </div>
      </div>

      <div class="mode-item" :class="{ on: mode === 'both', disabled: !canBoth }" @click="selectMode('both')">
        <el-radio v-model="mode" label="both" :disabled="!canBoth"></el-radio>
        <div class="mode-body">
          <div class="mode-head">
            <span class="mode-name">{{ $t('roomFeeCancel.modeBoth') }}</span>
            <el-tag size="mini" type="primary">{{ $t('roomFeeCancel.modeBothTag') }}</el-tag>
          </div>
          <div class="mode-desc">{{ $t('roomFeeCancel.modeBothDesc') }}</div>
        </div>
      </div>
    </div>

    <div class="reason-box">
      <div class="reason-label">{{ $t('roomFeeCancel.reason') }}</div>
      <el-select v-model="reason" size="small" style="width: 100%">
        <el-option v-for="item in reasonOptions" :key="item" :label="$t('roomFeeCancel.reason_' + item)" :value="item">
        </el-option>
      </el-select>
    </div>

    <div class="preview-box">
      <span>{{ $t('roomFeeCancel.preview') }}</span>
      <b>{{ previewText }}</b>
    </div>
    <div class="hint-box">{{ $t('roomFeeCancel.payRecordTip') }}</div>

    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="danger" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
        {{ confirmText }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { deleteFee } from '@/api/fee/deleteFeeApi'
import { updatePayFeeRule } from '@/api/fee/finishFeeRuleApi'
import { getCommunityId } from '@/api/community/communityApi'

export default {
  name: 'RoomFeeCancelDialog',
  data() {
    return {
      visible: false,
      submitting: false,
      rule: null,
      bill: null,
      mode: 'both',
      reason: 'sold',
      reasonOptions: ['sold', 'vacant', 'wrong', 'duplicate', 'other']
    }
  },
  computed: {
    hasRule() {
      return !!(this.rule && !this.rule.syntheticOther && this.rule.ruleId)
    },
    feeName() {
      if (!this.rule) {
        return ''
      }
      return this.rule.feeName || this.$t('roomFeeRuleBill.syntheticName')
    },
    ruleDetail() {
      if (!this.hasRule) {
        return ''
      }
      const parts = []
      if (this.rule.feeFlagName) {
        parts.push(this.rule.feeFlagName)
      }
      if (this.rule.squarePrice) {
        parts.push(this.rule.squarePrice + ' 元')
      }
      const maxTime = this.rule.maxTime ? String(this.rule.maxTime).substring(0, 10) : ''
      if (maxTime) {
        parts.push(this.$t('roomFeeCancel.ruleValidTo', { maxTime: maxTime }))
      }
      return parts.join(' · ')
    },
    canCancelBill() {
      return !!(this.bill && this.bill.feeId && this.bill.billState === 'unpaid')
    },
    canFinishRule() {
      return this.hasRule && this.rule.state === '2008001'
    },
    canBoth() {
      return this.canCancelBill && this.canFinishRule
    },
    canSubmit() {
      if (this.mode === 'bill') {
        return this.canCancelBill
      }
      if (this.mode === 'rule') {
        return this.canFinishRule
      }
      return this.canBoth
    },
    billText() {
      if (!this.bill) {
        return this.$t('roomFeeCancel.noBill')
      }
      const amount = parseFloat(this.bill.receivableAmount)
      return this.$t('roomFeeCancel.periodAmount', {
        yearMonth: this.bill.yearMonth || '-',
        amount: isNaN(amount) ? '0.00' : amount.toFixed(2)
      })
    },
    previewText() {
      const parts = []
      if (this.mode === 'bill') {
        parts.push(this.bill ? this.$t('roomFeeCancel.previewBill', { bill: this.billText }) : this.$t('roomFeeCancel.previewNoBill'))
        parts.push(this.$t('roomFeeCancel.previewKeepRule', { feeName: this.feeName }))
      } else if (this.mode === 'rule') {
        parts.push(this.$t('roomFeeCancel.previewRule', { feeName: this.feeName }))
        if (this.bill) {
          parts.push(this.$t('roomFeeCancel.previewKeepBill', { bill: this.billText }))
        }
      } else {
        if (this.bill) {
          parts.push(this.$t('roomFeeCancel.previewBill', { bill: this.billText }))
        }
        parts.push(this.$t('roomFeeCancel.previewRule', { feeName: this.feeName }))
      }
      parts.push(this.$t('roomFeeCancel.previewReason', { reason: this.$t('roomFeeCancel.reason_' + this.reason) }))
      return parts.join(' · ')
    },
    confirmText() {
      return this.mode === 'rule' ? this.$t('roomFeeCancel.confirmRule') : this.$t('roomFeeCancel.confirmCancel')
    },
    successText() {
      if (this.mode === 'bill') {
        return this.$t('roomFeeCancel.successBill')
      }
      if (this.mode === 'rule') {
        return this.$t('roomFeeCancel.successRule')
      }
      return this.$t('roomFeeCancel.successBoth')
    }
  },
  methods: {
    open(params) {
      this.rule = params.rule || null
      this.bill = params.bill || null
      this.reason = 'sold'
      this.mode = this.defaultMode(params.mode)
      this.visible = true
    },
    defaultMode(mode) {
      if (mode === 'bill') {
        return this.canCancelBill ? 'bill' : (this.canFinishRule ? 'rule' : 'both')
      }
      if (mode === 'rule') {
        return this.canFinishRule ? 'rule' : (this.canCancelBill ? 'bill' : 'both')
      }
      if (this.canBoth) {
        return 'both'
      }
      if (this.canCancelBill) {
        return 'bill'
      }
      return this.canFinishRule ? 'rule' : 'both'
    },
    selectMode(mode) {
      if (mode === 'bill' && !this.canCancelBill) {
        return
      }
      if (mode === 'rule' && !this.canFinishRule) {
        return
      }
      if (mode === 'both' && !this.canBoth) {
        return
      }
      this.mode = mode
    },
    handleClose() {
      this.visible = false
      this.submitting = false
      this.rule = null
      this.bill = null
    },
    async handleSubmit() {
      if (!this.canSubmit) {
        this.$message.warning(this.$t('roomFeeCancel.noBill'))
        return
      }
      this.submitting = true
      let applied = false
      try {
        if (this.mode === 'rule' || this.mode === 'both') {
          await updatePayFeeRule({
            ruleId: this.rule.ruleId,
            state: '2009001',
            communityId: getCommunityId()
          })
          applied = true
        }
        if (this.mode === 'bill' || this.mode === 'both') {
          await deleteFee({
            feeId: this.bill.feeId,
            communityId: getCommunityId()
          })
          applied = true
        }
        this.$message.success(this.successText)
      } catch (error) {
        this.$message.error(error.message || this.$t('common.operateFailed'))
      } finally {
        this.submitting = false
        if (applied) {
          this.$emit('success')
          this.handleClose()
        }
      }
    }
  }
}
</script>

<style scoped>
.dlg-head {
  padding-right: 20px;
}

.dlg-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.dlg-sub {
  margin-top: 4px;
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.rule-info {
  padding: 8px 12px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #606266;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.mode-list {
  display: flex;
  flex-direction: column;
}

.mode-item {
  display: flex;
  align-items: flex-start;
  padding: 10px 12px;
  margin-bottom: 8px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  cursor: pointer;
}

.mode-item.on {
  background: #ecf5ff;
  border-color: #409eff;
}

.mode-item.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.mode-body {
  flex: 1;
  margin-left: 6px;
}

.mode-head {
  display: flex;
  align-items: center;
}

.mode-name {
  margin-right: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.mode-desc {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.5;
  color: #909399;
}

.reason-box {
  margin-bottom: 12px;
}

.reason-label {
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}

.preview-box {
  padding: 8px 12px;
  font-size: 12px;
  line-height: 1.6;
  color: #1f2d3d;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 4px;
}

.hint-box {
  margin-top: 8px;
  font-size: 12px;
  color: #e6a23c;
}
</style>
