<template>
  <el-dialog
    :title="$t('communitySpaceManage.addSpaceTitle')"
    :visible.sync="visible"
    width="560px"
    @close="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" size="small">
      <el-form-item :label="$t('communitySpaceManage.venue')" prop="venueId">
        <el-select v-model="form.venueId" style="width:100%">
          <el-option v-for="item in venues" :key="item.venueId" :label="item.name" :value="item.venueId" />
        </el-select>
      </el-form-item>

      <el-form-item :label="$t('communitySpaceManage.name')" prop="name">
        <el-input v-model="form.name" :placeholder="$t('communitySpaceManage.spaceNamePlaceholder')" clearable />
      </el-form-item>

      <el-form-item :label="$t('communitySpaceManage.feeMoney')" prop="feeMoney">
        <el-input v-model="form.feeMoney" placeholder="0.00" clearable />
      </el-form-item>

      <el-form-item :label="$t('communitySpaceManage.state')" prop="state">
        <el-select v-model="form.state" style="width:100%">
          <el-option :label="$t('communitySpaceManage.status1001')" value="1001" />
          <el-option :label="$t('communitySpaceManage.status2002')" value="2002" />
        </el-select>
      </el-form-item>

      <el-form-item :label="$t('communitySpaceManage.adminName')" prop="adminName">
        <el-input v-model="form.adminName" :placeholder="$t('communitySpaceManage.adminNamePlaceholder')" clearable />
      </el-form-item>

      <el-form-item :label="$t('communitySpaceManage.tel')" prop="tel">
        <el-input v-model="form.tel" :placeholder="$t('communitySpaceManage.telPlaceholder')" clearable />
      </el-form-item>

      <div class="form-tip">
        {{ $t('communitySpaceManage.inheritLabel') }}
        <b>{{ inheritText }}</b>{{ $t('communitySpaceManage.inheritSuffix') }}
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
import { saveCommunitySpace } from '@/api/community/communitySpaceManageApi'

export default {
  name: 'AddCommunitySpace',
  data() {
    return {
      visible: false,
      loading: false,
      venues: [],
      form: {
        venueId: '',
        name: '',
        feeMoney: '',
        startTime: '00:00',
        endTime: '23:59',
        adminName: '',
        tel: '',
        state: '1001'
      },
      rules: {
        venueId: [
          { required: true, message: this.$t('communitySpaceManage.selectVenue'), trigger: 'change' }
        ],
        name: [
          { required: true, message: this.$t('communitySpaceManage.requiredSpaceName'), trigger: 'blur' }
        ],
        feeMoney: [
          { required: true, message: this.$t('communitySpaceManage.feeMoney'), trigger: 'blur' }
        ],
        state: [
          { required: true, message: this.$t('communitySpaceManage.selectStatus'), trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    inheritText() {
      const venue = this.venues.find(item => item.venueId === this.form.venueId)
      if (!venue || !venue.startTime || !venue.endTime) {
        return this.$t('communitySpaceManage.inheritEmpty')
      }
      return venue.startTime + ' - ' + venue.endTime
    }
  },
  methods: {
    open(venueId, venues) {
      this.venues = venues || []
      this.form.venueId = venueId
      this.visible = true
    },
    handleClose() {
      this.visible = false
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields()
      }
      this.form.startTime = '00:00'
      this.form.endTime = '23:59'
      this.form.venueId = ''
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
        saveCommunitySpace(this.form).then(() => {
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
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  color: #64748b;
  font-size: 12px;
  line-height: 20px;
}
</style>
