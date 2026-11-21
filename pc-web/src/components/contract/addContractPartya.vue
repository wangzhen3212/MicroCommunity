<template>
  <el-dialog
    :visible.sync="visible"
    width="620px"
    custom-class="cc-partya-dialog"
    @close="handleClose"
  >
    <div slot="title" class="cc-dlg-head">
      <div class="cc-dlg-title">{{ $t('contractPartyaCenter.form.titleAdd') }}</div>
      <p class="cc-dlg-sub">{{ $t('contractPartyaCenter.form.subtitle') }}</p>
    </div>
    <el-form ref="form" :model="formData" :rules="rules" label-position="top" class="cc-partya-form">
      <el-form-item :label="$t('contractPartyaCenter.fullName')" prop="partyA">
        <el-input
          v-model="formData.partyA"
          maxlength="200"
          :placeholder="$t('contractPartyaCenter.form.fullNamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="$t('contractPartyaCenter.creditCode')" prop="creditCode">
        <el-input
          v-model="formData.creditCode"
          maxlength="18"
          :placeholder="$t('contractPartyaCenter.form.creditCodePlaceholder')"
        />
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="$t('contractPartyaCenter.shortName')">
            <el-input v-model="formData.shortName" maxlength="50" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('contractPartyaCenter.legalPerson')">
            <el-input v-model="formData.legalPerson" maxlength="50" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="$t('contractPartyaCenter.contacts')" prop="aContacts">
            <el-input
              v-model="formData.aContacts"
              maxlength="64"
              :placeholder="$t('contractPartyaManage.form.aContactsPlaceholder')"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('contractPartyaCenter.tel')" prop="aLink">
            <el-input
              v-model="formData.aLink"
              maxlength="11"
              :placeholder="$t('contractPartyaManage.form.aLinkPlaceholder')"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item :label="$t('contractPartyaCenter.bankName')">
            <el-input v-model="formData.bankName" maxlength="100" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="$t('contractPartyaCenter.bankAccount')">
            <el-input
              v-model="formData.bankAccount"
              maxlength="50"
              :placeholder="$t('contractPartyaCenter.form.bankAccountPlaceholder')"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="visible = false">
        {{ $t('common.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleSubmit">
        {{ $t('contractPartyaCenter.form.submit') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { saveContractPartya } from '@/api/contract/contractPartyaManageApi'
import { getCommunityId } from '@/api/community/communityApi'

export default {
  name: 'AddContractPartya',
  data() {
    return {
      visible: false,
      formData: {
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
        ]
      }
    }
  },
  methods: {
    open() {
      this.resetForm()
      this.visible = true
      this.formData.communityId = getCommunityId()
    },
    resetForm() {
      this.formData = {
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
        communityId: getCommunityId()
      }
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate()
        }
      })
    },
    handleClose() {
      this.resetForm()
    },
    handleSubmit() {
      this.$refs.form.validate(async valid => {
        if (valid) {
          try {
            await saveContractPartya(this.formData)
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
.cc-dlg-head {
  padding-right: 24px;
}

.cc-dlg-title {
  font-size: 16px;
  font-weight: 700;
  line-height: 24px;
  color: #1f2937;
}

.cc-dlg-sub {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 18px;
  font-weight: 400;
  color: #9ca3af;
}

.cc-partya-form ::v-deep .el-form-item {
  margin-bottom: 16px;
}

.cc-partya-form ::v-deep .el-form-item__label {
  padding-bottom: 6px;
  font-size: 12px;
  line-height: 18px;
  font-weight: 600;
  color: #64748b;
}

.cc-partya-form ::v-deep .el-form-item.is-required .el-form-item__label::before {
  display: none;
}

.cc-partya-form ::v-deep .el-form-item.is-required .el-form-item__label::after {
  content: '*';
  margin-left: 4px;
  color: #f43f5e;
}
</style>
