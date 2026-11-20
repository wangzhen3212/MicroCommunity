<template>
  <div class="print-contract-container">
    <div class="print-toolbar no-print">
      <div class="print-toolbar-title">{{ $t('printContract.title') }}</div>
      <div class="print-toolbar-actions">
        <el-button type="primary" size="small" @click="_printContractDiv">
          <i class="el-icon-printer"></i>&nbsp;{{ $t('printContract.print') }}
        </el-button>
        <el-button type="warning" size="small" @click="_closePage">
          {{ $t('printContract.close') }}
        </el-button>
      </div>
    </div>

    <div v-if="context" class="print-meta no-print" :title="$t('printContract.meta')">
      {{ versionTip }}
    </div>

    <div v-if="context" class="cc-print-doc" v-html="context"></div>
    <div v-else-if="renderFail" class="print-empty no-print">{{ $t('printContract.renderFail') }}</div>
    <div v-else class="print-empty no-print">{{ $t('printContract.empty') }}</div>
  </div>
</template>

<script>
import { getPrintContractTemplate } from '@/api/contract/printContractApi'
import { getCommunityId } from '@/api/community/communityApi'
import { amountToChinese, renderContractContent, sanitizeHtml } from '@/utils/contractUtil'

const SOURCE_KEYS = {
  content: 'printContract.sourceContent',
  rendered: 'printContract.sourceRendered',
  template: 'printContract.sourceTemplate'
}

export default {
  name: 'PrintContractList',
  data() {
    return {
      communityId: '',
      contractId: '',
      contract: {},
      templateContext: '',
      baseRepalce: [],
      printContent: '',
      contentSource: '',
      contentVersion: 0,
      renderFail: false,
      context: ''
    }
  },
  computed: {
    versionTip() {
      return this.$t('printContract.versionTip', {
        version: this.contentVersion || 0,
        source: this.sourceText
      })
    },
    sourceText() {
      const key = SOURCE_KEYS[this.contentSource]
      return key ? this.$t(key) : '-'
    }
  },
  created() {
    this.communityId = getCommunityId()
    this.contractId = this.$route.query.contractId
    this._loadContract()
  },
  methods: {
    _loadContract() {
      const param = {
        page: 1,
        row: 1,
        contractId: this.contractId,
        communityId: this.communityId
      }

      getPrintContractTemplate(param)
        .then(res => {
          const _data = res.data || {}
          this.contract = (_data.contract || [])[0] || {}
          const templates = _data.contractTypeTemplate || []
          this.templateContext = templates.length ? (templates[0].context || '') : ''
          this.baseRepalce = _data.baseRepalce || []
          this.contentSource = _data.contentSource || ''
          this.contentVersion = _data.contentVersion || 0
          this.printContent = _data.printContent || ''
          this._render()
        })
        .catch(error => {
          this.renderFail = true
          this.$message.error(error.message)
        })
    },
    _render() {
      this._setTitle()
      let html = this.printContent
      if (!html) {
        if (!this.templateContext) {
          this.context = ''
          return
        }
        html = renderContractContent(this.templateContext, this._buildVariables())
      }
      html = this._applyLegacyPlaceholders(html)
      this.context = sanitizeHtml(html)
    },
    _buildVariables() {
      const contract = this.contract || {}
      return {
        contractName: contract.contractName,
        contractCode: contract.contractCode,
        amount: contract.amount,
        amountText: amountToChinese(contract.amount),
        startTime: contract.startTime,
        endTime: contract.endTime,
        partyA: contract.partyA,
        aContacts: contract.aContacts,
        aLink: contract.aLink,
        partyB: contract.partyB,
        bContacts: contract.bContacts,
        bLink: contract.bLink,
        operator: contract.operator,
        operatorLink: contract.operatorLink,
        signingTime: contract.signingTime,
        houses: contract.houses || ''
      }
    },
    _applyLegacyPlaceholders(html) {
      if (!html || !this.baseRepalce.length) {
        return html
      }
      const contract = this.contract || {}
      return this.baseRepalce.reduce((content, item) => {
        if (!item || !item.name || !item.key) {
          return content
        }
        if (!Object.prototype.hasOwnProperty.call(contract, item.key)) {
          return content
        }
        const value = contract[item.key]
        return content.split('#' + item.name + '#').join(value === null || value === undefined ? '' : String(value))
      }, html)
    },
    _setTitle() {
      const code = this.contract && this.contract.contractCode ? this.contract.contractCode + '-' : ''
      document.title = code + this.$t('printContract.title')
    },
    _printContractDiv() {
      window.print()
    },
    _closePage() {
      window.opener = null
      window.close()
    }
  }
}
</script>

<style lang="scss" scoped>
.print-contract-container {
  padding: 20px;
  text-align: left;
  background: #f5f6f8;
}

.print-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 21cm;
  margin: 0 auto 12px;
}

.print-toolbar-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.print-meta {
  max-width: 21cm;
  margin: 0 auto 8px;
  font-size: 12px;
  color: #909399;
}

.print-empty {
  max-width: 21cm;
  margin: 0 auto;
  padding: 40px 0;
  text-align: center;
  font-size: 14px;
  color: #909399;
  background: #fff;
}

.cc-print-doc {
  width: 21cm;
  min-height: 29.7cm;
  margin: 0 auto;
  padding: 18mm;
  background: #fff;
  text-align: left;
  line-height: 1.9;
  font-size: 14px;
  word-break: break-word;
  box-sizing: border-box;

  ::v-deep table {
    width: 100%;
    border-collapse: collapse;
  }

  ::v-deep td,
  ::v-deep th {
    border: 1px solid #333;
    padding: 4px 8px;
  }

  ::v-deep img {
    max-width: 100%;
  }
}

@media print {
  .no-print {
    display: none !important;
  }

  .print-contract-container {
    padding: 0;
    background: #fff;
  }

  .cc-print-doc {
    width: auto;
    min-height: auto;
    margin: 0;
    padding: 0;
  }

  body {
    margin: 0;
  }
}
</style>
