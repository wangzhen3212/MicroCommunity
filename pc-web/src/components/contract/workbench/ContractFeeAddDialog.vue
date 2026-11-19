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
        <el-input v-model="form.unitPrice" />
      </el-form-item>

      <el-form-item v-if="showAdditional" :label="$t('contractWorkbench.plan.additional')">
        <el-input v-model="form.additionalAmount" />
      </el-form-item>

      <el-form-item v-if="showCycle" :label="$t('contractWorkbench.plan.cycle')">
        <el-input v-model="form.cycle" />
      </el-form-item>

      <el-form-item v-if="isManual" :label="$t('contractWorkbench.field.amount')">
        <el-input v-model="form.manualAmount" />
      </el-form-item>

      <el-form-item :label="$t('contractWorkbench.plan.includeInAmount')">
        <el-switch v-model="form.includeInAmount" />
        <span class="cc-fee-dialog-tip">{{ $t('contractWorkbench.plan.excludeTip') }}</span>
      </el-form-item>

      <el-form-item :label="$t('contractWorkbench.plan.estimate')">
        <span class="cc-fee-estimate">{{ formatAmount(estimate) }}</span>
      </el-form-item>

      <el-form-item :label="$t('contractWorkbench.field.remark')">
        <el-input v-model="form.remark" maxlength="100" />
      </el-form-item>
    </el-form>

    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" @click="handleSubmit">{{ $t('common.confirm') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { listFeeConfigsByType, listFeeTypes } from '@/api/contract/contractFeeCompositionApi'
import { formatAmount } from '@/utils/contractUtil'
import {
  FEE_FORMULA,
  calcFeeAmount,
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
    },
    estimate() {
      return calcFeeAmount(this.form, this.feeContext())
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
        manualAmount: '',
        includeInAmount: true,
        remark: ''
      }
    },
    open() {
      this.visible = true
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
    feeContext() {
      return {
        rooms: this.rooms,
        startTime: this.startTime,
        endTime: this.endTime
      }
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
      this.form.unitPrice = config.squarePrice || ''
      this.form.additionalAmount = config.additionalAmount || '0.00'
      this.form.cycle = config.paymentCycle || '1'
      this.form.manualAmount = ''
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) {
          return
        }
        if (this.isManual && !this.form.manualAmount) {
          this.$message.warning(this.$t('contractWorkbench.msg.feeAmountEmpty'))
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
          coefficient: '1',
          manualAmount: this.form.manualAmount,
          baseAmount: '0.00',
          amount: '0.00',
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

.cc-fee-estimate {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
}
</style>
