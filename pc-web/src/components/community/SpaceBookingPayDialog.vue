<template>
  <el-dialog
    :title="$t('reportCommunitySpace.payDialogTitle')"
    :visible.sync="visible"
    width="560px"
    :before-close="handleClose"
    @close="handleClose"
  >
    <div class="pay-body">
      <div class="summary-box">
        <div class="summary-row">
          <span class="summary-label">{{ $t('reportCommunitySpace.venue') }}</span>
          <span class="summary-value">{{ venueName || '-' }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">{{ $t('reportCommunitySpace.space') }}</span>
          <span class="summary-value">{{ spaceName || '-' }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">{{ $t('reportCommunitySpace.appointmentDate') }}</span>
          <span class="summary-value">{{ appointmentTime || '-' }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">{{ $t('reportCommunitySpace.selectedHours') }}</span>
          <span class="summary-value">{{ hoursText || '-' }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">{{ $t('reportCommunitySpace.receivableAmount') }}</span>
          <span class="summary-value amount">￥{{ receivableAmount }}</span>
        </div>
      </div>

      <el-form label-position="top" size="small" class="pay-form">
        <el-form-item :label="$t('reportCommunitySpace.payWay')">
          <el-radio-group v-model="payType" :disabled="paying" @change="handlePayTypeChange">
            <el-radio-button label="cash">{{ $t('reportCommunitySpace.payWayCash') }}</el-radio-button>
            <el-radio-button label="qrcode">{{ $t('reportCommunitySpace.payWayQrCode') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="payType === 'cash'" :label="$t('reportCommunitySpace.receivedAmount')">
          <el-input v-model="receivedAmount" clearable
            :placeholder="$t('reportCommunitySpace.receivedAmountPlaceholder')" />
          <div class="field-tip">{{ $t('reportCommunitySpace.receivedAmountTip') }}</div>
        </el-form-item>

        <template v-if="payType === 'qrcode'">
          <el-form-item :label="$t('reportCommunitySpace.qrPayWay')">
            <el-select v-model="primeRate" style="width: 100%" :disabled="paying"
              @change="handlePrimeRateChange">
              <el-option :label="$t('reportCommunitySpace.qrPayWayWechat')" value="3" />
              <el-option :label="$t('reportCommunitySpace.qrPayWayAlipay')" value="4" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('reportCommunitySpace.authCode')">
            <el-input ref="authCodeInput" v-model="authCode" clearable
              :disabled="paying"
              :placeholder="$t('reportCommunitySpace.authCodePlaceholder')"
              @keyup.enter.native="handleAuthCode" />
            <div class="field-tip">{{ $t('reportCommunitySpace.authCodeTip') }}</div>
          </el-form-item>
          <el-form-item :label="$t('reportCommunitySpace.payStatus')">
            <span class="pay-status" :class="'pay-status-' + payStatus">{{ payStatusText }}</span>
          </el-form-item>
        </template>
      </el-form>
    </div>

    <span slot="footer">
      <el-button :disabled="paying" @click="handleClose">{{ $t('reportCommunitySpace.close') }}</el-button>
      <el-button v-if="payType === 'cash'" type="primary" :loading="paying" @click="handleCashSubmit">
        {{ $t('reportCommunitySpace.confirmPay') }}
      </el-button>
    </span>
  </el-dialog>
</template>

<script>
import {
  saveCommunitySpacePerson,
  qrCodePayment,
  checkPayFinish,
  listCommunitySpaceOpenTime
} from '@/api/community/reportCommunitySpaceApi'

export default {
  name: 'SpaceBookingPayDialog',
  data() {
    return {
      visible: false,
      payType: 'cash',
      booking: null,
      receivableAmount: '0.00',
      receivedAmount: '0.00',
      venueName: '',
      spaceName: '',
      appointmentTime: '',
      hoursText: '',
      primeRate: '3',
      authCode: '',
      paying: false,
      payStatus: 'waiting',
      checkTimer: null,
      retryCount: 0,
      MAX_RETRY: 60,
      pollingOrderId: '',
      pollingPaymentPoolId: ''
    }
  },
  computed: {
    payStatusText() {
      if (this.payStatus === 'paying') {
        return this.$t('reportCommunitySpace.paying')
      }
      if (this.payStatus === 'success') {
        return this.$t('reportCommunitySpace.paySuccess')
      }
      return this.$t('reportCommunitySpace.waitingScan')
    }
  },
  beforeDestroy() {
    this.clearCheckTimer()
  },
  methods: {
    open(params) {
      this.booking = params.booking || null
      this.receivableAmount = params.receivableAmount || '0.00'
      this.receivedAmount = this.receivableAmount
      this.venueName = params.venueName || ''
      this.spaceName = params.spaceName || ''
      this.appointmentTime = params.appointmentTime || ''
      this.hoursText = params.hoursText || ''
      this.payType = 'cash'
      this.primeRate = '3'
      this.authCode = ''
      this.paying = false
      this.payStatus = 'waiting'
      this.retryCount = 0
      this.pollingOrderId = ''
      this.pollingPaymentPoolId = ''
      this.clearCheckTimer()
      this.visible = true
      this.focusAuthCode()
    },
    handleClose() {
      this.paying = false
      this.clearCheckTimer()
      this.retryCount = 0
      this.pollingOrderId = ''
      this.pollingPaymentPoolId = ''
      this.authCode = ''
      this.visible = false
    },
    handlePayTypeChange() {
      if (this.payType === 'qrcode') {
        this.payStatus = 'waiting'
        this.focusAuthCode()
      }
    },
    handlePrimeRateChange() {
      this.payStatus = 'waiting'
      this.focusAuthCode()
    },
    focusAuthCode() {
      this.$nextTick(() => {
        setTimeout(() => {
          const authCodeInput = this.$refs.authCodeInput
          if (!authCodeInput) {
            return
          }
          authCodeInput.focus()
          authCodeInput.select()
        }, 300)
      })
    },
    resetAuthCode() {
      this.authCode = ''
      this.focusAuthCode()
    },
    verifyAmount(value) {
      if (!value || !/^\d+(\.\d{1,2})?$/.test(value)) {
        this.$message.warning(this.$t('reportCommunitySpace.amountFormatError'))
        return false
      }
      return true
    },
    buildBookingParams(payWay, receivedAmount) {
      const booking = this.booking || {}
      return {
        communityId: booking.communityId,
        spaceId: booking.spaceId,
        personName: booking.personName,
        personTel: booking.personTel,
        appointmentTime: booking.appointmentTime,
        remark: booking.remark,
        openTimes: booking.openTimes,
        payWay: payWay,
        receivedAmount: receivedAmount
      }
    },
    // 扫码渠道映射支付方式：4 支付宝二维码 → 3，3 微信二维码 → 2
    getPayWayByPrimeRate() {
      return this.primeRate === '4' ? '3' : '2'
    },
    buildScanPayload() {
      const booking = this.booking || {}
      return {
        communityId: booking.communityId,
        spaceId: booking.spaceId,
        personName: booking.personName,
        personTel: booking.personTel,
        appointmentTime: booking.appointmentTime,
        remark: booking.remark,
        openTimes: booking.openTimes,
        receivedAmount: this.receivableAmount,
        primeRate: this.primeRate,
        payWay: this.getPayWayByPrimeRate()
      }
    },
    async handleCashSubmit() {
      if (this.paying) {
        return
      }
      const receivedAmount = (this.receivedAmount || '').trim()
      if (!this.verifyAmount(receivedAmount)) {
        return
      }
      if (Number(receivedAmount) > Number(this.receivableAmount)) {
        this.$message.warning(this.$t('reportCommunitySpace.receivedAmountExceed'))
        return
      }
      this.paying = true
      try {
        const res = await saveCommunitySpacePerson(this.buildBookingParams('1', receivedAmount))
        this.dealPaySuccess(res)
      } catch (error) {
        this.$message.error(error.message || this.$t('reportCommunitySpace.payFailed'))
      } finally {
        this.paying = false
      }
    },
    async checkOpenTimeAvailable() {
      const booking = this.booking || {}
      if (!booking.spaceId || !booking.appointmentTime) {
        return true
      }
      const hours = []
      const openTimes = booking.openTimes || []
      openTimes.forEach(item => {
        if (item && item.hours !== undefined && item.hours !== null && item.hours !== '') {
          hours.push(Number(item.hours))
        }
      })
      if (hours.length < 1) {
        return true
      }
      try {
        const res = await listCommunitySpaceOpenTime({
          venueId: booking.venueId,
          spaceId: booking.spaceId,
          appointmentTime: booking.appointmentTime,
          communityId: booking.communityId
        })
        if (!res || res.code !== 0) {
          return true
        }
        const openList = res.data || []
        for (let index = 0; index < hours.length; index++) {
          const matched = openList.filter(item => Number(item.hours) === hours[index])
          if (matched.length < 1 || matched[0].isOpen !== 'Y') {
            return false
          }
        }
        return true
      } catch (error) {
        return true
      }
    },
    async handleAuthCode() {
      if (this.paying) {
        return
      }
      const authCode = (this.authCode || '').trim()
      if (!authCode) {
        this.$message.warning(this.$t('reportCommunitySpace.authCodeRequired'))
        this.focusAuthCode()
        return
      }
      this.paying = true
      this.payStatus = 'paying'
      try {
        const available = await this.checkOpenTimeAvailable()
        if (!available) {
          this.paying = false
          this.payStatus = 'waiting'
          this.$message.warning(this.$t('reportCommunitySpace.slotOccupied'))
          this.resetAuthCode()
          return
        }
        const payload = this.buildScanPayload()
        payload.authCode = authCode
        payload.subServiceCode = 'communitySpace.saveCommunitySpacePerson'
        const res = await qrCodePayment(payload)
        this.dealPayResponse(res)
      } catch (error) {
        this.paying = false
        this.payStatus = 'waiting'
        this.$message.error(error.message || this.$t('reportCommunitySpace.payFailed'))
        this.resetAuthCode()
      }
    },
    dealPayResponse(res) {
      if (!res) {
        this.paying = false
        this.payStatus = 'waiting'
        this.$message.error(this.$t('reportCommunitySpace.payFailed'))
        this.resetAuthCode()
        return
      }
      if (res.code === 0) {
        this.dealPaySuccess(res)
        return
      }
      if (res.code === 404 || res.code === 41) {
        if (res.code === 404) {
          this.$message.warning(res.msg || this.$t('reportCommunitySpace.paying'))
        }
        if (!res.data || !res.data.orderId) {
          this.paying = false
          this.payStatus = 'waiting'
          this.resetAuthCode()
          return
        }
        this.pollingOrderId = res.data.orderId
        this.pollingPaymentPoolId = res.data.paymentPoolId
        this.retryCount = 0
        this.startCheckPayFinish(5000)
        return
      }
      this.paying = false
      this.payStatus = 'waiting'
      this.$message.error(res.msg || this.$t('reportCommunitySpace.payFailed'))
      this.resetAuthCode()
    },
    dealPaySuccess(res) {
      this.clearCheckTimer()
      this.paying = false
      this.payStatus = 'success'
      const data = res && res.data ? res.data : {}
      this.$message.success(this.$t('reportCommunitySpace.paySuccess'))
      this.$emit('success', data)
      this.handleClose()
    },
    startCheckPayFinish(delay) {
      this.clearCheckTimer()
      this.checkTimer = setTimeout(() => {
        this.checkTimer = null
        this.requestCheckPayFinish()
      }, delay || 5000)
    },
    clearCheckTimer() {
      if (this.checkTimer) {
        clearTimeout(this.checkTimer)
        this.checkTimer = null
      }
    },
    async requestCheckPayFinish() {
      if (!this.visible || !this.pollingOrderId) {
        return
      }
      const payload = this.buildScanPayload()
      payload.authCode = (this.authCode || '').trim()
      payload.subServiceCode = 'communitySpace.saveCommunitySpacePerson'
      payload.orderId = this.pollingOrderId
      payload.paymentPoolId = this.pollingPaymentPoolId
      try {
        const res = await checkPayFinish(payload)
        if (!res) {
          this.retryPayCheck()
          return
        }
        if (res.code === 41) {
          this.retryPayCheck()
          return
        }
        if (res.code === 0) {
          this.dealPaySuccess(res)
          return
        }
        this.paying = false
        this.payStatus = 'waiting'
        this.$message.error(res.msg || this.$t('reportCommunitySpace.payFailed'))
        this.resetAuthCode()
      } catch (error) {
        this.retryPayCheck()
      }
    },
    retryPayCheck() {
      this.retryCount++
      if (this.retryCount < this.MAX_RETRY) {
        this.startCheckPayFinish(5000)
        return
      }
      this.paying = false
      this.payStatus = 'waiting'
      this.$message.warning(this.$t('reportCommunitySpace.payTimeout'))
      this.resetAuthCode()
    }
  }
}
</script>

<style lang="scss" scoped>
.pay-body {
  text-align: left;
}

.summary-box {
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
  font-weight: 700;
  font-size: 16px;
}

.pay-form ::v-deep .el-form-item {
  margin-bottom: 14px;
  text-align: left;
}

.field-tip {
  font-size: 12px;
  color: #909399;
  line-height: 20px;
}

.pay-status {
  font-size: 13px;
}

.pay-status-waiting {
  color: #d97706;
}

.pay-status-paying {
  color: #2563eb;
}

.pay-status-success {
  color: #059669;
}
</style>
