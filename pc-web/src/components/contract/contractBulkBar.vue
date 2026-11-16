<template>
  <div v-if="count > 0" class="cc-bulk-bar">
    <span>{{ $t('contractLedger.bulk.selected', { count: count }) }}</span>
    <span>
      <el-button size="mini" type="primary" @click="$emit('submit')">{{ $t('contractLedger.bulk.submit') }}</el-button>
      <el-button size="mini" @click="$emit('export')">{{ $t('contractLedger.bulk.export') }}</el-button>
      <el-button size="mini" type="danger" @click="$emit('stop')">{{ $t('contractLedger.bulk.stop') }}</el-button>
      <el-button size="mini" type="text" @click="$emit('clear')">{{ $t('contractLedger.bulk.clear') }}</el-button>
    </span>
  </div>
</template>

<script>
export default {
  name: 'ContractBulkBar',
  props: {
    count: {
      type: Number,
      default: 0
    }
  }
}
</script>
