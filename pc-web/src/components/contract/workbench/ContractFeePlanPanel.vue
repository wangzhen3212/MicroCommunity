<template>
  <div>
    <div v-if="isRent" class="cc-fee-panel">
      <div class="cc-section-sub">{{ $t('contractWorkbench.plan.feeTitle') }}</div>
      <el-table :data="feeRows" size="mini" border>
        <el-table-column :label="$t('contractWorkbench.plan.rent')" width="180">
          <template slot-scope="scope">
            <span>{{ feeLabel(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('contractWorkbench.target.rentUnitPrice')" width="150" align="center">
          <template slot-scope="scope">
            <el-input
              v-if="scope.row.key !== 'other'"
              size="mini"
              v-model="scope.row.unitPrice"
              @input="handleFeeChange"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('contractWorkbench.field.amount')" align="center" width="150">
          <template slot-scope="scope">
            <el-input
              v-if="scope.row.key === 'other' || scope.row.key === 'water'"
              size="mini"
              v-model="scope.row.value"
              @input="handleFeeChange"
            />
            <span v-else>{{ formatAmount(scope.row.value) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="$t('contractWorkbench.field.remark')" align="center">
          <template slot-scope="scope">
            <span v-if="scope.row.key === 'water'" class="cc-fee-tip">{{ $t('contractWorkbench.plan.waterTip') }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="cc-fee-total">
        {{ $t('contractWorkbench.plan.total') }}：<span>{{ formatAmount(feeTotal) }}</span>
      </div>
    </div>

    <contract-plan-editor
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
import { CONTRACT_CATEGORY, formatAmount, termMonths, toNumber } from '@/utils/contractUtil'

export default {
  name: 'ContractFeePlanPanel',
  components: {
    ContractPlanEditor
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
  data() {
    return {
      feeRows: [
        { key: 'rent', unitPrice: '', value: 0, countable: true },
        { key: 'propertyFee', unitPrice: '', value: 0, countable: true },
        { key: 'other', unitPrice: '', value: '', countable: true },
        { key: 'water', unitPrice: '', value: '', countable: false }
      ]
    }
  },
  computed: {
    isRent() {
      return String(this.category) === CONTRACT_CATEGORY.RENT
    },
    area() {
      return this.rooms.reduce((sum, room) => sum + toNumber(room.builtUpArea), 0)
    },
    months() {
      return termMonths(this.startTime, this.endTime)
    },
    feeTotal() {
      return this.feeRows
        .filter(row => row.countable)
        .reduce((sum, row) => sum + toNumber(row.value), 0)
    }
  },
  watch: {
    rooms() {
      this.recalc()
    },
    months() {
      this.recalc()
    }
  },
  methods: {
    formatAmount,
    feeLabel(row) {
      if (row.key === 'rent') {
        return this.$t('contractWorkbench.plan.rent')
      }
      if (row.key === 'propertyFee') {
        return this.$t('contractWorkbench.plan.propertyFee')
      }
      if (row.key === 'water') {
        return this.$t('contractWorkbench.plan.water')
      }
      return this.$t('contractWorkbench.plan.other')
    },
    recalc() {
      this.feeRows.forEach(row => {
        if (row.key === 'rent' || row.key === 'propertyFee') {
          row.value = this.area * toNumber(row.unitPrice) * this.months
        }
      })
      this.handleFeeChange()
    },
    handleFeeChange() {
      this.$emit('fee-change', {
        rows: this.feeRows,
        total: this.feeTotal
      })
      this.$emit('amount-change', formatAmount(this.feeTotal))
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-fee-panel {
  margin-bottom: 18px;

  .cc-section-sub {
    font-size: 13px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 8px;
  }

  .cc-fee-tip {
    font-size: 12px;
    color: #909399;
  }

  .cc-fee-total {
    margin-top: 10px;
    text-align: right;
    font-size: 13px;
    color: #606266;

    span {
      font-weight: 600;
      color: #4f46e5;
    }
  }
}
</style>
