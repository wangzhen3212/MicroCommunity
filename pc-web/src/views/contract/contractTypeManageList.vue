<template>
  <div class="contract-template-center">
    <el-tabs v-model="activeTab">
      <el-tab-pane :label="$t('contractTemplate.title')" name="template">
        <div class="cc-page-head">
          <div>
            <div class="cc-page-title">{{ $t('contractTemplate.title') }}</div>
            <div class="cc-page-subtitle">{{ $t('contractTemplate.subtitle') }}</div>
          </div>
          <div>
            <el-input
              style="width: 200px; margin-right: 8px"
              v-model="conditions.templateNameLike"
              :placeholder="$t('contractTemplate.searchPlaceholder')"
              clearable
              @keyup.enter.native="handleSearch"
            />
            <el-button type="primary" size="small" @click="openEditor()">
              <i class="el-icon-plus"></i>
              {{ $t('contractTemplate.newTemplate') }}
            </el-button>
          </div>
        </div>

        <div class="cc-variable-tip">{{ $t('contractTemplate.variableTip') }}</div>

        <div class="cc-tabs">
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

        <el-row :gutter="16" v-loading="loading">
          <el-col
            v-for="item in templates"
            :key="item.templateId"
            :xs="24"
            :sm="12"
            :md="8"
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

        <div v-if="!loading && !templates.length" class="cc-empty">{{ $t('contractTemplate.empty') }}</div>

        <el-pagination
          :current-page="page.current"
          :page-sizes="[9, 18, 36]"
          :page-size="page.size"
          :total="page.total"
          layout="total, sizes, prev, pager, next"
          style="margin-top: 8px; text-align: right"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </el-tab-pane>

      <el-tab-pane :label="$t('contractTypeManage.list.title')" name="type">
        <div class="cc-toolbar">
          <el-input
            style="width: 200px"
            v-model="typeConditions.typeName"
            :placeholder="$t('contractTypeManage.search.typeNamePlaceholder')"
            clearable
          />
          <el-select
            style="width: 160px"
            v-model="typeConditions.audit"
            :placeholder="$t('contractTypeManage.search.auditPlaceholder')"
            clearable
          >
            <el-option v-for="item in audits" :key="item.statusCd" :label="item.name" :value="item.statusCd" />
          </el-select>
          <el-button type="primary" @click="queryContractTypeMethod">
            <i class="el-icon-search"></i>
            {{ $t('common.search') }}
          </el-button>
          <el-button @click="resetContractTypeMethod">
            <i class="el-icon-refresh"></i>
            {{ $t('common.reset') }}
          </el-button>
          <el-button type="primary" @click="openAddContractType">
            <i class="el-icon-plus"></i>
            {{ $t('common.add') }}
          </el-button>
        </div>

        <el-table :data="contractTypes" border style="width: 100%">
          <el-table-column prop="typeName" :label="$t('contractTypeManage.table.typeName')" align="center" />
          <el-table-column prop="auditName" :label="$t('contractTypeManage.table.audit')" align="center" />
          <el-table-column prop="remark" :label="$t('contractTypeManage.table.remark')" align="center" />
          <el-table-column prop="createTime" :label="$t('contractTypeManage.table.createTime')" align="center" />
          <el-table-column :label="$t('common.operation')" align="center" width="300">
            <template slot-scope="scope">
              <el-button size="mini" @click="openTypeSpec(scope.row)">
                {{ $t('contractTypeManage.table.specInfo') }}
              </el-button>
              <el-button size="mini" @click="openTypeTemplate(scope.row)">
                {{ $t('contractTypeManage.table.template') }}
              </el-button>
              <el-button size="mini" type="primary" @click="openEditContractType(scope.row)">
                {{ $t('common.edit') }}
              </el-button>
              <el-button size="mini" type="danger" @click="openDeleteContractType(scope.row)">
                {{ $t('common.delete') }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          :current-page="typePage.current"
          :page-sizes="[10, 20, 30, 50]"
          :page-size="typePage.size"
          :total="typePage.total"
          layout="total, sizes, prev, pager, next, jumper"
          style="margin-top: 16px; text-align: right"
          @size-change="handleTypeSizeChange"
          @current-change="handleTypeCurrentChange"
        />
      </el-tab-pane>
    </el-tabs>

    <contract-template-editor
      ref="templateEditor"
      :contract-types="contractTypes"
      @success="handleSearch"
    />
    <contract-template-preview ref="templatePreview" />
    <add-template-view ref="addTemplateView" @success="handleSearch" />
    <add-contract-type ref="addContractType" @success="handleSuccess" />
    <edit-contract-type ref="editContractType" @success="handleSuccess" />
    <delete-contract-type ref="deleteContractType" @success="handleSuccess" />
  </div>
</template>

<script>
import ContractTemplateCard from '@/components/contract/contractTemplateCard'
import ContractTemplateEditor from '@/components/contract/contractTemplateEditor'
import ContractTemplatePreview from '@/components/contract/contractTemplatePreview'
import AddTemplateView from '@/components/contract/addTemplateView'
import AddContractType from '@/components/contract/addContractType'
import EditContractType from '@/components/contract/editContractType'
import DeleteContractType from '@/components/contract/deleteContractType'
import { getDict } from '@/api/community/communityApi'
import {
  deleteContractTypeTemplate,
  listContractTemplates,
  queryContractTypeTemplate
} from '@/api/contract/contractTemplateApi'
import { queryContractType } from '@/api/contract/contractTypeManageApi'
import { guessCategory } from '@/utils/contractUtil'

export default {
  name: 'ContractTypeManageList',
  components: {
    ContractTemplateCard,
    ContractTemplateEditor,
    ContractTemplatePreview,
    AddTemplateView,
    AddContractType,
    EditContractType,
    DeleteContractType
  },
  data() {
    return {
      activeTab: 'template',
      loading: false,
      templates: [],
      allTemplates: [],
      contractTypes: [],
      audits: [],
      conditions: {
        templateNameLike: '',
        category: ''
      },
      typeConditions: {
        typeName: '',
        audit: ''
      },
      page: {
        current: 1,
        size: 9,
        total: 0
      },
      typePage: {
        current: 1,
        size: 10,
        total: 0
      }
    }
  },
  computed: {
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
    this.queryContractTypes()
    this.loadAudits()
  },
  methods: {
    countByCategory(category) {
      return this.allTemplates.filter(item => guessCategory({
        category: item.category,
        contractTypeName: item.contractTypeName
      }) === category).length
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
        this.$refs.templateEditor.open(null)
        return
      }
      queryContractTypeTemplate({ templateId: template.templateId, page: 1, row: 1 }).then(list => {
        const detail = (list || [])[0] || template
        this.$refs.templateEditor.open({ ...template, ...detail })
      }).catch(() => {
        this.$refs.templateEditor.open(template)
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
      queryContractTypeTemplate({ templateId: template.templateId, page: 1, row: 1 }).then(list => {
        const detail = (list || [])[0] || template
        this.$refs.templateEditor.open({
          templateId: '',
          templateName: detail.templateName + ' (副本)',
          contractTypeId: detail.contractTypeId,
          category: detail.category,
          templateVersion: detail.templateVersion,
          templateDesc: detail.templateDesc,
          context: detail.context,
          sourceType: detail.sourceType
        })
      }).catch(error => {
        this.$message.error(error.message)
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
    },
    queryContractTypes() {
      queryContractType({
        page: this.typePage.current,
        row: this.typePage.size,
        typeName: this.typeConditions.typeName,
        audit: this.typeConditions.audit
      }).then(res => {
        this.contractTypes = res.data || []
        this.typePage.total = res.total || (res.data || []).length
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractTypeManage.fetchError'))
      })
    },
    loadAudits() {
      getDict('contract_type', 'audit').then(data => {
        this.audits = data || []
      }).catch(() => {
        this.audits = []
      })
    },
    queryContractTypeMethod() {
      this.typePage.current = 1
      this.queryContractTypes()
    },
    resetContractTypeMethod() {
      this.typeConditions = {
        typeName: '',
        audit: ''
      }
      this.queryContractTypeMethod()
    },
    handleTypeSizeChange(size) {
      this.typePage.size = size
      this.queryContractTypes()
    },
    handleTypeCurrentChange(current) {
      this.typePage.current = current
      this.queryContractTypes()
    },
    handleSuccess() {
      this.queryContractTypes()
      this.loadTemplates()
    },
    openAddContractType() {
      this.$refs.addContractType.open()
    },
    openEditContractType(row) {
      this.$refs.editContractType.open(row)
    },
    openDeleteContractType(row) {
      this.$refs.deleteContractType.open(row)
    },
    openTypeSpec(row) {
      this.$router.push(`/views/contract/contractTypeSpecManage?contractTypeId=${row.contractTypeId}`)
    },
    openTypeTemplate(row) {
      this.$refs.addTemplateView.open(row)
    }
  }
}
</script>

<style lang="scss" scoped>
.contract-template-center {
  padding: 16px;

  .cc-page-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 12px;

    .cc-page-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }

    .cc-page-subtitle {
      margin-top: 4px;
      font-size: 12px;
      color: #909399;
    }
  }

  .cc-variable-tip {
    font-size: 12px;
    color: #909399;
    margin-bottom: 10px;
  }

  .cc-tabs {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 16px;

    .cc-tab {
      padding: 8px 16px;
      font-size: 14px;
      color: #606266;
      cursor: pointer;
      border-bottom: 2px solid transparent;

      &.is-active {
        color: #4f46e5;
        border-bottom-color: #4f46e5;
      }

      .cc-tab-count {
        margin-left: 6px;
        font-size: 12px;
        color: #909399;
      }
    }
  }
}
</style>
