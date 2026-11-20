<template>
  <div>
    <div v-if="isRent && hasRooms" class="cc-fee-panel">
      <div class="cc-fee-toolbar">
        <span class="cc-section-sub">{{ $t('contractWorkbench.plan.feeTitle') }}</span>
        <el-button size="mini" type="primary" @click="openFeeDialog">
          <i class="el-icon-plus"></i>{{ $t('contractWorkbench.plan.addFee') }}
        </el-button>
        <span class="cc-fee-total">
          {{ $t('contractWorkbench.plan.includeTotal') }}：<span>{{ formatAmount(includeTotal) }}</span>
        </span>
      </div>

      <el-table v-if="fees.length" :data="fees" size="mini" border>
        <el-table-column prop="feeTypeName" :label="$t('contractWorkbench.plan.feeType')" width="110" align="center" />
        <el-table-column prop="feeName" :label="$t('contractWorkbench.plan.feeItem')" min-width="120" />
        <el-table-column prop="computingFormulaName" :label="$t('contractWorkbench.plan.formula')" width="130" align="center" />
        <el-table-column :label="$t('contractWorkbench.plan.unitPrice')" width="110" align="center">
          <template slot-scope="scope">
            <span v-if="isUnitPriceRow(scope.row)">{{ formatAmount(scope.row.unitPrice) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('contractWorkbench.plan.additional')" width="100" align="center">
          <template slot-scope="scope">
            <span v-if="isAdditionalRow(scope.row)">{{ formatAmount(scope.row.additionalAmount) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('contractWorkbench.plan.baseAmount')" width="110" align="center">
          <template slot-scope="scope">{{ formatAmount(scope.row.baseAmount) }}</template>
        </el-table-column>
        <el-table-column :label="$t('contractWorkbench.field.amount')" width="130" align="center">
          <template slot-scope="scope">
            <el-input
              v-if="isManualRow(scope.row)"
              size="mini"
              v-model="scope.row.manualAmount"
              @input="handleManualInput(scope.row)"
            />
            <span v-else>{{ formatAmount(scope.row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('contractWorkbench.plan.includeInAmount')" width="110" align="center">
          <template slot-scope="scope">
            <el-switch v-model="scope.row.includeInAmount" @change="handleFeeInput" />
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.operation')" width="70" align="center">
          <template slot-scope="scope">
            <el-button type="text" @click="handleFeeRemove(scope.$index)">{{ $t('common.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-else class="cc-empty">{{ $t('contractWorkbench.plan.feeEmpty') }}</div>

      <div class="cc-fee-summary">
        <span>{{ $t('contractWorkbench.plan.total') }}：{{ formatAmount(feeTotal) }}</span>
        <span>{{ $t('contractWorkbench.plan.includeTotal') }}：{{ formatAmount(includeTotal) }}</span>
        <span class="cc-fee-tip">{{ $t('contractWorkbench.plan.excludeTip') }}</span>
      </div>

      <contract-fee-add-dialog
        ref="feeDialog"
        :rooms="rooms"
        :start-time="startTime"
        :end-time="endTime"
        @confirm="handleFeeConfirm"
      />
    </div>

    <el-alert
      v-else-if="isRent"
      type="info"
      :closable="false"
      show-icon
      :title="$t('contractWorkbench.msg.roomFirst')"
    />

    <contract-plan-editor
      v-if="!isRent"
      :plans="plans"
      :amount="amount"
      :start-time="startTime"
      :end-time="endTime"
      :pay-mode="payMode"
      @plan-change="$emit('plan-change', $event)"
    />
  </div>
</template>

<script>
import ContractPlanEditor from '@/components/contract/workbench/ContractPlanEditor'
import ContractFeeAddDialog from '@/components/contract/workbench/ContractFeeAddDialog'
import { CONTRACT_CATEGORY, formatAmount } from '@/utils/contractUtil'
import {
  applyManualAmount,
  isAdditionalFormula,
  isManualFormula,
  isUnitPriceFormula,
  sumFeeTotal,
  sumIncludedAmount
} from '@/utils/contractFeeUtil'

export default {
  name: 'ContractFeePlanPanel',
  components: {
    ContractPlanEditor,
    ContractFeeAddDialog
  },
  props: {
    category: {
      type: [String, Number],
      default: CONTRACT_CATEGORY.RENT
    },
    rooms: {
      type: Array,
      default: function () {
        return []
      }
    },
    fees: {
      type: Array,
      default: function () {
        return []
      }
    },
    plans: {
      type: Array,
      default: function () {
        return []
      }
    },
    amount: {
      type: [String, Number],
      default: 0
    },
    startTime: {
      type: String,
      default: ''
    },
    endTime: {
      type: String,
      default: ''
    },
    payMode: {
      type: String,
      default: ''
    }
  },
  computed: {
    isRent() {
      return String(this.category) === CONTRACT_CATEGORY.RENT
    },
    hasRooms() {
      return this.rooms.length > 0
    },
    feeTotal() {
      return sumFeeTotal(this.fees)
    },
    includeTotal() {
      return sumIncludedAmount(this.fees)
    }
  },
  methods: {
    formatAmount,
    isManualRow(row) {
      return isManualFormula(row.computingFormula)
    },
    isUnitPriceRow(row) {
      return isUnitPriceFormula(row.computingFormula)
    },
    isAdditionalRow(row) {
      return isAdditionalFormula(row.computingFormula)
    },
    openFeeDialog() {
      this.$refs.feeDialog.open()
    },
    handleFeeConfirm(row) {
      this.fees.push(row)
      this.$emit('fees-change', this.fees)
    },
    handleManualInput(row) {
      applyManualAmount(row)
      this.handleFeeInput()
    },
    handleFeeInput() {
      this.$emit('fees-change', this.fees)
    },
    handleFeeRemove(index) {
      this.fees.splice(index, 1)
      this.$emit('fees-change', this.fees)
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-fee-panel {
  margin-bottom: 18px;

  .cc-fee-toolbar {
    display: flex;
    align-items: center;
    margin-bottom: 8px;

    .cc-section-sub {
      font-size: 13px;
      font-weight: 600;
      color: #303133;
      margin-right: 10px;
    }

    .cc-fee-total {
      margin-left: auto;
      font-size: 13px;
      color: #606266;

      span {
        font-weight: 600;
        color: #409eff;
      }
    }
  }

  .cc-fee-tip {
    font-size: 12px;
    color: #909399;
  }

  .cc-fee-summary {
    margin-top: 10px;
    text-align: right;
    font-size: 13px;
    color: #606266;

    > span {
      margin-left: 16px;
    }
  }
}
</style>
