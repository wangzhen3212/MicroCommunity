<template>
  <el-dialog
    :title="$t('deleteCommunitySpacePerson.confirmTitle')"
    :visible.sync="visible"
    width="440px"
    @close="handleClose"
  >
    <div class="confirm-body">
      <i class="el-icon-warning confirm-icon"></i>
      <div>
        <p class="confirm-text">{{ $t('deleteCommunitySpacePerson.confirmContent') }}</p>
        <p class="confirm-tip">{{ $t('deleteCommunitySpacePerson.releaseTip') }}</p>
      </div>
    </div>
    <span slot="footer">
      <el-button @click="handleClose">{{ $t('deleteCommunitySpacePerson.cancel') }}</el-button>
      <el-button type="danger" :loading="loading" @click="handleSubmit">
        {{ $t('deleteCommunitySpacePerson.confirmCancel') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { deleteCommunitySpacePerson } from '@/api/community/communitySpacePersonManageApi'
import { getCommunityId } from '@/api/community/communityApi'

export default {
  name: 'DeleteCommunitySpacePerson',
  data() {
    return {
      visible: false,
      loading: false,
      deleteData: {}
    }
  },
  methods: {
    open(row) {
      this.deleteData = { ...row }
      this.visible = true
    },
    handleClose() {
      this.visible = false
      this.deleteData = {}
    },
    handleSubmit() {
      this.loading = true
      deleteCommunitySpacePerson({
        cspId: this.deleteData.cspId,
        communityId: getCommunityId()
      }).then(() => {
        this.$message.success(this.$t('deleteCommunitySpacePerson.cancelSuccess'))
        this.$emit('success')
        this.handleClose()
        this.loading = false
      }).catch(error => {
        this.$message.error(error.message || this.$t('deleteCommunitySpacePerson.cancelFailed'))
        this.loading = false
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.confirm-body {
  display: flex;
  align-items: flex-start;
  padding: 10px 0;
}

.confirm-icon {
  font-size: 26px;
  color: #f56c6c;
  margin-right: 12px;
}

.confirm-text {
  margin: 2px 0 0 0;
  font-size: 14px;
  color: #334155;
  line-height: 22px;
}

.confirm-tip {
  margin: 8px 0 0 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 20px;
}
</style>
