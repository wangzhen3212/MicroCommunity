<template>
  <el-dialog
    :title="$t('communitySpaceManage.confirmDelete')"
    :visible.sync="visible"
    width="420px"
    @close="handleClose"
  >
    <div class="confirm-body">
      <i class="el-icon-warning confirm-icon"></i>
      <p class="confirm-text">
        {{ $t('communitySpaceManage.deleteVenueConfirm', { name: venueName, count: spaceCount }) }}
      </p>
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
import { deleteCommunityVenue } from '@/api/community/communitySpaceManageApi'

export default {
  name: 'DeleteCommunityVenue',
  data() {
    return {
      visible: false,
      loading: false,
      venueId: '',
      venueName: '',
      spaceCount: 0
    }
  },
  methods: {
    open(venue, spaceCount) {
      this.venueId = venue.venueId
      this.venueName = venue.name
      this.spaceCount = spaceCount || 0
      this.visible = true
    },
    handleClose() {
      this.visible = false
      this.venueId = ''
      this.venueName = ''
      this.spaceCount = 0
    },
    handleSubmit() {
      this.loading = true
      deleteCommunityVenue(this.venueId).then(() => {
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
