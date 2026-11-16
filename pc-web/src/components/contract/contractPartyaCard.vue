<template>
  <div class="cc-card" :class="{ 'is-default': isDefault, 'is-disabled': isDisabled }">
    <div class="cc-card-head">
      <div class="cc-card-title">
        {{ partya.partyA }}
        <span v-if="isDefault" class="cc-badge is-primary">{{ $t('contractPartyaCenter.default') }}</span>
        <span v-if="isSealed" class="cc-badge is-success">{{ $t('contractPartyaCenter.seal') }}</span>
      </div>
      <span class="cc-badge" :class="isDisabled ? 'is-info' : 'is-success'">
        {{ isDisabled ? $t('contractPartyaCenter.disabled') : $t('contractPartyaCenter.enabled') }}
      </span>
    </div>

    <div class="cc-card-meta cc-kv-grid">
      <div v-if="partya.shortName" class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractPartyaCenter.shortName') }}</span>
        <span class="cc-kv-value">{{ partya.shortName }}</span>
      </div>
      <div class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractPartyaCenter.creditCode') }}</span>
        <span class="cc-kv-value">{{ partya.creditCode || '-' }}</span>
      </div>
      <div class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractPartyaCenter.legalPerson') }}</span>
        <span class="cc-kv-value">{{ partya.legalPerson || '-' }}</span>
      </div>
      <div class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractPartyaCenter.contacts') }}</span>
        <span class="cc-kv-value">{{ partya.contactPerson || partya.aContacts || '-' }}</span>
      </div>
      <div class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractPartyaCenter.tel') }}</span>
        <span class="cc-kv-value">{{ partya.contactTel || partya.aLink || '-' }}</span>
      </div>
      <div class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractPartyaCenter.bankName') }}</span>
        <span class="cc-kv-value">{{ partya.bankName || '-' }}</span>
      </div>
      <div class="cc-kv">
        <span class="cc-kv-label">{{ $t('contractPartyaCenter.bankAccount') }}</span>
        <span class="cc-kv-value">{{ partya.bankAccount || '-' }}</span>
      </div>
    </div>

    <div class="cc-card-footer">
      <el-button size="mini" @click="$emit('edit', partya)">{{ $t('contractPartyaCenter.op.edit') }}</el-button>
      <el-button
        v-if="!isDefault"
        size="mini"
        @click="$emit('set-default', partya)"
      >{{ $t('contractPartyaCenter.op.setDefault') }}</el-button>
      <el-button size="mini" type="danger" @click="$emit('delete', partya)">
        {{ $t('contractPartyaCenter.op.delete') }}
      </el-button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ContractPartyaCard',
  props: {
    partya: {
      type: Object,
      required: true
    }
  },
  computed: {
    isDefault() {
      return String(this.partya.defaultFlag) === '1'
    },
    isSealed() {
      return String(this.partya.sealFlag) === '1'
    },
    isDisabled() {
      return String(this.partya.statusCd) !== '0'
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-card {
  &.is-default {
    border-color: #4f46e5;
    box-shadow: 0 2px 10px rgba(79, 70, 229, 0.12);
  }

  &.is-disabled {
    background: #fafafa;
  }
}
</style>
