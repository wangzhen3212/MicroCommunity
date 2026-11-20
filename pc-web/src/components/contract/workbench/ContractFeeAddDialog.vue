<template>
  <el-dialog
    :title="$t('contractWorkbench.plan.addFee')"
    :visible.sync="visible"
    width="560px"
    @close="handleClose"
  >
    <el-form ref="form" :model="form" :rules="rules" label-width="130px">
      <el-form-item :label="$t('contractWorkbench.plan.feeType')" prop="feeTypeCd">
        <el-select
          v-model="form.feeTypeCd"
          :placeholder="$t('contractWorkbench.plan.selectFeeType')"
          style="width: 100%"
          @change="handleFeeTypeChange"
        >
          <el-option v-for="item in feeTypes" :key="item.statusCd" :label="item.name" :value="item.statusCd" />
        </el-select>
      </el-form-item>

      <el-form-item :label="$t('contractWorkbench.plan.feeItem')" prop="configId">
        <el-select
          v-model="form.configId"
          :placeholder="$t('contractWorkbench.plan.selectFeeItem')"
          :no-data-text="$t('contractWorkbench.plan.feeItemEmpty')"
          style="width: 100%"
          @change="handleConfigChange"
        >
          <el-option v-for="item in feeConfigs" :key="item.configId" :label="item.feeName" :value="item.configId" />
        </el-select>
      </el-form-item>

      <el-form-item v-if="showUnitPrice" :label="unitPriceLabel">
        <span class="cc-fee-readonly">{{ form.unitPrice || '0.00' }}</span>
        <span class="cc-fee-dialog-tip">{{ $t('contractWorkbench.plan.unitPriceTip') }}</span>
      </el-form-item>

      <el-form-item v-if="showAdditional" :label="$t('contractWorkbench.plan.additional')">
        <span class="cc-fee-readonly">{{ form.additionalAmount || '0.00' }}</span>
        <span class="cc-fee-dialog-tip">{{ $t('contractWorkbench.plan.unitPriceTip') }}</span>
      </el-form-item>

      <el-form-item v-if="showCycle" :label="$t('contractWorkbench.plan.cycle')">
        <span class="cc-fee-readonly">{{ form.cycle || '-' }}</span>
        <span v-if="form.months" class="cc-fee-dialog-tip">
          {{ $t('contractWorkbench.plan.monthsTip', { months: form.months }) }}
        </span>
      </el-form-item>

      <el-form-item v-if="isManual" :label="$t('contractWorkbench.field.amount')">
        <el-input v-model="form.manualAmount" />
      </el-form-item>

      <el-form-item :label="$t('contractWorkbench.plan.includeInAmount')">
        <el-switch v-model="form.includeInAmount" />
        <span class="cc-fee-dialog-tip">{{ $t('contractWorkbench.plan.excludeTip') }}</span>
      </el-form-item>

      <el-form-item :label="$t('contractWorkbench.plan.estimate')">
        <span v-loading="computing" class="cc-fee-estimate">{{ formatAmount(form.amount) }}</span>
        <span v-if="computing" class="cc-fee-dialog-tip">{{ $t('contractWorkbench.msg.feeComputing') }}</span>
      </el-form-item>

      <el-form-item v-if="form.computeRemark" :label="$t('contractWorkbench.plan.remarkTitle')">
        <span class="cc-fee-remark">{{ form.computeRemark }}</span>
      </el-form-item>

      <el-form-item :label="$t('contractWorkbench.field.remark')">
        <el-input v-model="form.remark" maxlength="100" />
      </el-form-item>
    </el-form>

    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="computing" @click="handleSubmit">{{ $t('common.confirm') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { computeContractFeeAmount, listFeeConfigsByType, listFeeTypes } from '@/api/contract/contractFeeCompositionApi'
import { formatAmount, termMonths } from '@/utils/contractUtil'
import {
  FEE_FORMULA,
  applyManualAmount,
  applyServerAmount,
  buildComputeParams,
  defaultIncludeInAmount,
  isAdditionalFormula,
  isManualFormula,
  isUnitPriceFormula
} from '@/utils/contractFeeUtil'

export default {
  name: 'ContractFeeAddDialog',
  props: {
    rooms: {
      type: Array,
      default: function () {
        return []
      }
    },
    startTime: {
      type: String,
      default: ''
    },
    endTime: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      visible: false,
      computing: false,
      computeError: '',
      feeTypes: [],
      feeConfigs: [],
      form: this.createEmptyForm(),
      rules: {
        feeTypeCd: [
          {
            required: true,
            message: this.$t('contractWorkbench.plan.selectFeeType'),
            trigger: 'change'
          }
        ],
        configId: [
          {
            required: true,
            message: this.$t('contractWorkbench.plan.selectFeeItem'),
            trigger: 'change'
          }
        ]
      }
    }
  },
  computed: {
    formula() {
      return String(this.form.computingFormula || '')
    },
    isManual() {
      return isManualFormula(this.formula)
    },
    unitPriceLabel() {
      return this.formula === FEE_FORMULA.FIXED
        ? this.$t('contractWorkbench.field.amount')
        : this.$t('contractWorkbench.plan.unitPrice')
    },
    showUnitPrice() {
      return isUnitPriceFormula(this.formula)
    },
    showAdditional() {
      return isAdditionalFormula(this.formula)
    },
    showCycle() {
      return this.formula === FEE_FORMULA.RENT || this.formula === FEE_FORMULA.RENT_STEP
    }
  },
  methods: {
    formatAmount,
    createEmptyForm() {
      return {
        feeTypeCd: '',
        feeTypeName: '',
        configId: '',
        feeName: '',
        computingFormula: '',
        computingFormulaName: '',
        unitPrice: '',
        additionalAmount: '0.00',
        cycle: '1',
        months: 0,
        baseAmount: '0.00',
        amount: '0.00',
        computeRemark: '',
        manualAmount: '',
        includeInAmount: true,
        remark: ''
      }
    },
    open() {
      if (!this.rooms.length) {
        this.$message.warning(this.$t('contractWorkbench.msg.roomFirst'))
        return
      }
      this.visible = true
      this.computing = false
      this.computeError = ''
      this.form = this.createEmptyForm()
      this.feeConfigs = []
      this.loadFeeTypes()
    },
    handleClose() {
      this.visible = false
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
    },
    months() {
      return termMonths(this.startTime, this.endTime)
    },
    buildParams() {
      const params = buildComputeParams(this.rooms, this.months(), this.startTime, this.endTime)
      if (!params) {
        return null
      }
      params.configId = this.form.configId
      return params
    },
    loadFeeTypes() {
      listFeeTypes().then(data => {
        this.feeTypes = data || []
      }).catch(error => {
        this.feeTypes = []
        this.$message.error(error.message)
      })
    },
    handleFeeTypeChange(feeTypeCd) {
      this.form.feeTypeCd = feeTypeCd
      this.form.configId = ''
      this.form.feeName = ''
      this.form.computingFormula = ''
      this.form.computingFormulaName = ''
      this.form.includeInAmount = defaultIncludeInAmount(feeTypeCd)
      this.form.amount = '0.00'
      this.form.baseAmount = '0.00'
      this.form.computeRemark = ''
      this.computeError = ''
      this.feeConfigs = []
      const matched = this.feeTypes.filter(item => item.statusCd === feeTypeCd)[0]
      this.form.feeTypeName = matched ? matched.name : ''
      listFeeConfigsByType({ feeTypeCd: feeTypeCd }).then(list => {
        this.feeConfigs = list || []
      }).catch(error => {
        this.feeConfigs = []
        this.$message.error(error.message)
      })
    },
    handleConfigChange(configId) {
      const config = this.feeConfigs.filter(item => item.configId === configId)[0]
      if (!config) {
        return
      }
      this.form.configId = config.configId
      this.form.feeName = config.feeName
      this.form.computingFormula = config.computingFormula
      this.form.computingFormulaName = config.computingFormulaName || config.computingFormula
      this.form.unitPrice = config.squarePrice || '0.00'
      this.form.additionalAmount = config.additionalAmount || '0.00'
      this.form.cycle = config.paymentCycle || '1'
      this.form.manualAmount = ''
      this.form.amount = '0.00'
      this.form.baseAmount = '0.00'
      this.form.computeRemark = ''
      this.computeError = ''
      if (this.isManual) {
        return
      }
      this.computeAmount()
    },
    computeAmount() {
      if (this.isManual) {
        return
      }
      const params = this.buildParams()
      if (!params) {
        this.computeError = this.$t('contractWorkbench.msg.roomFirst')
        this.$message.warning(this.computeError)
        return
      }
      if (params.months <= 0) {
        this.computeError = this.$t('contractWorkbench.msg.termEmpty')
        this.$message.warning(this.computeError)
        return
      }
      this.computing = true
      computeContractFeeAmount(params).then(res => {
        applyServerAmount(this.form, res)
        this.computeError = ''
      }).catch(error => {
        this.form.amount = '0.00'
        this.form.baseAmount = '0.00'
        this.form.computeRemark = ''
        this.computeError = error.message || this.$t('contractWorkbench.msg.feeComputeError')
        this.$message.warning(this.computeError)
      }).finally(() => {
        this.computing = false
      })
    },
    trimManualAmount() {
      const value = this.form.manualAmount
      if (value === null || value === undefined) {
        return ''
      }
      return String(value).trim()
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        let manualAmount = null
        if (this.isManual) {
          manualAmount = this.trimManualAmount()
          if (!manualAmount) {
            this.$message.warning(this.$t('contractWorkbench.msg.feeAmountEmpty'))
            return
          }
          this.form.manualAmount = manualAmount
          applyManualAmount(this.form)
        } else if (!(parseFloat(this.form.amount) > 0)) {
          this.$message.warning(this.computeError || this.$t('contractWorkbench.msg.feeComputeError'))
          return
        }
        this.$emit('confirm', {
          configId: this.form.configId,
          feeTypeCd: this.form.feeTypeCd,
          feeTypeName: this.form.feeTypeName,
          feeName: this.form.feeName,
          computingFormula: this.form.computingFormula,
          computingFormulaName: this.form.computingFormulaName,
          unitPrice: this.form.unitPrice,
          additionalAmount: this.form.additionalAmount,
          cycle: this.form.cycle,
          months: this.form.months,
          coefficient: '1',
          manualAmount: manualAmount,
          baseAmount: this.form.baseAmount,
          amount: this.form.amount,
          computeRemark: this.form.computeRemark,
          includeInAmount: this.form.includeInAmount,
          remark: this.form.remark
        })
        this.handleClose()
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-fee-dialog-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.cc-fee-readonly {
  font-size: 13px;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.cc-fee-estimate {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}

.cc-fee-remark {
  display: inline-block;
  font-size: 12px;
  line-height: 18px;
  color: #606266;
  white-space: pre-line;
}
</style>
