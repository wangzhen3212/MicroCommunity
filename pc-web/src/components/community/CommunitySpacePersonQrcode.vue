<template>
  <el-dialog
    :title="$t('communitySpacePersonManage.verifyQrcodeTitle')"
    :visible.sync="visible"
    width="420px"
    @close="handleClose"
  >
    <div v-if="person" class="qrcode-body">
      <div class="summary-box">
        <div class="summary-row">
          <span class="summary-label">{{ $t('communitySpacePersonManage.orderId') }}</span>
          <span class="summary-value">{{ qrCodeText }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">{{ $t('communitySpacePersonManage.venue') }}</span>
          <span class="summary-value">{{ person.venueName || '-' }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">{{ $t('communitySpacePersonManage.space') }}</span>
          <span class="summary-value">{{ person.spaceName || '-' }}</span>
        </div>
        <div class="summary-row">
          <span class="summary-label">{{ $t('communitySpacePersonManage.appointmentDate') }}</span>
          <span class="summary-value">{{ person.appointmentTime || '-' }}</span>
        </div>
      </div>

      <div class="times-block">
        <div class="block-title">{{ $t('communitySpacePersonManage.verifyQrcodeTimesTitle') }}</div>
        <div class="times-list">
          <div v-for="item in timesList" :key="item.timeId" class="time-item">
            <span class="time-hours">{{ formatHourRange(item.hours) }}</span>
            <span class="time-state" :class="timeStateClass(item.state)">{{ timeStateText(item.state) }}</span>
          </div>
          <div v-if="timesList.length < 1" class="times-empty">{{ $t('communitySpacePersonManage.noTime') }}</div>
        </div>
      </div>

      <div class="qrcode-block">
        <div ref="qrCodeBox" class="qr-code-canvas"></div>
        <div class="qrcode-code">{{ qrCodeText }}</div>
        <div class="qrcode-tip">{{ qrcodeTip }}</div>
      </div>
    </div>
    <span slot="footer">
      <el-button @click="handleClose">{{ $t('common.close') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import QRCode from 'qrcodejs2'

export default {
  name: 'CommunitySpacePersonQrcode',
  data() {
    return {
      visible: false,
      person: null
    }
  },
  computed: {
    timesList() {
      if (!this.person || !this.person.times) {
        return []
      }
      return this.person.times.slice().sort((a, b) => Number(a.hours) - Number(b.hours))
    },
    qrCodeText() {
      if (!this.person) {
        return ''
      }
      return this.person.orderId || this.person.cspId || ''
    },
    qrcodeTip() {
      if (!this.person) {
        return ''
      }
      if (this.person.state === 'WP') {
        return this.$t('communitySpacePersonManage.verifyQrcodeUnpaidTip')
      }
      if (this.person.state === 'C') {
        return this.$t('communitySpacePersonManage.verifyQrcodeDoneTip')
      }
      if (this.person.state === 'CL') {
        return this.$t('communitySpacePersonManage.verifyQrcodeCancelTip')
      }
      return this.$t('communitySpacePersonManage.verifyQrcodeScanTip')
    }
  },
  methods: {
    open(row) {
      this.person = row
      this.visible = true
      this.$nextTick(() => {
        this.renderQrcode()
      })
    },
    renderQrcode() {
      const qrCodeBox = this.$refs.qrCodeBox
      if (!qrCodeBox || !this.qrCodeText) {
        return
      }
      qrCodeBox.innerHTML = ''
      new QRCode(qrCodeBox, {
        text: this.qrCodeText,
        width: 220,
        height: 220,
        colorDark: '#000000',
        colorLight: '#ffffff',
        correctLevel: QRCode.CorrectLevel.M
      })
    },
    handleClose() {
      if (this.$refs.qrCodeBox) {
        this.$refs.qrCodeBox.innerHTML = ''
      }
      this.person = null
      this.visible = false
    },
    formatHourRange(hours) {
      const value = Number(hours)
      return this.formatHour(value) + '~' + this.formatHour(value + 1)
    },
    formatHour(value) {
      return (value < 10 ? '0' + value : '' + value) + ':00'
    },
    timeStateText(state) {
      if (state === 'C') {
        return this.$t('communitySpacePersonManage.timeStateDone')
      }
      if (state === 'CL') {
        return this.$t('communitySpacePersonManage.timeStateCanceled')
      }
      return this.$t('communitySpacePersonManage.timeStateWait')
    },
    timeStateClass(state) {
      if (state === 'C') {
        return 'time-done'
      }
      if (state === 'CL') {
        return 'time-canceled'
      }
      return 'time-wait'
    }
  }
}
</script>

<style lang="scss" scoped>
.qrcode-body {
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

.block-title {
  margin-bottom: 8px;
  font-size: 13px;
  color: #64748b;
}

.time-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 10px;
  border-radius: 6px;
  background: #f8fafc;
  margin-bottom: 6px;
}

.time-hours {
  color: #334155;
  font-size: 13px;
}

.time-state {
  font-size: 12px;
}

.time-done {
  color: #10b981;
}

.time-wait {
  color: #f59e0b;
}

.time-canceled {
  color: #94a3b8;
}

.times-empty {
  color: #94a3b8;
  font-size: 13px;
}

.qrcode-block {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
  text-align: center;
}

.qr-code-canvas {
  display: inline-block;
  padding: 8px;
  background: #fff;
  border: 1px solid #f1f5f9;
  border-radius: 8px;
}

.qrcode-code {
  margin-top: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  word-break: break-all;
}

.qrcode-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 20px;
}
</style>
