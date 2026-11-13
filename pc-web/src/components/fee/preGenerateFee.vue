<template>
  <el-dialog :title="$t('simplifyRoomFee.preGenerateTitle')" :visible.sync="visible" width="660px" @close="handleClose">
    <div class="ibox-content">
      <div class="margin-bottom text-left">
        <span>{{ feeName }}</span>
        <span class="text-muted" style="margin-left:10px;">{{ endTime }}</span>
      </div>
      <div class="margin-bottom text-left text-muted">{{ $t('simplifyRoomFee.selectPeriodTip') }}</div>
      <div class="period-cards">
        <div v-for="item in periods" :key="item.seq" class="period-card"
          :class="{ 'period-card-active': isSelected(item) }" @click="handleSelectPeriod(item)">
          <i v-if="isSelected(item)" class="el-icon-check period-card-check"></i>
          <div class="period-card-title">{{ $t('simplifyRoomFee.period') }}{{ item.seq }}</div>
          <div class="period-card-time">{{ dateFormat(item.startTime) }}</div>
          <div class="period-card-time">~ {{ dateFormat(item.endTime) }}</div>
        </div>
      </div>
    </div>
    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">{{ $t('common.submit') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { preGenerateFee } from '@/api/fee/preGenerateFeeApi'
import { dateFormat } from '@/utils/dateUtil'

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
      selectedSeqs: []
    }
  },
  methods: {
    dateFormat,
    open(data) {
      this.communityId = data.communityId
      this.ruleId = data.ruleId
      this.feeName = data.feeName
      this.endTime = data.endTime
      this.periods = data.periods || []
      this.selectedSeqs = []
      this.visible = true
    },
    handleClose() {
      this.visible = false
      this.loading = false
      this.selectedSeqs = []
    },
    isSelected(item) {
      return this.selectedSeqs.indexOf(item.seq) > -1
    },
    handleSelectPeriod(item) {
      if (this.isSelected(item)) {
        // 连锁取消：取消该期及其之后的所有期次
        this.selectedSeqs = this.selectedSeqs.filter(seq => seq < item.seq)
        return
      }
      // 连锁勾选：自动勾选第 1 期至该期
      const seqs = []
      this.periods.forEach(period => {
        if (period.seq <= item.seq) {
          seqs.push(period.seq)
        }
      })
      if (this.selectedSeqs.length < item.seq - 1) {
        this.$message.info(this.$t('simplifyRoomFee.continuousTip'))
      }
      this.selectedSeqs = seqs
    },
    handleSubmit() {
      const selectedPeriods = this.periods.filter(item => this.isSelected(item))
      if (selectedPeriods.length < 1) {
        this.$message.warning(this.$t('simplifyRoomFee.selectPeriodTip'))
        return
      }

      if (selectedPeriods[0].seq !== 1) {
        this.$message.warning(this.$t('simplifyRoomFee.continuousRequired'))
        return
      }
      for (let i = 1; i < selectedPeriods.length; i++) {
        if (selectedPeriods[i].seq !== selectedPeriods[i - 1].seq + 1) {
          this.$message.warning(this.$t('simplifyRoomFee.continuousRequired'))
          return
        }
      }

      const data = {
        communityId: this.communityId,
        ruleId: this.ruleId,
        periods: selectedPeriods.map(item => {
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

<style scoped>
.ibox-content {
  padding: 5px 10px;
}

.period-cards {
  display: flex;
  flex-wrap: wrap;
}

.period-card {
  position: relative;
  width: 132px;
  padding: 10px 6px;
  margin: 0 10px 10px 0;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  text-align: center;
  cursor: pointer;
  background-color: #fff;
  transition: all 0.2s;
}

.period-card:hover {
  border-color: #409eff;
}

.period-card-active {
  border-color: #409eff;
  background-color: #ecf5ff;
  color: #409eff;
}

.period-card-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 6px;
}

.period-card-time {
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}

.period-card-check {
  position: absolute;
  top: 4px;
  right: 6px;
  font-size: 14px;
  color: #409eff;
}
</style>
