<template>
  <el-dialog :visible.sync="visible" :title="$t('roomFeeCancel.title')" width="600px" @close="handleClose">
    <div class="cancel-target">
      <div class="target-line">
        <span class="target-label">{{ $t('roomFeeCancel.targetRule') }}</span>
        <span class="target-value">{{ _ruleName() }}</span>
      </div>
      <div class="target-line">
        <span class="target-label">{{ $t('roomFeeCancel.targetBill') }}</span>
        <span class="target-value">{{ _billName() }}</span>
      </div>
    </div>

    <el-radio-group v-model="form.mode" class="mode-group">
      <el-radio label="bill" class="mode-item">
        <span class="mode-name">{{ $t('roomFeeCancel.modeBill') }}</span>
        <span class="mode-desc">{{ $t('roomFeeCancel.descBill') }}</span>
      </el-radio>
      <el-radio label="rule" class="mode-item">
        <span class="mode-name">{{ $t('roomFeeCancel.modeRule') }}</span>
        <span class="mode-desc">{{ $t('roomFeeCancel.descRule') }}</span>
      </el-radio>
      <el-radio label="both" class="mode-item">
        <span class="mode-name">{{ $t('roomFeeCancel.modeBoth') }}</span>
        <span class="mode-desc">{{ $t('roomFeeCancel.descBoth') }}</span>
      </el-radio>
    </el-radio-group>

    <div class="preview-box">
      <div class="preview-title">{{ $t('roomFeeCancel.previewTitle') }}</div>
      <div class="preview-text">{{ _previewText() }}</div>
    </div>

    <el-form label-width="90px" class="remark-form">
      <el-form-item :label="$t('roomFeeCancel.reason')">
        <el-input v-model="form.reason" type="textarea" :rows="2" maxlength="200" show-word-limit
          :placeholder="$t('roomFeeCancel.reasonPlaceholder')"></el-input>
      </el-form-item>
    </el-form>

    <el-alert v-if="errorMsg" :title="errorMsg" :description="$t('roomFeeCancel.failTip')" type="error" show-icon
      :closable="false"></el-alert>

    <span slot="footer" class="dialog-footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="danger" :loading="loading" @click="handleSubmit">{{ _confirmText() }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { updatePayFeeRule } from '@/api/fee/finishFeeRuleApi'
import { deleteFee } from '@/api/fee/deleteFeeApi'
import { getCommunityId } from '@/api/community/communityApi'

export default {
  name: 'SimplifyRoomFeeCancelDialog',
  data() {
    return {
      visible: false,
      loading: false,
      errorMsg: '',
      rule: {},
      bill: {},
      form: {
        mode: 'both',
        reason: ''
      }
    }
  },
  methods: {
    open(data) {
      const params = data || {}
      this.rule = params.rule || {}
      this.bill = params.bill || {}
      this.errorMsg = ''
      this.form = {
        mode: 'both',
        reason: ''
      }
      this.visible = true
    },
    handleClose() {
      this.visible = false
      this.loading = false
      this.errorMsg = ''
    },
    _ruleName() {
      return this.rule.feeName || '—'
    },
    _billName() {
      const parts = []
      if (this.bill.feeName) {
        parts.push(this.bill.feeName)
      }
      if (this.bill.yearMonth) {
        parts.push(this.bill.yearMonth)
      }
      const period = this._billPeriod()
      if (period) {
        parts.push(period)
      }
      return parts.length > 0 ? parts.join(' · ') : '—'
    },
    _billPeriod() {
      if (!this.bill.startTime && !this.bill.endTime) {
        return ''
      }
      const start = this.bill.startTime ? String(this.bill.startTime).substring(0, 10) : '—'
      const end = this.bill.endTime ? String(this.bill.endTime).substring(0, 10) : '—'
      return start + ' ~ ' + end
    },
    _previewText() {
      const ruleName = this._ruleName()
      const billName = this._billName()
      if (this.form.mode === 'bill') {
        return this.$t('roomFeeCancel.previewBill', { bill: billName })
      }
      if (this.form.mode === 'rule') {
        return this.$t('roomFeeCancel.previewRule', { rule: ruleName })
      }
      return this.$t('roomFeeCancel.previewBoth', { rule: ruleName, bill: billName })
    },
    _confirmText() {
      if (this.form.mode === 'bill') {
        return this.$t('roomFeeCancel.confirmBill')
      }
      if (this.form.mode === 'rule') {
        return this.$t('roomFeeCancel.confirmRule')
      }
      return this.$t('roomFeeCancel.confirmBoth')
    },
    _successText() {
      if (this.form.mode === 'bill') {
        return this.$t('roomFeeCancel.successBill')
      }
      if (this.form.mode === 'rule') {
        return this.$t('roomFeeCancel.successRule')
      }
      return this.$t('roomFeeCancel.successBoth')
    },
    handleSubmit() {
      if (this.form.mode === 'rule' && !this.rule.ruleId) {
        this.errorMsg = this.$t('roomFeeCancel.noRule')
        return
      }
      if ((this.form.mode === 'bill' || this.form.mode === 'both') && !this.bill.feeId) {
        this.errorMsg = this.$t('roomFeeCancel.noBill')
        return
      }
      this.loading = true
      this.errorMsg = ''
      this._doSubmit().then(() => {
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    async _doSubmit() {
      const communityId = getCommunityId()
      try {
        if (this.form.mode === 'rule' || this.form.mode === 'both') {
          await updatePayFeeRule({
            ruleId: this.rule.ruleId,
            state: '2009001',
            communityId: communityId
          })
        }
        if (this.form.mode === 'bill' || this.form.mode === 'both') {
          await deleteFee({
            feeId: this.bill.feeId,
            communityId: communityId
          })
        }
        this.$message.success(this._successText())
        this.$emit('success')
        this.handleClose()
      } catch (error) {
        this.errorMsg = error.message || this.$t('common.operateFailed')
      }
    }
  }
}
</script>

<style scoped>
.cancel-target {
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #f8fafc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.target-line {
  display: flex;
  align-items: baseline;
  font-size: 12px;
  line-height: 20px;
}

.target-label {
  width: 84px;
  color: #909399;
}

.target-value {
  flex: 1;
  color: #303133;
}

.mode-group {
  display: block;
}

.mode-item {
  display: flex;
  align-items: flex-start;
  height: auto;
  margin-bottom: 10px;
  white-space: normal;
}

.mode-name {
  display: block;
  font-size: 13px;
  color: #303133;
}

.mode-desc {
  display: block;
  font-size: 12px;
  line-height: 18px;
  color: #909399;
}

.preview-box {
  padding: 10px 12px;
  margin-bottom: 12px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 4px;
}

.preview-title {
  font-size: 12px;
  color: #b88230;
}

.preview-text {
  margin-top: 4px;
  font-size: 12px;
  line-height: 18px;
  color: #606266;
}

.remark-form {
  margin-bottom: 0;
}
</style>
