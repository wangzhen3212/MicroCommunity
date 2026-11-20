<template>
  <div class="cc-workbench">
    <div class="cc-wb-head">
      <div class="cc-wb-title">
        <div class="cc-wb-title-row">
          <span class="cc-wb-name">{{ pageTitle }}</span>
          <span class="cc-wb-tag" :class="auditState ? 'is-audit' : 'is-draft'">
            {{ $t(stateTextKey) }}
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
        <el-button size="small" class="cc-wb-return" @click="handleBack">
          {{ $t('contractWorkbench.back') }}
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
                :partyas="partyas"
                :templates="templates"
                :amount-manual="amountManual"
                @regenerate-code="handleGenerateCode"
                @template-change="handleTemplateChange"
                @partya-change="handlePartyaChange"
                @amount-change="handleAmountChange"
                @recalc-amount="handleRecalcAmount"
                @select-owner="openOwnerPicker"
              />
            </el-form>
            <el-alert
              v-if="!hasTemplate"
              class="cc-wb-alert"
              type="info"
              :closable="false"
              show-icon
              :title="$t('contractWorkbench.msg.templateFirst')"
            />
          </div>
        </div>

        <div v-if="hasTemplate" class="cc-panel">
          <div class="cc-panel-head">
            <span class="cc-panel-index">02</span>
            <h4 class="cc-panel-title">{{ sectionTargetTitle }}</h4>
            <span v-if="isRent" class="cc-panel-tip">{{ $t('contractWorkbench.target.feeOptionalTip') }}</span>
          </div>
          <div class="cc-panel-body">
            <contract-target-table
              :category="form.category"
              :rooms="rooms"
              :targets="targets"
              :rent-amount-map="rentAmountMap"
              :rent-total-amount="rentTotalAmount"
              :start-time="form.startTime"
              :end-time="form.endTime"
              :fee-visible="feePanelVisible"
              :fee-count="fees.length"
              :fee-disabled="!hasRooms"
              @add-house="openHouseDialog"
              @remove-house="removeHouse"
              @add-target="addTarget"
              @remove-target="removeTarget"
              @fill-amount="handleFillAmount"
              @clear="clearTargets"
              @toggle-fee="toggleFeePanel"
            >
              <template v-slot:fee-panel>
                <contract-fee-plan-panel
                  v-if="feePanelVisible"
                  :category="form.category"
                  :rooms="rooms"
                  :fees="fees"
                  :plans="plans"
                  :amount="form.amount"
                  :start-time="form.startTime"
                  :end-time="form.endTime"
                  :pay-mode="form.payMode"
                  @plan-change="handlePlanChange"
                  @fees-change="handleFeesChange"
                />
              </template>
            </contract-target-table>
          </div>
        </div>

        <div v-if="hasTemplate && !isRent" class="cc-panel">
          <div class="cc-panel-head">
            <span class="cc-panel-index">03</span>
            <h4 class="cc-panel-title">{{ sectionPlanTitle }}</h4>
          </div>
          <div class="cc-panel-body">
            <contract-fee-plan-panel
              :category="form.category"
              :rooms="rooms"
              :fees="fees"
              :plans="plans"
              :amount="form.amount"
              :start-time="form.startTime"
              :end-time="form.endTime"
              :pay-mode="form.payMode"
              @plan-change="handlePlanChange"
              @fees-change="handleFeesChange"
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
              :show-variable="false"
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
          :fee-total="includeFeeTotal"
          :has-content="!!content"
          :state="form.state"
          :records="records"
        />
      </div>
    </div>

    <contract-house-dialog ref="houseDialog" @choose="handleChooseRooms" />
    <search-owner ref="ownerPicker" @chooseOwner="handleChooseOwner" />
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
import SearchOwner from '@/components/owner/SearchOwner'
import { queryContract } from '@/api/contract/contractManageApi'
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
import { queryContractAuditFlow } from '@/api/contract/contractAuditApi'
import { queryContractRoom } from '@/api/contract/contractDetailRoomApi'
import { queryContractFile } from '@/api/contract/contractDetailFileApi'
import { computeContractFeeAmount, computeContractRentAmount } from '@/api/contract/contractFeeCompositionApi'
import {
  CONTRACT_CATEGORY,
  CONTRACT_STATE,
  amountToChinese,
  auditStepNameOf,
  guessCategory,
  renderContractContent,
  sanitizeHtml,
  termMonths
} from '@/utils/contractUtil'
import {
  PAYER_OBJ_TYPE,
  applyManualAmount,
  applyServerAmount,
  detailAmountMap,
  isManualFormula,
  normalizeFeeForSubmit,
  normalizePlanForSubmit,
  payerObjTypeOf,
  sumIncludedAmount
} from '@/utils/contractFeeUtil'
import { getDateStr, getDateYYYYMMDD } from '@/utils/dateUtil'

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
    ContractTemplatePreview,
    SearchOwner
  },
  data() {
    return {
      saving: false,
      submitting: false,
      previewing: false,
      loading: false,
      dirty: false,
      initializing: true,
      hadRooms: false,
      partyas: [],
      templates: [],
      templateContent: '',
      rooms: [],
      targets: [],
      fees: [],
      plans: [],
      feePanelVisible: false,
      files: [],
      content: '',
      records: [],
      rentAmountMap: {},
      rentTotalAmount: '',
      recalcTimer: null,
      amountManual: false,
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
      const state = String(this.form.state)
      return state === CONTRACT_STATE.AUDIT || state === CONTRACT_STATE.AUDIT_LEADER
    },
    stateTextKey() {
      return auditStepNameOf(this.form.state) || 'contractState.draft'
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
    isRent() {
      return String(this.form.category) === CONTRACT_CATEGORY.RENT
    },
    hasTemplate() {
      return !!this.form.templateId
    },
    hasRooms() {
      return this.rooms.length > 0
    },
    sectionTargetTitle() {
      return this.isRent
        ? this.$t('contractWorkbench.section.targetRent')
        : this.$t('contractWorkbench.section.target')
    },
    sectionPlanTitle() {
      return this.isRent
        ? this.$t('contractWorkbench.section.planRent')
        : this.$t('contractWorkbench.section.planPay')
    },
    includeFeeTotal() {
      return sumIncludedAmount(this.fees)
    },
    docVariables() {
      return {
        amount: this.form.amount,
        amountText: amountToChinese(this.form.amount),
        startTime: this.form.startTime,
        endTime: this.form.endTime,
        partyB: this.form.partyB,
        houses: this.rooms
          .map(room => this.roomNameOf(room))
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
        this.scheduleRecalc()
      }
    },
    'form.startTime'() {
      this.scheduleRecalc()
    },
    'form.endTime'() {
      this.scheduleRecalc()
    },
    'form.category'() {
      if (this.isRent) {
        this.form.payMode = ''
      }
      this.scheduleRecalc(true)
    },
    fees: {
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
      const user = this.$getUser() || {}
      return {
        contractId: '',
        contractName: '',
        contractCode: '',
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
        signingTime: getDateStr(),
        startTime: getDateYYYYMMDD(),
        endTime: '',
        operator: user.name || '',
        operatorLink: user.tel || '',
        payMode: '',
        place: '',
        remark: '',
        state: '',
        objType: '1111',
        objId: '-1',
        objPersonId: '',
        objPersonName: ''
      }
    },
    loadBaseData() {
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
        this.rooms = this.normalizeRooms((res && res.data) || [])
      }).catch(() => {
        this.rooms = []
      })
    },
    loadPlans(contractId) {
      listContractCollectionPlans({ contractId }).then(data => {
        this.plans = data.plans || []
        this.fees = this.normalizeFees(data.fees)
        this.feePanelVisible = this.fees.length > 0
      }).catch(() => {
        this.plans = []
        this.fees = []
        this.feePanelVisible = false
      })
    },
    normalizeFees(list) {
      return (list || []).map((row, index) => {
        const fee = { ...row }
        fee.feeSeq = row.feeSeq || String(index + 1)
        fee.includeInAmount = String(row.includeInAmount) === '1' || row.includeInAmount === true
        return fee
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
      queryContractContent({ contractId, raw: true }).then(data => {
        this.content = data.content || ''
      }).catch(() => {
        this.content = ''
      })
    },
    loadAuditFlow(contractId) {
      queryContractAuditFlow({ bizId: contractId, bizType: '1' }).then(data => {
        this.records = data.records || []
      }).catch(() => {
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
    applyTemplate(templateId) {
      this.form.templateId = templateId
      queryContractTypeTemplate({ templateId, page: 1, row: 1 }).then(list => {
        const template = (list || [])[0]
        if (!template) {
          this.handleGenerateCode()
          return
        }
        this.form.category = guessCategory(template)
        this.handleGenerateCode()
        this.templateContent = template.context || ''
        if (this.templateContent && !this.content) {
          this.content = this.templateContent
        }
      }).catch(() => {
        this.templateContent = ''
        this.handleGenerateCode()
      })
    },
    handleTemplateChange(templateId) {
      const previousCategory = String(this.form.category || '')
      if (!templateId) {
        this.form.category = ''
        this.templateContent = ''
        this.rooms = []
        this.fees = []
        this.targets = []
        this.plans = []
        this.feePanelVisible = false
        this.form.amount = ''
        this.amountManual = false
        return
      }
      queryContractTypeTemplate({ templateId, page: 1, row: 1 }).then(list => {
        const template = (list || [])[0]
        if (!template) {
          this.templateContent = ''
          this.handleGenerateCode()
          return
        }
        if (!template.category) {
          this.$message.warning(this.$t('contractWorkbench.msg.templateCategoryEmpty'))
        }
        const nextCategory = guessCategory(template)
        this.applyCategorySwitch(previousCategory, nextCategory)
        this.form.category = nextCategory
        this.handleGenerateCode()
        this.templateContent = template.context || ''
        if (this.templateContent && !this.content) {
          this.content = this.templateContent
        }
      }).catch(() => {
        this.templateContent = ''
      })
    },
    applyCategorySwitch(previousCategory, nextCategory) {
      const wasRent = String(previousCategory) === CONTRACT_CATEGORY.RENT
      const isRent = String(nextCategory) === CONTRACT_CATEGORY.RENT
      if (wasRent === isRent) {
        return
      }
      if (isRent) {
        this.targets = []
        this.plans = []
        this.form.payMode = ''
        this.feePanelVisible = false
        return
      }
      this.rooms = []
      this.fees = []
      this.feePanelVisible = false
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
      this.amountManual = true
    },
    handleRecalcAmount() {
      this.amountManual = false
      this.recalcFeeRows(true)
    },
    openOwnerPicker() {
      if (this.$refs.ownerPicker) {
        this.$refs.ownerPicker.open()
      }
    },
    handleChooseOwner(owner) {
      if (!owner) {
        return
      }
      const ownerName = owner.name || ''
      this.form.partyB = ownerName
      this.form.bLink = owner.link || ''
      if (!this.form.bContacts) {
        this.form.bContacts = ownerName
      }
      this.form.objPersonId = owner.ownerId || owner.memberId || ''
      this.form.objPersonName = ownerName
    },
    handlePlanChange(plans) {
      this.plans = plans
      this.dirty = true
      if (!this.isRent && this.plans.length) {
        this.form.payMode = this.plans.length + ' 期'
      }
    },
    roomNameOf(row) {
      if (!row) {
        return ''
      }
      if (row.objName) {
        return row.objName
      }
      if (payerObjTypeOf(row) === PAYER_OBJ_TYPE.SHOP) {
        return row.shopNum || row.locationName || row.roomName || ''
      }
      return row.roomName || ((row.floorNum || '') + '-' + (row.unitNum || '') + '-' + (row.roomNum || ''))
    },
    normalizeRooms(list) {
      return (list || []).map(row => {
        const payerObjType = payerObjTypeOf(row)
        const objId = row.objId || row.roomId || row.shopId
        return {
          ...row,
          payerObjType: payerObjType,
          objId: objId,
          roomId: objId,
          objName: row.objName || this.roomNameOf(row),
          areaValue: row.areaValue || row.roomArea,
          rentValue: row.rentValue || row.roomRent,
          stateName: row.stateName || row.state
        }
      })
    },
    termMonthsOf() {
      return termMonths(this.form.startTime, this.form.endTime)
    },
    scheduleRecalc(force) {
      if (this.recalcTimer) {
        clearTimeout(this.recalcTimer)
      }
      this.recalcTimer = setTimeout(() => {
        this.recalcFeeRows(force)
      }, 300)
    },
    recalcFeeRows(force) {
      if (!this.isRent) {
        this.fees.forEach(row => {
          if (isManualFormula(row.computingFormula)) {
            applyManualAmount(row)
          }
        })
        this.hadRooms = this.hasRooms
        return
      }
      if (!this.hasRooms) {
        if ((this.hadRooms || force) && !this.amountManual) {
          this.form.amount = '0.00'
        }
        this.hadRooms = false
        this.rentAmountMap = {}
        this.rentTotalAmount = ''
        return
      }
      this.hadRooms = true
      this.refreshRentAmounts()
      this.refreshFeeAmounts(force)
    },
    buildObjParams() {
      if (!this.rooms.length) {
        return null
      }
      const payerObjType = payerObjTypeOf(this.rooms[0])
      if (this.rooms.some(row => payerObjTypeOf(row) !== payerObjType)) {
        this.$message.warning(this.$t('contractWorkbench.msg.mixPayerObjType'))
        return null
      }
      return {
        payerObjType: payerObjType,
        payerObjIds: this.rooms.map(row => row.objId || row.roomId).join(','),
        months: this.termMonthsOf(),
        startTime: this.form.startTime,
        endTime: this.form.endTime
      }
    },
    refreshRentAmounts() {
      const params = this.buildObjParams()
      if (!params || params.months <= 0) {
        this.rentAmountMap = {}
        this.rentTotalAmount = ''
        return
      }
      computeContractRentAmount(params).then(res => {
        this.rentAmountMap = detailAmountMap(res)
        const data = res.data || res
        this.rentTotalAmount = data.amount || ''
      }).catch(error => {
        this.rentAmountMap = {}
        this.rentTotalAmount = ''
        this.$message.warning(error.message)
      })
    },
    refreshFeeAmounts(force) {
      const params = this.buildObjParams()
      const months = params ? params.months : 0
      const tasks = []
      this.fees.forEach(row => {
        if (isManualFormula(row.computingFormula)) {
          applyManualAmount(row)
          return
        }
        if (!row.configId) {
          return
        }
        if (months <= 0) {
          row.amount = '0.00'
          row.baseAmount = '0.00'
          row.computeRemark = this.$t('contractWorkbench.msg.termEmpty')
          return
        }
        tasks.push(computeContractFeeAmount({
          configId: row.configId,
          payerObjType: params.payerObjType,
          payerObjIds: params.payerObjIds,
          months: months,
          startTime: params.startTime,
          endTime: params.endTime
        }).then(res => {
          applyServerAmount(row, res)
        }).catch(error => {
          row.amount = '0.00'
          row.baseAmount = '0.00'
          row.computeRemark = error.message || this.$t('contractWorkbench.msg.feeComputeError')
        }))
      })
      Promise.all(tasks).then(() => {
        this.refreshFormAmount(force)
      })
    },
    refreshFormAmount(force) {
      if (!this.isRent || !this.hasRooms || this.amountManual) {
        return
      }
      if (this.fees.length || force) {
        this.form.amount = this.includeFeeTotal.toFixed(2)
      }
    },
    handleFeesChange(fees) {
      if (fees) {
        this.fees = fees
      }
      this.recalcFeeRows(true)
    },
    toggleFeePanel() {
      if (!this.feePanelVisible) {
        if (!this.hasRooms) {
          this.$message.warning(this.$t('contractWorkbench.target.feeRoomFirst'))
          return
        }
        this.feePanelVisible = true
        return
      }
      if (this.readonly) {
        this.feePanelVisible = false
        return
      }
      if (!this.fees.length) {
        this.feePanelVisible = false
        return
      }
      this.$confirm(this.$t('contractWorkbench.target.feeClearConfirm'), '提示', {
        type: 'warning'
      }).then(() => {
        this.fees = []
        this.feePanelVisible = false
        this.handleFeesChange(this.fees)
      }).catch(() => {
      })
    },
    openHouseDialog() {
      this.$refs.houseDialog.open(this.rooms)
    },
    handleChooseRooms(rooms) {
      this.rooms = this.normalizeRooms(rooms)
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
      this.amountManual = true
    },
    clearTargets() {
      if (String(this.form.category) === '1') {
        this.rooms = []
        this.fees = []
        this.feePanelVisible = false
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
      const variables = this.docVariables
      return {
        ...this.form,
        state: state,
        rooms: this.rooms,
        plans: this.isRent ? [] : this.plans.map(normalizePlanForSubmit),
        targets: this.targets,
        fees: this.fees.map(normalizeFeeForSubmit),
        content: renderContractContent(this.content, variables),
        templateContent: this.content,
        variables: variables,
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
      this.saveAndSubmit()
    },
    saveAndSubmit() {
      const payload = this.buildPayload(CONTRACT_STATE.AUDIT)
      const save = this.isEdit ? updateContract(payload) : saveContract(payload)
      save.then(() => {
        this.$message.success(this.$t('contractWorkbench.msg.submitSuccess'))
        this.leavePage()
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
  padding: 16px 24px 24px;
  min-height: calc(100vh - 140px);
  background: #f7f8fa;
  box-sizing: border-box;

  .cc-wb-head {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding-bottom: 14px;
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
    font-size: 20px;
    font-weight: 700;
    line-height: 28px;
    color: #1f2937;
  }

  .cc-wb-tag {
    height: 20px;
    line-height: 20px;
    padding: 0 6px;
    border-radius: 5px;
    font-size: 10px;
    font-weight: 600;

    &.is-draft {
      color: #409eff;
      background: #ecf5ff;
    }

    &.is-audit {
      color: #e6a23c;
      background: #fdf6ec;
    }
  }

  .cc-wb-sub {
    margin: 2px 0 0;
    font-size: 12px;
    line-height: 18px;
    color: #9ca3af;
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
    flex: none;

    .el-button {
      height: 34px;
      border-radius: 8px;
      font-size: 13px;
    }

    .cc-wb-tip {
      font-size: 11px;
      color: #67c23a;
    }

    .cc-wb-return {
      color: #409eff;
      background: #ecf5ff;
      border-color: #d9ecff;

      &:hover,
      &:focus {
        color: #409eff;
        background: #d9ecff;
        border-color: #409eff;
      }
    }
  }

  .cc-wb-body {
    display: flex;
    align-items: flex-start;
    padding: 0;

    .cc-wb-main {
      flex: 1;
      min-width: 0;
    }

    .cc-wb-side {
      width: 320px;
      margin-left: 16px;
      position: sticky;
      top: 16px;
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
      background: #ecf5ff;
      color: #409eff;
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

    .cc-panel-tip {
      margin-left: auto;
      font-size: 11px;
      line-height: 18px;
      color: #909399;
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

  .cc-wb-alert {
    margin-top: 2px;
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
