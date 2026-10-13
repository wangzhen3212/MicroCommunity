<template>
  <div class="booking-panel">
    <div class="panel-header">
      <span class="panel-title">{{ $t('reportCommunitySpace.bookingInfo') }}</span>
    </div>
    <div class="panel-body">
      <div class="summary-box">
        <el-row :gutter="12">
          <el-col v-for="item in summaryItems" :key="item.label" :span="12">
            <div class="summary-row">
              <span class="summary-label">{{ item.label }}</span>
              <span class="summary-value">{{ item.value }}</span>
            </div>
          </el-col>
        </el-row>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="small" class="booking-form">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item :label="$t('reportCommunitySpace.personName')" prop="personName">
              <el-input v-model="form.personName" :placeholder="$t('reportCommunitySpace.personNamePlaceholder')" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('reportCommunitySpace.personTel')" prop="personTel">
              <el-input v-model="form.personTel" :placeholder="$t('reportCommunitySpace.personTelPlaceholder')" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('reportCommunitySpace.remark')" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2"
            :placeholder="$t('reportCommunitySpace.remarkPlaceholder')" />
        </el-form-item>
      </el-form>

      <div class="amount-row">
        <span class="amount-label">{{ $t('reportCommunitySpace.receivableAmount') }}</span>
        <span class="amount-value">￥{{ amount }}</span>
      </div>

      <el-button type="primary" class="submit-button" :loading="loading" @click="handleSubmit">
        {{ $t('reportCommunitySpace.submitBooking') }}
      </el-button>
      <el-button type="text" class="clear-button" @click="handleClear">
        {{ $t('reportCommunitySpace.clearSelection') }}
      </el-button>
    </div>

    <space-booking-pay-dialog ref="payDialog" @success="handlePaySuccess" />
  </div>
</template>

<script>
import { saveCommunitySpacePerson } from '@/api/community/reportCommunitySpaceApi'
import { getCommunityId } from '@/api/community/communityApi'
import SpaceBookingPayDialog from '@/components/community/SpaceBookingPayDialog'

