<template>
  <div class="contract-template-editor">
    <div class="cc-tpl-head">
      <div class="cc-tpl-title">
        <div class="cc-tpl-title-row">
          <span class="cc-tpl-name">{{ pageTitle }}</span>
          <span class="cc-tpl-badge">{{ $t('contractTemplate.editor.badgeOnline') }}</span>
        </div>
        <p class="cc-tpl-sub">{{ $t('contractTemplate.editor.pageSubtitle') }}</p>
      </div>
      <div class="cc-tpl-actions">
        <el-button size="small" type="primary" :loading="saving" @click="handleSave">
          {{ $t('contractTemplate.editor.save') }}
        </el-button>
        <el-button size="small" class="cc-tpl-return" @click="handleBack">
          {{ $t('contractTemplate.editor.back') }}
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="cc-tpl-body">
      <div class="cc-tpl-main">
        <div class="cc-tpl-panel">
          <div class="cc-tpl-panel-head">
            <span class="cc-tpl-panel-index">01</span>
            <h4 class="cc-tpl-panel-title">{{ $t('contractTemplate.editor.sectionProps') }}</h4>
          </div>
          <div class="cc-tpl-panel-body">
            <el-form ref="form" :model="form" label-position="top" class="cc-tpl-form">
              <el-row :gutter="16">
                <el-col :span="9">
                  <el-form-item :label="$t('contractTemplate.editor.name')" required>
                    <el-input
                      v-model="form.templateName"
                      maxlength="100"
                      :placeholder="$t('contractTemplate.editor.namePlaceholder')"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="9">
                  <el-form-item :label="$t('contractTemplate.editor.category')" required>
                    <el-select v-model="form.category" style="width: 100%">
                      <el-option :label="$t('contractCategory.rent')" value="1" />
                      <el-option :label="$t('contractCategory.purchase')" value="2" />
                      <el-option :label="$t('contractCategory.maintain')" value="3" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="6">
                  <el-form-item :label="$t('contractTemplate.editor.version')">
                    <el-input v-model="form.templateVersion" maxlength="20" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="24">
                  <el-form-item :label="$t('contractTemplate.editor.desc')">
                    <el-input v-model="form.templateDesc" maxlength="200" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>
        </div>

        <div class="cc-tpl-panel">
          <div class="cc-tpl-panel-head">
            <span class="cc-tpl-panel-index">02</span>
            <h4 class="cc-tpl-panel-title">{{ $t('contractTemplate.editor.sectionDoc') }}</h4>
            <span class="cc-tpl-panel-tag">{{ $t('contractTemplate.editor.supportPaste') }}</span>
            <div class="cc-tpl-panel-tools">
              <el-button size="mini" @click="clearDoc">{{ $t('contractTemplate.editor.clearDoc') }}</el-button>
            </div>
          </div>
          <div class="cc-tpl-panel-body">
            <contract-doc-editor
              ref="docEditor"
              v-model="form.context"
              :template-content="form.context"
            >
              <template #toolbar>
                <el-button size="mini" @click="openPaste">
                  <i class="el-icon-document-copy"></i>
                  <span>{{ $t('contractTemplate.editor.pasteWord') }}</span>
                </el-button>
              </template>
            </contract-doc-editor>
          </div>
        </div>
      </div>

      <div class="cc-tpl-side">
        <div class="cc-tpl-panel">
          <div class="cc-tpl-panel-head">
            <h4 class="cc-tpl-panel-title">{{ $t('contractTemplate.editor.variableInsert') }}</h4>
            <span class="cc-tpl-panel-hint">{{ $t('contractTemplate.editor.clickInsert') }}</span>
          </div>
          <div class="cc-tpl-panel-body">
            <div class="cc-var-chips">
              <button
                v-for="item in variables"
                :key="item.key"
                type="button"
                class="cc-var-chip"
                @click="insertVariable(item)"
              >{{ item.label }}</button>
            </div>
            <p class="cc-var-note">{{ $t('contractTemplate.editor.variableNote') }}</p>
          </div>
        </div>

        <div class="cc-tpl-panel">
          <div class="cc-tpl-panel-head">
            <h4 class="cc-tpl-panel-title">{{ $t('contractTemplate.editor.statsTitle') }}</h4>
          </div>
          <div class="cc-tpl-panel-body">
            <div class="cc-stat-line">
              <span>{{ $t('contractTemplate.editor.statChars') }}</span>
              <b>{{ stats.chars }}</b>
            </div>
            <div class="cc-stat-line">
              <span>{{ $t('contractTemplate.editor.statClauses') }}</span>
              <b>{{ stats.clauses }}</b>
            </div>
            <div class="cc-stat-line">
              <span>{{ $t('contractTemplate.editor.statTables') }}</span>
              <b>{{ stats.tables }}</b>
            </div>
            <div class="cc-stat-line">
              <span>{{ $t('contractTemplate.editor.statAttachments') }}</span>
              <b>{{ stats.attachments }}</b>
            </div>
            <div class="cc-stat-line">
              <span>{{ $t('contractTemplate.editor.statVars') }}</span>
              <b>{{ stats.variables }}</b>
            </div>
          </div>
        </div>

        <p class="cc-side-tip">{{ $t('contractTemplate.editor.tipSave') }}</p>
      </div>
    </div>

    <contract-paste-import ref="pasteImport" @generated="handlePasteGenerated" />
  </div>
