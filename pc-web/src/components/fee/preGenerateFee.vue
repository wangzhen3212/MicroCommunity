<template>
  <el-dialog :title="$t('simplifyRoomFee.preGenerateTitle')" :visible.sync="visible" width="60%" @close="handleClose">
    <div class="ibox-content">
      <div class="margin-bottom text-left">
        <span>{{ feeName }}</span>
        <span class="text-muted" style="margin-left:10px;">{{ endTime }}</span>
      </div>
      <div class="margin-bottom text-left text-muted">{{ $t('simplifyRoomFee.selectPeriodTip') }}</div>
      <el-table ref="periodTable" :data="periods" border size="small" @select="handleSelect"
        @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" :selectable="selectable"></el-table-column>
        <el-table-column prop="seq" :label="$t('simplifyRoomFee.period')" align="center" width="120"></el-table-column>
        <el-table-column prop="startTime" :label="$t('simplifyRoomFee.periodStart')" align="center"></el-table-column>
        <el-table-column prop="endTime" :label="$t('simplifyRoomFee.periodEnd')" align="center"></el-table-column>
      </el-table>
    </div>
    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">{{ $t('common.submit') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { preGenerateFee } from '@/api/fee/preGenerateFeeApi'

export default {
  name: 'PreGenerateFee',
  data() {
    return {
      visible: false,
      loading: false,
      communityId: '',
      ruleId: '',
      feeName: '',
      endTime: '',
      periods: [],
      selectedPeriods: []
    }
  },
  methods: {
    open(data) {
      this.communityId = data.communityId
      this.ruleId = data.ruleId
      this.feeName = data.feeName
      this.endTime = data.endTime
      this.periods = data.periods || []
      this.selectedPeriods = []
      this.visible = true
    },
    handleClose() {
      this.visible = false
      this.loading = false
      this.selectedPeriods = []
      if (this.$refs.periodTable) {
        this.$refs.periodTable.clearSelection()
      }
    },
    selectable() {
      return true
    },
    handleSelectionChange(selection) {
      this.selectedPeriods = selection
    },
    handleSelect(selection, row) {
      const checked = selection.indexOf(row) > -1
      let changed = false
      if (checked) {
        this.periods.forEach(item => {
          if (item.seq <= row.seq && selection.indexOf(item) < 0) {
            this.toggleRowSelection(item, true)
            changed = true
          }
        })
      } else {
        this.periods.forEach(item => {
          if (item.seq > row.seq && selection.indexOf(item) > -1) {
            this.toggleRowSelection(item, false)
            changed = true
          }
        })
      }
      if (changed) {
        this.$message.info(this.$t('simplifyRoomFee.continuousTip'))
      }
    },
    toggleRowSelection(row, selected) {
      if (!this.$refs.periodTable) {
        return
      }
      this.$refs.periodTable.toggleRowSelection(row, selected)
    },
    handleSubmit() {
      const selectedPeriods = this.selectedPeriods.slice()
      if (selectedPeriods.length < 1) {
        this.$message.warning(this.$t('simplifyRoomFee.selectPeriodTip'))
        return
      }

      const sortedPeriods = selectedPeriods.sort((a, b) => a.seq - b.seq)
      if (sortedPeriods[0].seq !== 1) {
        this.$message.warning(this.$t('simplifyRoomFee.continuousRequired'))
        return
      }
      for (let i = 1; i < sortedPeriods.length; i++) {
        if (sortedPeriods[i].seq !== sortedPeriods[i - 1].seq + 1) {
          this.$message.warning(this.$t('simplifyRoomFee.continuousRequired'))
          return
        }
      }

      const data = {
        communityId: this.communityId,
        ruleId: this.ruleId,
        periods: sortedPeriods.map(item => {
          return {
            startTime: item.startTime,
            endTime: item.endTime
          }
        })
      }

      this.loading = true
      preGenerateFee(data).then(res => {
        this.loading = false
        this.$message.success(this.$t('simplifyRoomFee.preGenerateSuccess', { count: res.data }))
        this.handleClose()
        this.$emit('success')
      }).catch(error => {
        this.loading = false
        this.$message.error(error.message || this.$t('simplifyRoomFee.preGenerateFailed'))
      })
    }
  }
}
</script>
