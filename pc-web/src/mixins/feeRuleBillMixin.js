import { listFeeConfigs } from '@/api/fee/simplifyRoomFeeApi'
import { getDict, getCommunityId } from '@/api/community/communityApi'
import { listRoomFeeRuleWithBill, listRuleBills } from '@/api/fee/roomFeeRuleBillApi'
import { queryPreGenerateFeePeriods, preGenerateFee as preGenerateFeeApi } from '@/api/fee/preGenerateFeeApi'
import { EXCLUDE_COMPUTING_FORMULAS } from '@/utils/feeUtil'

export default {
  data() {
    return {
      ruleBillInfo: {
        payerObjType: '',
        roomInfo: {
          roomId: '',
          roomName: '',
          ownerId: '',
          ownerName: '',
          roomSubTypeName: '',
          stateName: '',
          link: '',
          builtUpArea: ''
        },
        loading: false,
        rules: [],
        feeTypeCds: [],
        feeTypeCd: '',
        configId: '',
        feeConfigs: [],
        stateFilter: '',
        showSettled: false,
        riskIgnored: false,
        riskOnly: false,
        currentPage: 1,
        pageSize: 20,
        filteredCount: 0,
        pageRows: [],
        riskRules: [],
        riskCount: 0,
        hiddenSettledRules: [],
        hiddenSettledCount: 0
      }
    }
  },
  methods: {
    initFeeRuleBill(params) {
      const data = params || {}
      const info = this.ruleBillInfo
      info.payerObjType = this._billConfig('payerObjType', '3333')
      info.roomInfo = {
        roomId: data.payerObjId || data.roomId || '',
        roomName: data.payerObjName || data.roomName || '',
        ownerId: data.ownerId || '',
        ownerName: data.ownerName || '',
        roomSubTypeName: data.roomSubTypeName || '',
        stateName: data.stateName || '',
        link: data.link || '',
        builtUpArea: data.builtUpArea || data.roomArea || ''
      }
      info.stateFilter = ''
      info.feeTypeCd = ''
      info.configId = ''
      info.showSettled = false
      info.riskIgnored = false
      info.riskOnly = false
      info.currentPage = 1
      info.rules = []
      this._refreshPageData()
      this.loadData()
      this.loadFeeTypeCds()
      this.loadFeeConfigs('')
    },
    _billConfig(key, fallback) {
      const config = this.feeRuleBillConfig || {}
      const value = config[key]
      return value || fallback
    },
    loadData() {
      const info = this.ruleBillInfo
      if (!info.roomInfo.roomId) {
        info.rules = []
        this._refreshPageData()
        return
      }
      info.loading = true
      this._clearBillCache()
      listRoomFeeRuleWithBill({
        communityId: getCommunityId(),
        payerObjId: info.roomInfo.roomId,
        payerObjType: info.payerObjType
      }).then(res => {
        const rules = res.data || []
        rules.forEach(rule => {
          this._initRuleBills(rule)
        })
        info.rules = rules
        info.loading = false
        this._refreshPageData()
      }).catch(error => {
        info.rules = []
        info.loading = false
        this._refreshPageData()
        this.$message.error(error.message || this.$t('roomFeeRuleBill.loadFailed'))
      })
    },
    loadFeeTypeCds() {
      getDict('pay_fee_config', 'fee_type_cd').then(data => {
        this.ruleBillInfo.feeTypeCds = data || []
      }).catch(error => {
        console.error('Failed to fetch fee type cds:', error)
      })
    },
    loadFeeConfigs(feeTypeCd) {
      const params = {
        page: 1,
        row: 500,
        communityId: getCommunityId(),
        feeTypeCd: feeTypeCd,
        isDefault: '',
        valid: '1'
      }
      listFeeConfigs(params).then(res => {
        this.ruleBillInfo.feeConfigs = res.feeConfigs || []
      }).catch(error => {
        console.error('Failed to fetch fee configs:', error)
      })
    },
    handleFeeTypeChange(feeTypeCd) {
      this.ruleBillInfo.feeTypeCd = feeTypeCd
      this.ruleBillInfo.configId = ''
      this.loadFeeConfigs(feeTypeCd)
      this.handleQuery()
    },
    handleBillChanged() {
      this.loadData()
    },
    handleConfigChange(configId) {
      this.ruleBillInfo.configId = configId
      this.handleQuery()
    },
    handleStateChange(state) {
      this.ruleBillInfo.stateFilter = state
      this.handleQuery()
    },
    _buildFilteredRules() {
      const info = this.ruleBillInfo
      return info.rules.filter(item => {
        if (info.riskOnly && !item.risk) {
          return false
        }
        if (item.settled && !info.showSettled) {
          return false
        }
        if (info.stateFilter && item.state !== info.stateFilter) {
          return false
        }
        if (info.feeTypeCd && item.feeTypeCd !== info.feeTypeCd) {
          return false
        }
        if (info.configId && item.configId !== info.configId) {
          return false
        }
        return true
      })
    },
    _buildRiskRules() {
      return this.ruleBillInfo.rules.filter(item => item.risk)
    },
    _buildHiddenSettledList() {
      return this.ruleBillInfo.rules.filter(item => item.settled)
    },
    _buildPageRows(filteredRules) {
      const info = this.ruleBillInfo
      const start = (info.currentPage - 1) * info.pageSize
      return filteredRules.slice(start, start + info.pageSize)
    },
    _refreshPageData() {
      const info = this.ruleBillInfo
      const filteredRules = this._buildFilteredRules()
      info.filteredCount = filteredRules.length
      const maxPage = Math.max(1, Math.ceil(info.filteredCount / info.pageSize))
      if (info.currentPage > maxPage) {
        info.currentPage = maxPage
      }
      info.pageRows = this._buildPageRows(filteredRules)
      info.riskRules = this._buildRiskRules()
      info.riskCount = info.riskRules.length
      info.hiddenSettledRules = this._buildHiddenSettledList()
      info.hiddenSettledCount = info.hiddenSettledRules.length
    },
    _initRuleBills(rule) {
      this.$set(rule, 'billStateFilter', 'unpaid')
      this.$set(rule, 'filteredBills', [])
      this.$set(rule, 'billCount', 0)
      this.$set(rule, 'billsLoading', false)
      this.$set(rule, 'billCache', {})
      this.$set(rule, 'expanded', false)
    },
    _buildRuleBills(rule) {
      const bills = rule.bills || []
      if (rule.billStateFilter === 'paid') {
        return bills.filter(bill => bill.billState === 'paid')
      }
      if (rule.billStateFilter === 'unpaid') {
        return bills.filter(bill => bill.billState === 'unpaid' || bill.billState === 'cancelled')
      }
      return bills.slice(0)
    },
    _clearBillCache() {
      const rules = this.ruleBillInfo.rules || []
      rules.forEach(rule => {
        this.$set(rule, 'billCache', {})
        this.$set(rule, 'filteredBills', [])
        this.$set(rule, 'billCount', 0)
        this.$set(rule, 'billsLoading', false)
        this.$set(rule, 'expanded', false)
      })
    },
    _loadRuleBills(rule, state) {
      if (!rule) {
        return
      }
      const billState = state || 'unpaid'
      this.$set(rule, 'billStateFilter', billState)
      if (rule.syntheticOther || !rule.ruleId || rule.ruleId === '-1') {
        const localBills = this._buildRuleBills(rule)
        this.$set(rule, 'filteredBills', localBills)
        this.$set(rule, 'billCount', localBills.length)
        return
      }
      const cache = rule.billCache || {}
      if (cache[billState]) {
        this.$set(rule, 'filteredBills', cache[billState])
        this.$set(rule, 'billCount', cache[billState].length)
        return
      }
      if (rule.billsLoading) {
        return
      }
      const communityId = getCommunityId()
      if (!communityId) {
        return
      }
      this.$set(rule, 'billsLoading', true)
      listRuleBills({
        communityId: communityId,
        ruleId: rule.ruleId,
        billState: billState,
        page: -1
      }).then(res => {
        const bills = res.data || []
        const nextCache = Object.assign({}, rule.billCache || {})
        nextCache[billState] = bills
        this.$set(rule, 'billCache', nextCache)
        this.$set(rule, 'filteredBills', bills)
        this.$set(rule, 'billCount', bills.length)
        this.$set(rule, 'billsLoading', false)
      }).catch(error => {
        this.$set(rule, 'billsLoading', false)
        this.$message.error(error.message || this.$t('roomFeeRuleBill.loadFailed'))
      })
    },
    _expandRule(rule) {
      if (!rule) {
        return
      }
      this.$set(rule, 'expanded', true)
      this._loadRuleBills(rule, rule.billStateFilter || 'unpaid')
    },
    _toggleRule(rule) {
      if (!rule) {
        return
      }
      if (rule.expanded) {
        this.$set(rule, 'expanded', false)
        return
      }
      this._expandRule(rule)
    },
    _switchBillFilter(rule, state) {
      if (!rule) {
        return
      }
      this._loadRuleBills(rule, state)
      this.$set(rule, 'expanded', true)
    },
    _isShowRisk() {
      return this.ruleBillInfo.riskCount > 0 && !this.ruleBillInfo.riskIgnored
    },
    _emptyText() {
      const info = this.ruleBillInfo
      if (info.roomInfo.roomId) {
        return this.$t(this._billConfig('emptyTextKey', 'roomFeeRuleBill.emptyPayerObj'))
      }
      return this.$t(this._billConfig('selectTipKey', 'roomFeeRuleBill.selectPayerObjFirst'))
    },
    _statOwe() {
      let total = 0
      this.ruleBillInfo.rules.forEach(item => {
        const amount = parseFloat(item.oweAmount)
        if (!isNaN(amount)) {
          total += amount
        }
      })
      return total.toFixed(2)
    },
    handleQuery() {
      this.ruleBillInfo.currentPage = 1
      this._refreshPageData()
    },
    handlePageChange(page) {
      this.ruleBillInfo.currentPage = page
      this._refreshPageData()
    },
    handleToggleSettled() {
      const info = this.ruleBillInfo
      info.showSettled = !info.showSettled
      info.currentPage = 1
      this._refreshPageData()
    },
    handleRiskView() {
      const info = this.ruleBillInfo
      info.stateFilter = ''
      info.showSettled = false
      info.riskOnly = true
      info.currentPage = 1
      this._refreshPageData()
      this.$nextTick(() => {
        info.pageRows.forEach(row => {
          if (row.risk) {
            this._expandRule(row)
          }
        })
      })
    },
    handleRiskIgnore() {
      this.ruleBillInfo.riskIgnored = true
    },
    handleRiskClear() {
      this.ruleBillInfo.riskOnly = false
      this.ruleBillInfo.currentPage = 1
      this._refreshPageData()
    },
    _money(value) {
      const amount = parseFloat(value)
      return isNaN(amount) ? '0.00' : amount.toFixed(2)
    },
    _dateOnly(value) {
      if (!value) {
        return '—'
      }
      return String(value).substring(0, 10)
    },
    _ruleDisplayName(row) {
      if (row.syntheticOther) {
        return this.$t('roomFeeRuleBill.syntheticName')
      }
      return row.feeName || '-'
    },
    _rulePriceText(row) {
      if (row.syntheticOther) {
        return ''
      }
      const parts = []
      if (!isNaN(parseFloat(row.squarePrice))) {
        parts.push(this.$t('roomFeeRuleBill.unitPrice', { price: this._money(row.squarePrice) }))
      }
      if (!isNaN(parseFloat(row.additionalAmount))) {
        parts.push(this.$t('roomFeeRuleBill.extraFee', { amount: this._money(row.additionalAmount) }))
      }
      return parts.join(' · ')
    },
    _ruleCycleText(row) {
      if (row.syntheticOther) {
        return '-'
      }
      const cycle = row.feeFlagName || '-'
      return this._isMeterRule(row) ? this.$t('roomFeeRuleBill.cycleMeter', { cycle: cycle }) : cycle
    },
    _rulePeriodText(row) {
      if (row.syntheticOther || !row.startTime || !row.maxTime) {
        return ''
      }
      return this._dateOnly(row.startTime) + ' ~ ' + this._dateOnly(row.maxTime)
    },
    _isMeterRule(row) {
      return EXCLUDE_COMPUTING_FORMULAS.indexOf(row.computingFormula) >= 0
    },
    _stateText(row) {
      if (row.syntheticOther) {
        return this.$t('roomFeeRuleBill.stateNoRule')
      }
      if (row.state === '2008001') {
        return this._isMeterRule(row) ? this.$t('roomFeeRuleBill.nextBillMeter') : this.$t('roomFeeRuleBill.stateDoing')
      }
      if (row.feeFlag !== '2006012') {
        return this.$t('roomFeeRuleBill.stateStopped')
      }
      if (row.unpaidCount > 0) {
        return this.$t('roomFeeRuleBill.stateOnceUnpaid')
      }
      return this.$t('roomFeeRuleBill.stateOnceSettled')
    },
    _stateTone(row) {
      if (row.syntheticOther) {
        return 'muted'
      }
      if (row.state === '2008001') {
        return this._isMeterRule(row) ? 'meter' : 'success'
      }
      if (Number(row.unpaidCount) > 0) {
        return 'danger'
      }
      return 'muted'
    },
    _nextBillText(row) {
      if (row.syntheticOther || row.state !== '2008001') {
        return '—'
      }
      if (this._isMeterRule(row)) {
        return this.$t('roomFeeRuleBill.nextBillMeter')
      }
      return this._dateOnly(row.curYearMonth)
    },
    _billTone(bill) {
      if (bill.billState === 'paid') {
        return 'success'
      }
      if (bill.billState === 'cancelled') {
        return 'muted'
      }
      return 'danger'
    },
    _billStateText(bill) {
      if (bill.billState === 'paid') {
        return this.$t('roomFeeRuleBill.billPaid')
      }
      if (bill.billState === 'cancelled') {
        return this.$t('roomFeeRuleBill.billCancelled')
      }
      return this.$t('roomFeeRuleBill.billUnpaid')
    },
    _billTotalCount(row) {
      if (row.syntheticOther) {
        return (row.bills || []).length
      }
      const paidCount = Number(row.paidCount) > 0 ? Number(row.paidCount) : 0
      const unpaidCount = Number(row.unpaidCount) > 0 ? Number(row.unpaidCount) : 0
      const cancelledCount = Number(row.cancelledCount) > 0 ? Number(row.cancelledCount) : 0
      return paidCount + unpaidCount + cancelledCount
    },
    _billEmptyText(row) {
      if (this._billTotalCount(row) < 1) {
        return this._isMeterRule(row)
          ? this.$t('roomFeeRuleBill.subEmptyNoBillMeter')
          : this.$t('roomFeeRuleBill.subEmptyNoBill')
      }
      if (row.billStateFilter === 'unpaid' && Number(row.paidCount) > 0) {
        return this.$t('roomFeeRuleBill.subEmptyPaid')
      }
      return this.$t('roomFeeRuleBill.billFilterEmpty')
    },
    _checkPayerObj() {
      if (!this.ruleBillInfo.roomInfo.roomId) {
        this.$message.warning(this.$t(this._billConfig('selectTipKey', 'roomFeeRuleBill.selectPayerObjFirst')))
        return false
      }
      return true
    },
    _toBatchPayFee() {
      if (!this.ruleBillInfo.roomInfo.ownerId) {
        this.$message.warning(this.$t(this._billConfig('selectTipKey', 'roomFeeRuleBill.selectPayerObjFirst')))
        return
      }
      this.$router.push({
        path: '/views/fee/batchPayFeeOrder',
        query: {
          ownerId: this.ruleBillInfo.roomInfo.ownerId,
          payerObjType: this.ruleBillInfo.payerObjType
        }
      })
    },
    _toPrePayFee() {
      if (!this._checkPayerObj()) {
        return
      }
      this.$router.push({
        path: '/views/fee/prePayFeeOrder',
        query: {
          payerObjId: this.ruleBillInfo.roomInfo.roomId,
          payerObjType: this.ruleBillInfo.payerObjType
        }
      })
    },
    _feeHis(bill) {
      if (!bill) {
        return
      }
      this.$router.push({
        path: '/views/fee/feeDetail',
        query: { feeId: bill.feeId }
      })
    },
    _cancelBill(bill) {
      if (!bill || !this.$refs.deleteFee) {
        return
      }
      this.$refs.deleteFee.open({
        communityId: getCommunityId(),
        feeId: bill.feeId
      })
    },
    _finishRule(rule) {
      if (this.$refs.finishFeeRule) {
        this.$refs.finishFeeRule.open({
          ruleId: rule.ruleId,
          communityId: getCommunityId()
        })
      }
    },
    _deleteRule(rule) {
      if (this.$refs.deleteFeeRule) {
        this.$refs.deleteFeeRule.open({
          ruleId: rule.ruleId,
          communityId: getCommunityId()
        })
      }
    },
    _editRule(rule) {
      if (this.$refs.editFeeRule) {
        this.$refs.editFeeRule.open(rule)
      }
    },
    _editFee(bill) {
      if (!bill) {
        return
      }
      if (this.$refs.editFee) {
        this.$refs.editFee.open(bill)
      }
    },
    _splitFee(bill) {
      if (!bill) {
        return
      }
      if (this.$refs.splitFee) {
        this.$refs.splitFee.open(bill)
      }
    },
    _preGenerateFee(rule) {
      const communityId = getCommunityId()
      queryPreGenerateFeePeriods({
        communityId: communityId,
        ruleId: rule.ruleId
      }).then(res => {
        const data = res.data
        if (!data) {
          this.$message.warning(this.$t('simplifyRoomFee.noAvailablePeriod'))
          return
        }
        if (data.feeFlag === '2006012') {
          this.$confirm(this.$t('simplifyRoomFee.preGenerateConfirmTip'), this.$t('simplifyRoomFee.preGenerate'), {
            confirmButtonText: this.$t('common.confirm'),
            cancelButtonText: this.$t('common.cancel'),
            type: 'warning'
          }).then(() => {
            preGenerateFeeApi({
              communityId: communityId,
              ruleId: rule.ruleId,
              periods: []
            }).then(response => {
              this.$message.success(this.$t('simplifyRoomFee.preGenerateSuccess', { count: response.data }))
              this.loadData()
            }).catch(error => {
              this.$message.error(error.message || this.$t('simplifyRoomFee.preGenerateFailed'))
            })
          }).catch(() => {})
          return
        }
        if (!data.periods || data.periods.length < 1) {
          this.$message.warning(this.$t('simplifyRoomFee.noAvailablePeriod'))
          return
        }
        if (this.$refs.preGenerateFee) {
          this.$refs.preGenerateFee.open({
            communityId: communityId,
            ruleId: rule.ruleId,
            feeName: rule.feeName,
            endTime: data.endTime,
            periods: data.periods
          })
        }
      }).catch(error => {
        this.$message.error(error.message || this.$t('simplifyRoomFee.preGenerateFailed'))
      })
    }
  }
}
