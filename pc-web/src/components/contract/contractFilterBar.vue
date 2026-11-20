<template>
  <div class="cc-toolbar">
    <el-input
      class="cc-toolbar-item"
      style="width: 220px"
      v-model="value.searchText"
      :placeholder="$t('contractLedger.filter.keywordPlaceholder')"
      clearable
      @keyup.enter.native="$emit('search')"
    />
    <el-select
      class="cc-toolbar-item"
      style="width: 150px"
      v-model="value.state"
      :placeholder="$t('contractLedger.filter.statePlaceholder')"
      clearable
    >
      <el-option :label="$t('contractState.draft')" value="10" />
      <el-option :label="$t('contractLedger.stat.auditing')" value="auditing" />
      <el-option :label="$t('contractState.auditing')" value="11" />
      <el-option :label="$t('contractState.leaderAudit')" value="56" />
      <el-option :label="$t('contractState.effective')" value="22" />
      <el-option :label="$t('contractState.fail')" value="33" />
      <el-option :label="$t('contractState.stopped')" value="44" />
      <el-option :label="$t('contractLedger.filter.expiring')" value="expiring" />
      <el-option :label="$t('contractLedger.filter.expired')" value="expired" />
    </el-select>
    <el-select
      v-if="showPartya"
      class="cc-toolbar-item"
      style="width: 200px"
      v-model="value.partyaId"
      :placeholder="$t('contractLedger.filter.partyaPlaceholder')"
      clearable
      filterable
    >
      <el-option
        v-for="item in partyas"
        :key="item.partyaId"
        :label="item.partyA"
        :value="item.partyaId"
      />
    </el-select>
    <el-select
      v-if="showChanged"
      class="cc-toolbar-item"
      style="width: 140px"
      v-model="value.changed"
      :placeholder="$t('contractLedger.filter.changedAll')"
      clearable
    >
      <el-option :label="$t('contractLedger.filter.changedYes')" value="1" />
      <el-option :label="$t('contractLedger.filter.changedNo')" value="0" />
    </el-select>
    <el-button class="cc-toolbar-item" type="primary" @click="$emit('search')">
      <i class="el-icon-search"></i>
      {{ $t('common.search') }}
    </el-button>
    <el-button class="cc-toolbar-item" @click="$emit('reset')">
      <i class="el-icon-refresh"></i>
      {{ $t('common.reset') }}
    </el-button>
  </div>
</template>

<script>
export default {
  name: 'ContractFilterBar',
  props: {
    value: {
      type: Object,
      required: true
    },
    partyas: {
      type: Array,
      default: function () {
        return []
      }
    },
    showPartya: {
      type: Boolean,
      default: true
    },
    showChanged: {
      type: Boolean,
      default: true
    }
  }
}
</script>
