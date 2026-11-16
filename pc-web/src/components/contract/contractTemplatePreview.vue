<template>
  <el-dialog :title="title" :visible.sync="visible" width="760px" @close="handleClose">
    <div class="cc-tpl-preview">
      <div class="cc-tpl-doc" v-html="safeContent"></div>
    </div>
    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.close') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { sanitizeHtml } from '@/utils/contractUtil'

export default {
  name: 'ContractTemplatePreview',
  data() {
    return {
      visible: false,
      title: '',
      content: ''
    }
  },
  computed: {
    safeContent() {
      return sanitizeHtml(this.content)
    }
  },
  methods: {
    open(content, title) {
      this.content = content || ''
      this.title = title || this.$t('contractTemplate.preview')
      this.visible = true
    },
    handleClose() {
      this.visible = false
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-tpl-preview {
  max-height: 560px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px;

  .cc-tpl-doc {
    font-size: 13px;
    line-height: 1.9;
    color: #303133;

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
}
</style>
