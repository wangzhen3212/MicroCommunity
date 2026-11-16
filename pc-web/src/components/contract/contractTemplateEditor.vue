<template>
  <el-dialog :title="title" :visible.sync="visible" width="960px" @close="handleClose">
    <el-form label-width="110px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item :label="$t('contractTemplate.editor.name')" required>
            <el-input v-model="form.templateName" maxlength="100" :placeholder="$t('contractTemplate.editor.namePlaceholder')" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('contractTemplate.editor.contractType')" required>
            <el-select v-model="form.contractTypeId" style="width: 100%" @change="handleTypeChange">
              <el-option
                v-for="item in contractTypes"
                :key="item.contractTypeId"
                :label="item.typeName"
                :value="item.contractTypeId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('contractTemplate.editor.version')">
            <el-input v-model="form.templateVersion" maxlength="20" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="16">
          <el-form-item :label="$t('contractTemplate.editor.desc')">
            <el-input v-model="form.templateDesc" maxlength="200" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item :label="$t('contractTemplate.editor.category')">
            <el-select v-model="form.category" style="width: 100%" clearable>
              <el-option :label="$t('contractCategory.rent')" :value="'1'" />
              <el-option :label="$t('contractCategory.purchase')" :value="'2'" />
              <el-option :label="$t('contractCategory.maintain')" :value="'3'" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="cc-tpl-editor">
      <div class="cc-tpl-editor-main">
        <div class="cc-tpl-editor-toolbar">
          <el-button size="mini" @click="openPaste">{{ $t('contractTemplate.editor.paste.title') }}</el-button>
          <el-button size="mini" @click="validateVariables">{{ $t('contractTemplate.editor.validate') }}</el-button>
          <el-button size="mini" @click="previewTemplate">{{ $t('contractTemplate.editor.preview') }}</el-button>
        </div>
        <contract-doc-editor
          v-model="form.context"
          :template-content="form.context"
        />
      </div>
      <div class="cc-tpl-editor-side">
        <div class="cc-section-title">{{ $t('contractTemplate.editor.variablePanel') }}</div>
        <div class="cc-variable-tip">{{ $t('contractTemplate.variableTip') }}</div>
        <div class="cc-variable-list">
          <el-tag
            v-for="item in variables"
            :key="item.key"
            size="small"
            @click="insertVariable(item)"
          >{{ item.label }}</el-tag>
        </div>
      </div>
    </div>

    <contract-paste-import ref="pasteImport" @generated="handlePasteGenerated" />
    <contract-template-preview ref="templatePreview" />

    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">
        {{ $t('common.submit') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import ContractDocEditor from '@/components/contract/workbench/ContractDocEditor'
import ContractPasteImport from '@/components/contract/contractPasteImport'
import ContractTemplatePreview from '@/components/contract/contractTemplatePreview'
import { saveContractTypeTemplate, updateContractTemplate } from '@/api/contract/contractTemplateApi'

export default {
  name: 'ContractTemplateEditor',
  components: {
    ContractDocEditor,
    ContractPasteImport,
    ContractTemplatePreview
  },
  props: {
    contractTypes: {
      type: Array,
      default: function () {
        return []
      }
    }
  },
  data() {
    return {
      visible: false,
      saving: false,
      form: {
        templateId: '',
        templateName: '',
        contractTypeId: '',
        category: '',
        templateVersion: 'V1.0',
        templateDesc: '',
        context: '',
        sourceType: 'online'
      }
    }
  },
  computed: {
    title() {
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
    }
  },
  methods: {
    open(template) {
      const source = template || {}
      this.form = {
        templateId: source.templateId || '',
        templateName: source.templateName || '',
        contractTypeId: source.contractTypeId || '',
        category: source.category ? String(source.category) : '',
        templateVersion: source.templateVersion || 'V1.0',
        templateDesc: source.templateDesc || '',
        context: source.context || '',
        sourceType: source.sourceType || 'online'
      }
      this.visible = true
    },
    handleClose() {
      this.visible = false
    },
    handleTypeChange(contractTypeId) {
      const matched = this.contractTypes.filter(item => item.contractTypeId === contractTypeId)[0]
      if (matched && matched.category) {
        this.form.category = String(matched.category)
      }
    },
    insertVariable(item) {
      this.form.context = (this.form.context || '') + '<span class="cc-var">{{' + item.key + '}}</span>'
    },
    openPaste() {
      this.$refs.pasteImport.open()
    },
    handlePasteGenerated(html) {
      this.form.context = html
      this.form.sourceType = 'paste'
    },
    validateVariables() {
      const matched = (this.form.context || '').match(/\{\{\s*[a-zA-Z0-9_]+\s*}}/g) || []
      const known = this.variables.map(item => '{{' + item.key + '}}')
      const missing = matched.filter(item => known.indexOf(item) < 0)
      if (missing.length < 1) {
        this.$message.success(this.$t('contractTemplate.variableTip'))
        return
      }
      this.$message.warning(this.$t('contractTemplate.editor.missingVars', { vars: missing.join('、') }))
    },
    previewTemplate() {
      this.$refs.templatePreview.open(this.form.context, this.form.templateName)
    },
    handleSave() {
      if (!this.form.templateName) {
        this.$message.error(this.$t('contractTemplate.editor.nameRequired'))
        return
      }
      if (!this.form.contractTypeId) {
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
        contractTypeId: this.form.contractTypeId,
        templateVersion: this.form.templateVersion,
        templateDesc: this.form.templateDesc,
        context: this.form.context,
        sourceType: this.form.sourceType
      }
      this.saving = true
      const request = this.form.templateId ? updateContractTemplate(data) : saveContractTypeTemplate(data)
      request.then(() => {
        this.$message.success(this.$t('contractTemplate.editor.saveSuccess'))
        this.$emit('success')
        this.handleClose()
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
.cc-tpl-editor {
  display: flex;

  .cc-tpl-editor-main {
    flex: 1;
    min-width: 0;
  }

  .cc-tpl-editor-toolbar {
    margin-bottom: 8px;
  }

  .cc-tpl-editor-side {
    width: 220px;
    margin-left: 16px;
    padding-left: 16px;
    border-left: 1px solid #ebeef5;

    .cc-variable-tip {
      font-size: 12px;
      line-height: 1.7;
      color: #909399;
      margin-bottom: 10px;
    }

    .cc-variable-list {
      .el-tag {
        margin: 0 6px 8px 0;
        cursor: pointer;
      }
    }
  }
}
</style>
