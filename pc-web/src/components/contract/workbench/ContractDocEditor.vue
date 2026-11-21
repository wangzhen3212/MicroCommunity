<template>
  <div class="cc-doc-editor">
    <div class="cc-doc-toolbar">
      <el-button size="mini" :disabled="readonly" @click="execCommand('bold')">{{ $t('contractWorkbench.doc.bold') }}</el-button>
      <el-button size="mini" :disabled="readonly" @click="execCommand('italic')">{{ $t('contractWorkbench.doc.italic') }}</el-button>
      <el-button size="mini" :disabled="readonly" @click="execCommand('underline')">{{ $t('contractWorkbench.doc.underline') }}</el-button>
      <el-dropdown size="mini" :disabled="readonly" @command="insertClause">
        <el-button size="mini">
          {{ $t('contractWorkbench.doc.insertClause') }}<i class="el-icon-arrow-down el-icon--right"></i>
        </el-button>
        <el-dropdown-menu slot="dropdown">
          <el-dropdown-item command="maintain">{{ $t('contractWorkbench.doc.clauseMaintain') }}</el-dropdown-item>
          <el-dropdown-item command="sublease">{{ $t('contractWorkbench.doc.clauseSublease') }}</el-dropdown-item>
          <el-dropdown-item command="penalty">{{ $t('contractWorkbench.doc.clausePenalty') }}</el-dropdown-item>
          <el-dropdown-item command="special">{{ $t('contractWorkbench.doc.clauseSpecial') }}</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
      <el-dropdown v-if="showVariable" size="mini" :disabled="readonly" @command="insertVariable">
        <el-button size="mini">
          {{ $t('contractWorkbench.doc.insertVariable') }}<i class="el-icon-arrow-down el-icon--right"></i>
        </el-button>
        <el-dropdown-menu slot="dropdown" class="cc-var-dropdown">
          <template v-for="group in variableGroups">
            <li :key="group.key" class="cc-var-group-title">{{ group.label }}</li>
            <el-dropdown-item
              v-for="item in group.items"
              :key="item.key"
              :command="item.key"
            >{{ item.label }}</el-dropdown-item>
          </template>
        </el-dropdown-menu>
      </el-dropdown>
      <slot name="toolbar"></slot>
      <el-button v-if="templateContent" size="mini" :disabled="readonly" @click="resetToTemplate">
        {{ $t('contractWorkbench.doc.reset') }}
      </el-button>
    </div>
    <div ref="editor" class="cc-doc-body"></div>
  </div>
</template>

<script>
import E from 'wangeditor'
import { buildVariableGroups } from '@/utils/contractDocVariables'

const CLAUSES = {
  maintain: '第 X 条 维修与费用分摊：租赁期间房屋主体结构及公共设施的维修由甲方负责；因乙方使用不当造成的损坏，维修费用由乙方承担。',
  sublease: '第 X 条 禁止转租：未经甲方书面同意，乙方不得将房屋全部或部分转租、转借他人使用。',
  penalty: '第 X 条 违约金：任何一方违反本合同约定，应向守约方支付合同金额 20% 的违约金，并赔偿由此造成的实际损失。',
  special: '第 X 条 特别约定：双方另行约定的其他事项，作为本合同附件，与本合同具有同等法律效力。'
}

export default {
  name: 'ContractDocEditor',
  props: {
    value: {
      type: String,
      default: ''
    },
    templateContent: {
      type: String,
      default: ''
    },
    variables: {
      type: Object,
      default: function () {
        return {}
      }
    },
    readonly: {
      type: Boolean,
      default: false
    },
    showVariable: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      editor: null,
      innerValue: ''
    }
  },
  computed: {
    variableGroups() {
      return buildVariableGroups(key => this.$t(key))
    }
  },
  watch: {
    value(val) {
      if (val === this.innerValue) {
        return
      }
      this.setContent(val)
    },
    readonly(val) {
      if (this.editor) {
        this.editor.enableReadOnly = val
        if (val) {
          this.editor.disable()
          return
        }
        this.editor.enable()
      }
    }
  },
  mounted() {
    this.initEditor()
  },
  beforeDestroy() {
    if (this.editor) {
      this.editor.destroy()
      this.editor = null
    }
  },
  methods: {
    initEditor() {
      this.editor = new E(this.$refs.editor)
      this.editor.config.zIndex = 100
      this.editor.config.menus = ['list', 'justify', 'table', 'undo', 'redo']
      this.editor.config.placeholder = this.$t('contractWorkbench.doc.placeholder')
      this.editor.config.onchange = html => {
        this.innerValue = html
        this.$emit('input', html)
      }
      this.editor.create()
      if (this.value) {
        this.setContent(this.value)
      }
      if (this.readonly) {
        this.editor.disable()
      }
    },
    setContent(html) {
      if (!this.editor) {
        return
      }
      this.innerValue = html || ''
      this.editor.txt.html(html || '')
    },
    getContent() {
      return this.editor ? this.editor.txt.html() : this.innerValue
    },
    execCommand(command) {
      if (!this.editor || this.readonly) {
        return
      }
      this.editor.cmd.do(command)
      this.$emit('input', this.editor.txt.html())
    },
    insertClause(type) {
      if (!this.editor || this.readonly) {
        return
      }
      this.editor.cmd.do('insertHTML', '<p>' + CLAUSES[type] + '</p>')
      this.$emit('input', this.editor.txt.html())
    },
    insertVariable(key) {
      if (!this.editor || this.readonly) {
        return
      }
      const html = '<span class="cc-var">{{' + key + '}}</span>'
      this.editor.cmd.do('insertHTML', html)
      this.$emit('input', this.editor.txt.html())
    },
    resetToTemplate() {
      this.$confirm(this.$t('contractWorkbench.doc.resetConfirm'), '提示', {
        type: 'warning'
      }).then(() => {
        this.setContent(this.templateContent)
        this.$emit('input', this.templateContent)
      }).catch(() => {
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-doc-editor {
  .cc-doc-toolbar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    margin-bottom: 8px;

    .el-button,
    .el-dropdown {
      margin-right: 8px;
      margin-left: 0;
    }
  }

  .cc-var-dropdown {
    max-height: 320px;
    overflow-y: auto;

    .cc-var-group-title {
      padding: 6px 20px 2px;
      font-size: 11px;
      line-height: 16px;
      color: #9ca3af;
      list-style: none;
    }
  }

  .cc-doc-body {
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    min-height: 320px;
    overflow: hidden;
  }
}
</style>
