<template>
  <el-dialog
    :title="$t('contractPartyaCenter.form.titleEdit')"
    :visible.sync="visible"
    width="50%"
    @close="handleClose"
  >
    <el-form ref="form" :model="formData" :rules="rules" label-width="130px">
      <el-form-item
        :label="$t('contractPartyaCenter.fullName')"
        prop="partyA"
      >
        <el-input
          v-model="formData.partyA"
          :placeholder="$t('contractPartyaCenter.form.fullNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('contractPartyaCenter.shortName')">
        <el-input v-model="formData.shortName" maxlength="50" />
      </el-form-item>
      <el-form-item :label="$t('contractPartyaCenter.creditCode')" prop="creditCode">
        <el-input
          v-model="formData.creditCode"
          maxlength="18"
          :placeholder="$t('contractPartyaCenter.form.creditCodePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('contractPartyaCenter.legalPerson')">
        <el-input v-model="formData.legalPerson" maxlength="50" />
      </el-form-item>
      <el-form-item
        :label="$t('contractPartyaCenter.contacts')"
        prop="aContacts"
      >
        <el-input
          v-model="formData.aContacts"
          :placeholder="$t('contractPartyaManage.form.aContactsPlaceholder')"
        />
      </el-form-item>
      <el-form-item
        :label="$t('contractPartyaCenter.tel')"
        prop="aLink"
      >
        <el-input
          v-model="formData.aLink"
          :placeholder="$t('contractPartyaManage.form.aLinkPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('contractPartyaCenter.bankName')">
        <el-input v-model="formData.bankName" maxlength="100" />
      </el-form-item>
      <el-form-item :label="$t('contractPartyaCenter.bankAccount')">
        <el-input
          v-model="formData.bankAccount"
          maxlength="50"
          :placeholder="$t('contractPartyaCenter.form.bankAccountPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('contractPartyaCenter.seal')">
        <el-switch
          v-model="formData.sealFlag"
          active-value="1"
          inactive-value="0"
        />
        <span class="cc-form-tip">{{ $t('contractPartyaCenter.form.sealTip') }}</span>
      </el-form-item>
      <el-form-item :label="$t('contractPartyaCenter.default')">
        <el-switch
          v-model="formData.defaultFlag"
          active-value="1"
          inactive-value="0"
        />
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="visible = false">
        {{ $t('common.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleSubmit">
        {{ $t('common.confirm') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { updateContractPartya } from '@/api/contract/contractPartyaManageApi'
import { getCommunityId } from '@/api/community/communityApi'

export default {
  name: 'EditContractPartya',
  data() {
    return {
      visible: false,
      formData: {
        partyaId: '',
        partyA: '',
        shortName: '',
        creditCode: '',
        legalPerson: '',
        aContacts: '',
        aLink: '',
        bankName: '',
        bankAccount: '',
        sealFlag: '0',
        defaultFlag: '0',
        communityId: ''
      },
      rules: {
        partyA: [
          {
            required: true,
            message: this.$t('contractPartyaCenter.validate.fullNameRequired'),
            trigger: 'blur'
          },
          {
            max: 200,
            message: this.$t('contractPartyaManage.validate.partyAMaxLength'),
            trigger: 'blur'
          }
        ],
        creditCode: [
          {
            pattern: /^[0-9A-HJ-NPQRTUWXY]{18}$/,
            message: this.$t('contractPartyaCenter.validate.creditCodeFormat'),
            trigger: 'blur'
          }
        ],
        aContacts: [
          {
            required: true,
            message: this.$t('contractPartyaManage.validate.aContactsRequired'),
            trigger: 'blur'
          },
          {
            max: 64,
            message: this.$t('contractPartyaManage.validate.aContactsMaxLength'),
            trigger: 'blur'
          }
        ],
        aLink: [
          {
            required: true,
            message: this.$t('contractPartyaManage.validate.aLinkRequired'),
            trigger: 'blur'
          },
          {
            pattern: /^1[3-9]\d{9}$/,
            message: this.$t('contractPartyaCenter.validate.telFormat'),
            trigger: 'blur'
          }
        ],
        partyaId: [
          {
            required: true,
            message: this.$t('contractPartyaManage.validate.partyaIdRequired'),
            trigger: 'blur'
          }
        ]
      }
    }
  },
  methods: {
    open(data) {
      this.visible = true
      this.formData = {
        partyaId: data.partyaId,
        partyA: data.partyA || '',
        shortName: data.shortName || '',
        creditCode: data.creditCode || '',
        legalPerson: data.legalPerson || '',
        aContacts: data.aContacts || '',
        aLink: data.aLink || '',
        bankName: data.bankName || '',
        bankAccount: data.bankAccount || '',
        sealFlag: String(data.sealFlag) === '1' ? '1' : '0',
        defaultFlag: String(data.defaultFlag) === '1' ? '1' : '0',
        communityId: getCommunityId()
      }
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    handleClose() {
      if (this.$refs.form) {
        this.$refs.form.clearValidate()
      }
    },
    handleSubmit() {
      this.$refs.form.validate(async valid => {
        if (valid) {
          try {
            await updateContractPartya(this.formData)
            this.$message.success(this.$t('contractPartyaCenter.msg.saveSuccess'))
            this.visible = false
            this.$emit('success')
          } catch (error) {
            this.$message.error(error.message || this.$t('contractPartyaCenter.msg.saveError'))
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}
</style>
