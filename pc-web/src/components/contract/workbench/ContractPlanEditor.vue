<template>
  <div>
    <div class="cc-toolbar">
      <span v-if="!isRent" class="cc-toolbar-label">{{ $t('contractWorkbench.plan.mode') }}</span>
      <el-select
        v-if="!isRent"
        style="width: 180px"
        v-model="modeValue"
        :placeholder="$t('contractWorkbench.plan.modePlaceholder')"
        clearable
        @change="handleModeChange"
      >
        <el-option
          v-for="item in payModes"
          :key="item.code"
          :label="$t(item.labelKey)"
          :value="item.code"
        />
      </el-select>
      <el-button size="mini" @click="handleReorder">{{ $t('contractWorkbench.plan.reorder') }}</el-button>
      <el-button size="mini" @click="handleDivide">{{ $t('contractWorkbench.plan.divide') }}</el-button>
      <el-button size="mini" type="primary" @click="handleAddPlan">{{ $t('contractWorkbench.plan.addPlan') }}</el-button>
    </div>

    <el-table v-if="plans.length" :data="plans" size="mini" border>
      <el-table-column :label="$t('contractWorkbench.plan.planName')" width="170">
        <template slot-scope="scope">
          <el-input size="mini" v-model="scope.row.planName" @input="notifyChange" />
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.plan.planTime')" width="170" align="center">
        <template slot-scope="scope">
          <el-date-picker
            size="mini"
            v-model="scope.row.planTime"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
            @change="notifyChange"
          />
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.plan.ratio')" width="110" align="center">
        <template slot-scope="scope">
          <el-input size="mini" v-model="scope.row.ratio" @input="handleRatioInput(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.plan.planAmount')" width="140" align="center">
        <template slot-scope="scope">
          <el-input size="mini" v-model="scope.row.planAmount" @input="notifyChange" />
        </template>
      </el-table-column>
      <el-table-column :label="$t('common.operation')" align="center" width="80">
        <template slot-scope="scope">
          <el-button type="text" @click="handleRemove(scope.$index)">{{ $t('contractWorkbench.plan.remove') }}</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-else class="cc-empty">{{ $t('contractWorkbench.plan.empty') }}</div>

    <div v-if="plans.length" class="cc-plan-summary">
      <span :class="{ 'is-error': !ratioValid }">
        {{ $t('contractWorkbench.plan.ratio') }}：{{ formatNumber(ratioTotal) }}%
      </span>
      <span :class="{ 'is-error': !amountValid }">
        {{ $t('contractWorkbench.plan.planAmount') }}：{{ formatAmount(amountTotal) }}
      </span>
      <span v-if="!ratioValid" class="cc-plan-error">{{ $t('contractWorkbench.plan.ratioError') }}</span>
      <span v-if="!amountValid" class="cc-plan-error">{{ $t('contractWorkbench.plan.amountError') }}</span>
    </div>
  </div>
</template>

<script>
import {
  PAY_MODES,
  amountValid,
  buildPlansByMode,
  createEmptyPlan,
  divideAmount,
  normalizeAmount,
  ratioValid,
  reorderByTerm,
  sumAmount,
  sumRatio
} from '@/utils/contractPlanUtil'
import { formatAmount, toNumber, CONTRACT_CATEGORY } from '@/utils/contractUtil'

export default {
  name: 'ContractPlanEditor',
  props: {
    category: {
      type: [String, Number],
      default: CONTRACT_CATEGORY.PURCHASE
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
      payModes: PAY_MODES,
      modeValue: this.payMode
    }
  },
  computed: {
    isRent() {
      return String(this.category) === CONTRACT_CATEGORY.RENT
    },
    ratioTotal() {
      return sumRatio(this.plans)
    },
    amountTotal() {
      return sumAmount(this.plans)
    },
    ratioValid() {
      return ratioValid(this.plans)
    },
    amountValid() {
      return amountValid(this.plans, this.amount)
    }
  },
  watch: {
    payMode(value) {
      this.modeValue = value
    }
  },
  methods: {
    formatAmount,
    formatNumber(value) {
      return toNumber(value).toFixed(2)
    },
    handleModeChange(mode) {
      const generated = buildPlansByMode(mode, {
        startTime: this.startTime,
        endTime: this.endTime,
        amount: this.amount,
        planName: this.$t('contractWorkbench.field.payMode')
      })
      this.plans.splice(0, this.plans.length)
      generated.forEach(plan => this.plans.push(plan))
      this.notifyChange()
    },
    handleReorder() {
      reorderByTerm(this.plans, this.startTime, this.endTime)
      this.notifyChange()
    },
    handleDivide() {
      divideAmount(this.plans, this.amount)
      this.notifyChange()
    },
    handleAddPlan() {
      this.plans.push(createEmptyPlan(this.plans.length + 1))
      this.notifyChange()
    },
    handleRemove(index) {
      this.plans.splice(index, 1)
      this.resortSeq()
      this.notifyChange()
    },
    handleRatioInput(row) {
      const ratio = toNumber(row.ratio)
      row.planAmount = formatAmount((toNumber(this.amount) * ratio) / 100)
      this.notifyChange()
    },
    resortSeq() {
      this.plans.forEach((plan, index) => {
        plan.planSeq = index + 1
      })
    },
    notifyChange() {
      if (this.plans.length) {
        normalizeAmount(this.plans, this.amount)
      }
      this.$emit('plan-change', this.plans)
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 10px;

  .cc-toolbar-label {
    margin-right: 8px;
    font-size: 13px;
    color: #606266;
  }

  .el-button {
    margin-left: 8px;
  }
}

.cc-plan-summary {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;

  > span {
    margin-right: 16px;
  }

  .is-error {
    color: #f56c6c;
  }

  .cc-plan-error {
    color: #f56c6c;
  }
}
</style>
