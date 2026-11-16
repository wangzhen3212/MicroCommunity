<template>
  <div class="cc-workbench">
    <div class="cc-wb-head">
      <button
        type="button"
        class="cc-wb-back"
        :title="$t('contractWorkbench.back')"
        @click="handleBack"
      >
        <i class="el-icon-arrow-left"></i>
      </button>
      <div class="cc-wb-title">
        <div class="cc-wb-title-row">
          <span class="cc-wb-name">{{ pageTitle }}</span>
          <span class="cc-wb-tag" :class="auditState ? 'is-audit' : 'is-draft'">
            {{ auditState ? $t('contractState.auditing') : $t('contractState.draft') }}
          </span>
        </div>
        <p class="cc-wb-sub">
          {{ $t('contractWorkbench.field.contractCode') }}
          <span class="cc-wb-sub-strong">{{ form.contractCode || '-' }}</span>
          <span class="cc-wb-dot">·</span>
          {{ $t('contractWorkbench.field.template') }}
          <span class="cc-wb-sub-strong">{{ templateName || '-' }}</span>
        </p>
      </div>
      <div class="cc-wb-head-actions">
        <span class="cc-wb-tip">{{ $t('contractWorkbench.autoSaveTip') }}</span>
        <el-button size="small" :loading="saving" @click="handleSaveDraft">{{ $t('contractWorkbench.saveDraft') }}</el-button>
        <el-button size="small" :loading="previewing" @click="handlePreview">{{ $t('contractWorkbench.preview') }}</el-button>
        <el-button size="small" type="primary" :loading="submitting" @click="handleSubmit">
          {{ $t('contractWorkbench.submit') }}
        </el-button>
      </div>
    </div>

    <div class="cc-wb-body" v-loading="loading">
      <div class="cc-wb-main">
        <div class="cc-panel">
          <div class="cc-panel-head">
            <span class="cc-panel-index">01</span>
            <h4 class="cc-panel-title">{{ $t('contractWorkbench.section.base') }}</h4>
          </div>
          <div class="cc-panel-body">
            <el-form label-position="top" class="cc-wb-form">
              <contract-base-info-form
                :form="form"
                :contract-types="contractTypes"
                :partyas="partyas"
                :templates="templates"
                @regenerate-code="handleGenerateCode"
                @type-change="handleTypeChange"
                @template-change="handleTemplateChange"
                @partya-change="handlePartyaChange"
                @amount-change="handleAmountChange"
              />
            </el-form>
          </div>
        </div>

        <div class="cc-panel">
          <div class="cc-panel-head">
            <span class="cc-panel-index">02</span>
            <h4 class="cc-panel-title">{{ $t('contractWorkbench.section.target') }}</h4>
          </div>
          <div class="cc-panel-body">
            <contract-target-table
              :category="form.category"
              :rooms="rooms"
              :targets="targets"
              @add-house="openHouseDialog"
              @remove-house="removeHouse"
              @add-target="addTarget"
              @remove-target="removeTarget"
              @fill-amount="handleFillAmount"
              @clear="clearTargets"
            />
          </div>
        </div>

        <div class="cc-panel">
          <div class="cc-panel-head">
            <span class="cc-panel-index">03</span>
            <h4 class="cc-panel-title">{{ $t('contractWorkbench.section.plan') }}</h4>
          </div>
          <div class="cc-panel-body">
            <contract-fee-plan-panel
              :category="form.category"
              :rooms="rooms"
              :plans="plans"
              :amount="form.amount"
              :start-time="form.startTime"
              :end-time="form.endTime"
              :pay-mode="form.payMode"
              @amount-change="handleAmountChange"
              @plan-change="handlePlanChange"
              @fee-change="handleFeeChange"
            />
          </div>
        </div>

        <div class="cc-panel">
          <div class="cc-panel-head">
            <span class="cc-panel-index">04</span>
            <h4 class="cc-panel-title">{{ $t('contractWorkbench.section.doc') }}</h4>
          </div>
          <div class="cc-panel-body">
            <contract-doc-editor
              v-model="content"
              :template-content="templateContent"
              :variables="docVariables"
              :readonly="readonly"
            />
          </div>
        </div>

        <div class="cc-panel">
          <div class="cc-panel-head">
            <span class="cc-panel-index">05</span>
            <h4 class="cc-panel-title">{{ $t('contractWorkbench.section.file') }}</h4>
          </div>
          <div class="cc-panel-body">
            <contract-file-panel :files="files" :readonly="readonly" @change="handleFileChange" />
          </div>
        </div>
      </div>


      <div class="cc-wb-side">
        <contract-summary-panel
          ref="summaryPanel"
          :form="form"
          :target-count="targetCount"
          :plan-count="plans.length"
          :has-content="!!content"
          :audit="audit"
          :records="records"
        />
      </div>
    </div>

    <contract-house-dialog ref="houseDialog" @choose="handleChooseRooms" />
    <contract-template-preview ref="templatePreview" />
  </div>
