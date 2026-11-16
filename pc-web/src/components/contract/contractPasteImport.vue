<template>
  <el-dialog
    :title="$t('contractTemplate.editor.paste.title')"
    :visible.sync="visible"
    width="720px"
    @close="handleClose"
  >
    <div
      ref="pasteArea"
      class="cc-paste-area"
      contenteditable="true"
      :data-placeholder="$t('contractTemplate.editor.paste.placeholder')"
    ></div>
    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" @click="handleGenerate">
        {{ $t('contractTemplate.editor.paste.clean') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { sanitizeHtml } from '@/utils/contractUtil'

export default {
  name: 'ContractPasteImport',
  data() {
    return {
      visible: false
    }
  },
  methods: {
    open() {
      this.visible = true
      this.$nextTick(() => {
        if (this.$refs.pasteArea) {
          this.$refs.pasteArea.innerHTML = ''
        }
      })
    },
    handleClose() {
      this.visible = false
    },
    handleGenerate() {
      const html = this.$refs.pasteArea ? this.$refs.pasteArea.innerHTML : ''
      const content = sanitizeHtml(html)
      if (!content || !content.replace(/<[^>]+>/g, '').trim()) {
        this.$message.error(this.$t('contractTemplate.editor.paste.empty'))
        return
      }
      this.$message.success(this.$t('contractTemplate.editor.paste.success'))
      this.$emit('generated', content)
      this.handleClose()
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-paste-area {
  min-height: 280px;
  max-height: 420px;
  overflow-y: auto;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  padding: 12px;
  font-size: 13px;
  line-height: 1.8;

  &:empty:before {
    content: attr(data-placeholder);
    color: #c0c4cc;
  }

  table {
    border-collapse: collapse;
    width: 100%;

    td,
    th {
      border: 1px solid #ebeef5;
      padding: 4px 8px;
    }
  }
}
</style>
