<template>
  <div class="contract-template-center cc-page">
    <div class="cc-page-head">
      <div>
        <div class="cc-page-title">{{ $t('contractTemplate.title') }}</div>
        <div class="cc-page-subtitle">{{ $t('contractTemplate.subtitle') }}</div>
      </div>
      <div class="cc-page-actions">
        <el-input
          style="width: 200px"
          v-model="conditions.templateNameLike"
          :placeholder="$t('contractTemplate.searchPlaceholder')"
          clearable
          @keyup.enter.native="handleSearch"
        />
        <el-button @click="handleImportWord">
          <i class="el-icon-document-copy"></i>
          <span>{{ $t('contractTemplate.editor.pasteWord') }}</span>
        </el-button>
        <el-button type="primary" @click="openEditor()">
          <i class="el-icon-plus"></i>
          <span>{{ $t('contractTemplate.newTemplate') }}</span>
        </el-button>
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
      <p class="cc-tpl-legend">
        <span class="cc-tpl-legend-label">{{ $t('contractTemplate.variableLabel') }}</span>
        <span v-for="item in templateVariables" :key="item.token" class="cc-var-item">
          <code class="cc-var-chip">{{ item.token }}</code>
          <em class="cc-var-name">{{ $t('contractTemplate.variables.' + item.key) }}</em>
        </span>
      </p>
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
            @use="handleUseTemplate"
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
    handleImportWord() {
      this.$router.push({
        path: '/views/contract/templateEditor',
        query: { paste: '1' }
      })
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
    handleUseTemplate(template) {
      this.$router.push({
        path: '/views/contract/addContract',
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
.cc-tpl-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;

  .cc-tabs {
    margin-bottom: 0;
  }

  .cc-tpl-legend {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
    margin: 0;
    font-size: 12px;
    line-height: 18px;
    color: #9ca3af;
    text-align: right;
  }

  .cc-tpl-legend-label {
    margin-right: 2px;
  }

  .cc-var-item {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }

  .cc-var-chip {
    padding: 1px 6px;
    border-radius: 5px;
    background: #eef0f4;
    color: #606266;
    font-size: 11px;
    font-family: Consolas, Monaco, monospace;
  }

  .cc-var-name {
    font-style: normal;
    font-size: 11px;
    color: #b1b3b8;
  }
}
</style>