</template>

<script>
import ContractBaseInfoForm from '@/components/contract/workbench/ContractBaseInfoForm'
import ContractTargetTable from '@/components/contract/workbench/ContractTargetTable'
import ContractHouseDialog from '@/components/contract/workbench/ContractHouseDialog'
import ContractFeePlanPanel from '@/components/contract/workbench/ContractFeePlanPanel'
import ContractDocEditor from '@/components/contract/workbench/ContractDocEditor'
import ContractFilePanel from '@/components/contract/workbench/ContractFilePanel'
import ContractSummaryPanel from '@/components/contract/workbench/ContractSummaryPanel'
import ContractTemplatePreview from '@/components/contract/contractTemplatePreview'
import { queryContract, queryContractType } from '@/api/contract/contractManageApi'
import { queryContractPartya } from '@/api/contract/contractPartyaApi'
import { listContractTemplates, queryContractTypeTemplate } from '@/api/contract/contractTemplateApi'
import {
  generateContractCode,
  previewContractContent,
  queryContractContent,
  saveContract,
  updateContract
} from '@/api/contract/contractWorkbenchApi'
import { listContractCollectionPlans } from '@/api/contract/contractPerformanceApi'
import { queryContractAuditFlow, submitContractAudit } from '@/api/contract/contractAuditApi'
import { queryContractRoom } from '@/api/contract/contractDetailRoomApi'
import { queryContractFile } from '@/api/contract/contractDetailFileApi'
import { amountToChinese, guessCategory, sanitizeHtml } from '@/utils/contractUtil'

