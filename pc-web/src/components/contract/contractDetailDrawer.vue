<template>
  <el-drawer
    :visible.sync="visible"
    :size="620"
    :with-header="false"
    custom-class="cc-drawer"
    @close="handleClose"
  >
    <div v-if="contract" class="cc-drawer-body">
      <div class="cc-drawer-header">
        <div>
          <div class="cc-drawer-title">
            {{ contract.contractName }}
            <contract-status-tag
              :state="contract.state"
              :state-name="contract.stateName"
              :category="contract.category"
              :days="days"
              show-category
              show-days
            />
          </div>
          <div class="cc-drawer-eyebrow">{{ contract.contractCode }}</div>
        </div>
        <i class="el-icon-close cc-drawer-close" @click="handleClose"></i>
      </div>

      <div class="cc-drawer-section">
        <div class="cc-section-title">{{ $t('contractDetailDrawer.summary') }}</div>
        <div class="cc-kv">
          <span class="cc-kv-label">{{ $t('contractDetailDrawer.amount') }}</span>
          <span class="cc-kv-value">{{ formatAmount(contract.amount) }}</span>
        </div>
        <div class="cc-kv">
          <span class="cc-kv-label">{{ $t('contractDetailDrawer.amountText') }}</span>
          <span class="cc-kv-value">{{ amountText }}</span>
        </div>
        <div class="cc-kv">
          <span class="cc-kv-label">{{ $t('contractDetailDrawer.term') }}</span>
          <span class="cc-kv-value">{{ formatTerm(contract.startTime, contract.endTime) }}</span>
        </div>
        <div class="cc-kv">
          <span class="cc-kv-label">{{ $t('contractDetailDrawer.signTime') }}</span>
          <span class="cc-kv-value">{{ formatTime(contract.signingTime) }}</span>
        </div>
        <div class="cc-kv">
          <span class="cc-kv-label">{{ $t('contractDetailDrawer.partyA') }}</span>
          <span class="cc-kv-value">{{ contract.partyA || '-' }}</span>
        </div>
        <div class="cc-kv">
          <span class="cc-kv-label">{{ $t('contractDetailDrawer.partyB') }}</span>
          <span class="cc-kv-value">{{ contract.partyB || '-' }} {{ contract.bLink || '' }}</span>
        </div>
        <div class="cc-kv">
          <span class="cc-kv-label">{{ $t('contractDetailDrawer.operator') }}</span>
          <span class="cc-kv-value">{{ contract.operator || '-' }} {{ contract.operatorLink || '' }}</span>
        </div>
        <div class="cc-kv">
          <span class="cc-kv-label">{{ $t('contractDetailDrawer.payMode') }}</span>
          <span class="cc-kv-value">{{ contract.payMode || '-' }}</span>
        </div>
      </div>

      <div class="cc-drawer-section">
        <div class="cc-section-title">{{ $t('contractDetailDrawer.targets') }}</div>
        <el-table v-if="rooms.length" :data="rooms" size="mini" border>
          <el-table-column :label="$t('contractDetailDrawer.house')">
            <template slot-scope="scope">
              {{ scope.row.roomName || ((scope.row.floorNum || '') + '-' + (scope.row.unitNum || '') + '-' + (scope.row.roomNum || '')) }}
            </template>
          </el-table-column>
          <el-table-column prop="ownerName" :label="$t('contractDetailDrawer.owner')" align="center" />
          <el-table-column :label="$t('contractDetailDrawer.area')" align="center">
            <template slot-scope="scope">{{ scope.row.builtUpArea || '-' }}</template>
          </el-table-column>
        </el-table>
        <div v-else class="cc-empty">{{ $t('contractWorkbench.target.empty') }}</div>
      </div>

      <div class="cc-drawer-section">
        <div class="cc-section-title">{{ $t('contractDetailDrawer.plans') }}</div>
        <el-table v-if="fees.length" :data="fees" size="mini" border>
          <el-table-column prop="feeName" :label="$t('contractDetailDrawer.feeItem')" />
          <el-table-column :label="$t('contractDetailDrawer.formula')" align="center">
            <template slot-scope="scope">{{ scope.row.computingFormulaName || '-' }}</template>
          </el-table-column>
          <el-table-column :label="$t('contractDetailDrawer.unitPrice')" align="center">
            <template slot-scope="scope">{{ formatAmount(scope.row.unitPrice) }}</template>
          </el-table-column>
          <el-table-column :label="$t('contractDetailDrawer.feeAmount')" align="center">
            <template slot-scope="scope">{{ formatAmount(scope.row.amount) }}</template>
          </el-table-column>
          <el-table-column :label="$t('contractDetailDrawer.include')" align="center" width="80">
            <template slot-scope="scope">
              <span class="cc-badge" :class="includeClass(scope.row.includeInAmount)">
                {{ includeText(scope.row.includeInAmount) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="fees.length" class="cc-fee-summary">
          <span>{{ $t('contractDetailDrawer.feeTotal') }}：{{ formatAmount(feeSummary.feeTotal) }}</span>
          <span>{{ $t('contractDetailDrawer.includeTotal') }}：{{ formatAmount(feeSummary.includeTotal) }}</span>
        </div>
        <el-table v-if="plans.length" :data="plans" size="mini" border class="margin-top-sm">
          <el-table-column prop="planName" :label="$t('contractDetailDrawer.planName')" />
          <el-table-column :label="$t('contractDetailDrawer.planTime')" align="center">
            <template slot-scope="scope">{{ formatTime(scope.row.planTime) }}</template>
          </el-table-column>
          <el-table-column :label="$t('contractDetailDrawer.planAmount')" align="center">
            <template slot-scope="scope">{{ formatAmount(scope.row.planAmount) }}</template>
          </el-table-column>
          <el-table-column :label="$t('contractDetailDrawer.payState')" align="center" width="90">
            <template slot-scope="scope">
              <span class="cc-badge" :class="payStateClass(scope.row.payState)">{{ payStateText(scope.row.payState) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!fees.length && !plans.length" class="cc-empty">{{ $t('contractDetailDrawer.noFees') }}</div>
      </div>

      <div class="cc-drawer-section">
        <div class="cc-section-title">{{ $t('contractDetailDrawer.files') }}</div>
        <div v-if="files.length">
          <div v-for="(file, index) in files" :key="'file' + index" class="cc-file-row">
            <i class="el-icon-paperclip"></i>
            <span>{{ file.fileRealName }}</span>
          </div>
        </div>
        <div v-else class="cc-empty">{{ $t('contractDetailDrawer.noFiles') }}</div>
      </div>

      <div class="cc-drawer-section">
        <div class="cc-section-title">{{ $t('contractDetailDrawer.changes') }}</div>
        <div v-if="changes.length">
          <div v-for="(change, index) in changes" :key="'change' + index" class="cc-change-row">
            <span>{{ change.planTypeName || '-' }}</span>
            <span>{{ change.changePersonName || '-' }}</span>
            <span>{{ formatTime(change.createTime) }}</span>
            <span class="cc-badge is-info">{{ change.stateName || '-' }}</span>
          </div>
        </div>
        <div v-else class="cc-empty">{{ $t('contractDetailDrawer.noChanges') }}</div>
      </div>

      <div class="cc-drawer-section">
        <div class="cc-section-title">{{ $t('contractDetailDrawer.audit') }}</div>
        <contract-audit-flow :state="stateValue" :records="records" />
      </div>

      <div class="cc-drawer-section">
        <div class="cc-section-title">{{ $t('contractDetailDrawer.content') }}</div>
        <div v-if="content" class="cc-doc-preview" v-html="safeContent"></div>
        <div v-else class="cc-empty">{{ $t('contractDetailDrawer.noContent') }}</div>
      </div>

      <div class="cc-drawer-footer">
        <el-button size="mini" @click="$emit('print', contract)">{{ $t('contractLedger.op.print') }}</el-button>
        <el-button
          v-if="canEdit"
          size="mini"
          type="primary"
          @click="$emit('edit', contract)"
        >{{ $t('contractLedger.op.edit') }}</el-button>
        <el-button
          v-if="canSubmit"
          size="mini"
          type="success"
          @click="$emit('submit', contract)"
        >{{ $t('contractLedger.op.submit') }}</el-button>
        <el-button
          v-if="canStop"
          size="mini"
          type="warning"
          @click="$emit('stop', contract)"
        >{{ $t('contractLedger.op.stop') }}</el-button>
        <el-button
          v-if="canDelete"
          size="mini"
          type="danger"
          @click="$emit('delete', contract)"
        >{{ $t('contractLedger.op.delete') }}</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script>
import ContractStatusTag from '@/components/contract/contractStatusTag'
import ContractAuditFlow from '@/components/contract/contractAuditFlow'
import { listContractCollectionPlans } from '@/api/contract/contractPerformanceApi'
import { queryContractContent } from '@/api/contract/contractWorkbenchApi'
import { queryContractAuditFlow } from '@/api/contract/contractAuditApi'
import { queryContractRoom } from '@/api/contract/contractDetailRoomApi'
import { queryContractFile } from '@/api/contract/contractDetailFileApi'
import { queryContractChangePlan } from '@/api/contract/contractChangeManageApi'
import {
  CONTRACT_STATE,
  amountToChinese,
  daysLeft,
  formatAmount,
  formatTerm,
  parseDate,
  sanitizeHtml
} from '@/utils/contractUtil'

export default {
  name: 'ContractDetailDrawer',
  components: {
    ContractStatusTag,
    ContractAuditFlow
  },
  data() {
    return {
      visible: false,
      contract: null,
      plans: [],
      fees: [],
      feeSummary: { feeTotal: '0.00', includeTotal: '0.00' },
      rooms: [],
      files: [],
      changes: [],
      records: [],
      content: ''
    }
  },
  computed: {
    days() {
      if (!this.contract) {
        return null
      }
      return daysLeft(this.contract.endTime)
    },
    amountText() {
      return this.contract ? amountToChinese(this.contract.amount) : ''
    },
    stateValue() {
      return this.contract ? String(this.contract.state) : ''
    },
    canEdit() {
      return this.stateValue === CONTRACT_STATE.DRAFT || this.stateValue === CONTRACT_STATE.FAIL
    },
    canSubmit() {
      return this.canEdit
    },
    canStop() {
      return this.stateValue === CONTRACT_STATE.DRAFT ||
        this.stateValue === CONTRACT_STATE.AUDIT ||
        this.stateValue === CONTRACT_STATE.AUDIT_LEADER ||
        this.stateValue === CONTRACT_STATE.EFFECTIVE
    },
    canDelete() {
      return this.stateValue === CONTRACT_STATE.DRAFT
    },
    safeContent() {
      return sanitizeHtml(this.content)
    }
  },
  methods: {
    formatAmount,
    formatTerm,
    open(contract) {
      this.contract = contract
      this.visible = true
      this.plans = []
      this.fees = []
      this.feeSummary = { feeTotal: '0.00', includeTotal: '0.00' }
      this.rooms = []
      this.files = []
      this.changes = []
      this.records = []
      this.content = ''
      this.loadPlans()
      this.loadRooms()
      this.loadFiles()
      this.loadChanges()
      this.loadAuditFlow()
      this.loadContent()
    },
    handleClose() {
      this.visible = false
    },
    loadPlans() {
      listContractCollectionPlans({ contractId: this.contract.contractId }).then(data => {
        this.plans = data.plans || []
        this.fees = data.fees || []
        this.feeSummary = data.feeSummary || { feeTotal: '0.00', includeTotal: '0.00' }
      }).catch(() => {
        this.plans = []
        this.fees = []
        this.feeSummary = { feeTotal: '0.00', includeTotal: '0.00' }
      })
    },
    loadRooms() {
      queryContractRoom({
        contractId: this.contract.contractId,
        page: 1,
        row: 100
      }).then(res => {
        this.rooms = (res && res.data) || []
      }).catch(() => {
        this.rooms = []
      })
    },
    loadFiles() {
      queryContractFile({
        contractId: this.contract.contractId,
        page: 1,
        row: 100
      }).then(res => {
        this.files = (res && res.data) || []
      }).catch(() => {
        this.files = []
      })
    },
    loadChanges() {
      queryContractChangePlan({
        contractId: this.contract.contractId,
        page: 1,
        row: 20
      }).then(res => {
        this.changes = (res && res.data) || []
      }).catch(() => {
        this.changes = []
      })
    },
    loadAuditFlow() {
      queryContractAuditFlow({
        bizId: this.contract.contractId,
        bizType: '1'
      }).then(data => {
        this.records = data.records || []
      }).catch(() => {
        this.records = []
      })
    },
    loadContent() {
      queryContractContent({ contractId: this.contract.contractId }).then(data => {
        this.content = data.content || ''
      }).catch(() => {
        this.content = ''
      })
    },
    formatTime(time) {
      if (!time) {
        return '-'
      }
      if (time instanceof Date) {
        return parseDate(time) ? this.formatDate(time) : String(time)
      }
      return String(time).replace('T', ' ').substring(0, 19)
    },
    formatDate(date) {
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      return date.getFullYear() + '-' + month + '-' + day
    },
    payStateText(payState) {
      const state = String(payState)
      if (state === '1') {
        return this.$t('contractDetailDrawer.payStatePaid')
      }
      if (state === '2') {
        return this.$t('contractDetailDrawer.payStateOverdue')
      }
      return this.$t('contractDetailDrawer.payStateDoing')
    },
    payStateClass(payState) {
      const state = String(payState)
      if (state === '1') {
        return 'is-success'
      }
      if (state === '2') {
        return 'is-danger'
      }
      return 'is-info'
    },
    includeText(includeInAmount) {
      return String(includeInAmount) === '1'
        ? this.$t('contractDetailDrawer.includeYes')
        : this.$t('contractDetailDrawer.includeNo')
    },
    includeClass(includeInAmount) {
      return String(includeInAmount) === '1' ? 'is-success' : 'is-info'
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-drawer-body {
  padding: 20px;

  .cc-drawer-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 18px;

    .cc-drawer-close {
      cursor: pointer;
      font-size: 18px;
      color: #909399;
    }
  }

  .cc-doc-preview {
    border: 1px solid #ebeef5;
    border-radius: 6px;
    padding: 12px;
    max-height: 320px;
    overflow-y: auto;
    font-size: 13px;
    line-height: 1.8;

    table {
      border-collapse: collapse;
      width: 100%;

      td,
      th {
        border: 1px solid #ebeef5;
        padding: 4px 8px;
      }
    }
  }

  .cc-fee-summary {
    display: flex;
    justify-content: flex-end;
    padding: 8px 4px 0;
    font-size: 13px;
    color: #303133;

    >span {
      margin-left: 16px;
    }
  }

  .cc-file-row,
  .cc-change-row {
    display: flex;
    align-items: center;
    font-size: 13px;
    line-height: 26px;

    >* {
      margin-right: 12px;
    }
  }

  .cc-drawer-footer {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid #ebeef5;
    text-align: right;
  }
}
</style>
