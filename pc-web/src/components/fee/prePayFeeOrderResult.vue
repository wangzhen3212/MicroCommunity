<template>
  <el-dialog
    :title="$t('prePayFeeOrderResult.title')"
    :visible.sync="visible"
    width="50%"
    @close="handleClose"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="true"
    :modal="true"
    :append-to-body="true">
    <p>{{ $t('prePayFeeOrderResult.success') }}</p>
    <p v-if="!resultInfo.receiptIds">{{ $t('prePayFeeOrderResult.receiptTip') }}</p>
    <div slot="footer" class="dialog-footer">
      <el-button @click="_back()">{{ $t('prePayFeeOrderResult.back') }}</el-button>
      <el-button v-if="resultInfo.receiptIds" type="primary" @click="_printSmall()">{{ $t('prePayFeeOrderResult.printSmall') }}</el-button>
      <el-button v-if="resultInfo.receiptIds" type="primary" @click="_print('ON')">{{ $t('prePayFeeOrderResult.mergePrint') }}</el-button>
      <el-button v-if="resultInfo.receiptIds" type="primary" @click="_print('OFF')">{{ $t('prePayFeeOrderResult.printReceipt') }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { queryFeeReceipt, listFeePrintPage } from '@/api/fee/feeReceiptApi'

export default {
  name: 'PrePayFeeOrderResult',
  data() {
    return {
      visible: false,
      resultInfo: {
        receiptCode: '',
        receiptIds: '',
        printUrl: '/#/pages/property/printPayFee'
      }
    }
  },
  created() {
    this._listFeePrintPage()
  },
  methods: {
    open(data) {
      this.resultInfo.receiptCode = data && data.receiptCode ? data.receiptCode : ''
      this.resultInfo.receiptIds = ''
      this.visible = true
      this._queryFeeReceipt()
    },

    handleClose() {
      this.resultInfo.receiptIds = ''
      this.$emit('closed')
    },

    _back() {
      this.visible = false
    },

    async _queryFeeReceipt() {
      if (!this.resultInfo.receiptCode) {
        return
      }
      try {
        const params = {
          receiptCode: this.resultInfo.receiptCode,
          page: 1,
          row: 100
        }
        const response = await queryFeeReceipt(params)
        if (response && response.data && response.data.length > 0) {
          const receiptIds = []
          response.data.forEach(item => {
            receiptIds.push(item.receiptId)
          })
          this.resultInfo.receiptIds = receiptIds.join(',')
        }
      } catch (error) {
        console.error('查询收据失败', error)
      }
    },

    _print(merge) {
      window.open(this.resultInfo.printUrl + '?receiptIds=' + this.resultInfo.receiptIds + '&merge=' + merge)
    },

    _printSmall() {
      window.open('/#/pages/property/printSmallPayFee?receiptIds=' + this.resultInfo.receiptIds)
    },

    async _listFeePrintPage() {
      try {
        const params = {
          page: 1,
          row: 1,
          state: 'T'
        }
        const response = await listFeePrintPage(params)
        if (response && response.data && response.data.length > 0) {
          this.resultInfo.printUrl = response.data[0].url
        }
      } catch (error) {
        console.error('查询打印页面配置失败', error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.dialog-footer {
  text-align: right;
}
</style>
