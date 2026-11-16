<template>
  <div>
    <div class="cc-target-toolbar">
      <el-button v-if="isRent" size="mini" type="primary" @click="$emit('add-house')">
        <i class="el-icon-plus"></i>{{ $t('contractWorkbench.target.add') }}
      </el-button>
      <el-button v-else size="mini" type="primary" @click="$emit('add-target')">
        <i class="el-icon-plus"></i>{{ $t('contractWorkbench.target.add') }}
      </el-button>
      <el-button size="mini" @click="fillAmount">{{ $t('contractWorkbench.target.fillTotal') }}</el-button>
      <el-button size="mini" @click="$emit('clear')">{{ $t('contractWorkbench.target.clear') }}</el-button>
      <span class="cc-target-total">{{ formatAmount(totalAmount) }}</span>
    </div>

    <el-table v-if="isRent" :data="rooms" size="mini" border>
      <el-table-column :label="$t('contractWorkbench.target.house')">
        <template slot-scope="scope">
          {{ scope.row.roomName || ((scope.row.floorNum || '') + '-' + (scope.row.unitNum || '') + '-' + (scope.row.roomNum || '')) }}
        </template>
      </el-table-column>
      <el-table-column prop="ownerName" :label="$t('contractWorkbench.target.owner')" align="center" />
      <el-table-column :label="$t('contractWorkbench.target.area')" align="center" width="100">
        <template slot-scope="scope">{{ scope.row.builtUpArea || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.target.rentUnitPrice')" align="center" width="150">
        <template slot-scope="scope">
          <el-input size="mini" v-model="scope.row.rentUnitPrice" />
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.field.amount')" align="center" width="120">
        <template slot-scope="scope">{{ formatAmount(roomAmount(scope.row)) }}</template>
      </el-table-column>
      <el-table-column :label="$t('common.operation')" align="center" width="80">
        <template slot-scope="scope">
          <el-button type="text" @click="$emit('remove-house', scope.$index)">{{ $t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-table v-else :data="targets" size="mini" border>
      <el-table-column :label="firstColumnLabel">
        <template slot-scope="scope">
          <el-input size="mini" v-model="scope.row.name" />
        </template>
      </el-table-column>
      <el-table-column :label="secondColumnLabel">
        <template slot-scope="scope">
          <el-input size="mini" v-model="scope.row.spec" />
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.target.num')" align="center" width="110">
        <template slot-scope="scope">
          <el-input size="mini" v-model="scope.row.num" />
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.target.price')" align="center" width="130">
        <template slot-scope="scope">
          <el-input size="mini" v-model="scope.row.price" />
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.field.amount')" align="center" width="120">
        <template slot-scope="scope">{{ formatAmount(targetAmount(scope.row)) }}</template>
      </el-table-column>
      <el-table-column :label="$t('common.operation')" align="center" width="80">
        <template slot-scope="scope">
          <el-button type="text" @click="$emit('remove-target', scope.$index)">{{ $t('common.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="isEmpty" class="cc-empty">{{ $t('contractWorkbench.target.empty') }}</div>
  </div>
</template>

<script>
import { CONTRACT_CATEGORY, formatAmount, toNumber } from '@/utils/contractUtil'

export default {
  name: 'ContractTargetTable',
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
    targets: {
      type: Array,
      default: function () {
        return []
      }
    }
  },
  computed: {
    isRent() {
      return String(this.category) === CONTRACT_CATEGORY.RENT
    },
    isMaintain() {
      return String(this.category) === CONTRACT_CATEGORY.MAINTAIN
    },
    firstColumnLabel() {
      return this.isMaintain ? this.$t('contractWorkbench.target.device') : this.$t('contractWorkbench.target.goods')
    },
    secondColumnLabel() {
      return this.isMaintain ? this.$t('contractWorkbench.target.service') : this.$t('contractWorkbench.target.spec')
    },
    isEmpty() {
      return this.isRent ? !this.rooms.length : !this.targets.length
    },
    totalAmount() {
      if (this.isRent) {
        return this.rooms.reduce((sum, row) => sum + this.roomAmount(row), 0)
      }
      return this.targets.reduce((sum, row) => sum + this.targetAmount(row), 0)
    }
  },
  methods: {
    formatAmount,
    roomAmount(row) {
      return toNumber(row.builtUpArea) * toNumber(row.rentUnitPrice)
    },
    targetAmount(row) {
      return toNumber(row.num) * toNumber(row.price)
    },
    fillAmount() {
      this.$emit('fill-amount', this.totalAmount)
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-target-toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 10px;

  .cc-target-total {
    margin-left: auto;
    font-size: 14px;
    font-weight: 600;
    color: #4f46e5;
  }
}
</style>
