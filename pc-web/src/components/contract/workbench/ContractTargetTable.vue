<template>
  <div>
    <div class="cc-target-toolbar">
      <el-button v-if="isRent" size="mini" type="primary" @click="$emit('add-house')">
        <i class="el-icon-plus"></i>{{ $t('contractWorkbench.target.addRent') }}
      </el-button>
      <el-button v-else size="mini" type="primary" @click="$emit('add-target')">
        <i class="el-icon-plus"></i>{{ $t('contractWorkbench.target.add') }}
      </el-button>
      <el-button size="mini" @click="fillAmount">{{ $t('contractWorkbench.target.fillTotal') }}</el-button>
      <el-button size="mini" @click="$emit('clear')">{{ $t('contractWorkbench.target.clear') }}</el-button>
      <span class="cc-target-total">{{ formatAmount(totalAmount) }}</span>
    </div>

    <el-table v-if="isRent" :data="rooms" size="mini" border>
      <el-table-column :label="isShop ? $t('contractWorkbench.target.shopName') : $t('contractWorkbench.target.roomName')">
        <template slot-scope="scope">{{ rowName(scope.row) }}</template>
      </el-table-column>
      <el-table-column v-if="isShop" prop="locationName" :label="$t('contractWorkbench.target.shopLocation')" />
      <el-table-column :label="$t('contractWorkbench.target.state')" align="center" width="130">
        <template slot-scope="scope">
          <span>{{ scope.row.stateName || '-' }}</span>
          <span class="cc-target-tip">{{ $t('contractWorkbench.target.stateTip') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.target.area')" align="center" width="100">
        <template slot-scope="scope">{{ scope.row.builtUpArea || '-' }}</template>
      </el-table-column>
      <el-table-column
        :label="isShop ? $t('contractWorkbench.target.shopArea') : $t('contractWorkbench.target.roomArea')"
        align="center"
        width="100"
      >
        <template slot-scope="scope">{{ scope.row.areaValue || scope.row.roomArea || '-' }}</template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.target.rentUnitPrice')" align="center" width="150">
        <template slot-scope="scope">
          <span>{{ formatAmount(scope.row.rentValue || scope.row.roomRent) }}</span>
          <span class="cc-target-tip">{{ $t('contractWorkbench.target.rentTip') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('contractWorkbench.target.subtotal')" align="center" width="120">
        <template slot-scope="scope">{{ rowAmountText(scope.row) }}</template>
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
import { PAYER_OBJ_TYPE, payerObjTypeOf } from '@/utils/contractFeeUtil'

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
    },
    rentAmountMap: {
      type: Object,
      default: function () {
        return {}
      }
    },
    rentTotalAmount: {
      type: [String, Number],
      default: ''
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
  computed: {
    isRent() {
      return String(this.category) === CONTRACT_CATEGORY.RENT
    },
    isMaintain() {
      return String(this.category) === CONTRACT_CATEGORY.MAINTAIN
    },
    isShop() {
      return this.rooms.length > 0 && payerObjTypeOf(this.rooms[0]) === PAYER_OBJ_TYPE.SHOP
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
        if (this.rentTotalAmount !== '' && this.rentTotalAmount !== null) {
          return toNumber(this.rentTotalAmount)
        }
        return this.rooms.reduce((sum, row) => sum + this.rowAmount(row), 0)
      }
      return this.targets.reduce((sum, row) => sum + this.targetAmount(row), 0)
    }
  },
  methods: {
    formatAmount,
    rowName(row) {
      if (!row) {
        return ''
      }
      if (row.objName) {
        return row.objName
      }
      if (this.isShop && (row.shopNum || row.locationName)) {
        return row.shopNum || row.locationName
      }
      return row.roomName || ((row.floorNum || '') + '-' + (row.unitNum || '') + '-' + (row.roomNum || ''))
    },
    rowObjId(row) {
      return (row && (row.objId || row.roomId || row.shopId)) || ''
    },
    rowAmount(row) {
      const objId = this.rowObjId(row)
      if (!objId || !this.rentAmountMap) {
        return 0
      }
      return toNumber(this.rentAmountMap[objId])
    },
    rowAmountText(row) {
      const objId = this.rowObjId(row)
      if (!objId || !this.rentAmountMap || typeof this.rentAmountMap[objId] === 'undefined') {
        return '-'
      }
      return formatAmount(this.rentAmountMap[objId])
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
    color: #409eff;
  }
}

.cc-target-tip {
  margin-left: 6px;
  font-size: 11px;
  color: #909399;
}
</style>
