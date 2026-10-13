<template>
  <el-dialog
    :title="$t('communitySpaceManage.editVenueTitle')"
    :visible.sync="visible"
    width="520px"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" size="small">
      <el-form-item :label="$t('communitySpaceManage.venueName')" prop="name">
        <el-input v-model="form.name" :placeholder="$t('communitySpaceManage.venueNamePlaceholder')" clearable />
      </el-form-item>

      <el-form-item :label="$t('communitySpaceManage.description')" prop="remark">
        <el-input v-model="form.remark" :placeholder="$t('communitySpaceManage.venueDescribePlaceholder')" clearable />
      </el-form-item>

      <el-form-item :label="$t('communitySpaceManage.startTime')" prop="startTime">
        <el-time-picker v-model="form.startTime" value-format="HH:mm" format="HH:mm"
          :placeholder="$t('communitySpaceManage.startTime')" style="width:100%" />
      </el-form-item>

      <el-form-item :label="$t('communitySpaceManage.endTime')" prop="endTime">
        <el-time-picker v-model="form.endTime" value-format="HH:mm" format="HH:mm"
          :placeholder="$t('communitySpaceManage.endTime')" style="width:100%" />
      </el-form-item>

      <div class="form-tip">
        {{ $t('communitySpaceManage.inheritTip') }}
      </div>
    </el-form>

    <span slot="footer">
      <el-button @click="handleClose">{{ $t('communitySpaceManage.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        {{ $t('communitySpaceManage.save') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { updateCommunityVenue } from '@/api/community/communitySpaceManageApi'

export default {
  name: 'EditCommunityVenue',
  data() {
    return {
      visible: false,
      loading: false,
      form: {
        venueId: '',
        name: '',
        remark: '',
        startTime: '',
        endTime: ''
      },
      rules: {
        name: [
          { required: true, message: this.$t('communitySpaceManage.requiredVenueName'), trigger: 'blur' }
        ],
        startTime: [
          { required: true, message: this.$t('communitySpaceManage.selectVenue'), trigger: 'change' }
        ],
        endTime: [
          { required: true, message: this.$t('communitySpaceManage.selectVenue'), trigger: 'change' },
          { validator: this.validateEndTime, trigger: 'change' }
        ]
      }
    }
  },
  methods: {
    open(venue) {
      this.form = {
        venueId: venue.venueId,
        name: venue.name,
        remark: venue.remark,
        startTime: venue.startTime,
        endTime: venue.endTime
      }
      this.visible = true
    },
    handleClose() {
      this.visible = false
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields()
      }
    },
    validateEndTime(rule, value, callback) {
      if (!value || !this.form.startTime) {
        callback()
        return
      }
      if (value <= this.form.startTime) {
        callback(new Error(this.$t('communitySpaceManage.closeTimeAfterOpen')))
        return
      }
      callback()
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
        updateCommunityVenue(this.form).then(() => {
          this.$message.success(this.$t('common.operationSuccess'))
          this.$emit('success')
          this.handleClose()
          this.loading = false
        }).catch(error => {
          this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
          this.loading = false
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.form-tip {
  margin: 0 0 0 110px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  color: #2563eb;
  font-size: 12px;
  line-height: 20px;
}
</style>
