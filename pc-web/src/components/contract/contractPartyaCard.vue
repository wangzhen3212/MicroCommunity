<template>
  <div class="cc-card cc-partya-card" :class="{ 'is-disabled': isDisabled }">
    <div class="cc-partya-head">
      <div class="cc-partya-ident">
        <div class="cc-partya-avatar">
          <i class="el-icon-office-building"></i>
        </div>
        <div class="cc-partya-name-wrap">
          <p class="cc-partya-name" :title="partya.partyA">{{ partya.partyA }}</p>
          <p class="cc-partya-code-text">{{ identText }}</p>
        </div>
      </div>
      <span v-if="isDefault" class="cc-partya-badge">{{ $t('contractPartyaCenter.default') }}</span>
    </div>

    <div class="cc-partya-meta">
      <p class="cc-partya-row">
        <span class="cc-partya-label">{{ $t('contractPartyaCenter.creditCode') }}</span>
        <span class="cc-partya-mono">{{ partya.creditCode || '-' }}</span>
      </p>
      <p class="cc-partya-row">
        <span class="cc-partya-label">{{ $t('contractPartyaCenter.legalPerson') }}</span>
        <span>{{ partya.legalPerson || '-' }}</span>
      </p>
      <p class="cc-partya-row">
        <span class="cc-partya-label">{{ $t('contractPartyaCenter.bankName') }}</span>
        <span>{{ partya.bankName || '-' }}</span>
      </p>
    </div>

    <div class="cc-partya-foot">
      <span class="cc-partya-state" :class="{ 'is-off': isDisabled }">
        <span class="cc-partya-dot"></span>
        {{ isDisabled ? $t('contractPartyaCenter.disabled') : $t('contractPartyaCenter.enabled') }}
        <em v-if="isSealed" class="cc-partya-seal">{{ $t('contractPartyaCenter.sealBound') }}</em>
      </span>
      <div class="cc-partya-ops">
        <button
          v-if="!isDefault && !isDisabled"
          type="button"
          class="cc-partya-link"
          @click="$emit('set-default', partya)"
        >{{ $t('contractPartyaCenter.op.setDefault') }}</button>
        <button
          type="button"
          class="cc-partya-link is-primary"
          @click="$emit('edit', partya)"
        >{{ $t('contractPartyaCenter.op.edit') }}</button>
        <button
          type="button"
          class="cc-partya-link is-danger"
          @click="$emit('delete', partya)"
        >{{ $t('contractPartyaCenter.op.delete') }}</button>
      </div>
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
    },
    identText() {
      const shortName = this.partya.shortName || ''
      const partyaId = this.partya.partyaId || ''
      if (partyaId && shortName) {
        return partyaId + ' · ' + shortName
      }
      return partyaId || shortName || '-'
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-partya-card {
  &.is-disabled {
    opacity: .72;
  }

  .cc-partya-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
  }

  .cc-partya-ident {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
  }

  .cc-partya-avatar {
    width: 40px;
    height: 40px;
    flex: none;
    border-radius: 10px;
    background: linear-gradient(135deg, #4f46e5, #818cf8);
    color: #fff;
    font-size: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .cc-partya-name-wrap {
    min-width: 0;
  }

  .cc-partya-name {
    margin: 0;
    font-size: 14px;
    font-weight: 600;
    line-height: 20px;
    color: #303133;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .cc-partya-code-text {
    margin: 2px 0 0;
    font-size: 11px;
    line-height: 16px;
    color: #909399;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .cc-partya-badge {
    flex: none;
    height: 18px;
    line-height: 18px;
    padding: 0 6px;
    border-radius: 9px;
    font-size: 11px;
    color: #4f46e5;
    background: #eef2ff;
  }

  .cc-partya-meta {
    margin-top: 14px;
  }

  .cc-partya-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 0 0 4px;
    font-size: 12px;
    line-height: 18px;
    color: #606266;

    .cc-partya-label {
      width: 92px;
      flex: none;
      color: #909399;
      white-space: nowrap;
    }

    > span:last-child {
      flex: 1;
      min-width: 0;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }

    .cc-partya-mono {
      font-family: Consolas, Monaco, monospace;
      font-size: 11px;
      color: #606266;
    }
  }

  .cc-partya-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
  }

  .cc-partya-state {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 11px;
    color: #67c23a;

    &.is-off {
      color: #909399;

      .cc-partya-dot {
        background: #c0c4cc;
      }
    }
  }

  .cc-partya-dot {
    width: 6px;
    height: 6px;
    flex: none;
    border-radius: 50%;
    background: #67c23a;
  }

  .cc-partya-seal {
    font-style: normal;
    color: #909399;
  }

  .cc-partya-ops {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .cc-partya-link {
    padding: 0;
    border: none;
    background: none;
    font-size: 12px;
    line-height: 18px;
    color: #909399;
    cursor: pointer;
    transition: color .2s;

    &:hover {
      color: #409eff;
    }

    &.is-primary {
      color: #409eff;
      font-weight: 500;
    }

    &.is-danger {
      color: #f56c6c;
    }
  }
}
</style>
