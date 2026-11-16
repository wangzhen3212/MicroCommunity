<template>
  <div class="cc-file-panel">
    <el-upload
      drag
      action="#"
      :show-file-list="false"
      :http-request="handleUpload"
      :before-upload="beforeUpload"
      :disabled="readonly"
      accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
    >
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">{{ $t('contractWorkbench.file.choose') }}</div>
      <div slot="tip" class="el-upload__tip">{{ $t('contractWorkbench.file.tip') }}</div>
    </el-upload>

    <div v-if="files.length" class="cc-file-list">
      <div v-for="(file, index) in files" :key="'file' + index" class="cc-file-row">
        <i class="el-icon-paperclip"></i>
        <span class="cc-file-name">{{ file.fileRealName }}</span>
        <el-button
          v-if="!readonly"
          type="text"
          size="mini"
          @click="handleRemove(index)"
        >{{ $t('contractWorkbench.file.remove') }}</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { uploadFile } from '@/api/system/paymentPoolApi'

const MAX_SIZE = 20 * 1024 * 1024

export default {
  name: 'ContractFilePanel',
  props: {
    files: {
      type: Array,
      default: function () {
        return []
      }
    },
    readonly: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    beforeUpload(file) {
      if (file.size > MAX_SIZE) {
        this.$message.error(this.$t('contractWorkbench.file.limit'))
        return false
      }
      return true
    },
    handleUpload(options) {
      const file = options.file
      const formData = new FormData()
      formData.append('uploadFile', file)
      uploadFile(formData, {}).then(res => {
        const realFileName = res && (res.realFileName || (res.data && res.data.realFileName))
        if (!realFileName) {
          this.$message.error(this.$t('contractWorkbench.file.uploadError'))
          return
        }
        this.files.push({
          seq: this.files.length,
          fileRealName: file.name,
          fileSaveName: realFileName
        })
        this.$emit('change', this.files)
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractWorkbench.file.uploadError'))
      })
    },
    handleRemove(index) {
      this.files.splice(index, 1)
      this.$emit('change', this.files)
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-file-panel {
  .cc-file-list {
    margin-top: 12px;
  }

  .cc-file-row {
    display: flex;
    align-items: center;
    font-size: 13px;
    line-height: 26px;

    .cc-file-name {
      margin-left: 6px;
    }

    .el-button {
      margin-left: auto;
    }
  }
}
</style>
