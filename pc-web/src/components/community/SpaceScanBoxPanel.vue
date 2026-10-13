<template>
  <div class="scan-panel">
    <div class="panel-header">
      <span class="panel-title">{{ $t('communitySpaceConfirm.scanBoxTitle') }}</span>
    </div>

    <div class="panel-body">
      <p class="panel-tip">{{ $t('communitySpaceConfirm.scanBoxTip') }}</p>

      <div class="scan-box" :class="{ 'scan-box-active': focused }">
        <div class="scan-icon">
          <i class="el-icon-full-screen"></i>
        </div>
        <div class="scan-hint">{{ $t('communitySpaceConfirm.scanBoxScanTip') }}</div>
        <el-input
          ref="scanInput"
          v-model="code"
          class="scan-input"
          :placeholder="$t('communitySpaceConfirm.scanBoxPlaceholder')"
          autocomplete="off"
          @keyup.enter.native="handleEnter"
          @input="handleInput"
          @focus="handleFocus"
          @blur="handleBlur"
        />
        <div class="focus-state" :class="focused ? 'focus-on' : 'focus-off'">
          <i class="status-dot" :class="focused ? 'dot-on' : 'dot-off'"></i>
          <span v-if="focused">{{ $t('communitySpaceConfirm.focusOn') }} · {{ focusTime }}</span>
          <span v-else>{{ $t('communitySpaceConfirm.focusOff') }}</span>
        </div>
      </div>

      <div class="switch-row">
        <span class="switch-label">{{ $t('communitySpaceConfirm.autoVerifyLabel') }}</span>
        <el-switch :value="autoVerify" @change="handleAutoChange" />
      </div>

      <div class="meta-row">
        <span>{{ $t('communitySpaceConfirm.focusIntervalTip') }}</span>
        <span>{{ $t('communitySpaceConfirm.scanCountTip', { count: scanCount }) }}</span>
      </div>

      <p class="panel-tip bottom-tip">{{ $t('communitySpaceConfirm.manualInputTip') }}</p>
    </div>
  </div>
</template>

<script>
const FOCUS_INTERVAL = 2000
const MANUAL_INPUT_INTERVAL = 1000
const FOCUSABLE_TAGS = ['INPUT', 'TEXTAREA', 'SELECT']

export default {
  name: 'SpaceScanBoxPanel',
  props: {
    autoVerify: {
      type: Boolean,
      default: true
    },
    scanCount: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      code: '',
      focused: false,
      focusTime: '',
      focusTimer: null,
      inputStartTime: 0
    }
  },
  mounted() {
    this.refreshFocusTime()
    this.focusTimer = setInterval(this.handleFocusInterval, FOCUS_INTERVAL)
    setTimeout(() => {
      this.focusScanBox(true)
    }, 120)
  },
  beforeDestroy() {
    if (this.focusTimer) {
      clearInterval(this.focusTimer)
      this.focusTimer = null
    }
  },
  methods: {
    getNativeInput() {
      const input = this.$refs.scanInput
      if (!input) {
        return null
      }
      if (input.$refs && input.$refs.input) {
        return input.$refs.input
      }
      return null
    },
    isTypingElsewhere(nativeInput) {
      const active = document.activeElement
      if (!active || active === nativeInput) {
        return false
      }
      return FOCUSABLE_TAGS.indexOf(active.tagName) > -1
    },
    focusScanBox(force) {
      const nativeInput = this.getNativeInput()
      if (!nativeInput) {
        return
      }
      if (this.isTypingElsewhere(nativeInput) && !force) {
        return
      }
      if (document.activeElement !== nativeInput) {
        nativeInput.focus()
      }
      this.focused = true
      this.refreshFocusTime()
    },
    handleFocusInterval() {
      const nativeInput = this.getNativeInput()
      if (!nativeInput) {
        return
      }
      if (!this.isTypingElsewhere(nativeInput) && document.activeElement !== nativeInput) {
        nativeInput.focus()
      }
      this.focused = document.activeElement === nativeInput
      this.refreshFocusTime()
    },
    refreshFocusTime() {
      const now = new Date()
      this.focusTime = this.pad(now.getHours()) + ':' + this.pad(now.getMinutes()) + ':' + this.pad(now.getSeconds())
    },
    pad(value) {
      return value < 10 ? '0' + value : '' + value
    },
    handleFocus() {
      this.focused = true
    },
    handleBlur() {
      this.focused = false
    },
    handleAutoChange(value) {
      this.$emit('update:autoVerify', value)
    },
    handleInput() {
      if (!this.inputStartTime) {
        this.inputStartTime = Date.now()
      }
    },
    handleEnter() {
      this.emitScan()
    },
    emitScan() {
      const value = this.code ? this.code.trim() : ''
      if (!value) {
        return
      }
      const manual = this.inputStartTime > 0 && Date.now() - this.inputStartTime >= MANUAL_INPUT_INTERVAL
      this.inputStartTime = 0
      this.code = ''
      this.$emit('scan', value, manual)
    },
    focus() {
      setTimeout(() => {
        this.focusScanBox(true)
      }, 30)
    },
    clear() {
      this.inputStartTime = 0
      this.code = ''
      this.focus()
    }
  }
}
</script>

<style lang="scss" scoped>
.scan-panel {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px -4px rgba(15, 23, 42, 0.12);
}

.panel-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot-on {
  background: #10b981;
}

.dot-off {
  background: #f59e0b;
}

.panel-body {
  flex: 1;
  padding: 16px;
}

.panel-tip {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.6;
}

.scan-box {
  margin-top: 14px;
  padding: 16px;
  border: 2px solid #e2e8f0;
  border-radius: 16px;
  background: #f8fafc;
  transition: all 0.15s;
}

.scan-box-active {
  border-color: #93c5fd;
  background: #f0f7ff;
}

.scan-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto;
  border-radius: 14px;
  background: #0f172a;
  color: #93c5fd;
  font-size: 26px;
  line-height: 52px;
  text-align: center;
}

.scan-hint {
  margin-top: 10px;
  font-size: 12px;
  color: #64748b;
  text-align: center;
}

.scan-input {
  margin-top: 12px;
}

.scan-input ::v-deep .el-input__inner {
  height: 48px;
  border-radius: 12px;
  border: 2px solid #bfdbfe;
  font-size: 18px;
  font-family: Consolas, Monaco, monospace;
  letter-spacing: 0.3em;
  text-align: center;
  color: #1e293b;
}

.scan-input ::v-deep .el-input__inner:focus {
  border-color: #2563eb;
}

.scan-input ::v-deep .el-input__inner::placeholder {
  font-size: 14px;
  letter-spacing: normal;
}

.focus-state {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
}

.focus-on {
  color: #059669;
}

.focus-off {
  color: #d97706;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  font-size: 13px;
  color: #475569;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.bottom-tip {
  margin-top: 10px;
}
</style>
