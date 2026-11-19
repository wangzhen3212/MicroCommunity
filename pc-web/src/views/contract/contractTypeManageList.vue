<template>
  <div class="contract-template-center cc-page">
    <div class="cc-surface">
      <div class="cc-surface-head is-stacked">
        <div>
          <div class="cc-surface-title">{{ $t('contractTemplate.title') }}</div>
          <div class="cc-surface-subtitle">{{ $t('contractTemplate.subtitle') }}</div>
        </div>
      </div>

      <div class="cc-tpl-bar">
        <div class="cc-tabs is-segment">
          <div
            v-for="tab in categoryTabs"
            :key="tab.key"
            class="cc-tab"
            :class="{ 'is-active': conditions.category === tab.key }"
            @click="switchCategory(tab.key)"
          >
            {{ $t(tab.labelKey) }}
            <span class="cc-tab-count">{{ tab.count }}</span>
          </div>
        </div>

        <div class="cc-tpl-actions">
          <el-popover placement="bottom-end" width="300" trigger="hover" popper-class="cc-tpl-vars">
            <div class="cc-var-list">
              <span v-for="item in templateVariables" :key="item.token" class="cc-var-item">
                <code class="cc-var-chip">{{ item.token }}</code>
                <em class="cc-var-name">{{ $t('contractTemplate.variables.' + item.key) }}</em>
              </span>
            </div>
            <span slot="reference" class="cc-tpl-help">
              <i class="el-icon-info"></i>
              <span>{{ $t('contractTemplate.variableHelp') }}</span>
            </span>
          </el-popover>

          <el-input
            class="cc-tpl-search"
            v-model="conditions.templateNameLike"
            :placeholder="$t('contractTemplate.searchPlaceholder')"
            prefix-icon="el-icon-search"
            clearable
            @keyup.enter.native="handleSearch"
          />
          <el-button type="primary" @click="openEditor()">
            <i class="el-icon-plus"></i>
            <span>{{ $t('contractTemplate.newTemplate') }}</span>
          </el-button>
        </div>
      </div>

      <div class="cc-card-grid">
        <el-row :gutter="16" v-loading="loading">
          <el-col
            v-for="item in templates"
            :key="item.templateId"
            :xs="24"
            :sm="12"
            :lg="8"
          >
            <contract-template-card
              :template="item"
              @edit="openEditor"
              @preview="openPreview"
              @copy="copyTemplate"
              @disable="disableTemplate"
            />
          </el-col>
        </el-row>
      </div>

      <div v-if="!loading && !templates.length" class="cc-empty">{{ $t('contractTemplate.empty') }}</div>

      <el-pagination
        class="cc-pagination"
        :current-page="page.current"
        :page-sizes="[9, 18, 36]"
        :page-size="page.size"
        :total="page.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <contract-template-preview ref="templatePreview" />
  </div>
</template>

<script>
import ContractTemplateCard from '@/components/contract/contractTemplateCard'
import ContractTemplatePreview from '@/components/contract/contractTemplatePreview'
import {
  deleteContractTypeTemplate,
  listContractTemplates,
  queryContractTypeTemplate
} from '@/api/contract/contractTemplateApi'
import { guessCategory } from '@/utils/contractUtil'