export default {
  name: 'AddContractList',
  components: {
    ContractBaseInfoForm,
    ContractTargetTable,
    ContractHouseDialog,
    ContractFeePlanPanel,
    ContractDocEditor,
    ContractFilePanel,
    ContractSummaryPanel,
    ContractTemplatePreview
  },
  data() {
    return {
      saving: false,
      submitting: false,
      previewing: false,
      loading: false,
      dirty: false,
      initializing: true,
      contractTypes: [],
      partyas: [],
      templates: [],
      templateContent: '',
      rooms: [],
      targets: [],
      plans: [],
      files: [],
      content: '',
      audit: null,
      records: [],
      form: this.createEmptyForm(),
      parentContract: null
    }
  },
  computed: {
    isEdit() {
      return !!this.form.contractId
    },
    isRenew() {
      return !this.form.contractId && !!this.parentContract
    },
    pageTitle() {
      if (this.isEdit) {
        return this.$t('contractWorkbench.titleEdit')
      }
      if (this.isRenew) {
        return this.$t('contractWorkbench.titleRenew')
      }
      return this.$t('contractWorkbench.titleNew')
    },
    auditState() {
      return String(this.form.state) === '11'
    },
    templateName() {
      if (!this.form.templateId) {
        return ''
      }
      const template = this.templates.filter(item => item.templateId === this.form.templateId)[0]
      if (!template) {
        return ''
      }
      const version = template.templateVersion ? ' ' + template.templateVersion : ''
      return (template.templateName || '') + version
    },
    readonly() {
      return this.auditState
    },
    targetCount() {
      return String(this.form.category) === '1' ? this.rooms.length : this.targets.length
    },
    docVariables() {
      return {
        amount: this.form.amount,
        amountText: amountToChinese(this.form.amount),
        startTime: this.form.startTime,
        endTime: this.form.endTime,
        partyB: this.form.partyB,
        houses: this.rooms
          .map(room => room.roomName || ((room.floorNum || '') + '-' + (room.unitNum || '') + '-' + (room.roomNum || '')))
          .join('、')
      }
    }
  },
  watch: {
    form: {
      deep: true,
      handler() {
        this.markDirty()
      }
    },
    content() {
      this.markDirty()
    },
    rooms: {
      deep: true,
      handler() {
        this.markDirty()
      }
    },
    plans: {
      deep: true,
      handler() {
        this.markDirty()
      }
    },
    targets: {
      deep: true,
      handler() {
        this.markDirty()
      }
    },
    files: {
      deep: true,
      handler() {
        this.markDirty()
      }
    }
  },
  created() {
    this.loadBaseData()
    const query = this.$route.query || {}
    setTimeout(() => {
      this.initializing = false
    }, 1500)
    if (query.contractId) {
      this.loadContract(query.contractId)
      return
    }
    if (query.parentContractId) {
      this.loadParentContract(query.parentContractId)
      return
    }
    if (query.templateId) {
      this.applyTemplate(query.templateId)
      return
    }
    this.handleGenerateCode()
  },
  methods: {
    markDirty() {
      if (this.initializing) {
        return
      }
      this.dirty = true
    },
    createEmptyForm() {
      return {
        contractId: '',
        contractName: '',
        contractCode: '',
        contractType: '',
        category: '2',
        templateId: '',
        partyaId: '',
        partyA: '',
        aContacts: '',
        aLink: '',
        partyB: '',
        bContacts: '',
        bLink: '',
        creditCode: '',
        amount: '',
        signingTime: '',
        startTime: '',
        endTime: '',
        operator: '',
        operatorLink: '',
        payMode: '',
        place: '',
        remark: '',
        state: '',
        objType: '1111',
        objId: '-1'
      }
    },
    loadBaseData() {
      queryContractType({ page: 1, row: 100 }).then(res => {
        this.contractTypes = res.data || []
      }).catch(() => {
        this.contractTypes = []
      })
      queryContractPartya({ page: 1, row: 200 }).then(res => {
        this.partyas = res.data || []
        const defaultPartya = this.partyas.filter(item => String(item.defaultFlag) === '1')[0]
        if (defaultPartya && !this.form.partyaId) {
          this.handlePartyaChange(defaultPartya.partyaId)
        }
      }).catch(() => {
        this.partyas = []
      })
      listContractTemplates({ page: 1, row: 100 }).then(res => {
        this.templates = res.data || []
      }).catch(() => {
        this.templates = []
      })
    },
    loadContract(contractId) {
      this.loading = true
      queryContract({ contractId, page: 1, row: 1 }).then(res => {
        const contract = (res.data || [])[0]
        if (!contract) {
          return
        }
        this.fillForm(contract)
        this.loadRooms(contractId)
        this.loadPlans(contractId)
        this.loadFiles(contractId)
        this.loadContent(contractId)
        this.loadAuditFlow(contractId)
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractWorkbench.msg.loadError'))
      }).finally(() => {
        this.loading = false
      })
    },
    loadParentContract(contractId) {
      queryContract({ contractId, page: 1, row: 1 }).then(res => {
        const contract = (res.data || [])[0]
        if (!contract) {
          return
        }
        this.parentContract = contract
        this.fillForm(contract, true)
        this.handleGenerateCode()
        this.loadRooms(contractId)
        this.loadContent(contractId)
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractWorkbench.msg.loadError'))
      })
    },
    fillForm(contract, isRenew) {
      const form = this.createEmptyForm()
      Object.keys(form).forEach(key => {
        if (contract[key] !== undefined && contract[key] !== null) {
          form[key] = contract[key]
        }
      })
      form.contractCode = (contract.contractCode || '') + (isRenew ? '-R' : '')
      form.category = guessCategory(contract)
      form.partyA = contract.partyA || ''
      if (isRenew) {
        form.contractId = ''
        form.state = ''
        form.contractParentId = contract.contractId
        form.startTime = contract.endTime ? contract.endTime.substring(0, 10) : ''
      } else {
        form.contractId = contract.contractId
      }
      this.form = form
    },
    loadRooms(contractId) {
      queryContractRoom({ contractId, page: 1, row: 200 }).then(res => {
        this.rooms = (res && res.data) || []
      }).catch(() => {
        this.rooms = []
      })
    },
    loadPlans(contractId) {
      listContractCollectionPlans({ contractId }).then(data => {
        this.plans = data.plans || []
      }).catch(() => {
        this.plans = []
      })
    },
    loadFiles(contractId) {
      queryContractFile({ contractId, page: 1, row: 100 }).then(res => {
        this.files = (res && res.data) || []
      }).catch(() => {
        this.files = []
      })
    },
    loadContent(contractId) {
      queryContractContent({ contractId }).then(data => {
        this.content = data.content || ''
      }).catch(() => {
        this.content = ''
      })
    },
    loadAuditFlow(contractId) {
      queryContractAuditFlow({ bizId: contractId, auditType: '1' }).then(data => {
        this.audit = data.audit || null
        this.records = data.records || []
      }).catch(() => {
        this.audit = null
        this.records = []
      })
    },
    handleGenerateCode() {
      generateContractCode({ category: this.form.category || '2' }).then(data => {
        this.form.contractCode = data.contractCode || ''
      }).catch(error => {
        this.$message.error(error.message)
      })
    },
    handleTypeChange(contractTypeId) {
      const matched = this.contractTypes.filter(item => item.contractTypeId === contractTypeId)[0]
      if (matched && matched.category) {
        this.form.category = String(matched.category)
      }
      this.handleGenerateCode()
    },
    applyTemplate(templateId) {
      this.form.templateId = templateId
      this.handleGenerateCode()
      queryContractTypeTemplate({ templateId, page: 1, row: 1 }).then(list => {
        const template = (list || [])[0]
        if (!template) {
          return
        }
        this.form.category = guessCategory({
          category: template.category,
          contractTypeName: template.contractTypeName
        })
        if (template.contractTypeId) {
          this.form.contractType = template.contractTypeId
        }
        this.templateContent = template.context || ''
        if (this.templateContent && !this.content) {
          this.content = this.templateContent
        }
      }).catch(() => {
        this.templateContent = ''
      })
    },
    handleTemplateChange(templateId) {
      if (!templateId) {
        this.templateContent = ''
        return
      }
      queryContractTypeTemplate({ templateId, page: 1, row: 1 }).then(list => {
        const template = (list || [])[0]
        this.templateContent = template ? template.context : ''
        if (this.templateContent && !this.content) {
          this.content = this.templateContent
        }
      }).catch(() => {
        this.templateContent = ''
      })
    },
    handlePartyaChange(partyaId) {
      const matched = this.partyas.filter(item => item.partyaId === partyaId)[0]
      if (!matched) {
        return
      }
      this.form.partyaId = matched.partyaId
      this.form.partyA = matched.partyA
      this.form.aContacts = matched.contactPerson || matched.aContacts || ''
      this.form.aLink = matched.contactTel || matched.aLink || ''
    },
    handleAmountChange(amount) {
      this.form.amount = amount
    },
    handlePlanChange(plans) {
      this.plans = plans
      this.dirty = true
      if (this.plans.length) {
        this.form.payMode = this.plans.length + ' 期'
      }
    },
    handleFeeChange(fee) {
      if (fee && fee.total !== undefined) {
        this.form.amount = fee.total
      }
    },
    openHouseDialog() {
      this.$refs.houseDialog.open(this.rooms)
    },
    handleChooseRooms(rooms) {
      this.rooms = rooms || []
    },
    removeHouse(index) {
      this.rooms.splice(index, 1)
    },
    addTarget() {
      this.targets.push({
        name: '',
        spec: '',
        num: 1,
        price: '',
        amount: ''
      })
    },
    removeTarget(index) {
      this.targets.splice(index, 1)
    },
    handleFillAmount(total) {
      this.form.amount = total
    },
    clearTargets() {
      if (String(this.form.category) === '1') {
        this.rooms = []
        return
      }
      this.targets = []
    },
    handleFileChange(files) {
      this.files = files
    },
    handlePreview() {
      if (!this.content && !this.form.templateId) {
        this.$message.warning(this.$t('contractWorkbench.msg.previewError'))
        return
      }
      this.previewing = true
      previewContractContent({
        content: this.content,
        templateId: this.form.templateId,
        variables: this.docVariables
      }).then(data => {
        this.$refs.templatePreview.open(sanitizeHtml(data.content), this.form.contractName)
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractWorkbench.msg.previewError'))
      }).finally(() => {
        this.previewing = false
      })
    },
    buildPayload(state) {
      return {
        ...this.form,
        state: state,
        rooms: this.rooms,
        plans: this.plans,
        targets: this.targets,
        content: this.content,
        contractFilePo: this.files
      }
    },
    handleSaveDraft() {
      if (!this.form.contractName) {
        this.$message.error(this.$t('contractWorkbench.msg.required', {
          fields: this.$t('contractWorkbench.field.contractName')
        }))
        return
      }
      if (!this.form.contractCode) {
        this.$message.error(this.$t('contractWorkbench.msg.codeEmpty'))
        return
      }
      this.saving = true
      const payload = this.buildPayload('10')
      const request = this.isEdit ? updateContract(payload) : saveContract(payload)
      request.then(() => {
        this.$message.success(this.$t('contractWorkbench.msg.saveSuccess'))
        this.dirty = false
        if (!this.isEdit) {
          this.loadSavedContractId()
        }
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractWorkbench.msg.saveError'))
      }).finally(() => {
        this.saving = false
      })
    },
    loadSavedContractId() {
      this.initializing = true
      queryContract({ contractCode: this.form.contractCode, page: 1, row: 1 }).then(res => {
        const contract = (res.data || [])[0]
        if (contract) {
          this.form.contractId = contract.contractId
        }
      }).catch(() => {
      }).finally(() => {
        this.initializing = false
        this.dirty = false
      })
    },
    handleSubmit() {
      const failed = this.$refs.summaryPanel ? this.$refs.summaryPanel.failedLabels() : []
      if (failed.length) {
        this.$message.error(this.$t('contractWorkbench.msg.integrityError', { fields: failed.join('、') }))
        return
      }
      this.submitting = true
      const payload = this.buildPayload('')
      const save = this.isEdit ? updateContract(payload) : saveContract(payload)
      save.then(() => {
        if (!this.isEdit) {
          this.$message.success(this.$t('contractWorkbench.msg.submitSuccess'))
          this.leavePage()
          return
        }
        submitContractAudit({
          auditType: '1',
          bizId: this.form.contractId
        }).then(() => {
          this.$message.success(this.$t('contractWorkbench.msg.submitSuccess'))
          this.leavePage()
        }).catch(error => {
          this.$message.error(error.message || this.$t('contractWorkbench.msg.auditorMissing'))
        })
      }).catch(error => {
        this.$message.error(error.message || this.$t('contractWorkbench.msg.submitError'))
      }).finally(() => {
        this.submitting = false
      })
    },
    handleBack() {
      if (!this.dirty) {
        this.leavePage()
        return
      }
      this.$confirm(this.$t('contractWorkbench.backConfirm'), '提示', {
        type: 'warning'
      }).then(() => {
        this.leavePage()
      }).catch(() => {
      })
    },
    leavePage() {
      this.$router.push({ path: '/pages/admin/contractManage' })
    }
  }
}
</script>

