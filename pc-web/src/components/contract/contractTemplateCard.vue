<template>
  <div class="cc-card cc-tpl-card">
    <div class="cc-tpl-head">
      <div class="cc-tpl-name">{{ template.templateName || $t('contractTemplate.title') }}</div>
      <span class="cc-badge" :class="categoryClass">{{ categoryText }}</span>
    </div>

    <div class="cc-tpl-meta">
      <span>{{ $t('contractTemplate.version', { version: template.templateVersion || 'v1.0' }) }}</span>
      <span>{{ $t('contractTemplate.useCount', { count: template.useCount || 0 }) }}</span>
      <span>{{ $t('contractTemplate.clauseCount', { count: clauseCount }) }}</span>
    </div>

    <div class="cc-tpl-desc">{{ template.templateDesc || template.context || '-' }}</div>

    <div class="cc-tpl-foot">
      <span class="cc-tpl-source">{{ sourceText }}</span>
      <span class="cc-tpl-time">{{ formatTime(template.updateTime || template.createTime) }}</span>
    </div>

    <div class="cc-tpl-actions">
      <el-button size="mini" @click="$emit('edit', template)">{{ $t('contractTemplate.edit') }}</el-button>
      <el-button size="mini" @click="$emit('preview', template)">{{ $t('contractTemplate.preview') }}</el-button>
      <el-button size="mini" @click="$emit('copy', template)">{{ $t('contractTemplate.copy') }}</el-button>
      <el-button
        size="mini"
        type="danger"
        @click="$emit('disable', template)"
      >{{ $t('contractTemplate.disable') }}</el-button>
    </div>
  </div>
</template>

<script>
import { categoryTagOf, guessCategory } from '@/utils/contractUtil'

export default {
  name: 'ContractTemplateCard',
  props: {
    template: {
      type: Object,
      required: true
    }
  },
  computed: {
    category() {
      return guessCategory({ category: this.template.category, contractTypeName: this.template.typeName })
    },
    categoryInfo() {
      return categoryTagOf(this.category)
    },
    categoryText() {
      return this.$t(this.categoryInfo.key)
    },
    categoryClass() {
      return 'is-' + this.categoryInfo.type
    },
    clauseCount() {
      const context = this.template.context || ''
      const matched = context.match(/第[一二三四五六七八九十\d]+条/g)
      return matched ? matched.length : 0
    },
    sourceText() {
      const source = this.template.sourceType || 'online'
      if (source === 'word') {
        return this.$t('contractTemplate.source.word')
      }
      if (source === 'paste') {
        return this.$t('contractTemplate.source.paste')
      }
      return this.$t('contractTemplate.source.online')
    }
  },
  methods: {
    formatTime(time) {
      if (!time) {
        return '-'
      }
      return String(time).replace('T', ' ').substring(0, 16)
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-tpl-card {
  .cc-tpl-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;

    .cc-tpl-name {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
    }
  }

  .cc-tpl-meta {
    display: flex;
    flex-wrap: wrap;
    font-size: 12px;
    color: #909399;
    margin-bottom: 8px;

    span {
      margin-right: 12px;
    }
  }

  .cc-tpl-desc {
    flex: 1;
    font-size: 12px;
    line-height: 1.7;
    color: #606266;
    min-height: 60px;
    max-height: 84px;
    overflow: hidden;
  }

  .cc-tpl-foot {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: #909399;
    margin: 8px 0;
  }

  .cc-tpl-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;

    .el-button+.el-button {
      margin-left: 0;
    }
  }
}
</style>
