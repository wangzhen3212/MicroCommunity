<template>
  <div>
    <div class="margin-top-lg flex justify-between">
      <div class="flex justify-start">
        <div :span="3" class="padding-right-xs padding-left-xl">
          <el-select v-model="simplifyFeeReceiptInfo.objType" size="small" @change="_changeSimplifyFeeReceiptFeeTypeCd"
            :placeholder="$t('simplifyFeeReceipt.selectFeeType')">
            <el-option v-for="(item, index) in simplifyFeeReceiptInfo.objTypes" :key="index" :label="item.name"
              :value="item.statusCd"></el-option>
          </el-select>
        </div>
        <div :span="3" class="padding-right-xs padding-left-xl" v-if="simplifyFeeReceiptInfo.objType == '4444'">
          <el-select v-model="simplifyFeeReceiptInfo.shopId" size="small" @change="changeSimplifyFeeReceiptObj"
            :placeholder="$t('simplifyFeeReceipt.selectShop')">
            <el-option v-for="(item, index) in simplifyFeeReceiptInfo.ownerShops" :key="index" :label="item.shopNum"
              :value="item.shopId"></el-option>
          </el-select>
        </div>
        <div :span="3" class="padding-right-xs padding-left-xl" v-else-if="simplifyFeeReceiptInfo.objType == '6666'">
          <el-select v-model="simplifyFeeReceiptInfo.psId" size="small" @change="changeSimplifyFeeReceiptObj"
            :placeholder="$t('simplifyFeeReceipt.selectParkingSpace')">
            <el-option v-for="item in simplifyFeeReceiptInfo.ownerParkingSpaces" :key="item.psId"
              :label="_getParkingSpaceName(item)" :value="item.psId"></el-option>
          </el-select>
        </div>
        <div :span="3" class="padding-right-xs padding-left-xl" v-else-if="simplifyFeeReceiptInfo.objType == '7777'">
          <el-select v-model="simplifyFeeReceiptInfo.contractId" size="small" @change="changeSimplifyFeeReceiptObj"
            :placeholder="$t('simplifyFeeReceipt.selectContract')">
            <el-option v-for="(item, index) in simplifyFeeReceiptInfo.ownerContracts" :key="index"
              :label="item.contractCode" :value="item.contractId"></el-option>
          </el-select>
        </div>
        <div :span="3" class="padding-right-xs padding-left-xl" v-if="simplifyFeeReceiptInfo.objType != '7777'">
          <el-select v-model="simplifyFeeReceiptInfo.ownerFlag" size="small" @change="_queryFeeReceiptMethod">
            <el-option :label="$t('simplifyFeeReceipt.current') + _getFeeObjName(simplifyFeeReceiptInfo.objType)"
              value="F"></el-option>
            <el-option :label="$t('simplifyFeeReceipt.all') + _getFeeObjName(simplifyFeeReceiptInfo.objType)"
              value="T"></el-option>
          </el-select>
        </div>
        <div :span="3" class="padding-right-xs padding-left-xl">
          <el-input v-model="simplifyFeeReceiptInfo.receiptCode" size="small"
            :placeholder="$t('simplifyFeeReceipt.receiptCode')"></el-input>
        </div>
        <div :span="2" class="padding-right-xs padding-left-xl">
          <el-button type="primary" size="small" @click="_queryFeeReceiptMethod">
            {{ $t('simplifyFeeReceipt.query') }}
          </el-button>
        </div>
      </div>
      <div :span="8" class="text-right padding-right-lg">
        <el-button type="primary" size="small" @click="_printFeeReceipt('OFF')">
          {{ $t('simplifyFeeReceipt.print') }}
        </el-button>
        <el-button type="primary" size="small" @click="_printFeeReceipt('ON')">
          {{ $t('simplifyFeeReceipt.mergePrint') }}
        </el-button>
        <el-button type="primary" size="small" @click="_printFeeSmallReceipt">
          {{ $t('simplifyFeeReceipt.printSmall') }}
        </el-button>
        <el-button type="primary" size="small" @click="_printApplyFeeReceipt">
          {{ $t('simplifyFeeReceipt.apply') }}
        </el-button>
      </div>
    </div>
    <div class="margin-top">
      <el-table :data="simplifyFeeReceiptInfo.feeReceipts" @selection-change="handleSelectionChange" border stripe>
        <el-table-column type="selection" width="55" align="center"></el-table-column>
        <el-table-column prop="receiptCode" :label="$t('simplifyFeeReceipt.receiptCode')"
          align="center"></el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.feeType')" align="center">
          <template slot-scope="scope">
            {{ _getFeeObjName(scope.row.objType) }}{{ $t('simplifyFeeReceipt.fee') }}
          </template>
        </el-table-column>
        <el-table-column :label="_getFeeObjName(simplifyFeeReceiptInfo.objType)" align="center">
          <template slot-scope="scope">
            {{ _getFeeObjNameText(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column prop="payObjName" :label="$t('simplifyFeeReceipt.owner')" align="center"></el-table-column>
        <el-table-column prop="feeName" :label="$t('simplifyFeeReceipt.feeItem')" align="center"></el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.timePeriod')" align="center">
          <template slot-scope="scope">
            {{ dateFormat(scope.row.startTime) }}~<br>
            <span v-if="scope.row.startTime >= scope.row.endTime">
              {{ dateFormat(scope.row.endTime) }}
            </span>
            <span v-else>
              {{ dateFormat(scope.row.endTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="amount" :label="$t('simplifyFeeReceipt.totalAmount')" align="center">
          <template slot-scope="scope">
            {{ scope.row.amount }}{{ $t('simplifyFeeReceipt.yuan') }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.feeMonth')" align="center">
          <template slot-scope="scope">
            {{ scope.row.feeMonth || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.payWay')" align="center">
          <template slot-scope="scope">
            {{ scope.row.primeRate || scope.row.payWay || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.deductionAmount')" align="center">
          <template slot-scope="scope">
            {{ scope.row.deductionAmount || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.lateAmount')" align="center">
          <template slot-scope="scope">
            {{ scope.row.lateAmount || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.acctAmount')" align="center">
          <template slot-scope="scope">
            {{ scope.row.acctAmount || '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="$t('simplifyFeeReceipt.degree')" align="center">
          <template slot-scope="scope">
            <span v-if="scope.row.startDegree || scope.row.endDegree">
              {{ scope.row.startDegree || '-' }} ~ {{ scope.row.endDegree || '-' }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('simplifyFeeReceipt.payTime')" align="center"></el-table-column>
        <el-table-column prop="receiptId" :label="$t('simplifyFeeReceipt.receiptId')" align="center"></el-table-column>
      </el-table>
      <el-pagination @current-change="handleCurrentChange" :current-page="currentPage" :page-size="pageSize"
        layout="total, prev, pager, next" :total="total" class="text-right" style="margin-top: 20px">
      </el-pagination>
    </div>
  </div>
</template>

<script>
import { getCommunityId, getDict } from '@/api/community/communityApi'
import { queryFeeReceipt, listFeePrintPage, queryContract, listOwnerShops, listOwnerParkingSpaces } from '@/api/fee/simplifyFeeReceiptApi'
import { dateFormat } from '@/utils/dateUtil'

const DEFAULT_OBJ_TYPES = [
  { statusCd: '3333', nameKey: 'room' },
  { statusCd: '4444', nameKey: 'shop' },
  { statusCd: '6666', nameKey: 'parkingSpace' },
  { statusCd: '7777', nameKey: 'contract' }
]

export default {
  name: 'SimplifyFeeReceipt',
  data() {
    return {
      DEFAULT_PAGE: 1,
      DEFAULT_ROWS: 50,
      simplifyFeeReceiptInfo: {
        feeReceipts: [],
        objType: '3333',
        objId: '',
        payObjId: '',
        roomId: '',
        shopId: '',
        psId: '',
        receiptCode: '',
        total: '',
        records: '',
        ownerId: '',
        objTypes: [],
        ownerParkingSpaces: [],
        ownerContracts: [],
        ownerShops: [],
        selectReceipts: [],
        ownerFlag: 'F',
        contractId: '',
        quan: false,
        printUrl: '/#/pages/property/printPayFee'
      },
      currentPage: 1,
      pageSize: 10,
      total: 0
    }
  },
  watch: {
    'simplifyFeeReceiptInfo.selectReceipts': {
      handler(newVal) {
        this.simplifyFeeReceiptInfo.quan = newVal.length === this.simplifyFeeReceiptInfo.feeReceipts.length
      },
      deep: true
    }
  },
  created() {
    this._loadDictData()
  },
  methods: {
    async handleSwitch(param) {
      this.clearSimplifyFeeReceiptInfo()
      this._loadDictData()
      this._listFeePrintPages()

      this.simplifyFeeReceiptInfo.ownerId = param.ownerId || ''
      this.simplifyFeeReceiptInfo.payObjId = param.ownerId || ''
      this.simplifyFeeReceiptInfo.roomId = param.roomId || ''
      this.simplifyFeeReceiptInfo.objType = '3333'

      if (!this.simplifyFeeReceiptInfo.ownerId) {
        return
      }
      await this._initFeeReceiptObjType()
      this._queryFeeReceiptMethod()
    },
    async _initFeeReceiptObjType() {
      await Promise.all([
        this._listSimplifyFeeReceiptOwnerShop({ silent: true }),
        this._listSimplifyFeeReceiptOwnerParkingSpaces({ silent: true }),
        this._listSimplifyFeeReceiptOwnerContract({ silent: true })
      ])
      const info = this.simplifyFeeReceiptInfo
      if (info.roomId) {
        info.objType = '3333'
        info.ownerFlag = 'F'
        return
      }
      if (info.ownerParkingSpaces.length > 0) {
        info.objType = '6666'
        info.ownerFlag = 'F'
        info.psId = info.ownerParkingSpaces[0].psId
        return
      }
      if (info.ownerShops.length > 0) {
        info.objType = '4444'
        info.ownerFlag = 'F'
        info.shopId = info.ownerShops[0].shopId
        return
      }
      if (info.ownerContracts.length > 0) {
        info.objType = '7777'
        info.contractId = info.ownerContracts[0].contractId
        return
      }
      info.objType = '3333'
      info.ownerFlag = 'T'
    },
    handleCurrentChange(val) {
      this._listSimplifyFeeReceipt(val, this.DEFAULT_ROWS)
    },
    async _loadDictData() {
      let dictList = []
      try {
        const data = await getDict('fee_receipt', 'obj_type')
        dictList = Array.isArray(data) ? data : []
      } catch (error) {
        console.error('Failed to load dict data:', error)
      }
      const objTypes = []
      DEFAULT_OBJ_TYPES.forEach(item => {
        const dictItem = dictList.find(dict => dict.statusCd === item.statusCd)
        objTypes.push({
          statusCd: item.statusCd,
          name: dictItem ? dictItem.name : this.$t('simplifyFeeReceipt.' + item.nameKey)
        })
      })
      dictList.forEach(dictItem => {
        const exists = objTypes.some(item => item.statusCd === dictItem.statusCd)
        if (!exists) {
          objTypes.push({ statusCd: dictItem.statusCd, name: dictItem.name })
        }
      })
      this.simplifyFeeReceiptInfo.objTypes = objTypes
    },
    async _listSimplifyFeeReceipt(page, rows) {
      this.simplifyFeeReceiptInfo.selectReceipts = []
      this.simplifyFeeReceiptInfo.quan = false

      const objType = this.simplifyFeeReceiptInfo.objType
      const objIdMap = {
        '3333': this.simplifyFeeReceiptInfo.roomId,
        '4444': this.simplifyFeeReceiptInfo.shopId,
        '6666': this.simplifyFeeReceiptInfo.psId,
        '7777': this.simplifyFeeReceiptInfo.contractId
      }

      const params = {
        page,
        row: rows,
        objType,
        objId: '',
        receiptCode: this.simplifyFeeReceiptInfo.receiptCode,
        communityId: getCommunityId()
      }

      if (this.simplifyFeeReceiptInfo.ownerFlag === 'F') {
        const objId = objIdMap[objType] || ''
        if (!objId) {
          this.simplifyFeeReceiptInfo.ownerFlag = 'T'
          return this._listSimplifyFeeReceipt(page, rows)
        }
        params.objId = objId
      } else {
        params.payObjId = this.simplifyFeeReceiptInfo.payObjId
        if (!params.payObjId) {
          this.$message.warning(this.$t('simplifyFeeReceipt.needOwner'))
          return
        }
      }

      this.currentPage = page

      try {
        const res = await queryFeeReceipt(params)
        this.simplifyFeeReceiptInfo.total = res.total
        this.simplifyFeeReceiptInfo.records = res.records
        this.simplifyFeeReceiptInfo.feeReceipts = res.data
        this.total = res.total
      } catch (error) {
        console.error('Request failed:', error)
      }
    },
    _queryFeeReceiptMethod() {
      this._listSimplifyFeeReceipt(this.DEFAULT_PAGE, this.DEFAULT_ROWS)
    },
    _printFeeReceipt(merge) {
      if (this.simplifyFeeReceiptInfo.selectReceipts.length < 1) {
        this.$message.warning(this.$t('simplifyFeeReceipt.selectReceipt'))
        return
      }
      const receiptids = this.simplifyFeeReceiptInfo.selectReceipts.join(',')
      window.open(`${this.simplifyFeeReceiptInfo.printUrl}?receiptIds=${receiptids}&apply=N&merge=${merge}`)
    },
    _printApplyFeeReceipt() {
      if (this.simplifyFeeReceiptInfo.selectReceipts.length < 1) {
        this.$message.warning(this.$t('simplifyFeeReceipt.selectReceipt'))
        return
      }
      const receiptids = this.simplifyFeeReceiptInfo.selectReceipts.join(',')
      window.open(`/#/pages/property/printPayFee?receiptIds=${receiptids}&apply=Y`)
    },
    _printFeeSmallReceipt() {
      if (this.simplifyFeeReceiptInfo.selectReceipts.length < 1) {
        this.$message.warning(this.$t('simplifyFeeReceipt.selectReceipt'))
        return
      }
      const receiptids = this.simplifyFeeReceiptInfo.selectReceipts.join(',')
      window.open(`/#/pages/property/printSmallPayFee?receiptIds=${receiptids}`)
    },
    clearSimplifyFeeReceiptInfo() {
      this.simplifyFeeReceiptInfo = {
        feeReceipts: [],
        objType: '3333',
        objId: '',
        payObjId: '',
        roomId: '',
        shopId: '',
        psId: '',
        receiptCode: '',
        total: '',
        records: '',
        ownerId: '',
        objTypes: [],
        ownerParkingSpaces: [],
        ownerContracts: [],
        ownerShops: [],
        selectReceipts: [],
        ownerFlag: 'F',
        contractId: '',
        quan: false,
        printUrl: '/#/pages/property/printPayFee'
      }
    },
    async _changeSimplifyFeeReceiptFeeTypeCd(objType) {
      this.simplifyFeeReceiptInfo.shopId = ''
      this.simplifyFeeReceiptInfo.psId = ''
      this.simplifyFeeReceiptInfo.contractId = ''
      if (!this.simplifyFeeReceiptInfo.ownerId) {
        return
      }
      if (objType === '3333') {
        if (!this.simplifyFeeReceiptInfo.roomId) {
          this.$message.warning(this.$t('simplifyFeeReceipt.noRoom'))
          this.simplifyFeeReceiptInfo.ownerFlag = 'T'
        }
      } else if (objType === '4444') {
        await this._listSimplifyFeeReceiptOwnerShop()
      } else if (objType === '6666') {
        await this._listSimplifyFeeReceiptOwnerParkingSpaces()
      } else if (objType === '7777') {
        await this._listSimplifyFeeReceiptOwnerContract()
      }
      this._queryFeeReceiptMethod()
    },
    changeSimplifyFeeReceiptObj() {
      this._queryFeeReceiptMethod()
    },
    async _listSimplifyFeeReceiptOwnerShop(opt = {}) {
      try {
        const res = await listOwnerShops({
          page: 1,
          row: 100,
          ownerId: this.simplifyFeeReceiptInfo.ownerId
        })
        const shops = res && Array.isArray(res.data) ? res.data : []
        this.simplifyFeeReceiptInfo.ownerShops = shops
        if (shops.length > 0) {
          this.simplifyFeeReceiptInfo.shopId = shops[0].shopId
          return
        }
        if (!opt.silent) {
          this.$message.warning(this.$t('simplifyFeeReceipt.noShop'))
          this.simplifyFeeReceiptInfo.ownerFlag = 'T'
        }
      } catch (error) {
        console.error('Request failed:', error)
      }
    },
    async _listSimplifyFeeReceiptOwnerParkingSpaces(opt = {}) {
      try {
        const res = await listOwnerParkingSpaces({
          page: 1,
          row: 100,
          ownerId: this.simplifyFeeReceiptInfo.ownerId,
          communityId: getCommunityId()
        })
        const list = (res && Array.isArray(res.parkingSpaces) ? res.parkingSpaces : [])
          .filter(item => item.state === 'S' || item.state === 'H')
        this.simplifyFeeReceiptInfo.ownerParkingSpaces = list
        if (list.length > 0) {
          this.simplifyFeeReceiptInfo.psId = list[0].psId
          return
        }
        if (!opt.silent) {
          this.$message.warning(this.$t('simplifyFeeReceipt.noParkingSpace'))
          this.simplifyFeeReceiptInfo.ownerFlag = 'T'
        }
      } catch (error) {
        console.error('Request failed:', error)
      }
    },
    async _listSimplifyFeeReceiptOwnerContract(opt = {}) {
      try {
        const res = await queryContract({
          page: 1,
          row: 100,
          objPersonId: this.simplifyFeeReceiptInfo.ownerId,
          communityId: getCommunityId()
        })
        const contracts = res && Array.isArray(res.data) ? res.data : []
        this.simplifyFeeReceiptInfo.ownerContracts = contracts
        if (contracts.length > 0) {
          this.simplifyFeeReceiptInfo.contractId = contracts[0].contractId
          return
        }
        if (!opt.silent) {
          this.$message.warning(this.$t('simplifyFeeReceipt.noContract'))
          this.simplifyFeeReceiptInfo.ownerFlag = 'T'
        }
      } catch (error) {
        console.error('Request failed:', error)
      }
    },
    _getParkingSpaceName(item) {
      return item.sectionName
        ? `${item.areaNum}-${item.sectionName}-${item.num}`
        : `${item.areaNum}-${item.num}`
    },
    checkAllReceipt(e) {
      const checkObj = document.querySelectorAll('.checReceiptItem')
      if (e.target.checked) {
        checkObj.forEach(item => {
          if (!item.checked) {
            this.simplifyFeeReceiptInfo.selectReceipts.push(item.value)
          }
        })
      } else {
        this.simplifyFeeReceiptInfo.selectReceipts = []
      }
    },
    _getFeeObjName(feeTypeCd) {
      if (feeTypeCd === '3333') {
        return this.$t('simplifyFeeReceipt.room')
      } else if (feeTypeCd === '4444') {
        return this.$t('simplifyFeeReceipt.shop')
      } else if (feeTypeCd === '6666') {
        return this.$t('simplifyFeeReceipt.parkingSpace')
      } else {
        return this.$t('simplifyFeeReceipt.contract')
      }
    },
    _getFeeObjNameText(receipt) {
      if (receipt.objType === '6666' && receipt.psNum && receipt.psNum !== '-') {
        return receipt.psNum
      }
      return receipt.objName
    },
    async _listFeePrintPages() {
      try {
        const res = await listFeePrintPage({
          page: 1,
          row: 1,
          state: 'T',
          communityId: getCommunityId()
        })
        const feePrintPages = res.data
        if (feePrintPages && feePrintPages.length > 0) {
          this.simplifyFeeReceiptInfo.printUrl = feePrintPages[0].url
        }
      } catch (error) {
        console.error('Request failed:', error)
      }
    },
    open(params) {
      this.handleSwitch(params)
    },
    dateFormat(date) {
      return dateFormat(date)
    },
    handleSelectionChange(val) {
      this.simplifyFeeReceiptInfo.selectReceipts = val.map(item => item.receiptId)
    }
  }
}
</script>

<style scoped>
.margin-top-lg {
  margin-top: 20px;
}

.padding-right-xs {
  padding-right: 5px;
}

.padding-left-xl {
  padding-left: 20px;
}

.text-right {
  text-align: right;
}

.padding-right-lg {
  padding-right: 20px;
}
</style>