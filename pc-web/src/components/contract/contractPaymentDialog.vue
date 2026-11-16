<template>
  <el-dialog
    :title="$t('contractPerformance.dialog.title')"
    :visible.sync="visible"
    width="640px"
    @close="handleClose"
  >
    <div class="cc-pay-contract">
      <span class="cc-pay-code">{{ contract.contractCode }}</span>
      <span>{{ contract.contractName }}</span>
    </div>

    <el-table
      :data="plans"
      size="mini"
      border
      highlight-current-row
      v-loading="loading"
      @current-change="handleCurrentChange"
    >
      <el-table-column :label="$t('contractPerformance.dialog.planName')">
        <template slot-scope="scope">{{ scope.row.planName || scope.row.planSeq }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractPerformance.dialog.planAmount')" align="center" width="130">
        <template slot-scope="scope">{{ formatAmount(scope.row.planAmount) }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.plan.planTime')" align="center" width="130">
        <template slot-scope="scope">{{ formatTime(scope.row.planTime) }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractDetailDrawer.payState')" align="center" width="100">
        <template slot-scope="scope">
          <span class="cc-badge" :class="payStateClass(scope.row.payState)">{{ payStateText(scope.row.payState) }}</span>
        </template>
      </el-table-column>
    </el-table>

    <el-form :model="form" label-width="90px" class="cc-pay-form" v-if="currentPlan">
      <el-form-item :label="$t('contractPerformance.dialog.payAmount')" required>
        <el-input v-model="form.payAmount" />
      </el-form-item>
      <el-form-item :label="$t('contractPerformance.dialog.payTime')">
        <el-date-picker
          v-model="form.payTime"
          type="date"
          value-format="yyyy-MM-dd HH:mm:ss"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item :label="$t('contractPerformance.dialog.remark')">
        <el-input type="textarea" :rows="2" maxlength="100" v-model="form.remark" />
      </el-form-item>
    </el-form>

    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button
        v-if="currentPlan && String(currentPlan.payState) === '1'"
        type="warning"
        :loading="saving"
        @click="handleCancelPay"
      >{{ $t('contractPerformance.dialog.cancelPay') }}</el-button>
      <el-button type="primary" :loading="saving" :disabled="!currentPlan" @click="handleSubmit">
        {{ $t('contractPerformance.dialog.submit') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { listContractCollectionPlans, saveContractPayment } from '@/api/contract/contractPerformanceApi'
import { formatAmount, formatDate, parseDate, toNumber } from '@/utils/contractUtil'

export default {
  name: 'ContractPaymentDialog',
  data() {
    return {
      visible: false,
      loading: false,
      saving: false,
      contract: {},
      plans: [],
      currentPlan: null,
      form: {
        payAmount: '',
        payTime: '',
        remark: ''
      }
    }
  },
  methods: {
    formatAmount,
    open(contract) {
      this.contract = contract || {}
      this.plans = []
      this.currentPlan = null
      this.form.payAmount = ''
      this.form.payTime = formatDate(new Date())
      this.form.remark = ''
      this.visible = true
      this.loadPlans()
    },
    handleClose() {
      this.visible = false
    },
    loadPlans() {
      if (!this.contract.contractId) {
        return
      }
      this.loading = true
      listContractCollectionPlans({ contractId: this.contract.contractId }).then(data => {
        this.plans = data.plans || []
      }).catch(error => {
        this.plans = []
        this.$message.error(error.message)
      }).finally(() => {
        this.loading = false
      })
    },
    handleCurrentChange(row) {
      if (!row) {
        return
      }
      this.currentPlan = row
      this.form.payAmount = formatAmount(row.planAmount)
      this.form.payTime = row.payTime ? this.formatTime(row.payTime) : formatDate(new Date())
    },
    handleSubmit() {
      if (!this.currentPlan) {
        return
      }
      if (toNumber(this.form.payAmount) <= 0) {
        this.$message.error(this.$t('contractPerformance.dialog.required'))
        return
      }
      this.saving = true
      saveContractPayment({
        planId: this.currentPlan.planId,
        contractId: this.contract.contractId,
        payAmount: this.form.payAmount,
        payTime: this.form.payTime,
        remark: this.form.remark
      }).then(() => {
        this.$message.success(this.$t('contractPerformance.dialog.success'))
        this.$emit('success')
        this.loadPlans()
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractPerformance.dialog.fail'))
      }).finally(() => {
        this.saving = false
      })
    },
    handleCancelPay() {
      if (!this.currentPlan) {
        return
      }
      this.saving = true
      saveContractPayment({
        planId: this.currentPlan.planId,
        contractId: this.contract.contractId,
        cancel: '1',
        remark: this.form.remark
      }).then(() => {
        this.$message.success(this.$t('contractPerformance.dialog.cancelSuccess'))
        this.$emit('success')
        this.loadPlans()
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractPerformance.dialog.fail'))
      }).finally(() => {
        this.saving = false
      })
    },
    formatTime(time) {
      if (!time) {
        return '-'
      }
      const date = parseDate(time)
      if (!date) {
        return String(time)
      }
      return `${formatDate(date)} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}:00`
    },
    payStateText(payState) {
      const state = String(payState)
      if (state === '1') {
        return this.$t('contractDetailDrawer.payStatePaid')
      }
      if (state === '2') {
        return this.$t('contractDetailDrawer.payStateOverdue')
      }
      return this.$t('contractDetailDrawer.payStateDoing')
    },
    payStateClass(payState) {
      const state = String(payState)
      if (state === '1') {
        return 'is-success'
      }
      if (state === '2') {
        return 'is-danger'
      }
      return 'is-info'
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-pay-contract {
  margin-bottom: 12px;
  font-size: 13px;
  color: #303133;

  .cc-pay-code {
    margin-right: 10px;
    font-weight: 600;
  }
}

.cc-pay-form {
  margin-top: 16px;
}
</style>
