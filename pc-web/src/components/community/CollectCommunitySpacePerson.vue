<template>
  <el-dialog
    :title="$t('communitySpacePersonManage.collectTitle')"
    :visible.sync="visible"
    width="480px"
    @close="handleClose"
  >
    <div class="order-summary">
      <div class="summary-row">
        <span class="summary-label">{{ $t('communitySpacePersonManage.orderId') }}</span>
        <span class="summary-value">{{ person.orderId || person.cspId }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label">{{ $t('communitySpacePersonManage.venue') }}</span>
        <span class="summary-value">{{ person.venueName || '-' }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label">{{ $t('communitySpacePersonManage.space') }}</span>
        <span class="summary-value">{{ person.spaceName || '-' }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label">{{ $t('communitySpacePersonManage.appointmentDate') }}</span>
        <span class="summary-value">{{ person.appointmentTime || '-' }}</span>
      </div>
      <div class="summary-row">
        <span class="summary-label">{{ $t('communitySpacePersonManage.receivableAmount') }}</span>
        <span class="summary-value amount">￥{{ person.receivableAmount || '0.00' }}</span>
      </div>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="small">
      <el-form-item :label="$t('communitySpacePersonManage.collectAmount')" prop="receivedAmount">
        <el-input v-model="form.receivedAmount"
          :placeholder="$t('communitySpacePersonManage.collectAmountPlaceholder')" clearable />
      </el-form-item>
      <el-form-item :label="$t('communitySpacePersonManage.payWay')" prop="payWay">
        <el-select v-model="form.payWay" style="width:100%"
          :placeholder="$t('communitySpacePersonManage.payWayPlaceholder')">
          <el-option :label="$t('communitySpacePersonManage.payWayCash')" value="1" />
          <el-option :label="$t('communitySpacePersonManage.payWayWechat')" value="2" />
          <el-option :label="$t('communitySpacePersonManage.payWayAlipay')" value="3" />
        </el-select>
      </el-form-item>
    </el-form>

    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        {{ $t('communitySpacePersonManage.collect') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { updateCommunitySpacePerson } from '@/api/community/communitySpacePersonManageApi'
import { getCommunityId } from '@/api/community/communityApi'

export default {
  name: 'CollectCommunitySpacePerson',
  data() {
    return {
      visible: false,
      loading: false,
      person: {},
      form: {
        receivedAmount: '',
        payWay: '1'
      },
      rules: {
        receivedAmount: [
          { required: true, message: this.$t('communitySpacePersonManage.requiredCollectAmount'), trigger: 'blur' },
          {
            pattern: /^\d+(\.\d{1,2})?$/,
            message: this.$t('communitySpacePersonManage.collectAmountFormatError'),
            trigger: 'blur'
          }
        ],
        payWay: [
          { required: true, message: this.$t('communitySpacePersonManage.requiredPayWay'), trigger: 'change' }
        ]
      }
    }
  },
  methods: {
    open(row) {
      this.person = { ...row }
      this.form.receivedAmount = row.receivableAmount || ''
      this.form.payWay = '1'
      this.visible = true
    },
    handleClose() {
      this.visible = false
      this.person = {}
      this.form.receivedAmount = ''
      this.form.payWay = '1'
      if (this.$refs.formRef) {
        this.$refs.formRef.clearValidate()
      }
    },
    handleSubmit() {
      if (!this.$refs.formRef) {
        return
      }
      this.$refs.formRef.validate(valid => {
        if (!valid) {
          return
        }
        this.loading = true
        updateCommunitySpacePerson({
          cspId: this.person.cspId,
          communityId: getCommunityId(),
          receivedAmount: this.form.receivedAmount,
          payWay: this.form.payWay
        }).then(() => {
          this.$message.success(this.$t('communitySpacePersonManage.collectSuccess'))
          this.$emit('success')
          this.handleClose()
          this.loading = false
        }).catch(error => {
          this.$message.error(error.message || this.$t('communitySpacePersonManage.collectFailed'))
          this.loading = false
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.order-summary {
  padding: 10px 12px;
  margin-bottom: 14px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;
}

.summary-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  font-size: 13px;
  line-height: 24px;
}

.summary-label {
  color: #64748b;
  flex: none;
  margin-right: 12px;
}

.summary-value {
  color: #334155;
  text-align: right;
  word-break: break-all;
}

.amount {
  color: #f43f5e;
  font-weight: 600;
}
</style>