export default {
  name: 'ContractTypeManageList',
  components: {
    ContractTemplateCard,
    ContractTemplatePreview
  },
  data() {
    return {
      loading: false,
      templates: [],
      allTemplates: [],
      conditions: {
        templateNameLike: '',
        category: ''
      },
      page: {
        current: 1,
        size: 9,
        total: 0
      }
    }
  },
  computed: {
    templateVariables() {
      return [
        { token: '{{amount}}', key: 'amount' },
        { token: '{{startTime}}', key: 'startTime' },
        { token: '{{endTime}}', key: 'endTime' },
        { token: '{{partyB}}', key: 'partyB' },
        { token: '{{houses}}', key: 'houses' }
      ]
    },
    categoryTabs() {
      return [
        { key: '', labelKey: 'contractCategory.all', count: this.allTemplates.length },
        { key: '1', labelKey: 'contractCategory.rent', count: this.countByCategory('1') },
        { key: '2', labelKey: 'contractCategory.purchase', count: this.countByCategory('2') },
        { key: '3', labelKey: 'contractCategory.maintain', count: this.countByCategory('3') }
      ]
    }
  },
  created() {
    this.loadTemplates()
  },
  methods: {
    countByCategory(category) {
      return this.allTemplates.filter(item => guessCategory(item) === category).length
    },
    loadTemplates() {
      this.loading = true
      const params = {
        page: this.page.current,
        row: this.page.size,
        templateNameLike: this.conditions.templateNameLike,
        category: this.conditions.category
      }
      listContractTemplates(params).then(res => {
        this.templates = res.data || []
        this.page.total = res.total || 0
      }).catch(error => {
        this.templates = []
        this.page.total = 0
        this.$message.error(error.message || this.$t('contractTemplate.fetchError'))
      }).finally(() => {
        this.loading = false
      })
      listContractTemplates({ page: 1, row: 500 }).then(res => {
        this.allTemplates = res.data || []
      }).catch(() => {
        this.allTemplates = []
      })
    },
    handleSearch() {
      this.page.current = 1
      this.loadTemplates()
    },
    switchCategory(category) {
      this.conditions.category = category
      this.handleSearch()
    },
    handleSizeChange(size) {
      this.page.size = size
      this.loadTemplates()
    },
    handleCurrentChange(current) {
      this.page.current = current
      this.loadTemplates()
    },
    openEditor(template) {
      if (!template || !template.templateId) {
        this.$router.push({ path: '/views/contract/templateEditor' })
        return
      }
      this.$router.push({
        path: '/views/contract/templateEditor',
        query: { templateId: template.templateId }
      })
    },
    openPreview(template) {
      queryContractTypeTemplate({ templateId: template.templateId, page: 1, row: 1 }).then(list => {
        const detail = (list || [])[0] || template
        this.$refs.templatePreview.open(detail.context, detail.templateName)
      }).catch(error => {
        this.$message.error(error.message)
      })
    },
    copyTemplate(template) {
      this.$router.push({
        path: '/views/contract/templateEditor',
        query: { copyFrom: template.templateId }
      })
    },
    disableTemplate(template) {
      this.$confirm(this.$t('contractTemplate.disable') + ' ' + template.templateName, '提示', {
        type: 'warning'
      }).then(() => {
        deleteContractTypeTemplate({ templateId: template.templateId }).then(() => {
          this.$message.success(this.$t('common.operateSuccess'))
          this.handleSearch()
        }).catch(error => {
          this.$message.error(error.message)
        })
      }).catch(() => {
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.contract-template-center {
  .cc-tpl-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
    margin: 16px 0;

    .cc-tabs {
      margin-bottom: 0;
    }

    .cc-tabs.is-segment {
      padding: 0;
      border: none;
      border-radius: 0;
      background: transparent;
      box-shadow: none;
      gap: 6px;

      .cc-tab {
        padding: 6px 16px;
        border-radius: 999px;
        border-bottom: none;

        &:hover {
          background: #f5f6fa;
        }

        &.is-active {
          color: #409eff;
          background: #ecf5ff;
          font-weight: 500;
        }

        .cc-tab-count {
          color: inherit;
        }
      }
    }
  }

  .cc-tpl-actions {
    display: flex;
    align-items: center;
    gap: 8px;

    .el-button {
      height: 36px;
      padding: 0 16px;
      margin-left: 0;
    }
  }

  .cc-tpl-help {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: #909399;
    cursor: pointer;
    white-space: nowrap;

    &:hover {
      color: #409eff;
    }
  }

  .cc-tpl-search {
    width: 220px;

    ::v-deep .el-input__inner {
      height: 36px;
      line-height: 36px;
    }
  }
}
</style>