export default {
  name: 'SpaceBookingPanel',
  components: {
    SpaceBookingPayDialog
  },
  props: {
    venue: {
      type: Object,
      default: null
    },
    space: {
      type: Object,
      default: null
    },
    appointmentDate: {
      type: String,
      default: ''
    },
    selectedHours: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      loading: false,
      form: {
        personName: '',
        personTel: '',
        remark: ''
      },
      rules: {
        personName: [
          { required: true, message: this.$t('reportCommunitySpace.requiredPersonName'), trigger: 'blur' },
          { max: 64, message: this.$t('reportCommunitySpace.maxPersonName'), trigger: 'blur' }
        ],
        personTel: [
          { required: true, message: this.$t('reportCommunitySpace.requiredPersonTel'), trigger: 'blur' },
          { pattern: /^1\d{10}$/, message: this.$t('reportCommunitySpace.telFormatError'), trigger: 'blur' }
        ],
        remark: [
          { max: 512, message: this.$t('reportCommunitySpace.maxRemark'), trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    summaryItems() {
      return [
        { label: this.$t('reportCommunitySpace.venue'), value: this.venue ? this.venue.name : '-' },
        { label: this.$t('reportCommunitySpace.space'), value: this.space ? this.space.name : '-' },
        { label: this.$t('reportCommunitySpace.openTime'), value: this.openTimeText },
        { label: this.$t('reportCommunitySpace.appointmentDate'), value: this.appointmentDate || '-' },
        { label: this.$t('reportCommunitySpace.selectedHours'), value: this.hoursText },
        { label: this.$t('reportCommunitySpace.duration'), value: this.durationText }
      ]
    },
    sortedHours() {
      return this.selectedHours.slice().sort((a, b) => a - b)
    },
    hoursText() {
      if (this.sortedHours.length < 1) {
        return '-'
      }
      return this.sortedHours.map(hour => this.padHour(hour) + ':00').join('、')
    },
    durationText() {
      return this.sortedHours.length + ' ' + this.$t('reportCommunitySpace.hour')
    },
    openTimeText() {
      if (!this.venue || !this.venue.startTime || !this.venue.endTime) {
        return '-'
      }
      return this.venue.startTime + ' - ' + this.venue.endTime
    },
    amount() {
      const feeMoney = this.space ? Number(this.space.feeMoney) : 0
      const total = (isNaN(feeMoney) ? 0 : feeMoney) * this.sortedHours.length
      return total.toFixed(2)
    }
  },
  methods: {
    padHour(hour) {
      return hour < 10 ? '0' + hour : '' + hour
    },
    handleSubmit() {
      if (!this.space) {
        this.$message.warning(this.$t('reportCommunitySpace.selectSpaceFirst'))
        return
      }
      if (this.sortedHours.length < 1) {
        this.$message.warning(this.$t('reportCommunitySpace.selectHoursFirst'))
        return
      }
      if (!this.$refs.formRef) {
        return
      }
      this.$refs.formRef.validate(valid => {
        if (!valid) {
          return
        }
        this.handleBookingPay()
      })
    },
    buildBookingPayload() {
      const openTimes = []
      this.sortedHours.forEach(hour => {
        openTimes.push({ hours: '' + hour })
      })
      return {
        communityId: getCommunityId(),
        venueId: this.venue ? this.venue.venueId : '',
        spaceId: this.space ? this.space.spaceId : '',
        personName: this.form.personName,
        personTel: this.form.personTel,
        appointmentTime: this.appointmentDate,
        remark: this.form.remark,
        openTimes: openTimes
      }
    },
    // 免费场地不弹收款框，直接建单即收款（实收 0.00）
    async handleBookingPay() {
      if (Number(this.amount) > 0) {
        if (!this.$refs.payDialog) {
          return
        }
        this.$refs.payDialog.open({
          booking: this.buildBookingPayload(),
          receivableAmount: this.amount,
          venueName: this.venue ? this.venue.name : '',
          spaceName: this.space ? this.space.name : '',
          appointmentTime: this.appointmentDate,
          hoursText: this.hoursText
        })
        return
      }

      this.loading = true
      try {
        const booking = this.buildBookingPayload()
        booking.payWay = '1'
        booking.receivedAmount = '0.00'
        await saveCommunitySpacePerson(booking)
        this.$message.success(this.$t('reportCommunitySpace.bookingSuccess'))
        this.handlePaySuccess()
      } catch (error) {
        this.$message.error(error.message || this.$t('reportCommunitySpace.bookingFailed'))
      } finally {
        this.loading = false
      }
    },
    handlePaySuccess() {
      this.form.personName = ''
      this.form.personTel = ''
      this.form.remark = ''
      if (this.$refs.formRef) {
        this.$refs.formRef.clearValidate()
      }
      this.$emit('success')
    },
    handleClear() {
      this.form.personName = ''
      this.form.personTel = ''
      this.form.remark = ''
      if (this.$refs.formRef) {
        this.$refs.formRef.clearValidate()
      }
      this.$emit('clear')
    }
  }
}
</script>

<style lang="scss" scoped>
.booking-panel {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.12);
  text-align: left;
}

.panel-header {
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.panel-body {
  padding: 16px;
}

.summary-box {
  background: #f8fafc;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 16px;
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
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

.amount-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 4px;
  margin-bottom: 12px;
}

.booking-form ::v-deep .el-form-item__label {
  text-align: left;
}

.booking-form ::v-deep .el-form-item {
  margin-bottom: 14px;
  text-align: left;
}

.amount-label {
  font-size: 13px;
  color: #64748b;
}

.amount-value {
  font-size: 20px;
  font-weight: 700;
  color: #f43f5e;
}

.submit-button {
  width: 100%;
  background-color: #2563eb;
  border-color: #2563eb;
}

.clear-button {
  width: 100%;
  margin: 8px 0 0 0;
  color: #64748b;
}
</style>
