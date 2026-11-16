<template>
  <div class="cc-card cc-tpl-card">
    <div class="cc-tpl-head">
      <div class="cc-tpl-icon">
        <i class="el-icon-document"></i>
      </div>
      <div class="cc-tpl-badges">
        <span class="cc-tpl-badge" :class="categoryBadge">
          {{ categoryText }}
        </span>
        <span class="cc-tpl-badge" :class="sourceClass">{{ sourceText }}</span>
        <span class="cc-tpl-badge is-info">{{ versionText }}</span>
      </div>
    </div>

    <h3 class="cc-tpl-name" :title="template.templateName">{{ template.templateName || $t('contractTemplate.title') }}</h3>
    <p class="cc-tpl-desc">{{ descText || '-' }}</p>

    <div class="cc-tpl-meta">
      <span>{{ $t('contractTemplate.clauseCount', { count: clauseCount }) }}</span>
      <span class="cc-tpl-dot">·</span>
      <span>{{ $t('contractTemplate.pageCount', { count: pageCount }) }}</span>
      <span class="cc-tpl-dot">·</span>
      <span>{{ $t('contractTemplate.useCount', { count: template.useCount || 0 }) }}</span>
    </div>

    <div class="cc-tpl-foot">
      <span class="cc-tpl-time">{{ $t('contractTemplate.updatedOn', { date: formatTime(template.updateTime || template.createTime) }) }}</span>
      <div class="cc-tpl-ops">
        <button type="button" class="cc-tpl-btn" @click="$emit('edit', template)">{{ $t('contractTemplate.edit') }}</button>
        <button type="button" class="cc-tpl-btn" @click="$emit('preview', template)">{{ $t('contractTemplate.preview') }}</button>
        <button type="button" class="cc-tpl-btn is-primary" @click="$emit('use', template)">{{ $t('contractTemplate.use') }}</button>
        <el-dropdown trigger="click" class="cc-tpl-more" @command="handleCommand">
          <button type="button" class="cc-tpl-btn is-icon" :title="$t('contractTemplate.more')">
            <i class="el-icon-more"></i>
          </button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="copy">{{ $t('contractTemplate.copy') }}</el-dropdown-item>
            <el-dropdown-item command="disable">{{ $t('contractTemplate.disable') }}</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
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
    categoryBadge() {
      if (String(this.category) === '1') {
        return 'is-rent'
      }
      if (String(this.category) === '3') {
        return 'is-maintain'
      }
      return 'is-purchase'
    },
    clauseCount() {
      const context = this.template.context || ''
      const matched = context.match(/第[一二三四五六七八九十\d]+条/g)
      return matched ? matched.length : 0
    },
    descText() {
      const desc = this.template.templateDesc || ''
      if (desc) {
        return desc
      }
      return String(this.template.context || '')
        .replace(/<[^>]+>/g, ' ')
        .replace(/\s+/g, ' ')
        .trim()
    },
    pageCount() {
      const context = String(this.template.context || '').replace(/<[^>]+>/g, '')
      return Math.max(1, Math.ceil(context.length / 900))
    },
    sourceType() {
      return this.template.sourceType || 'online'
    },
    sourceText() {
      if (this.sourceType === 'word') {
        return this.$t('contractTemplate.source.word')
      }
      if (this.sourceType === 'paste') {
        return this.$t('contractTemplate.source.paste')
      }
      return this.$t('contractTemplate.source.online')
    },
    versionText() {
      const version = String(this.template.templateVersion || 'v1.0')
      return /^v/i.test(version) ? 'v' + version.substring(1) : version
    },
    sourceClass() {
      if (this.sourceType === 'word') {
        return 'is-word'
      }
      if (this.sourceType === 'paste') {
        return 'is-paste'
      }
      return 'is-info'
    }
  },
  methods: {
    formatTime(time) {
      if (!time) {
        return '-'
      }
      return String(time).replace('T', ' ').substring(0, 10)
    },
    handleCommand(command) {
      if (command === 'copy') {
        this.$emit('copy', this.template)
        return
      }
      this.$emit('disable', this.template)
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-tpl-card {
  padding: 20px;
  border-radius: 16px;

  .cc-tpl-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }

  .cc-tpl-icon {
    width: 44px;
    height: 48px;
    flex: none;
    border-radius: 8px;
    border: 1px solid #ebeef5;
    background: linear-gradient(180deg, #fafafa, #f2f3f5);
    color: #909399;
    font-size: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .cc-tpl-badges {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 6px;
    min-width: 0;
  }

  .cc-tpl-badge {
    height: 18px;
    line-height: 18px;
    padding: 0 6px;
    border-radius: 4px;
    font-size: 10px;

    &.is-rent {
      color: #1d4ed8;
      background: #eff6ff;
      box-shadow: inset 0 0 0 1px rgba(37, 99, 235, .2);
    }

    &.is-purchase {
      color: #6d28d9;
      background: #f5f3ff;
      box-shadow: inset 0 0 0 1px rgba(124, 58, 237, .2);
    }

    &.is-maintain {
      color: #0f766e;
      background: #f0fdfa;
      box-shadow: inset 0 0 0 1px rgba(13, 148, 136, .2);
    }

    &.is-word {
      color: #2563eb;
      background: #eff6ff;
      box-shadow: inset 0 0 0 1px rgba(37, 99, 235, .2);
    }

    &.is-paste {
      color: #059669;
      background: #ecfdf5;
      box-shadow: inset 0 0 0 1px rgba(5, 150, 105, .2);
    }

    &.is-info {
      color: #64748b;
      background: #f1f5f9;
    }
  }

  .cc-tpl-name {
    margin: 14px 0 0;
    font-size: 14px;
    font-weight: 600;
    line-height: 20px;
    color: #303133;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .cc-tpl-desc {
    margin: 6px 0 0;
    height: 34px;
    font-size: 12px;
    line-height: 17px;
    color: #64748b;
    overflow: hidden;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }

  .cc-tpl-meta {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 12px;
    font-size: 11px;
    color: #9ca3af;

    .cc-tpl-dot {
      color: #dcdfe6;
    }
  }

  .cc-tpl-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-top: 14px;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
  }

  .cc-tpl-time {
    font-size: 11px;
    color: #9ca3af;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .cc-tpl-ops {
    display: flex;
    align-items: center;
    gap: 6px;
    flex: none;
  }

  .cc-tpl-btn {
    height: 28px;
    padding: 0 10px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    background: #fff;
    font-size: 12px;
    line-height: 26px;
    color: #606266;
    cursor: pointer;
    transition: background-color .2s, color .2s, border-color .2s;

    &:hover {
      background: #f5f7fa;
    }

    &.is-primary {
      border-color: #4f46e5;
      background: #4f46e5;
      color: #fff;

      &:hover {
        border-color: #6366f1;
        background: #6366f1;
      }
    }

    &.is-icon {
      width: 28px;
      padding: 0;
      line-height: 26px;
    }
  }

  .cc-tpl-more {
    line-height: 28px;
  }
}
</style>
