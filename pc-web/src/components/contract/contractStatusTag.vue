<template>
  <span class="cc-status-tag">
    <el-tag v-if="showCategory" size="mini" :type="categoryInfo.type" effect="plain">
      {{ $t(categoryInfo.key) }}
    </el-tag>
    <el-tag size="mini" :type="stateInfo.type">
      {{ stateText }}
    </el-tag>
    <span v-if="showDays && daysText" class="cc-badge" :class="'is-' + daysType">{{ daysText }}</span>
  </span>
</template>

<script>
import { categoryTagOf, daysLeftType, stateTagOf } from '@/utils/contractUtil'

export default {
  name: 'ContractStatusTag',
  props: {
    state: {
      type: [String, Number],
      default: ''
    },
    stateName: {
      type: String,
      default: ''
    },
    category: {
      type: [String, Number],
      default: ''
    },
    days: {
      type: [String, Number],
      default: null
    },
    showCategory: {
      type: Boolean,
      default: false
    },
    showDays: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    stateInfo() {
      return stateTagOf(this.state)
    },
    stateText() {
      if (this.stateName) {
        return this.stateName
      }
      const key = this.stateInfo.key
      return key ? this.$t(key) : this.$t('contractState.unknown')
    },
    categoryInfo() {
      return categoryTagOf(this.category)
    },
    daysType() {
      return daysLeftType(this.days === null ? null : Number(this.days))
    },
    daysText() {
      if (this.days === null || this.days === '' || this.days === undefined) {
        return ''
      }
      const days = Number(this.days)
      if (days < 0) {
        return this.$t('contractLedger.daysLeft.expired', { days: Math.abs(days) })
      }
      return this.$t('contractLedger.daysLeft.remain', { days: days })
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-status-tag {
  display: inline-flex;
  align-items: center;

  .el-tag {
    margin-right: 6px;
  }

  .cc-badge {
    margin-left: 0;
  }
}
</style>
