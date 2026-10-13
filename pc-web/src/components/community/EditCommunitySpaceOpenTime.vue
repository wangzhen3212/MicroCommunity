<template>
  <el-dialog
    :title="$t('communitySpaceManage.openTimeTitle')"
    :visible.sync="visible"
    width="520px"
    @close="handleClose"
  >
    <div class="open-time-header">
      <span class="space-name">{{ spaceName }}</span>
      <span class="space-tip">{{ $t('communitySpaceManage.openTimeModalTip') }}</span>
    </div>

    <div class="open-time-list">
      <div v-for="item in sortedOpenTimes" :key="item.timeId" class="open-time-row">
        <span class="hour-text">{{ formatHour(item.hours) }} - {{ formatHour(item.hours, true) }}</span>
        <el-switch
          v-model="item.isOpen"
          active-value="Y"
          inactive-value="N"
          :active-text="$t('communitySpaceManage.openYes')"
          :inactive-text="$t('communitySpaceManage.openNo')"
          @change="handleChange(item)"
        />
      </div>
      <div v-if="sortedOpenTimes.length < 1" class="open-time-empty">{{ $t('communitySpaceManage.inheritEmpty') }}</div>
    </div>

    <span slot="footer">
      <el-button type="primary" @click="handleClose">{{ $t('common.close') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { updateCommunitySpaceOpenTime } from '@/api/community/communitySpaceManageApi'

export default {
  name: 'EditCommunitySpaceOpenTime',
  data() {
    return {
      visible: false,
      spaceId: '',
      spaceName: '',
      openTimes: []
    }
  },
  computed: {
    sortedOpenTimes() {
      return this.openTimes.slice().sort((a, b) => Number(a.hours) - Number(b.hours))
    }
  },
  methods: {
    open(space) {
      this.spaceId = space.spaceId
      this.spaceName = space.name
      this.openTimes = (space.openTimes || []).map(item => ({ ...item }))
      this.visible = true
    },
    handleClose() {
      this.visible = false
      this.openTimes = []
    },
    formatHour(hours, isEnd) {
      const value = isEnd ? Number(hours) + 1 : Number(hours)
      return (value < 10 ? '0' + value : '' + value) + ':00'
    },
    handleChange(item) {
      updateCommunitySpaceOpenTime({
        timeId: item.timeId,
        spaceId: item.spaceId,
        hours: item.hours,
        isOpen: item.isOpen
      }).then(() => {
        this.$message.success(this.$t('common.operationSuccess'))
        this.$emit('success')
      }).catch(error => {
        item.isOpen = item.isOpen === 'Y' ? 'N' : 'Y'
        this.$message.error(error.message || this.$t('communitySpaceManage.loadFailed'))
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.open-time-header {
  display: flex;
  flex-direction: column;
  padding-bottom: 10px;
  border-bottom: 1px solid #f1f5f9;
}

.space-name {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.space-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.open-time-list {
  max-height: 420px;
  overflow-y: auto;
  padding-top: 6px;
}

.open-time-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 4px;
  border-bottom: 1px solid #f8fafc;
  font-size: 13px;
}

.hour-text {
  color: #334155;
}

.open-time-empty {
  padding: 20px 0;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}
</style>
