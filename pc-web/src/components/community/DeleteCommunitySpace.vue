<template>
  <el-dialog
    :title="$t('communitySpaceManage.confirmDelete')"
    :visible.sync="visible"
    width="420px"
    @close="handleClose"
  >
    <div class="confirm-body">
      <i class="el-icon-warning confirm-icon"></i>
      <p class="confirm-text">{{ $t('communitySpaceManage.deleteSpaceConfirm', { name: spaceName }) }}</p>
    </div>
    <span slot="footer">
      <el-button @click="handleClose">{{ $t('communitySpaceManage.cancel') }}</el-button>
      <el-button type="danger" :loading="loading" @click="handleSubmit">
        {{ $t('communitySpaceManage.confirmDelete') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import { deleteCommunitySpace } from '@/api/community/communitySpaceManageApi'

export default {
  name: 'DeleteCommunitySpace',
  data() {
    return {
      visible: false,
      loading: false,
      spaceId: '',
      spaceName: ''
    }
  },
  methods: {
    open(space) {
      this.spaceId = space.spaceId
      this.spaceName = space.name
      this.visible = true
    },
    handleClose() {
      this.visible = false
      this.spaceId = ''
      this.spaceName = ''
    },
    handleSubmit() {
      this.loading = true
      deleteCommunitySpace(this.spaceId).then(() => {
        this.$message.success(this.$t('common.operationSuccess'))
        this.$emit('success')
        this.handleClose()
        this.loading = false
      }).catch(error => {
        this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
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
</style>
