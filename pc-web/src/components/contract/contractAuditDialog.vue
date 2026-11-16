<template>
  <el-dialog
    :title="$t('contractAuditCenter.dialog.title')"
    :visible.sync="visible"
    width="520px"
    @close="handleClose"
  >
    <el-form :model="form" ref="formRef" label-width="90px">
      <el-form-item :label="$t('contractAuditCenter.table.contract')">
        <span>{{ audit.bizCode }} {{ audit.bizName }}</span>
      </el-form-item>
      <el-form-item :label="$t('contractAuditCenter.dialog.opinion')" prop="opinion">
        <el-input
          type="textarea"
          :rows="4"
          maxlength="200"
          show-word-limit
          v-model="form.opinion"
          :placeholder="$t('contractAuditCenter.dialog.opinionPlaceholder')"
        />
      </el-form-item>
    </el-form>
    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="danger" :loading="loading" @click="handleSubmit('2')">
        {{ $t('contractAuditCenter.dialog.reject') }}
      </el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit('1')">
        {{ $t('contractAuditCenter.dialog.pass') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { auditContract } from '@/api/contract/contractAuditApi'

export default {
  name: 'ContractAuditDialog',
  data() {
    return {
      visible: false,
      loading: false,
      audit: {},
      form: {
        opinion: ''
      }
    }
  },
  methods: {
    open(item) {
      this.audit = (item && item.audit) || {}
      this.form.opinion = ''
      this.visible = true
    },
    handleClose() {
      this.visible = false
      this.form.opinion = ''
    },
    handleSubmit(result) {
      const opinion = (this.form.opinion || '').trim()
      if (result === '2' && !opinion) {
        this.$message.error(this.$t('contractAuditCenter.dialog.opinionRequired'))
        return
      }
      if (opinion.length > 200) {
        this.$message.error(this.$t('contractAuditCenter.dialog.opinionMax'))
        return
      }
      this.loading = true
      auditContract({
        auditId: this.audit.auditId,
        result: result,
        opinion: opinion
      }).then(() => {
        this.$message.success(this.$t('contractAuditCenter.dialog.success'))
        this.loading = false
        this.$emit('success')
        this.handleClose()
      }).catch(error => {
        this.loading = false
        this.$message.error(error.message || this.$t('contractAuditCenter.dialog.error'))
      })
    }
  }
}
</script>