</template>

<script>
import ContractDocEditor from '@/components/contract/workbench/ContractDocEditor'
import ContractPasteImport from '@/components/contract/contractPasteImport'
import { queryContractTypeTemplate, saveContractTypeTemplate, updateContractTemplate } from '@/api/contract/contractTemplateApi'

export default {
  name: 'ContractTemplateEditorList',
  components: {
    ContractDocEditor,
    ContractPasteImport
  },
  data() {
    return {
      loading: false,
      saving: false,
      form: {
        templateId: '',
        templateName: '',
        category: '2',
        templateVersion: 'V1.0',
        templateDesc: '',
        context: '',
        sourceType: 'online'
      }
    }
  },
  computed: {
    pageTitle() {
      return this.form.templateId
        ? this.$t('contractTemplate.editor.titleEdit')
        : this.$t('contractTemplate.editor.titleCreate')
    },
    variables() {
      return [
        { key: 'amount', label: this.$t('contractWorkbench.doc.varAmount') },
        { key: 'amountText', label: this.$t('contractWorkbench.doc.varAmountText') },
        { key: 'startTime', label: this.$t('contractWorkbench.doc.varStart') },
        { key: 'endTime', label: this.$t('contractWorkbench.doc.varEnd') },
        { key: 'partyB', label: this.$t('contractWorkbench.doc.varPartyB') },
        { key: 'houses', label: this.$t('contractWorkbench.doc.varHouses') }
      ]
    },
    plainText() {
      return String(this.form.context || '').replace(/<[^>]+>/g, '')
    },
    stats() {
      const context = String(this.form.context || '')
      return {
        chars: this.plainText.replace(/\s+/g, '').length,
        clauses: (this.plainText.match(/第[一二三四五六七八九十\d]+条/g) || []).length,
        tables: (context.match(/<table/gi) || []).length,
        attachments: (context.match(/<img/gi) || []).length,
        variables: (context.match(/\{\{\s*[a-zA-Z0-9_]+\s*\}\}/g) || []).length
      }
    }
  },
  created() {
    this.loadTemplate()
  },
  mounted() {
    if (this.$route.query && this.$route.query.paste === '1') {
      this.openPaste()
    }
  },
  methods: {
    loadTemplate() {
      const query = this.$route.query || {}
      const templateId = query.templateId || query.copyFrom || ''
      if (!templateId) {
        return
      }
      this.loading = true
      queryContractTypeTemplate({ templateId: templateId, page: 1, row: 1 }).then(list => {
        const detail = (list || [])[0]
        if (!detail) {
          return
        }
        this.form = {
          templateId: query.copyFrom ? '' : detail.templateId,
          templateName: query.copyFrom
            ? detail.templateName + this.$t('contractTemplate.copySuffix')
            : detail.templateName,
          category: detail.category ? String(detail.category) : '2',
          templateVersion: detail.templateVersion || 'V1.0',
          templateDesc: detail.templateDesc || '',
          context: detail.context || '',
          sourceType: detail.sourceType || 'online'
        }
      }).catch(error => {
        this.$message.error(error.message)
      }).finally(() => {
        this.loading = false
      })
    },
    insertVariable(item) {
      if (this.$refs.docEditor) {
        this.$refs.docEditor.insertVariable(item.key)
        this.form.context = this.$refs.docEditor.getContent()
        return
      }
      this.form.context = (this.form.context || '') + '<span class="cc-var">{{' + item.key + '}}</span>'
    },
    clearDoc() {
      this.$confirm(this.$t('contractTemplate.editor.clearConfirm'), '提示', { type: 'warning' }).then(() => {
        this.form.context = ''
        if (this.$refs.docEditor) {
          this.$refs.docEditor.setContent('')
        }
      }).catch(() => {
      })
    },
    openPaste() {
      this.$refs.pasteImport.open()
    },
    handlePasteGenerated(html) {
      this.form.context = html
      this.form.sourceType = 'paste'
      if (this.$refs.docEditor) {
        this.$refs.docEditor.setContent(html)
      }
    },
    handleBack() {
      this.$router.back()
    },
    handleSave() {
      if (!this.form.templateName) {
        this.$message.error(this.$t('contractTemplate.editor.nameRequired'))
        return
      }
      if (!this.form.category) {
        this.$message.error(this.$t('contractTemplate.editor.typeRequired'))
        return
      }
      if (!this.form.context) {
        this.$message.error(this.$t('contractTemplate.editor.contentRequired'))
        return
      }
      const data = {
        templateId: this.form.templateId,
        templateName: this.form.templateName,
        category: this.form.category,
        templateVersion: this.form.templateVersion,
        templateDesc: this.form.templateDesc,
        context: this.form.context,
        sourceType: this.form.sourceType
      }
      this.saving = true
      const request = this.form.templateId ? updateContractTemplate(data) : saveContractTypeTemplate(data)
      request.then(() => {
        this.$message.success(this.$t('contractTemplate.editor.saveSuccess'))
        this.$router.push({ path: '/pages/admin/contractTypeManage' })
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractTemplate.editor.saveError'))
      }).finally(() => {
        this.saving = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
$brand: #409eff;

.contract-template-editor {
  padding: 16px 24px 24px;
  background: #f7f8fa;
  min-height: calc(100vh - 96px);
  box-sizing: border-box;
  text-align: left;

  .cc-tpl-head {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding-bottom: 14px;
  }

  .cc-tpl-title {
    flex: 1;
    min-width: 0;
  }

  .cc-tpl-title-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .cc-tpl-name {
    font-size: 20px;
    font-weight: 700;
    line-height: 28px;
    color: #1f2937;
  }

  .cc-tpl-badge {
    height: 20px;
    line-height: 20px;
    padding: 0 6px;
    border-radius: 5px;
    background: #ecf5ff;
    color: $brand;
    font-size: 10px;
    font-weight: 600;
  }

  .cc-tpl-sub {
    margin: 2px 0 0;
    font-size: 12px;
    line-height: 18px;
    color: #9ca3af;
  }

  .cc-tpl-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: none;

    .el-button {
      height: 34px;
      border-radius: 8px;
      font-size: 13px;
    }

    .el-button--primary {
      background: #409eff;
      border-color: #409eff;
      border-radius: 10px;
      box-shadow: 0 6px 14px rgba(64, 158, 255, .18);

      &:hover,
      &:focus {
        background: #66b1ff;
        border-color: #66b1ff;
      }
    }

    .cc-tpl-return {
      color: #409eff;
      background: #ecf5ff;
      border-color: #d9ecff;

      &:hover,
      &:focus {
        color: #409eff;
        background: #d9ecff;
        border-color: #409eff;
      }
    }
  }

  .cc-tpl-body {
    display: flex;
    align-items: flex-start;
  }

  .cc-tpl-main {
    flex: 1;
    min-width: 0;
  }

  .cc-tpl-side {
    width: 300px;
    flex: none;
    margin-left: 16px;
    position: sticky;
    top: 76px;
  }

  .cc-tpl-panel {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 12px;
    margin-bottom: 16px;
    box-shadow: 0 1px 2px rgba(15, 23, 42, .04);
  }

  .cc-tpl-panel-head {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 18px;
    border-bottom: 1px solid #f2f3f5;
  }

  .cc-tpl-panel-index {
    width: 20px;
    height: 20px;
    flex: none;
    border-radius: 6px;
    background: #ecf5ff;
    color: $brand;
    font-size: 10px;
    font-weight: 700;
    line-height: 20px;
    text-align: center;
  }

  .cc-tpl-panel-title {
    margin: 0;
    font-size: 13.5px;
    font-weight: 600;
    line-height: 20px;
    color: #303133;
  }

  .cc-tpl-panel-tag {
    height: 18px;
    line-height: 18px;
    padding: 0 6px;
    border-radius: 4px;
    background: #ecfdf5;
    color: #059669;
    font-size: 10px;
  }

  .cc-tpl-panel-hint {
    margin-left: auto;
    font-size: 11px;
    color: #9ca3af;
  }

  .cc-tpl-panel-tools {
    margin-left: auto;
  }

  .cc-tpl-panel-body {
    padding: 18px;
  }

  .cc-tpl-form ::v-deep .el-form-item {
    margin-bottom: 14px;
  }

  .cc-tpl-form ::v-deep .el-form-item__label {
    padding-bottom: 4px;
    font-size: 12px;
    line-height: 18px;
    font-weight: 600;
    color: #64748b;
  }

  .cc-tpl-form ::v-deep .el-form-item.is-required .el-form-item__label::before {
    display: none;
  }

  .cc-tpl-form ::v-deep .el-form-item.is-required .el-form-item__label::after {
    content: '*';
    margin-left: 4px;
    color: #f43f5e;
  }

  .cc-tpl-form ::v-deep .el-input__inner {
    height: 36px;
    line-height: 36px;
    border-radius: 8px;
    border-color: #e2e8f0;
    font-size: 13px;
  }

  .cc-var-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .cc-var-chip {
    height: 26px;
    padding: 0 10px;
    border: 1px solid #d9ecff;
    border-radius: 8px;
    background: #ecf5ff;
    color: #409eff;
    font-size: 12px;
    cursor: pointer;
    transition: all .2s;

    &:hover {
      border-color: $brand;
      background: #d9ecff;
    }
  }

  .cc-var-note {
    margin: 12px 0 0;
    font-size: 11px;
    line-height: 18px;
    color: #9ca3af;
  }

  .cc-stat-line {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 12px;
    line-height: 28px;
    color: #64748b;

    b {
      color: #303133;
      font-weight: 600;
    }
  }

  .cc-side-tip {
    margin: 0;
    padding: 12px 14px;
    border: 1px solid #fde68a;
    border-radius: 12px;
    background: #fffbeb;
    font-size: 11px;
    line-height: 18px;
    color: #a16207;
  }

  @media (max-width: 1280px) {
    .cc-tpl-body {
      display: block;
    }

    .cc-tpl-side {
      width: auto;
      margin: 0;
      position: static;
    }
  }
}
</style>