<style lang="scss" scoped>
.cc-workbench {
  min-height: calc(100vh - 140px);
  background: #f5f7fa;

  .cc-wb-head {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 24px;
    background: #fff;
    border-bottom: 1px solid #ebeef5;
    position: sticky;
    top: 0;
    z-index: 10;
  }

  .cc-wb-back {
    width: 36px;
    height: 36px;
    flex: none;
    padding: 0;
    border: none;
    border-radius: 8px;
    background: none;
    color: #606266;
    font-size: 16px;
    cursor: pointer;
    transition: background-color .2s;

    &:hover {
      background: #f5f7fa;
    }
  }

  .cc-wb-title {
    min-width: 0;
  }

  .cc-wb-title-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .cc-wb-name {
    font-size: 15px;
    font-weight: 700;
    line-height: 22px;
    color: #303133;
  }

  .cc-wb-tag {
    height: 18px;
    line-height: 18px;
    padding: 0 6px;
    border-radius: 4px;
    font-size: 10px;
    font-weight: 500;

    &.is-draft {
      color: #7c3aed;
      background: #f3e8ff;
    }

    &.is-audit {
      color: #e6a23c;
      background: #fdf6ec;
    }
  }

  .cc-wb-sub {
    margin: 2px 0 0;
    font-size: 11px;
    line-height: 16px;
    color: #909399;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;

    .cc-wb-sub-strong {
      color: #606266;
      font-weight: 500;
      font-variant-numeric: tabular-nums;
    }

    .cc-wb-dot {
      margin: 0 6px;
      color: #dcdfe6;
    }
  }

  .cc-wb-head-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-left: auto;

    .cc-wb-tip {
      font-size: 11px;
      color: #67c23a;
    }
  }

  .cc-wb-body {
    display: flex;
    align-items: flex-start;
    padding: 16px 24px 24px;

    .cc-wb-main {
      flex: 1;
      min-width: 0;
    }

    .cc-wb-side {
      width: 320px;
      margin-left: 16px;
      position: sticky;
      top: 76px;
    }
  }

  .cc-panel {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 12px;
    margin-bottom: 16px;
    overflow: hidden;

    .cc-panel-head {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 20px;
      border-bottom: 1px solid #f2f3f5;
    }

    .cc-panel-index {
      width: 20px;
      height: 20px;
      flex: none;
      border-radius: 6px;
      background: #eef2ff;
      color: #4f46e5;
      font-size: 10px;
      font-weight: 700;
      line-height: 20px;
      text-align: center;
    }

    .cc-panel-title {
      margin: 0;
      font-size: 13.5px;
      font-weight: 600;
      line-height: 20px;
      color: #303133;
    }

    .cc-panel-body {
      padding: 20px;
    }
  }

  .cc-wb-form ::v-deep .el-form-item {
    margin-bottom: 14px;
  }

  .cc-wb-form ::v-deep .el-form-item__label {
    padding-bottom: 4px;
    font-size: 12px;
    line-height: 18px;
    font-weight: 500;
    color: #606266;
  }

  .cc-wb-form ::v-deep .el-form-item.is-required .el-form-item__label::before {
    margin-right: 2px;
  }

  @media (max-width: 1280px) {
    .cc-wb-body {
      display: block;

      .cc-wb-side {
        width: auto;
        margin: 16px 0 0;
        position: static;
      }
    }
  }
}
</style>
